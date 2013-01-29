// -*- tab-width:2 ; indent-tabs-mode:nil -*-
package vct.col.ast;

import java.util.*;

import vct.col.ast.PrimitiveType.Sort;
import static hre.System.*;

/**
 * Subclass of ASTNode meant for holding all type expressions.
 * 
 * Types need to be both manipulated in special ways (hence this class)
 * and treated as any AST node (hence we extend ASTNode).
 *  
 * @author sccblom
 *
 */
public abstract  class Type extends ASTNode {
  
  protected ASTNode args[];
  
  public Type(ASTNode ... args){
    this.args=Arrays.copyOf(args,args.length);
  }

  public ASTNode[] getArgs(){
    return args;
  }
  
  public ASTNode getArg(int i){
    return args[i];
  }
  
  public int getArgCount(){
    return args.length;
  }

  public boolean isBoolean() {
    return false;
  }

  public abstract boolean supertypeof(ProgramUnit context, Type t);

  public boolean isInteger() {
    return false;
  }

  public boolean isDouble() {
    return false;
  }

  public boolean isVoid() {
    return false;
  }

  public boolean isPrimitive(Sort fraction) {
    return false;
  }

  public ASTNode zero() {
    Abort("zero unimplemented for %s",getClass());
    return null;
  }

  public boolean comparableWith(ProgramUnit context, Type t2){
    if(equals(t2)) return true;
    if(this.supertypeof(context,t2)) return true;
    if(t2.supertypeof(context,this)) return true;
    return false;
  }

  public boolean isNull() {
    return false;
  }

}

