package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.compiler.gen.type
import com.timepath.compiler.types.Operation

abstract class UnaryExpression(val op: String, val operand: Expression, override val ctx: ParserRuleContext? = null) : Expression() {

    {
        add(operand)
    }

    override fun toString(): String = "$op($operand)"

    class Cast(val type: Type, val operand: Expression, override val ctx: ParserRuleContext? = null) : Expression() {
        override fun toString(): String = "(($type) $operand)"
    }

    abstract class Post(op: String, operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression(op, operand, ctx) {
        override fun toString(): String = "($operand $op)"
    }

    class PostIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post("++", operand, ctx)

    class PostDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression.Post("--", operand, ctx)

    class PreIncrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("++", operand, ctx)

    class PreDecrement(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("--", operand, ctx)

    class Address(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("&", operand, ctx)

    class Dereference(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("*", operand, ctx)

    class Plus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("+", operand, ctx)

    class Minus(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("-", operand, ctx)

    class BitNot(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("~", operand, ctx)

    class Not(operand: Expression, ctx: ParserRuleContext? = null) : UnaryExpression("!", operand, ctx)
}
