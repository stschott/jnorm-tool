package de.upb.sse.jnorm.integration.sootup.interceptors;

import de.upb.sse.jnorm.core.normalization.classnormalizers.AnonymousConstructorExceptionRemover;
import de.upb.sse.jnorm.core.normalization.classnormalizers.ClassModifierNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.ClassNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.ConstructorModifierNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.EnumValuesMethodRemover;
import de.upb.sse.jnorm.core.normalization.classnormalizers.EnumValuesModifierNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.SyntheticMethodRemover;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.ArithmeticNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.BufferMethodCallNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.CharSequenceToStringNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.DuplicateTypeCastEliminator;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.EmptyAnonymousClassEliminator;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.EnumNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.NestBasedAccessNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.NullCheckNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.PrivateInvokeNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.StringConcatNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.TypeCastEliminator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.transform.BodyInterceptor;
import sootup.core.types.ClassType;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;
import sootup.java.core.views.MutableJavaView;

public class InterceptorsIT {
  private static final Logger logger = LoggerFactory.getLogger(InterceptorsIT.class);

  private final String REF_CLASSES_PATH = "src/test/resources/reference-classes";

  @Disabled
  @Test
  public void typeCastEliminator_shouldRemoveDuplicateCasts() {
    String projectPath = REF_CLASSES_PATH + "/jjwt";
    String className =
        "io.jsonwebtoken.impl.security.AbstractAsymmetricJwkBuilder$DefaultOctetPrivateJwkBuilder";
    String v1 = "1.7";
    String v2 = "11";
    compareSingleClass(projectPath, v1, v2, className);
  }

  @Test
  public void normalizedClass_shouldNotContainSyntheticMethods() {
    String projectPath = REF_CLASSES_PATH + "/jjwt";
    String className = "io.jsonwebtoken.gson.io.GsonSerializer$TestSupplier";
    String v1 = "1.7";
    String v2 = "11";
    compareSingleClass(projectPath, v1, v2, className);
  }

  @Test
  public void normalizedClass_shouldNotContainBridgeMethods() {
    String projectPath = REF_CLASSES_PATH + "/jjwt";
    String className = "io.jsonwebtoken.lang.Maps$MapBuilder";
    String v1 = "1.7";
    String v2 = "11";
    compareSingleClass(projectPath, v1, v2, className);
  }

  private void compareSingleClass(String projectPath, String v1, String v2, String className) {
    JavaView normalizedView1 = getNormalizedView(projectPath, v1);
    JavaView normalizedView2 = getNormalizedView(projectPath, v2);

    JavaSootClass normalizedClazz1 = getSootClass(normalizedView1, className, true);
    JavaSootClass normalizedClazz2 = getSootClass(normalizedView2, className, true);

    Assertions.assertEquals(classToString(normalizedClazz1), classToString(normalizedClazz2));
  }

  private MutableJavaView getNormalizedView(String pathToBenchmark, String version) {
    List<AnalysisInputLocation> normalizedInputLocations = new ArrayList<>();

    List<BodyInterceptor> allInterceptors =
        Arrays.asList(
            new ArithmeticNormalizer(),
            new BufferMethodCallNormalizer(),
            new CharSequenceToStringNormalizer(),
            new DuplicateTypeCastEliminator(),
            new EmptyAnonymousClassEliminator(),
            new EnumNormalizer(),
            new NestBasedAccessNormalizer(),
            new NullCheckNormalizer(),
            new PrivateInvokeNormalizer(),
            new StringConcatNormalizer(),
            new TypeCastEliminator());

    normalizedInputLocations.add(
        new JavaClassPathAnalysisInputLocation(
            pathToBenchmark + "/" + version + "/classes", SourceType.Application, allInterceptors));
    normalizedInputLocations.add(new DefaultRuntimeAnalysisInputLocation());

    return new MutableJavaView(normalizedInputLocations);
  }

  private JavaSootClass getSootClass(JavaView view, String className, boolean normalize) {
    ClassType classType = view.getIdentifierFactory().getClassType(className);

    Optional<JavaSootClass> clazzOpt = view.getClass(classType);
    if (!clazzOpt.isPresent()) {
      throw new IllegalArgumentException("Provided class " + className + " does not exist");
    }
    JavaSootClass sootClass = clazzOpt.get();
    if (!normalize) return sootClass;

    return normalizeClass(sootClass);
  }

  private String classToString(JavaSootClass clazz) {
    JimplePrinter jimplePrinter = new JimplePrinter(JimplePrinter.Option.Deterministic);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    jimplePrinter.printTo(clazz, printWriter);

    printWriter.close();
    return stringWriter.toString();
  }

  private JavaSootClass normalizeClass(JavaSootClass clazz) {
    List<ClassNormalizer> classNormalizers =
        Arrays.asList(
            new SyntheticMethodRemover(),
            new EnumValuesMethodRemover(),
            new ClassModifierNormalizer(),
            new ConstructorModifierNormalizer(),
            new EnumValuesModifierNormalizer(),
            new AnonymousConstructorExceptionRemover());

    JavaSootClass normalizedClass = clazz;
    for (ClassNormalizer classNormalizer : classNormalizers) {
      normalizedClass = classNormalizer.normalize(normalizedClass);
    }

    return normalizedClass;
  }
}
