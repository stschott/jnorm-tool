package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.*;
import sootup.core.frontend.OverridingBodySource;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.*;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.stmt.FallsThroughStmt;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.MethodModifier;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.core.views.View;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;

public class EnumNormalizer implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    MethodSignature methodSignature = bodyBuilder.getMethodSignature();
    JavaSootClass declaringClass =
        (JavaSootClass) view.getClassOrThrow(methodSignature.getDeclClassType());

    // Check if the method is a static initializer and if the class has a $values method
    if (!view.getIdentifierFactory()
            .isStaticInitializerSubSignature(methodSignature.getSubSignature())
        || !declaringClass.getMethodsByName("$values").isEmpty()) {
      return;
    }

    if (!declaringClass.isEnum()) return;

    Optional<JavaSootField> enumValuesOpt = declaringClass.getField("$VALUES");
    if (!enumValuesOpt.isPresent()) return;

    Type enumType = enumValuesOpt.get().getType();
    if (enumType instanceof ArrayType) {
      enumType = ((ArrayType) enumType).getBaseType();
    }

    List<Stmt> statementsToMove = new ArrayList<>();
    List<Stmt> statementsToRemove = new ArrayList<>();
    JAssignStmt enumInit = null;
    LValue enumRef = null;
    int enumSize = 0;

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    // Find the enum array initialization
    for (Stmt stmt : stmtGraph.getStmts()) {
      if (!(stmt instanceof JAssignStmt)) continue;
      JAssignStmt assignStmt = (JAssignStmt) stmt;
      enumRef = assignStmt.getLeftOp();
      Value rhs = assignStmt.getRightOp();

      if (!(rhs instanceof JNewArrayExpr)) continue;
      JNewArrayExpr newArrayExpr = (JNewArrayExpr) rhs;

      if (!newArrayExpr.getBaseType().equals(enumType)) continue;
      enumSize = ((IntConstant) newArrayExpr.getSize()).getValue();
      enumInit = assignStmt;

      // Enum init statement has been found if loop reaches this point
      // Break is intentional, as only the first enum init has to be moved
      break;
    }

    if (enumInit == null) {
      return; // Exit if no array initialization was found
    }

    // get all enum calls
    Stmt currStmt = enumInit;
    for (int i = 0; i < enumSize * 2; i++) {
      currStmt = stmtGraph.successors(currStmt).get(0);

      if (!(currStmt instanceof JAssignStmt)) continue;
      JAssignStmt currAssign = (JAssignStmt) currStmt;
      Value lhs = currAssign.getLeftOp();

      Value newL = lhs;
      if (lhs instanceof JArrayRef) {
        newL = ((JArrayRef) lhs).withBase((Local) enumInit.getLeftOp());
      }

      statementsToMove.add(((JAssignStmt) currStmt).withVariable((LValue) newL));
      statementsToRemove.add(currStmt);
    }

    // Create new $values method
    ClassType enumClassType = declaringClass.getType();
    MethodSubSignature newMethodSubSignature =
        new MethodSubSignature(
            "$values", new ArrayList<>(), ArrayType.createArrayType(enumType, 1));
    MethodSignature newMethodSignature = new MethodSignature(enumClassType, newMethodSubSignature);

    // Create the body of the new method
    MutableBlockStmtGraph newStmtGraph = new MutableBlockStmtGraph();
    newStmtGraph.setStartingStmt(statementsToMove.get(0));
    // statementsToMove.remove(0);
    for (int i = 0; i < statementsToMove.size() - 1; i++) {
      newStmtGraph.putEdge((FallsThroughStmt) statementsToMove.get(i), statementsToMove.get(i + 1));
    }
    newStmtGraph.putEdge(
        (FallsThroughStmt) statementsToMove.get(statementsToMove.size() - 1),
        Jimple.newReturnStmt((Immediate) enumRef, StmtPositionInfo.getNoStmtPositionInfo()));

    Body.BodyBuilder newMethodBodyBuilder =
        Body.builder(newStmtGraph).setMethodSignature(newMethodSignature);
    Body newMethodBody = newMethodBodyBuilder.build();

    JavaSootMethod newEnumMethod =
        new JavaSootMethod(
            new OverridingBodySource(newMethodSignature, newMethodBody),
            newMethodSignature,
            EnumSet.of(MethodModifier.PRIVATE, MethodModifier.STATIC),
            Collections.emptyList(),
            Collections.emptyList(),
            NoPositionInformation.getInstance());

    // Add the new method to the declaring class
    // currently disabled: instead we remove the EnumValuesMethod via the EnumValuesMethodRemover
    //    ((MutableJavaView) view).addMethod(newEnumMethod);

    // Insert call to the new $values method
    JStaticInvokeExpr staticInvokeExpr = Jimple.newStaticInvokeExpr(newEnumMethod.getSignature());
    JAssignStmt newEnumMethodInvoke =
        Jimple.newAssignStmt(enumRef, staticInvokeExpr, StmtPositionInfo.getNoStmtPositionInfo());

    // Replace the first statement related to enum initialization with the new invoke statement
    stmtGraph.replaceNode(
        stmtGraph.predecessors(statementsToRemove.get(0)).get(0), newEnumMethodInvoke);
    //    statementsToRemove.remove(0); // Remove the first element as it has been replaced

    // Remove the remaining old statements
    for (Stmt stmt : statementsToRemove) {
      stmtGraph.removeNode(stmt);
    }
    tracker.recordModification(bodyBuilder.getMethodSignature().toString());
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
