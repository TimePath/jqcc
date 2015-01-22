package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class LoopExpression(val predicate: Expression,
           body: Expression,
           val checkBefore: Boolean = true,
           val initializer: List<Expression>? = null,
           val update: List<Expression>? = null,
           ctx: ParserRuleContext? = null) : Expression(ctx) {
    {
        add(body)
    }

    override fun generate(ctx: Generator): List<IR> {
        val genInit = initializer?.flatMap { it.doGenerate(ctx) }

        val genPred = predicate.doGenerate(ctx)
        val predCount = genPred.count { it.real }

        val genBody = children.flatMap { it.doGenerate(ctx) }
        val bodyCount = genBody.count { it.real }

        val genUpdate = update?.flatMap { it.doGenerate(ctx) }
        val updateCount = genUpdate?.count { it.real } ?: 0

        val totalCount = bodyCount + updateCount + predCount

        val ret = linkedListOf<IR>()
        if (genInit != null) {
            ret.addAll(genInit)
        }
        ret.addAll(genPred)
        if (checkBefore) {
            ret.add(IR(Instruction.IFNOT, array(genPred.last().ret,
                    totalCount + /* the last if */ 1 + /* the next instruction */ 1, 0)))
        }
        ret.addAll(genBody)
        if (genUpdate != null) {
            ret.addAll(genUpdate)
        }
        ret.addAll(genPred)
        ret.add(IR(Instruction.IF, array(genPred.last().ret, -totalCount, 0)))

        // break/continue; jump to end
        genBody.filter { it.real }.forEachIndexed {(i, IR) ->
            if (IR.instr == Instruction.GOTO && IR.args[0] == 0) {
                val after = (bodyCount - 1) - i
                IR.args[0] = after + 1 + when (IR.args[1]) {
                // break
                    1 -> updateCount + predCount + /* if */ 1
                    else -> 0
                }
                IR.args[1] = 0
            }
        }
        return ret
    }
}