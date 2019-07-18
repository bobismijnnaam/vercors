

package vct.antlr4.parser;

import static hre.lang.System.*;

import java.io.*;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.ParseTree;

import vct.antlr4.generated.*;
import vct.col.ast.stmt.decl.ProgramUnit;
import vct.col.rewrite.AnnotationInterpreter;
import vct.col.rewrite.FilterSpecIgnore;
import vct.col.rewrite.FlattenVariableDeclarations;
import vct.col.syntax.JavaDialect;
import vct.col.syntax.JavaSyntax;

/**
 * Parse specified code and convert the contents to COL. 
 */
public class ColJavaParser extends vct.col.util.Parser {

  public final int version;
  public final boolean twopass;
  
  public ColJavaParser(int version,boolean twopass){
    this.version=version;
    this.twopass=twopass;
  }
  
  @Override
  public ProgramUnit parse(String fileName, InputStream inputStream) throws IOException {
    TimeKeeper tk=new TimeKeeper();
    ANTLRInputStream input = new ANTLRInputStream(inputStream);

    ProgramUnit pu;
    ErrorCounter ec=new ErrorCounter(fileName);

    switch(version){
    case 7:
      if (twopass){
        Lexer lexer = new Java7JMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java7JMLParser parser = new Java7JMLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ec);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ec);
        ParseTree tree = parser.compilationUnit();
        ec.report();
        Progress("first parsing pass took %dms",tk.show());

        pu=Java7JMLtoCol.convert_tree(tree,fileName,tokens,parser);
        Progress("AST conversion took %dms",tk.show());
        Debug("program after Java parsing:%n%s",pu);

        pu=new CommentRewriter(pu,new Java7JMLCommentParser(ec)).rewriteAll();
        Progress("Specification parsing took %dms",tk.show());
        ec.report();
        Debug("program after specification parsing:%n%s",pu);
        break;
      } else {
        Lexer lexer = new Java7JMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java7JMLParser parser = new Java7JMLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ec);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ec);

        ParseTree tree = parser.compilationUnit();
        ec.report();
        Progress("first parsing pass took %dms",tk.show());

        pu=Java7JMLtoCol.convert_tree(tree,fileName,tokens,parser);
        Progress("AST conversion took %dms",tk.show());
        Debug("program after Java parsing:%n%s",pu);
        break;
      }
    case 8:{
      Lexer lexer = new Java8JMLLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      Java8JMLParser parser = new Java8JMLParser(tokens);
      parser.removeErrorListeners();
      parser.addErrorListener(ec);
      lexer.removeErrorListeners();
      lexer.addErrorListener(ec);
      ParseTree tree = parser.compilationUnit();
      ec.report();
      Progress("first parsing pass took %dms",tk.show());

      pu=Java8JMLtoCol.convert_tree(tree,fileName,tokens,parser);
      Progress("AST conversion took %dms",tk.show());
      Debug("program after Java parsing:%n%s",pu);

      if(twopass){
        pu=new CommentRewriter(pu,new Java8JMLCommentParser(ec)).rewriteAll();
        Progress("Specification parsing took %dms",tk.show());
        ec.report();
        Debug("program after specification parsing:%n%s",pu);
      }
      break;
    }
    default:
      throw new Error("bad java version: "+version);
    }
    pu=new FlattenVariableDeclarations(pu).rewriteAll();
    Progress("Flattening variables took %dms",tk.show());
    Debug("program after flattening variables:%n%s",pu);

    pu=new SpecificationCollector(JavaSyntax.getJava(JavaDialect.JavaVerCors),pu).rewriteAll();
    Progress("Shuffling specifications took %dms",tk.show());
    Debug("program after collecting specs:%n%s",pu);

    pu=new JavaPostProcessor(pu).rewriteAll();
    Progress("post processing took %dms",tk.show());

    pu=new AnnotationInterpreter(pu).rewriteAll();
    Progress("interpreting annotations took %dms",tk.show());

    pu=new FilterSpecIgnore(pu).rewriteAll();
    Progress("filtering spec_ignore took %dms",tk.show());

    return pu;
  }
}

