package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.TrapNormalizer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

class TrapNormalizerTest {

  JavaView view;
  private SootClass tryWithResourcesClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> tryWithResourcesClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("TryWithResources"));

    assertTrue(tryWithResourcesClassOpt.isPresent());
    tryWithResourcesClass = tryWithResourcesClassOpt.get();
  }

  @Test
  void testTrapNormalizationForSimpleCase() {
    SootMethod filterMethod = getTryWithResourcesClassMethod("doTryWithResources");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();

    new TrapNormalizer().interceptBody(builder, view);

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);

    assertEquals(expectedStmtsAfter, actualStmtsAfter);
  }

  @Test
  void testTrapNormalizationWithNoMatchingCases() {
    SootMethod filterMethod = getTryWithResourcesClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new TrapNormalizer().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  private SootMethod getTryWithResourcesClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(
                tryWithResourcesClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        tryWithResourcesClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
