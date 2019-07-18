package vct.silver;

import hre.ast.MessageOrigin;
import hre.ast.Origin;
import hre.lang.HREError;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import scala.io.Source;
import vct.col.ast.stmt.decl.ASTClass;
import vct.col.ast.generic.ASTNode;
import vct.col.ast.stmt.decl.DeclarationStatement;
import vct.col.ast.stmt.decl.ProgramUnit;
import vct.error.VerificationError;
import viper.api.ViperAPI;

public class ColSilverParser extends vct.col.util.Parser {
  public static <T,E,S,Decl,DFunc,DAxiom,Program>
  ProgramUnit run_test(String fileName, String data){
    ViperAPI<Origin,VerificationError,T,E,S,DFunc,DAxiom,Program> viper=
        SilverBackend.getVerifier("parser");
    Program program=viper.prog.parse_program(data);
    if (program==null){
      throw new HREError("parsing %s failed", fileName);
    }
    VerCorsViperAPI vercors=VerCorsViperAPI.get();
    ProgramUnit tmp=viper.prog.convert(vercors, program);
    ProgramUnit res=new ProgramUnit();
    ASTClass ref=new ASTClass("Ref",ASTClass.ClassKind.Record);
    ref.setOrigin(new MessageOrigin("implicit Ref for %s", fileName));
    res.add(ref);
    for(ASTNode d:tmp){
      if (d instanceof DeclarationStatement){
        ref.add_dynamic(d);
      } else {
        res.add(d);
      }
    }
    return res;
  }

  @Override
  public ProgramUnit parse(String fileName, InputStream inputStream) throws IOException {
    return run_test(fileName, Source.fromInputStream(inputStream, "utf-8").toString());
  }
}
