package com.timepath.compiler.backend.q1vm.impl

import com.timepath.compiler.Value
import com.timepath.compiler.ast.FunctionExpression
import com.timepath.compiler.backend.q1vm.CompilerOptions
import com.timepath.compiler.backend.q1vm.Pointer
import com.timepath.compiler.backend.q1vm.types.bool_t
import com.timepath.compiler.backend.q1vm.types.string_t
import com.timepath.compiler.ir.Allocator
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import com.timepath.with
import java.util.Deque

class AllocatorImpl(val opts: CompilerOptions) : Allocator {

    /**
     * Maps names to pointers
     */
    inner class AllocationMapImpl : Allocator.AllocationMap {

        inner data class EntryImpl(
                override val ref: Instruction.Ref,
                override val type: Type,
                override val value: Value?,
                /** Privately set */
                override var name: String
        ) : Allocator.AllocationMap.Entry {

            val separator = '|'
            val tags = name.split(separator).toMutableSet()

            fun tag(tag: String) {
                if (tag !in tags) {
                    tags.add(tag)
                    name += separator + tag
                }
            }
        }

        private val free = linkedListOf<EntryImpl>()
        private val pool: MutableList<EntryImpl> = linkedListOf()
        override val all: List<EntryImpl> = pool
        private val refs: MutableMap<Instruction.Ref, EntryImpl> = linkedMapOf()
        private val values: MutableMap<Value?, EntryImpl> = linkedMapOf()
        private val names: MutableMap<String, Allocator.AllocationMap.Entry> = linkedMapOf()

        /**
         * Considered inside a function at this depth
         */
        val insideFunc: Boolean
            get() = scope.size() >= 3

        fun allocate(id: String, ref: Instruction.Ref, type: Type, value: Value? = null): EntryImpl {
            // only consider uninitialized local references for now
            if (opts.scopeFolding && insideFunc && !free.isEmpty() && value == null) {
                val e = free.pop()
                // add the entry to the current scope so it can be used again later on exit
                scope.peek().add(e)
                e.tag(id)
                return e
            }
            val e = EntryImpl(ref, type, value, id)
            pool.add(e)
            if (scope.isNotEmpty() && value == null) {
                scope.peek().add(e)
            }
            refs[e.ref] = e
            values[e.value] = e
            names[e.name] = e
            return e
        }

        override fun contains(ref: Instruction.Ref) = ref in refs
        override fun get(ref: Instruction.Ref): EntryImpl? = refs[ref]

        override fun contains(value: Value) = value in values
        override fun get(value: Value): EntryImpl? = values[value]

        override fun contains(name: String) = name in names
        override fun get(name: String): Allocator.AllocationMap.Entry? = names[name]
        override fun set(name: String, value: Allocator.AllocationMap.Entry) {
            names[name] = value
        }

        override fun size() = pool.size()

        private val scope: Deque<MutableList<EntryImpl>> = linkedListOf()

        fun push() {
            scope.push(linkedListOf<EntryImpl>())
        }

        /**
         * Push all previously used entries to the head of the queue
         */
        fun pop() {
            val wasInside = insideFunc
            free.addAll(0, scope.pop())
            if (!opts.overlapLocals && wasInside != insideFunc) {
                free.clear()
            }
        }
    }

    private val functions = AllocationMapImpl()
    override val references = AllocationMapImpl()
    override val constants = AllocationMapImpl()
    override val strings = AllocationMapImpl()

    init {
        allocateString("")
    }

    data class Scope(override val id: Any, override val lookup: MutableMap<String, Allocator.AllocationMap.Entry> = linkedMapOf()) : Allocator.Scope

    override val scope: Deque<Allocator.Scope> = linkedListOf()

    val insideFunc: Boolean
        get() = scope.size() > 4

    override fun push(id: Any) {
        if (id is FunctionExpression) {
            localCounter = 0
        }
        scope.push(Scope(id))
        references.push()
    }

    override fun pop() {
        scope.pop()
        references.pop()
    }

    init {
        // FIXME: redundant
        push("<builtin>")
        allocateConstant(Value(0), bool_t, "false")
        allocateConstant(Value(1), bool_t, "true")
        push("<global>")
    }

    override fun contains(name: String) = scope.firstOrNull { name in it.lookup } != null
    override fun get(name: String): Allocator.AllocationMap.Entry? {
        scope.forEach {
            it.lookup[name]?.let {
                return it
            }
        }
        return null
    }

    /** Return the index to a constant referring to this function */
    override fun allocateFunction(id: String, type: function_t): Allocator.AllocationMap.Entry {
        val function = functions.allocate(id, Instruction.Ref(1 + functions.size(), Instruction.Ref.Scope.Global), type)
        // Allocate a constant so the function can be called
        return allocateConstant(Value(Pointer(function.ref.i)), type, id) with {
            scope.peek().lookup[id] = this
        }
    }

    private var refCounter = 0
    private var localCounter = 0
    private var globalCounter = 0
    /** Reserve space for this variable and add its name to the current scope */
    override fun allocateReference(id: String?, type: Type, value: Value?, scope: Instruction.Ref.Scope): Allocator.AllocationMap.Entry {
        val name = id ?: "var${refCounter++}"
        val i = when (scope) {
            Instruction.Ref.Scope.Local ->
                localCounter++
            Instruction.Ref.Scope.Global ->
                opts.userStorageStart + globalCounter++
        }
        val entry = references.allocate(name, Instruction.Ref(i, scope), type, value)
        this.scope.peek().lookup[name] = entry
        return entry
    }

    /** Reserve space for this constant */
    override fun allocateConstant(value: Value, type: Type, id: String?): Allocator.AllocationMap.Entry {
        val name = id ?: when (value.any) {
            is Int -> "${value.any}i"
            is Float -> "${value.any}f"
            else -> "$value"
        }
        if (value.any is String) {
            val str = allocateString(value.any)
            return allocateConstant(Value(Pointer(str.ref.i)), string_t, "str(${str.name})")
        }
        if (opts.mergeConstants) {
            constants[value]?.let {
                constants[name] = it
                it.tag(name)
                return it
            }
        }
        val i = opts.userStorageStart + globalCounter++
        val entry = constants.allocate(name, Instruction.Ref(i, Instruction.Ref.Scope.Global), type, value)
        this.scope.peek().lookup[name] = entry
        return entry
    }

    private var stringPtr = 0
    override fun allocateString(s: String): Allocator.AllocationMap.Entry {
        strings[s]?.let { return it } // merge strings
        val i = stringPtr
        stringPtr += s.length() + 1 // FIXME: count encoded bytes
        return strings.allocate(s, Instruction.Ref(i, Instruction.Ref.Scope.Global), string_t)
    }

    override fun toString(): String {
        val constants = constants.all.joinToString("\n")
        val functions = functions.all.joinToString("\n")
        val strings = strings.all.joinToString("\n")
        val references = references.all.joinToString("\n")
        return "constants:\n" + constants +
                "\n\n" +
                "functions:\n" + functions +
                "\n\n" +
                "strings:\n" + strings +
                "\n\n" +
                "references:\n" + references
    }

}
