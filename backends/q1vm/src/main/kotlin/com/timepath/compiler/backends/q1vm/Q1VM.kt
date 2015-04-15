package com.timepath.compiler.backends.q1vm

import com.timepath.compiler.Pointer
import com.timepath.compiler.Vector
import com.timepath.compiler.api.Backend
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backends.q1vm.gen.Allocator
import com.timepath.compiler.backends.q1vm.gen.Generator
import com.timepath.compiler.backends.q1vm.gen.IR
import com.timepath.compiler.backends.q1vm.gen.generate
import com.timepath.compiler.types.*
import com.timepath.q1vm.Instruction

val CompileState.allocator: Allocator get() = (this as Q1VM.State).allocator
val CompileState.opts: CompilerOptions get() = (this as Q1VM.State).opts

class Q1VM(opts: CompilerOptions = CompilerOptions()) : Backend {

    override fun generate(roots: List<Expression>) = state.gen.generate(roots)

    inner class State(val opts: CompilerOptions = CompilerOptions()) : CompileState() {
        val allocator = Allocator(opts)
        val gen = Generator(this)

        init {
            Types.types[javaClass<java.lang.Boolean>()] = bool_t
            Types.types[javaClass<java.lang.Character>()] = int_t
            Types.types[javaClass<java.lang.Integer>()] = int_t
            Types.types[javaClass<java.lang.Float>()] = float_t
            Types.types[javaClass<String>()] = string_t
            Types.types[javaClass<Vector>()] = vector_t
            Types.types[javaClass<Pointer>()] = int_t

            Types.handlers.add { it ->
                if (it.op != ",") null else
                    OperationHandler(it.right!!) { gen, left, right ->
                        with(linkedListOf<IR>()) {
                            addAll(left.generate(gen))
                            addAll(right!!.generate(gen))
                            this
                        }
                    }
            }
            Types.handlers.add { it.left.handle(it) }
            Types.handlers.add { void_t.handle(it.copy(left = void_t, right = void_t)) }
            function_t.handlers.add {
                val ops = mapOf(
                        Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FUNC),
                        Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FUNC),
                        Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FUNC),
                        Operation("!", this) to DefaultUnaryHandler(bool_t, Instruction.NOT_FUNC),
                        Operation("&", this) to OperationHandler(float_t) { gen, self, _ ->
                            BinaryExpression.Divide(MemoryReference(self.generate(gen).last().ret, float_t), ConstantExpression(Pointer(1))).generate(gen)
                        }
                )
                ops[it]
            }

            types["void"] = void_t
            types["float"] = float_t
            types["vector"] = vector_t
            types["string"] = string_t
            types["entity"] = entity_t
            types["int"] = int_t
            types["bool"] = bool_t
        }
    }

    override val state = State(opts)

    init {
        state.symbols.push("<builtin>")
        state.symbols.declare(DeclarationExpression("VA_ARGS", function_t(void_t, listOf(int_t))))
        state.symbols.declare(DeclarationExpression("false", bool_t, ConstantExpression(0)))
        state.symbols.declare(DeclarationExpression("true", bool_t, ConstantExpression(1)))
        // TODO: not really a function
        state.symbols.declare(DeclarationExpression("_", function_t(string_t, listOf(string_t))))
        state.symbols.push("<global>")
    }

}
