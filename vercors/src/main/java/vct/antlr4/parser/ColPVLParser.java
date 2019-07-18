package vct.antlr4.parser;

import static hre.lang.System.*;

import java.io.*;

import hre.lang.HREExitException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import vct.antlr4.generated.PVFullLexer;
import vct.antlr4.generated.PVFullParser;
import vct.col.ast.stmt.decl.ProgramUnit;
import vct.col.rewrite.FlattenVariableDeclarations;
import vct.col.syntax.PVLSyntax;

/**
 * Parse specified code and convert the contents to COL. 
 */
public class ColPVLParser extends vct.col.util.Parser {

  @Override
  public ProgramUnit parse(String fileName, InputStream inputStream) throws IOException {
    TimeKeeper tk = new TimeKeeper();
    ErrorCounter ec = new ErrorCounter(fileName);

    ANTLRInputStream input = new ANTLRInputStream(inputStream);
    PVFullLexer lexer = new PVFullLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(ec);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    PVFullParser parser = new PVFullParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(ec);
    ParseTree tree = parser.program();
    Progress("parsing pass took %dms", tk.show());
    ec.report();
    Debug("parser got: %s", tree.toStringTree(parser));

    ProgramUnit pu = PVLtoCOL.convert(tree, fileName, tokens, parser);
    Progress("AST conversion pass took %dms", tk.show());

    pu = new FlattenVariableDeclarations(pu).rewriteAll();
    Progress("Variable pass took %dms", tk.show());

    pu = new SpecificationCollector(PVLSyntax.get(), pu).rewriteAll();
    Progress("Shuffling specifications took %dms", tk.show());
    Debug("after collecting specifications %s", pu);

    pu = new PVLPostProcessor(pu).rewriteAll();
    Progress("Post processing pass took %dms", tk.show());
    return pu;
  }
}

