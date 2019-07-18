package vct.antlr4.parser;

import static hre.lang.System.*;

import java.io.*;

import vct.util.Configuration;
import vct.col.ast.stmt.decl.ProgramUnit;

/**
 * Parse specified code and convert the contents to COL. 
 */
public class ColCParser extends ColIParser {
  @Override
  public ProgramUnit parse(String fileName, InputStream inputStream) throws IOException {
    Runtime runtime=Runtime.getRuntime();

    String command=Configuration.cpp_command.get();
    command+=" -nostdinc -isystem " + Configuration.getIncludesHome();
    for(String p:Configuration.cpp_include_path){
      command+=" -I"+p;
    }
    for(String p:Configuration.cpp_defines){
      command+=" -D"+p;
    }
    command+=" "+fileName;

    Progress("pre-processing command line: %s",command);
    final Process process=runtime.exec(command);
    Thread t=new Thread(){
      public void run(){
        BufferedReader err=new BufferedReader(new InputStreamReader(process.getErrorStream()));
        boolean err_found=false;
        String s;
        try {
          while((s=err.readLine())!=null){
            Output("%s", s);
            if (s.matches(".*error.*")) err_found=true;
          }
        } catch (IOException e) {
          DebugException(e);
          err_found=true;
        }
        if (err_found){
          System.exit(1);
        }
      }
    };
    t.setDaemon(true);
    t.start();
    return parse(fileName, process.getInputStream());
  }
}

