package vct.silver;

import vct.col.ast.stmt.decl.ProgramUnit;
import vct.col.rewrite.SatCheckRewriter;
import viper.api.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import hre.ast.Origin;
import hre.config.IntegerSetting;
import hre.config.StringSetting;
import hre.io.Container;
import hre.io.JarContainer;
import hre.lang.HREError;
import hre.lang.HREException;
import hre.util.ContainerClassLoader;
import vct.error.VerificationError;
import vct.logging.MessageFactory;
import vct.logging.PassAddVisitor;
import vct.logging.PassReport;
import vct.logging.TaskBegin;
import vct.util.Configuration;

import static hre.lang.System.DebugException;
import static hre.lang.System.Output;
import static hre.lang.System.Warning;

public class SilverBackend {
  public static IntegerSetting silicon_z3_timeout=new IntegerSetting(30000);
  
  public static ViperAPI<Origin,VerificationError,?,?,?,?,?,?>
  getVerifier(String tool) {
    return getViperVerifier(tool);
  }
  
  public static ViperAPI<Origin,VerificationError,?,?,?,?,?,?>
  getViperVerifier(String tool){
    ViperAPI<Origin,VerificationError,?,?,?,?,?,?> verifier = null;
    switch(tool.trim()) {
    case "carbon":
      verifier = new CarbonVerifier<>(new HREOrigins());
      break;
    case "silicon":
      verifier = new SiliconVerifier<>(new HREOrigins());
      break;
    case "parser":
      verifier = new SilverImplementation<>(new HREOrigins());
      break;
    default:
      throw new HREError("cannot guess the main class of %s",tool);    
    }
    return verifier;
  }
  
  public static
  PassReport TestSilicon(PassReport given, String tool) {
    ViperAPI<Origin, VerificationError, ?, ?, ?, ?, ?, ?> verifier=getVerifier(tool);
    // We redirect through a new method, because we need to convince java the program type is consistent. The most brief
    // way to capture a  wildcard ("?") type is via a method.
    return TestSilicon(given, tool, verifier);
  }

  public static <Program> PassReport TestSilicon(PassReport given, String tool, ViperAPI<Origin, VerificationError, ?, ?, ?, ?, ?, Program> verifier) {
    //hre.System.Output("verifying with %s backend",silver_module.get());
    ProgramUnit arg=given.getOutput();
    PassReport report=new PassReport(arg);
    report.add(new PassAddVisitor(given));
    MessageFactory log=new MessageFactory(new PassAddVisitor(report));
    TaskBegin verification=log.begin("Viper verification");

    hre.lang.System.Progress("verifying with %s %s backend", "builtin", tool);
    //verifier.set_detail(Configuration.detailed_errors.get());
    VerCorsViperAPI vercors=VerCorsViperAPI.get();
    Program program = vercors.prog.convert(verifier,arg);
    log.phase(verification,"Backend AST conversion");
    String fname=vct.util.Configuration.backend_file.get();
    if (fname!=null){
      PrintWriter pw=null;
      try {
        pw = new java.io.PrintWriter(new java.io.File(fname));
        verifier.write_program(pw,program);
      } catch (FileNotFoundException e) {
        DebugException(e);
      } finally {
        if (pw!=null) pw.close();
      }
    }

    Properties settings=new Properties();
    if (tool.startsWith("silicon")){
      //settings.setProperty("smt.soft_timeout",silicon_z3_timeout.get()+"");
    }
    ViperControl control=new ViperControl(log);
    try {
      // Call into Viper to verify!
      List<? extends ViperError<Origin>> rawErrors = verifier.verify(
              Configuration.getZ3Path().toPath(),
              settings,
              program,
              control
      );
      // Put it in a new list so we can add ViperErrors to it ourselves
      List<ViperError<Origin>> errors = new ArrayList<>(rawErrors);

      // Filter SatCheck errors that are to be expected
      HashSet<SatCheckRewriter.AssertOrigin> satCheckAssertsSeen = new HashSet<>();
      errors.removeIf(e -> {
        for (int i = 0; i < e.getExtraCount(); i++) {
          Origin origin = e.getOrigin(i);
          if (origin instanceof SatCheckRewriter.AssertOrigin) {
            satCheckAssertsSeen.add((SatCheckRewriter.AssertOrigin) origin);
            return true;
          }
        }
        return false;
      });

      // For each satCheckAssert that did not error, it means the contract was requires false; or something similar
      // Therefore, we warn the user that their contracts are unsound
      HashSet<Origin> expectedSatCheckAsserts = vercors.getSatCheckAsserts();
      for (Origin expectedSatCheckAssert : expectedSatCheckAsserts) {
        if (!satCheckAssertsSeen.contains(expectedSatCheckAssert)) {
          ViperErrorImpl<Origin> error = new ViperErrorImpl<Origin>(expectedSatCheckAssert, "method.precondition.unsound:method.precondition.false");
          errors.add(error);
        }
      }

      if (errors.size() == 0) {
        Output("Success!");
      } else {
        Output("Errors! (%d)", errors.size());
        for(ViperError<Origin> e:errors){
          log.error(e);
        }
      }
    } catch (Exception e){
      log.exception(e);
    } finally {
      control.done();
    }
    log.end(verification);
    return report;
  }

}
