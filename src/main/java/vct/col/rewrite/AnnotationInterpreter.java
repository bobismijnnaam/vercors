package vct.col.rewrite;

import hre.lang.HREError;

import java.util.ArrayList;

import vct.col.ast.stmt.decl.*;
import vct.col.ast.generic.ASTNode;
import vct.col.ast.type.ASTReserved;
import vct.col.ast.util.ContractBuilder;
import vct.col.ast.expr.NameExpression;
import vct.col.ast.type.Type;

public class AnnotationInterpreter extends AbstractRewriter {

  public AnnotationInterpreter(ProgramUnit source) {
    super(source);
  }

  @Override
  public void visit(Method m){
    Method.Kind kind=m.kind;
    ArrayList<ASTNode> ann=new ArrayList<ASTNode>();
    Type returns=rewrite(m.getReturnType());
    ContractBuilder cb=new ContractBuilder();
    rewrite(m.getContract(),cb);
    Contract contract = cb.getContract();
    if (contract != null && contract.getOrigin() == null) {
      contract.setOrigin(m.getContract().getOrigin());
    }
    String name=m.getName();
    DeclarationStatement args[]=rewrite(m.getArgs());
    ASTNode body=rewrite(m.getBody());
    boolean varArgs=m.usesVarArgs();
    if (m.annotated()) for(ASTNode a:m.annotations()){
      if (a==null){
        Debug("ignoring null annotation");
        continue;
      }
      if (a.isReserved(ASTReserved.Pure)){
        Debug("found pure annotation");
        kind=Method.Kind.Pure;
      } else {
        ann.add(rewrite(a));
      }
    }

    ArrayList<Type> new_throws_types = new ArrayList<>();
    for (Type throws_type : m.getThrowsTypes()) {
      Type new_throws_type = rewrite(throws_type);
      if (new_throws_type != null) {
        new_throws_types.add(new_throws_type);
      }
    }
    Type[] new_throws_types_arr = new_throws_types.toArray(new Type[0]);

    Method res=create.method_kind(kind, returns, contract, name, args, varArgs, body, new_throws_types_arr);
    if (m.annotated()) {
      res.attach();
      for (ASTNode a : ann){
        if (a.isReserved(null)){
          switch(((NameExpression)a).reserved()){
          case Synchronized:
            break;
          case Final:
            res.setFlag(ASTFlags.FINAL, true);
            break;
          case Inline:
            res.setFlag(ASTFlags.INLINE, true);
            break;
          case ThreadLocal:
            res.setFlag(ASTFlags.THREAD_LOCAL, true);
            break;
          case Public:
          case Private:
          case Protected:
          case Abstract:
            break;
          default:
            throw new HREError("cannot set flag for reserved annotation %s",a);
          }
        } else {
          res.attach(a);
        }
      }
    }
    result=res;
  }
}
