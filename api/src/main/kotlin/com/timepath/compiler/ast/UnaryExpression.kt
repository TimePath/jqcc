package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import org.antlr.v4.runtime.ParserRuleContext as PRC

public fun Expression.inv(): UnaryExpression.BitNot = UnaryExpression.BitNot(this, null)
public fun Expression.not(): UnaryExpression.Not = UnaryExpression.Not(this, null)
public fun Expression.minus(): UnaryExpression.Minus = UnaryExpression.Minus(this, null)
public fun Expression.plus(): UnaryExpression.Plus = UnaryExpression.Plus(this, null)
public fun Expression.to(type: Type): UnaryExpression.Cast = UnaryExpression.Cast(type, this, null)
public fun Expression.address(): UnaryExpression.Address = UnaryExpression.Address(this, null)
public fun Expression.deref(): UnaryExpression.Dereference = UnaryExpression.Dereference(this, null)

public abstract class UnaryExpression protected constructor(val op: String, val operand: Expression, override val ctx: PRC?) : Expression() {

    init {
        add(operand)
    }

    override fun toString(): String = "$op($operand)"

    class Cast(val type: Type, operand: Expression, ctx: PRC?) : UnaryExpression("($type)", operand, ctx) {
        override val simpleName = "Cast"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

        override fun toString(): String = "(($type) $operand)"
    }

    public abstract class Post protected constructor(op: String, operand: Expression, ctx: PRC?) : UnaryExpression(op, operand, ctx) {

        override fun toString(): String = "($operand $op)"
    }

    class PostIncrement(operand: Expression, ctx: PRC?) : UnaryExpression.Post("++", operand, ctx) {
        override val simpleName = "PostIncrement"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class PostDecrement(operand: Expression, ctx: PRC?) : UnaryExpression.Post("--", operand, ctx) {
        override val simpleName = "PostDecrement"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class PreIncrement(operand: Expression, ctx: PRC?) : UnaryExpression("++", operand, ctx) {
        override val simpleName = "PreIncrement"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class PreDecrement(operand: Expression, ctx: PRC?) : UnaryExpression("--", operand, ctx) {
        override val simpleName = "PreDecrement"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Address(operand: Expression, ctx: PRC?) : UnaryExpression("&", operand, ctx) {
        override val simpleName = "Address"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Dereference(operand: Expression, ctx: PRC?) : UnaryExpression("*", operand, ctx) {
        override val simpleName = "Dereference"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Plus(operand: Expression, ctx: PRC?) : UnaryExpression("+", operand, ctx) {
        override val simpleName = "Plus"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Minus(operand: Expression, ctx: PRC?) : UnaryExpression("-", operand, ctx) {
        override val simpleName = "Minus"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class BitNot(operand: Expression, ctx: PRC?) : UnaryExpression("~", operand, ctx) {
        override val simpleName = "BitNot"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }

    class Not(operand: Expression, ctx: PRC?) : UnaryExpression("!", operand, ctx) {
        override val simpleName = "Not"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
    }
}
