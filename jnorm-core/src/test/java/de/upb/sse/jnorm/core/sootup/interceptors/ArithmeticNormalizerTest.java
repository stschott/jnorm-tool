package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.ArithmeticNormalizer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.JSubExpr;
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

class ArithmeticNormalizerTest {

  JavaView view;
  private SootClass arithmeticClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> arithmeticClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("Arithmetic"));

    assertTrue(arithmeticClassOpt.isPresent());
    arithmeticClass = arithmeticClassOpt.get();
  }

  @Test
  void testArithmeticInterceptorForAssignStmt() {
    SootMethod filterMethod = getArithmeticClassMethod("filterOutNumbersBelowFifty");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(filterMethod.getSignature());

    Local i1 = new Local("i1", PrimitiveType.IntType.getInstance());
    IntConstant one = IntConstant.getInstance(1);

    List<Stmt> stmtsBefore = builder.getStmts();

    new ArithmeticNormalizer().interceptBody(builder, view);

    Stmt newAssignStmt =
        new JAssignStmt(i1, new JSubExpr(i1, one), StmtPositionInfo.getNoStmtPositionInfo());

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);
    expectedStmtsAfter.remove(5);
    expectedStmtsAfter.set(5, newAssignStmt);

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  @Test
  void testArithmeticInterceptorForMultipleAssignStmts() {
    SootMethod filterMethod = getArithmeticClassMethod("decrementMultipleLocals");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(filterMethod.getSignature());

    List<Stmt> stmtsBefore = builder.getStmts();

    Local i0 = new Local("i0", PrimitiveType.IntType.getInstance());
    Local i1 = new Local("i1", PrimitiveType.IntType.getInstance());
    IntConstant one = IntConstant.getInstance(1);

    new ArithmeticNormalizer().interceptBody(builder, view);

    Stmt newAssignStmt1 =
        new JAssignStmt(i0, new JSubExpr(i0, one), StmtPositionInfo.getNoStmtPositionInfo());
    Stmt newAssignStmt2 =
        new JAssignStmt(i1, new JSubExpr(i1, one), StmtPositionInfo.getNoStmtPositionInfo());

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);
    expectedStmtsAfter.remove(3);
    expectedStmtsAfter.remove(4);
    expectedStmtsAfter.set(3, newAssignStmt1);
    expectedStmtsAfter.set(4, newAssignStmt2);

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  @Test
  void testArithmeticInterceptorWithNoMatchingCases() {
    SootMethod filterMethod = getArithmeticClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new ArithmeticNormalizer().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  @Test
  void testArithmeticInterceptorWithNestedAddition() {
    SootMethod nestedAdditionMethod = getArithmeticClassMethod("nestedAddition");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(nestedAdditionMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(nestedAdditionMethod.getSignature());

    List<Stmt> stmtsBefore = builder.getStmts();

    Local t1 = new Local("$t1", PrimitiveType.IntType.getInstance());
    Local i1 = new Local("i1", PrimitiveType.IntType.getInstance());
    IntConstant five = IntConstant.getInstance(5);

    Stmt transformedStmt =
        new JAssignStmt(t1, new JSubExpr(i1, five), StmtPositionInfo.getNoStmtPositionInfo());

    new ArithmeticNormalizer().interceptBody(builder, view);

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);
    expectedStmtsAfter.remove(0); // Remove "$t0 = (int) -5".
    expectedStmtsAfter.set(0, transformedStmt); // Replace "$t1 = i1 + $t0" with "$t1 = i1 - 5".

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  private SootMethod getArithmeticClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(arithmeticClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        arithmeticClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
