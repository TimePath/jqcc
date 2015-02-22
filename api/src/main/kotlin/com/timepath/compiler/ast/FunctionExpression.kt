package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.compiler.types.function_t

/**
 * Replaced with a number during compilation
 */
class FunctionExpression(val id: String? = null,
                         val signature: function_t,
                         val params: List<Expression>? = null,
                         val vararg: Expression? = null,
                         add: List<Expression>? = null,
                         val builtin: Int? = null,
                         override val ctx: ParserRuleContext? = null) : Expression() {

    {
        if (add != null) {
            addAll(add)
        }
    }

}