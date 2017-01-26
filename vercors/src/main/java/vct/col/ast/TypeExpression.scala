package vct.col.ast

import vct.col.util.VisitorHelper

class TypeExpression(val operator:TypeOperator, val types:Array[Type]) extends Type with VisitorHelper {
  def firstType = types.head
  def nrOfTypes = types.length
  def getType(i:Int) = types.apply(i)
  
  //@deprecated("will soon be removed", "next release")
  //def op = operator
  
  override def isNumeric = operator match {
    case TypeOperator.Local | TypeOperator.Global | TypeOperator.Long => types.head.isNumeric()
    case _ => false
  }
  
  override def supertypeof(context:ProgramUnit, t:Type) = false
  override def accept_simple[T,A](m:ASTMapping1[T,A], arg:A) = m.map(this, arg)
  override def accept_simple[T](v:ASTVisitor[T]) = handle_standard(() => v.visit(this))
  override def accept_simple[T](m:ASTMapping[T]) = handle_standard(() => m.map(this))
  override def accept_simple[T](m:TypeMapping[T]) = handle_standard(() => m.map(this))
}