package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.RedundantTrapEliminator;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

class RedundantTrapEliminatorTest {

  JavaView view;
  private SootClass emptyTryCatchClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> emptyTryCatchClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("EmptyTryCatch"));

    assertTrue(emptyTryCatchClassOpt.isPresent());
    emptyTryCatchClass = emptyTryCatchClassOpt.get();
  }

  @Disabled
  @Test
  void testRedundantTrapEliminationForSimpleCase() {
    SootMethod filterMethod = getEmptyTryCatchClassMethod("doEmptyTryCatch");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();

    new RedundantTrapEliminator().interceptBody(builder, view);

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);
    expectedStmtsAfter.remove(1);

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  @Test
  void testRedundantTrapEliminationWithNoMatchingCases() {
    SootMethod filterMethod = getEmptyTryCatchClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new RedundantTrapEliminator().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  private SootMethod getEmptyTryCatchClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(
                emptyTryCatchClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        emptyTryCatchClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
