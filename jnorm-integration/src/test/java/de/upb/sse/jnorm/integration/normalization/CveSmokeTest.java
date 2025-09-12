package de.upb.sse.jnorm.integration.normalization;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.transform.BodyInterceptor;
import sootup.interceptors.*;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.MutableJavaView;

class CveSmokeTest {

  private final List<BodyInterceptor> normalizerToTest =
      Arrays.asList(
          new NopEliminator(),
          new EmptySwitchEliminator(),
          new CastAndReturnInliner(),
          new LocalSplitter(),
          new Aggregator(),
          new CopyPropagator(),
          new ConstantPropagatorAndFolder(),
          new TypeAssigner(),
          new ArithmeticNormalizer(),
          new BufferMethodCallNormalizer(),
          new CharSequenceToStringNormalizer(),
          new DuplicateTypeCastEliminator(),
          new EmptyAnonymousClassEliminator(),
          new EnumNormalizer(),
          new NestBasedAccessNormalizer(),
          new NullCheckNormalizer(),
          new PrivateInvokeNormalizer(),
          new RedundantTrapEliminator(),
          new StringConcatNormalizer(),
          new TrapNormalizer(),
          new TypeCastEliminator(),
          new CustomLocalNameStandardizer(),
          new UnusedLocalEliminator());

  @BeforeEach
  void setUp() {}

  @Test
  void CVE_2014_8122() throws IOException {
    runNormalizationsForCVE("CVE-2014-8122");
  }

  @Test
  void CVE_2015_5348() throws IOException {
    runNormalizationsForCVE("CVE-2015-5348");
  }

  @Test
  void CVE_2015_6644() throws IOException {
    runNormalizationsForCVE("CVE-2015-6644");
  }

  @Test
  void CVE_2018_11047() throws IOException {
    runNormalizationsForCVE("CVE-2018-11047");
  }

  @Test
  void CVE_2018_11771() throws IOException {
    runNormalizationsForCVE("CVE-2018-11771");
  }

  @Test
  void CVE_2019_0201() throws IOException {
    runNormalizationsForCVE("CVE-2019-0201");
  }

  @Test
  void CVE_2019_0231() throws IOException {
    runNormalizationsForCVE("CVE-2019-0231");
  }

  @Test
  void CVE_2019_10091() throws IOException {
    runNormalizationsForCVE("CVE-2019-10091");
  }

  @Test
  void CVE_2020_1945() throws IOException {
    runNormalizationsForCVE("CVE-2020-1945");
  }

  @Test
  void CVE_2020_5289() throws IOException {
    runNormalizationsForCVE("CVE-2020-5289");
  }

  @Test
  void CVE_2020_9489() throws IOException {
    runNormalizationsForCVE("CVE-2020-9489");
  }

  @Test
  void CVE_2020_13935() throws IOException {
    runNormalizationsForCVE("CVE-2020-13935");
  }

  void runNormalizationsForCVE(String cveName) throws IOException {
    String CVE_LIST_PATH = "src/test/resources/bugs";
    String projPath = String.format("%s/%s", CVE_LIST_PATH, cveName);

    try (Stream<Path> stream = Files.walk(Paths.get(projPath), 4)) {
      Set<String> inputLocations =
          stream
              .filter(Files::isDirectory)
              .filter(d -> d.toString().endsWith("vul") || d.toString().endsWith("fix"))
              .map(Path::toString)
              .collect(Collectors.toSet());

      for (String inputLocation : inputLocations) {
        runNormalization(normalizerToTest, inputLocation);
      }
    }
  }

  void runNormalization(List<BodyInterceptor> interceptors, String inputLocation) {

    MutableJavaView viewWithNormalization = createViewWithInterceptors(interceptors, inputLocation);
    runTest(viewWithNormalization);
  }

  MutableJavaView createViewWithInterceptors(
      List<BodyInterceptor> interceptors, String inputLocation) {
    if (Paths.get(inputLocation).toFile().exists()) {
      List<BodyInterceptor> allInterceptors = new ArrayList<>(interceptors);
      allInterceptors.add(new LocalNameStandardizer());

      List<AnalysisInputLocation> inputLocationsWithNormalization =
          Collections.singletonList(
              new JavaClassPathAnalysisInputLocation(
                  inputLocation, SourceType.Application, allInterceptors));
      return new MutableJavaView(inputLocationsWithNormalization);
    }
    return null;
  }

  private void runTest(MutableJavaView view) {
    view.getClasses()
        .forEach(
            sc ->
                sc.getMethods().stream()
                    .filter(SootMethod::hasBody)
                    .forEach(m -> m.getBody().getStmts()));
  }
}
