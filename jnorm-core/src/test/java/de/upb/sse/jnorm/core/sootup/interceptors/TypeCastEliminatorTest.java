package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.TypeCastEliminator;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.PrimitiveType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

class TypeCastEliminatorTest {

  JavaView view;
  private SootClass typeChecksClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> typeChecksClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("TypeChecks"));

    assertTrue(typeChecksClassOpt.isPresent());
    typeChecksClass = typeChecksClassOpt.get();
  }

  @Test
  void testTypeCastEliminatorForObjectToIntegerCasting() {
    SootMethod filterMethod = getTypeChecksClassMethod("castToByte");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(filterMethod.getSignature());

    List<Stmt> stmtsBefore = builder.getStmts();

    new TypeCastEliminator().interceptBody(builder, view);

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);

    // Validate and remove the first typecast: obj = (java.lang.Object) s0;
    Stmt castToObjectStmt = expectedStmtsAfter.get(2);
    assertInstanceOf(JAssignStmt.class, castToObjectStmt);
    JAssignStmt castToObjectAssign = (JAssignStmt) castToObjectStmt;
    assertInstanceOf(JCastExpr.class, castToObjectAssign.getRightOp());
    Local castToObjectAssignLocal = (Local) ((JCastExpr) castToObjectAssign.getRightOp()).getOp();
    Local s0 = new Local("s0", PrimitiveType.ShortType.getInstance());
    assertTrue(castToObjectAssignLocal.equivTo(s0));
    Local castToObjectLocal = (Local) castToObjectAssign.getLeftOp();

    // Remove the (java.lang.Object) cast statement
    expectedStmtsAfter.remove(2);

    // Validate and remove the second typecast: i4 = (java.lang.Integer) obj;
    Stmt castToIntegerStmt = expectedStmtsAfter.get(2);
    assertInstanceOf(JAssignStmt.class, castToIntegerStmt);
    JAssignStmt castToIntegerAssign = (JAssignStmt) castToIntegerStmt;
    assertInstanceOf(JCastExpr.class, castToIntegerAssign.getRightOp());

    Local castToIntegerAssignLocal = (Local) ((JCastExpr) castToIntegerAssign.getRightOp()).getOp();
    assertTrue(castToIntegerAssignLocal.equivTo(castToObjectLocal));

    // Remove the (java.lang.Integer) cast statement
    expectedStmtsAfter.remove(2);

    // Validate the virtualinvoke
    Stmt arithmeticStmt = expectedStmtsAfter.get(2);
    assertInstanceOf(JAssignStmt.class, arithmeticStmt);
    JAssignStmt arithmeticAssign = (JAssignStmt) arithmeticStmt;
    assertInstanceOf(JVirtualInvokeExpr.class, arithmeticAssign.getRightOp());
    JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) arithmeticAssign.getRightOp();

    // Replace the virtualinvoke operand with the uncasted variable
    JVirtualInvokeExpr updatedVirtualInvokeExpr = virtualInvokeExpr.withBase(s0);
    JAssignStmt updatedArithmeticAssign = arithmeticAssign.withRValue(updatedVirtualInvokeExpr);

    // Add the updated statement
    expectedStmtsAfter.remove(2);
    expectedStmtsAfter.add(2, updatedArithmeticAssign);

    // Validate the resulting statements
    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      Stmt expectedStmt = expectedStmtsAfter.get(i);
      Stmt actualStmt = actualStmtsAfter.get(i);
      assertTrue(actualStmt.equivTo(expectedStmt));
    }
  }

  @Test
  void testTypeCastEliminatorWithNoMatchingCases() {
    SootMethod filterMethod = getTypeChecksClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new TypeCastEliminator().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  private SootMethod getTypeChecksClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(typeChecksClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        typeChecksClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
