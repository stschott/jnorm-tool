package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.PrivateInvokeNormalizer;
import java.nio.file.Paths;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.types.VoidType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

class PrivateInvokeNormalizerTest {

  JavaView view;
  private SootClass privateInvocationClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> privateInvocationClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("PrivateMethodInvocation"));

    assertTrue(privateInvocationClassOpt.isPresent());
    privateInvocationClass = privateInvocationClassOpt.get();
  }

  @Test
  void testPrivateInvokeNormalizationForSimpleCase() {
    SootMethod filterMethod = getPrivateInvocationClassMethod("callPrivateMethods");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(filterMethod.getSignature());

    List<Stmt> stmtsBefore = builder.getStmts();

    new PrivateInvokeNormalizer().interceptBody(builder, view);

    Local r0 = new Local("r0", privateInvocationClass.getType());
    MethodSubSignature privateMethodSubSignature =
        new MethodSubSignature("privateMethod", Collections.emptyList(), VoidType.getInstance());
    MethodSignature initMethodSignature =
        new MethodSignature(filterMethod.getDeclaringClassType(), privateMethodSubSignature);

    JSpecialInvokeExpr specialInvokeExpr =
        Jimple.newSpecialInvokeExpr(r0, initMethodSignature, Collections.emptyList());
    JInvokeStmt newInvokeStmt =
        Jimple.newInvokeStmt(specialInvokeExpr, StmtPositionInfo.getNoStmtPositionInfo());

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);
    expectedStmtsAfter.set(1, newInvokeStmt);

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  @Test
  void testPrivateInvokeNormalizationWithNoMatchingCases() {
    SootMethod filterMethod = getPrivateInvocationClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new PrivateInvokeNormalizer().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  private SootMethod getPrivateInvocationClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(
                privateInvocationClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        privateInvocationClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
