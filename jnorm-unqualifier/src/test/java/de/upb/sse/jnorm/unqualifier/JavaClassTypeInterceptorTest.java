package de.upb.sse.jnorm.unqualifier;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootClass;
import sootup.core.model.SourceType;
import sootup.core.signatures.PackageName;
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

public class JavaClassTypeInterceptorTest {
  static String inputDir = "src/test/resources/http-request/1.7/classes";
  static String sampleClassName = "HttpRequest$9";
  static String samplePackageName = "com.github.kevinsawicki.http";

  @Disabled
  @Test
  void testConstructorInterceptionInSampleClass() {
    assert (Files.exists(Paths.get(inputDir)));

    SootClass sampleClazz = getSampleClass(inputDir);
    sampleClazz.getMethods().forEach(a -> {});

    new JavaClassTypeInterceptor()
        .intercept(
            () -> {
              String expectedStmtStr =
                  "<HttpRequest$9: void <init>(HttpRequest,Closeable,boolean,Reader,Writer)>";
              String actualStmtStr =
                  sampleClazz.getMethods().stream()
                      .filter(method -> method.getName().equals("<init>"))
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException("No <init> method found"))
                      .getSignature()
                      .toString();
              assertEquals(expectedStmtStr, actualStmtStr);
            });
  }

  private SootClass getSampleClass(String inputDir) {
    JavaView view = createJavaView(inputDir);

    var sampleClassType = new JavaClassType(sampleClassName, new PackageName(samplePackageName));
    Optional<? extends SootClass> sampleClazzOpt = view.getClass(sampleClassType);

    assert sampleClazzOpt.isPresent();
    return sampleClazzOpt.get();
  }

  private JavaView createJavaView(String inputDir) {
    List<AnalysisInputLocation> inputLocations =
        Arrays.asList(
            new JavaClassPathAnalysisInputLocation(inputDir, SourceType.Application),
            new DefaultRuntimeAnalysisInputLocation());
    return new JavaView(inputLocations);
  }
}
