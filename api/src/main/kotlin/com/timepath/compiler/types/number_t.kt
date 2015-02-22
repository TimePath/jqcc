package com.timepath.compiler.types

import kotlin.properties.Delegates
import com.timepath.q1vm.Instruction
import com.timepath.compiler.ast.BinaryExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.gen.generate
import com.timepath.compiler.ast.MethodCallExpression
import com.timepath.compiler.ast.ReferenceExpression
import com.timepath.compiler.ast.UnaryExpression
import com.timepath.compiler.gen.IR
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.api.CompileState

open class number_t : Type {
    override fun handle(op: Operation) = ops[op]
    private val ops by Delegates.lazy {
        mapOf(
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT),
                Operation("+", this, this) to DefaultHandler(this, Instruction.ADD_FLOAT),
                Operation("-", this, this) to DefaultHandler(this, Instruction.SUB_FLOAT),
                Operation("&", this) to OperationHandler(this) { gen, self, _ ->
                    BinaryExpression.Divide(self, ConstantExpression(1)).generate(gen)
                },
                Operation("*", this) to OperationHandler(this) { gen, self, _ ->
                    BinaryExpression.Multiply(self, ConstantExpression(1)).generate(gen)
                },
                Operation("+", this) to OperationHandler(this) { gen, self, _ ->
                    self.generate(gen)
                },
                Operation("-", this) to OperationHandler(this) { gen, self, _ ->
                    BinaryExpression.Subtract(ConstantExpression(0f), self).generate(gen)
                },

                Operation("*", this, this) to DefaultHandler(this, Instruction.MUL_FLOAT),
                Operation("*", this, vector_t) to DefaultHandler(vector_t, Instruction.MUL_FLOAT_VEC),
                Operation("/", this, this) to DefaultHandler(this, Instruction.DIV_FLOAT),
                Operation("%", this, this) to OperationHandler(this) { gen, left, right ->
                    MethodCallExpression(ReferenceExpression("__builtin_mod"), listOf(left, right!!)).generate(gen)
                },

                // pre
                Operation("++", this) to OperationHandler(this) { gen, self, _ ->
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1f))
                    BinaryExpression.Assign(self, BinaryExpression.Add(self, one)).generate(gen)
                },
                // post
                Operation("++", this, this) to OperationHandler(this) { gen, self, _ ->
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1f))
                    with(linkedListOf<IR>()) {
                        val add = BinaryExpression.Add(self, one)
                        val assign = BinaryExpression.Assign(self, add)
                        // FIXME
                        val sub = BinaryExpression.Subtract(assign, one)
                        addAll(sub.generate(gen))
                        this
                    }
                },
                // pre
                Operation("--", this) to OperationHandler(this) { gen, self, _ ->
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1f))
                    BinaryExpression.Assign(self, BinaryExpression.Subtract(self, one)).generate(gen)
                },
                // post
                Operation("--", this, this) to OperationHandler(this) { gen, self, _ ->
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1f))
                    with(linkedListOf<IR>()) {
                        val sub = BinaryExpression.Subtract(self, one)
                        val assign = BinaryExpression.Assign(self, sub)
                        // FIXME
                        val add = BinaryExpression.Add(assign, one)
                        addAll(add.generate(gen))
                        this
                    }
                },

                Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FLOAT),
                Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FLOAT),
                Operation(">", this, this) to DefaultHandler(bool_t, Instruction.GT),
                Operation("<", this, this) to DefaultHandler(bool_t, Instruction.LT),
                Operation(">=", this, this) to DefaultHandler(bool_t, Instruction.GE),
                Operation("<=", this, this) to DefaultHandler(bool_t, Instruction.LE),

                Operation("!", this) to OperationHandler(bool_t) { gen, self, _ ->
                    BinaryExpression.Eq(ConstantExpression(0f), self).generate(gen)
                },
                Operation("~", this) to OperationHandler(this) { gen, self, _ ->
                    BinaryExpression.Subtract(ConstantExpression(-1f), self).generate(gen)
                },
                Operation("&", this, this) to DefaultHandler(this, Instruction.BITAND),
                Operation("|", this, this) to DefaultHandler(this, Instruction.BITOR),
                Operation("^", this, this) to OperationHandler(this) { gen, left, right ->
                    MethodCallExpression(ReferenceExpression("__builtin_xor"), listOf(left, right!!)).generate(gen)
                },
                // TODO
                Operation("<<", this, this) to DefaultHandler(int_t, Instruction.BITOR),
                // TODO
                Operation(">>", this, this) to DefaultHandler(int_t, Instruction.BITOR),
                // TODO
                Operation("**", this, this) to DefaultHandler(float_t, Instruction.BITOR),
                Operation("+=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Add(left, right)
                },
                Operation("-=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Subtract(left, right)
                },
                Operation("*=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Multiply(left, right)
                },
                Operation("/=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Divide(left, right)
                },
                Operation("%=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Modulo(left, right)
                },
                Operation("&=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.And(left, right)
                },
                Operation("|=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Or(left, right)
                },
                Operation("^=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.ExclusiveOr(left, right)
                },
                Operation("<<=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Lsh(left, right)
                },
                Operation(">>=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Rsh(left, right)
                }
        )
    }

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}

object int_t : number_t() {
    override fun handle(op: Operation): OperationHandler? {
        super.handle(op)?.let { return it }
        if (op.right == float_t) {
            return float_t.handle(op.copy(left = float_t))
        }
        if (op.right == bool_t) {
            return super.handle(op.copy(right = int_t))
        }
        return null
    }
}

object float_t : number_t() {
    override fun handle(op: Operation): OperationHandler? {
        super.handle(op)?.let { return it }
        if (op.right == int_t || op.right == bool_t) {
            return super.handle(op.copy(right = float_t))
        }
        return null
    }
}