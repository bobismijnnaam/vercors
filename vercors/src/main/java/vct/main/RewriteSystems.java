package vct.main;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import vct.antlr4.parser.Parsers;
import vct.col.ast.stmt.decl.ProgramUnit;
import vct.col.rewrite.RewriteSystem;
import vct.util.Configuration;

public class RewriteSystems {
  private static Map<String, ProgramUnit> systems = new ConcurrentHashMap<>();
  
  public static RewriteSystem getRewriteSystem(String name){
    if(!systems.containsKey(name)) {
      synchronized(systems) {
        if(!systems.containsKey(name)) {
          String fileName = "config/" + name + ".jspec";
          systems.put(name, Parsers.getParser("jspec").parseResource(fileName));
        }
      }
    }

    return new RewriteSystem(systems.get(name), name);
  }
}
