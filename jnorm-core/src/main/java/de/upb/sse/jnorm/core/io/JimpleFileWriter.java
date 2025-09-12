package de.upb.sse.jnorm.core.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.core.JavaSootClass;

public class JimpleFileWriter implements JimpleWriter {
  private final Path outputDirectory;

  public JimpleFileWriter(Path outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  @Override
  public void write(JavaSootClass clazz, String outputPath) throws IOException {
    File file = outputDirectory.resolve(outputPath).toFile();
    file.getParentFile().mkdirs();
    JimplePrinter jimplePrinter = new JimplePrinter(JimplePrinter.Option.Deterministic);
    FileWriter fileWriter = new FileWriter(file);
    PrintWriter printWriter = new PrintWriter(fileWriter);

    jimplePrinter.printTo(clazz, printWriter);
    printWriter.close();
  }
}
