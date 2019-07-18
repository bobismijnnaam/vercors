package vct.col.util;

import java.io.*;

import hre.lang.HREExitException;
import vct.col.ast.stmt.decl.ProgramUnit;
import vct.util.Configuration;

import static hre.lang.System.*;

/**
 * Parser interface.
 * 
 * @author sccblom
 *
 */
public abstract class Parser {

  /**
   * Parse the given input stream and return the contents as a program unit.
   *
   * @param fileName Display name of the program
   * @param inputStream Input stream to be parsed.
   * @return CompilationUnit representation of the contents of the file.
   */
  public abstract ProgramUnit parse(String fileName, InputStream inputStream) throws IOException;

  private ProgramUnit parseCatchIO(String fileName, InputStream inputStream) {
    try {
      ProgramUnit result = parse(fileName, inputStream);
      inputStream.close();
      return result;
    } catch (IOException e) {
      DebugException(e);
      Abort("IO error while reading from %s", fileName);
    }

    return null; // Unreachable
  }

  public ProgramUnit parseFile(File file) {
    try {
      InputStream inputStream = new FileInputStream(file);
      return parseCatchIO(file.getName(), inputStream);
    } catch(FileNotFoundException e) {
      Fail("File %s has not been found", file.getName());
    }

    return null; // Unreachable
  }

  public ProgramUnit parseResource(String resource) {
    return parseCatchIO(resource, Configuration.openResource(resource));
  }
}