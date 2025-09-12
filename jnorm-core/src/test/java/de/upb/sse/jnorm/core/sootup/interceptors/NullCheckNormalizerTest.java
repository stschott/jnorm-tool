package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.NullCheckNormalizer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.signatures.PackageName;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

class NullCheckNormalizerTest {

  JavaView view;
  private SootClass nullChecksClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> nullChecksClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("NullChecks"));

    assertTrue(nullChecksClassOpt.isPresent());
    nullChecksClass = nullChecksClassOpt.get();
  }

  @Test
  void testNullCheckNormalizerForSimpleCase() {
    SootMethod filterMethod = getNullChecksClassMethod("performNullChecks");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(filterMethod.getSignature());

    List<Stmt> stmtsBefore = builder.getStmts();

    new NullCheckNormalizer().interceptBody(builder, view);

    ClassType stringType = new JavaClassType("String", new PackageName("java.lang"));
    ClassType objectsType = new JavaClassType("Object", new PackageName("java.lang"));
    JavaClassType classType = new JavaClassType("Class", new PackageName("java.lang"));

    Local r0 = new Local("r0", stringType);
    MethodSubSignature getClassSubSignature =
        new MethodSubSignature("getClass", Collections.emptyList(), classType);
    MethodSignature getClassSignature = new MethodSignature(objectsType, getClassSubSignature);

    Stmt newInvokeStmt =
        Jimple.newInvokeStmt(
            Jimple.newVirtualInvokeExpr(r0, getClassSignature),
            StmtPositionInfo.getNoStmtPositionInfo());

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);
    expectedStmtsAfter.set(1, newInvokeStmt);

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  @Test
  void testNullCheckNormalizerWithNoMatchingCases() {
    SootMethod filterMethod = getNullChecksClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new NullCheckNormalizer().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  private SootMethod getNullChecksClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(nullChecksClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        nullChecksClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
