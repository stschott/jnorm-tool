package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.CharSequenceToStringNormalizer;
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

class CharSequenceToStringNormalizerTest {

  JavaView view;
  private SootClass charSequenceToStringClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> charSequenceToStringClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("CharSequenceToString"));

    assertTrue(charSequenceToStringClassOpt.isPresent());
    charSequenceToStringClass = charSequenceToStringClassOpt.get();
  }

  @Test
  void testCharSequenceToStringInterceptorForSimpleCase() {
    SootMethod filterMethod = getCharSequenceToStringClassMethod("print");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(filterMethod.getSignature());

    ClassType stringType = new JavaClassType("String", new PackageName("java.lang"));
    ClassType charSequenceType = new JavaClassType("CharSequence", new PackageName("java.lang"));
    ClassType objectType = new JavaClassType("Object", new PackageName("java.lang"));

    Local r1 = new Local("r1", stringType);
    Local text = new Local("text", charSequenceType);

    MethodSubSignature toStringSubSignature =
        new MethodSubSignature("toString", Collections.emptyList(), stringType);

    MethodSignature toStringSignatureInObject =
        new MethodSignature(objectType, toStringSubSignature);

    List<Stmt> stmtsBefore = builder.getStmts();

    new CharSequenceToStringNormalizer().interceptBody(builder, view);

    Stmt newAssignStmt =
        Jimple.newAssignStmt(
            r1,
            Jimple.newVirtualInvokeExpr(text, toStringSignatureInObject),
            StmtPositionInfo.getNoStmtPositionInfo());

    List<Stmt> actualStmtsAfter = builder.getStmts();
    List<Stmt> expectedStmtsAfter = new ArrayList<>(stmtsBefore);
    expectedStmtsAfter.set(4, newAssignStmt);

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  @Test
  void testCharSequenceToStringInterceptorWithNoMatchingCases() {
    SootMethod filterMethod = getCharSequenceToStringClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new CharSequenceToStringNormalizer().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  private SootMethod getCharSequenceToStringClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(
                charSequenceToStringClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        charSequenceToStringClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
