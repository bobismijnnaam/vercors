package vct.col.rewrite

import hre.ast.MessageOrigin
import vct.col.ast.`type`.{ASTReserved, PrimitiveSort, Type}
import vct.col.ast.expr.{OperatorExpression, StandardOperator}
import vct.col.ast.generic.ASTNode
import vct.col.ast.stmt.decl.{DeclarationStatement, ProgramUnit}
import vct.col.ast.util.ContractBuilder

import scala.collection.mutable

object RewriteSequenceFunctions {
  val getRemoveName: mutable.Map[Type, String] = mutable.Map()

  val namesUsed: mutable.Set[String] = mutable.Set()

  def getUniqueName(str: String): String = {
    var result = str.replaceAll("[^a-zA-Z0-9$_']", "_")
    while (namesUsed contains result) {
      result += "$"
    }
    namesUsed += result
    result
  }

  def getRemoveFunction(t: Type): String = {
    getRemoveName getOrElseUpdate(t, getUniqueName("remove_by_index_" + t.toString))
  }
}

class RewriteSequenceFunctions(source: ProgramUnit) extends AbstractRewriter(source) {
  override def rewriteAll(): ProgramUnit = {
    val res = super.rewriteAll()

    create.enter()
    create.setOrigin(new MessageOrigin("Sequence Function: Remove"))
    for ((t, name) <- RewriteSequenceFunctions.getRemoveName) {
      res.add(removeFromSequenceByIndex(t, name))
    }
    create.leave()

    res
  }

  override def visit(operator: OperatorExpression): Unit = {
    operator.operator match {
      case StandardOperator.RemoveAt =>
        val sequenceType = operator.arg(0).getType
        result = create.invokation(null, null, RewriteSequenceFunctions.getRemoveFunction(sequenceType), rewrite(operator.args.toArray):_*)
      case _ =>
        super.visit(operator)
    }
  }


  def removeFromSequenceByIndex(sequenceType: Type, functionName: String): ASTNode = {
    val contract = new ContractBuilder
    val result = create.reserved_name(ASTReserved.Result, sequenceType)

    val sequenceArgumentName = "seq0"
    val indexArgumentName = "i0"

    val sequenceArgument = new DeclarationStatement(sequenceArgumentName, sequenceType)
    val indexArgument = new DeclarationStatement(indexArgumentName, create.primitive_type(PrimitiveSort.Integer))

    contract.requires(validIndex(sequenceArgumentName, indexArgumentName))
    contract.ensures(
      eq(
        size(result),
        minus(
          size(name(sequenceArgumentName)),
          create.constant(1)
        )
      )
    )

    val forAllIndex = new DeclarationStatement("j0", create.primitive_type(PrimitiveSort.Integer))
    val indexNode = name("j0")

    contract.ensures(
      create.forall(
        valueInRange(indexNode, constant(0), name(indexArgumentName)),
        eq(
          get(result, indexNode),
          get(name(sequenceArgumentName), indexNode)),
        forAllIndex
      )
    )

    contract.ensures(
      create.forall(
        valueInRange(indexNode, name(indexArgumentName), size(result)),
        eq(
          get(result, indexNode),
          get(name(sequenceArgumentName), plus(indexNode, constant(1)))),
        forAllIndex
      )
    )

    val functionArguments = List(sequenceArgument, indexArgument)
    val declaration = create.function_decl(sequenceType, contract.getContract, functionName, functionArguments.toArray, null)
    declaration.setStatic(true)
    declaration
  }

  def validIndex(sequenceName: String, indexName: String, inclusive: Boolean=false): ASTNode = {
    valueInRange(name(indexName), constant(0), size(name(sequenceName)), inclusive)
  }


  def valueInRange(value: ASTNode, lowerbound: ASTNode, upperbound: ASTNode, inclusive: Boolean=false): ASTNode = {
    val left = lte(lowerbound, value)
    val right = if (inclusive) lte(value, upperbound) else less(value, upperbound)

    and(left, right)
  }
}
