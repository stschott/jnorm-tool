package de.upb.sse.jnorm.core.sootup.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.StringConcatNormalizer;
import java.nio.file.Paths;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Immediate;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.PackageName;
import sootup.core.types.*;
import sootup.java.core.JavaSootClass;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

class StringConcatNormalizerTest {

  JavaView view;
  private SootClass stringConcatClass;

  @BeforeEach
  void setUp() {
    String bodyInterceptorTestResources = "src/test/resources/body-interceptors/";
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JimpleAnalysisInputLocation(Paths.get(bodyInterceptorTestResources)));
    view = new JavaView(inputLocations);

    Optional<JavaSootClass> stringConcatClassOpt =
        view.getClass(view.getIdentifierFactory().getClassType("StringConcat"));

    assertTrue(stringConcatClassOpt.isPresent());
    stringConcatClass = stringConcatClassOpt.get();
  }

  @Test
  void testStringConcatNormalizerForMultipleStringBuilderCalls() {
    SootMethod filterMethod = getStringConcatClassMethod("doMultipleStringBuilderCalls");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));
    builder.setMethodSignature(filterMethod.getSignature());

    List<Stmt> stmtsBefore = builder.getStmts();

    new StringConcatNormalizer().interceptBody(builder, view);

    List<Stmt> actualStmtsAfter = builder.getStmts();

    List<Stmt> expectedStmtsAfter = new ArrayList<>();

    ClassType stringType = new JavaClassType("String", new PackageName("java.lang"));

    Stmt stmt1 = stmtsBefore.get(1);
    Stmt stmt2 =
        new JAssignStmt(
            new Local("r5", stringType),
            getDynamicStringConcat(
                "Amount: \u0001 Pieces",
                Collections.singletonList(PrimitiveType.getInt()),
                Collections.singletonList(new Local("i0", PrimitiveType.getInt()))),
            StmtPositionInfo.getNoStmtPositionInfo());
    Stmt stmt3 = new JReturnVoidStmt(StmtPositionInfo.getNoStmtPositionInfo());

    expectedStmtsAfter.add(stmt1);
    expectedStmtsAfter.add(stmt2);
    expectedStmtsAfter.add(stmt3);

    assertEquals(expectedStmtsAfter.size(), actualStmtsAfter.size());
    for (int i = 0; i < expectedStmtsAfter.size(); i++) {
      assertTrue(expectedStmtsAfter.get(i).equivTo(actualStmtsAfter.get(i)));
    }
  }

  private JDynamicInvokeExpr getDynamicStringConcat(
      String concatenatedString, List<Type> types, List<Immediate> args) {
    List<Type> bootstrapTypes = new ArrayList<>();
    bootstrapTypes.add(
        new JavaClassType("MethodHandles$Lookup", new PackageName("java.lang.invoke")));
    bootstrapTypes.add(new JavaClassType("String", new PackageName("java.lang")));
    bootstrapTypes.add(new JavaClassType("MethodType", new PackageName("java.lang.invoke")));
    bootstrapTypes.add(new JavaClassType("String", new PackageName("java.lang")));
    bootstrapTypes.add(new ArrayType(new JavaClassType("Object", new PackageName("java.lang")), 1));

    Type stringType = new JavaClassType("String", new PackageName("java.lang"));

    MethodSignature bootstrap =
        new MethodSignature(
            new JavaClassType("StringConcatFactory", new PackageName("java.lang.invoke")),
            "makeConcatWithConstants",
            bootstrapTypes,
            new JavaClassType("CallSite", new PackageName("java.lang.invoke")));
    MethodSignature stringConcat =
        new MethodSignature(
            new JavaClassType("InvokeDynamic", new PackageName("sootup.dummy")),
            "makeConcatWithConstants",
            types,
            stringType);

    List<Immediate> bootstrapArgs = new ArrayList<>();
    bootstrapArgs.add(new StringConstant(concatenatedString, stringType));

    return Jimple.newDynamicInvokeExpr(bootstrap, bootstrapArgs, stringConcat, args);
  }

  @Test
  void testStringConcatNormalizerWithNoMatchingCases() {
    SootMethod filterMethod = getStringConcatClassMethod("<init>");

    Body.BodyBuilder builder =
        Body.builder(new MutableBlockStmtGraph(filterMethod.getBody().getStmtGraph()));

    List<Stmt> stmtsBefore = builder.getStmts();
    new StringConcatNormalizer().interceptBody(builder, view);
    List<Stmt> stmtsAfter = builder.getStmts();

    assertEquals(stmtsBefore, stmtsAfter);
  }

  private SootMethod getStringConcatClassMethod(String methodName) {
    MethodSignature methodSignature =
        view.getIdentifierFactory()
            .getMethodSignature(stringConcatClass.getType(), methodName, "void", new ArrayList<>());
    Optional<? extends SootMethod> methodOpt =
        stringConcatClass.getMethod(methodSignature.getSubSignature());
    assertTrue(methodOpt.isPresent());

    return methodOpt.get();
  }
}
