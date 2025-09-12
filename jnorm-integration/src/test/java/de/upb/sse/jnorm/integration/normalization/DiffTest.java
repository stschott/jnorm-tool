package de.upb.sse.jnorm.integration.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.upb.sse.jnorm.core.config.ProcessingConfig;
import de.upb.sse.jnorm.core.processing.SourceProcessor;
import de.upb.sse.jnorm.core.processing.SourceProcessorFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import sootup.core.model.Body;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class DiffTest {
  ProcessingConfig config =
      new ProcessingConfig.Builder()
          .applyOptimizations(true)
          .applyNormalization(true)
          .applyAggressiveNormalization(true)
          .build();
  SourceProcessor sourceProcessor = SourceProcessorFactory.createSourceProcessor(config);

  // Test the name standardizing
  @Test
  public void testLibrec1() throws IOException {
    runTest(
        "src/test/resources/diffs/librec/1/JAVA_7/classes/",
        "src/test/resources/diffs/librec/1/JAVA_8/classes/");
  }

  // Test the conversion of "<java.util.Hashtable: void putAll(java.util.Map)>" into
  // "<java.util.Properties: void putAll(java.util.Map)> within
  // a dynamicinvoke's method handle
  @Test
  public void testMybatis1() throws IOException {
    runTest(
        "src/test/resources/diffs/mybatis-3/1/JAVA_11/classes/",
        "src/test/resources/diffs/mybatis-3/1/JAVA_17/classes/");
  }

  // Tets the conversion of "REF_INVOKE_SPECIAL" into "REF_INVOKE_VIRTUAL" within a dynamicinvoke's
  // method handle
  @Test
  public void testMybatis2() throws IOException {
    runTest(
        "src/test/resources/diffs/mybatis-3/2/JAVA_11/classes/",
        "src/test/resources/diffs/mybatis-3/2/JAVA_17/classes/");
  }

  // TypeCastEliminator not working correctly. The following test case fails for 3 methods
  @Disabled
  @Test
  public void testLearningSpark1() throws IOException {
    runTest(
        "src/test/resources/diffs/learning-spark/1/1.7/classes/",
        "src/test/resources/diffs/learning-spark/1/1.8/classes/");
  }

  // Tests StringBuild append normalization
  @Test
  public void testJSqlParser1() throws IOException {
    runTest(
        "src/test/resources/diffs/JSqlParser/1/1.8/", "src/test/resources/diffs/JSqlParser/1/11/");
  }

  // Tests StringBuild append normalization. This only works when the CopyPropagator is disabled
  @Test
  public void testJSqlParser2() throws IOException {
    runTest(
        "src/test/resources/diffs/JSqlParser/2/1.8/", "src/test/resources/diffs/JSqlParser/2/11/");
  }

  // Dynamic String concat with empty string literal ("")
  @Test
  public void testMockserver1() throws IOException {
    runTest(
        "src/test/resources/diffs/mockserver/1/1.8/", "src/test/resources/diffs/mockserver/1/11/");
  }

  // ConcurrentHashMap Normalization
  @Test
  public void testLightTaskScheduler1() throws IOException {
    runTest(
        "src/test/resources/diffs/light-task-scheduler/1/JAVA_7/",
        "src/test/resources/diffs/light-task-scheduler/1/JAVA_8/");
  }

  // Redundant Trap Eliminator
  @Test
  public void testLightTaskScheduler2() throws IOException {
    runTest(
        "src/test/resources/diffs/light-task-scheduler/2/JAVA_7/",
        "src/test/resources/diffs/light-task-scheduler/2/JAVA_8/");
  }

  // Trap Normalizer
  @Test
  public void testAtmosphere1() throws IOException {
    runTest(
        "src/test/resources/diffs/atmosphere/1/JAVA_8/",
        "src/test/resources/diffs/atmosphere/1/JAVA_11/");
  }

  // Trap Normalizer 2
  @Test
  public void testShardingsphere1() throws IOException {
    runTest(
        "src/test/resources/diffs/shardingsphere/1/JAVA_8/",
        "src/test/resources/diffs/shardingsphere/1/JAVA_11/");
  }

  // Trap Normalizer 3 (fails, but does not crash)
  @Disabled
  @Test
  public void testShardingsphere2() throws IOException {
    runTest(
        "src/test/resources/diffs/shardingsphere/2/JAVA_8/",
        "src/test/resources/diffs/shardingsphere/2/JAVA_11/");
  }

  // Trap Normalizer 4 (fails ATM)
  @Disabled
  @Test
  public void testShardingsphere3() throws IOException {
    runTest(
        "src/test/resources/diffs/shardingsphere/3/JAVA_8/",
        "src/test/resources/diffs/shardingsphere/3/JAVA_11/");
  }

  // Dynamic String Concat using java.lang.Throwable
  @Test
  public void testKkFileView1() throws IOException {
    runTest(
        "src/test/resources/diffs/kkFileView/1/1.8/", "src/test/resources/diffs/kkFileView/1/11/");
  }

  private void runTest(String diff1Input, String diff2Input) throws IOException {
    Stream<JavaSootClass> diff1 = sourceProcessor.process(diff1Input);
    Stream<JavaSootClass> diff2 = sourceProcessor.process(diff2Input);

    List<JavaSootClass> diff1Classes = diff1.toList();
    List<JavaSootClass> diff2Classes = diff2.toList();

    assertEquals(diff1Classes.size(), diff2Classes.size());
    assertEquals(1, diff1Classes.size());

    JavaSootClass class1 = diff1Classes.get(0);
    JavaSootClass class2 = diff2Classes.get(0);

    String class1Str = classToString(class1);
    String class2Str = classToString(class2);
    //        assertEquals(class1Str, class2Str);

    Set<JavaSootMethod> methods1 = class1.getMethods();
    Set<JavaSootMethod> methods2 = class2.getMethods();

    for (JavaSootMethod method1 : methods1) {
      for (JavaSootMethod method2 : methods2) {
        if (!method1.getSignature().equals(method2.getSignature())) continue;

        Body body1 = method1.getBody();
        Body body2 = method2.getBody();

        if (!body1.toString().equals(body2.toString())) {
          //
          // System.out.println(DotExporter.createUrlToWebeditor(body1.getStmtGraph()));
          //
          // System.out.println(DotExporter.createUrlToWebeditor(body2.getStmtGraph()));
          //                    System.out.println("Differing Methods: " + method1.getSignature());
          //                    Path tempDir = writeToTemp(method1, method2);
          //                    runWinMerge(tempDir);
        }
        assertEquals(body1.toString(), body2.toString());
      }
    }
  }

  private Path writeToTemp(JavaSootMethod method1, JavaSootMethod method2) throws IOException {
    Path tempDir = Files.createTempDirectory("jnorm-test");
    Path filePath1 = tempDir.resolve("1.txt");
    Path filePath2 = tempDir.resolve("2.txt");

    Files.write(filePath1, method2String(method1).getBytes());
    Files.write(filePath2, method2String(method2).getBytes());
    return tempDir;
  }

  private void runWinMerge(Path tempDir) throws IOException {
    ProcessBuilder pb =
        new ProcessBuilder(
            "WinMergeU", tempDir.resolve("1.txt").toString(), tempDir.resolve("2.txt").toString());
    pb.start();
  }

  private String method2String(JavaSootMethod method) {
    return method.getSubSignature() + " " + method.getBody();
  }

  private String classToString(JavaSootClass clazz) {
    JimplePrinter printer = new JimplePrinter(JimplePrinter.Option.Deterministic);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    printer.printTo(clazz, printWriter);
    printWriter.close();
    return stringWriter.toString();
  }
}
