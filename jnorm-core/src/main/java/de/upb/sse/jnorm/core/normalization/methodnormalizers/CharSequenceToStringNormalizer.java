package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.common.LValue;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.signatures.PackageName;
import sootup.core.views.View;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;

public class CharSequenceToStringNormalizer implements MethodNormalizer {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    JavaClassType objectClassType = new JavaClassType("Object", new PackageName("java.lang"));
    JavaClassType stringClassType = new JavaClassType("String", new PackageName("java.lang"));

    MethodSubSignature toStringSubSignature =
        new MethodSubSignature("toString", Collections.emptyList(), stringClassType);
    MethodSignature toStringMethodSignature =
        new MethodSignature(
            new JavaClassType("CharSequence", new PackageName("java.lang")), toStringSubSignature);

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();
    boolean modified = false;

    for (Stmt stmt : stmtGraph.getStmts()) {
      Stmt virtualInvokeCharSequence = null;

      if (stmt instanceof JAssignStmt) {
        JAssignStmt assignStmt = (JAssignStmt) stmt;
        LValue lhs = assignStmt.getLeftOp();
        Value rhs = assignStmt.getRightOp();
        if (rhs == null) continue;

        if (!(rhs instanceof AbstractInvokeExpr)) continue;

        AbstractInvokeExpr invokeExpr = (AbstractInvokeExpr) rhs;
        if (!(invokeExpr instanceof JInterfaceInvokeExpr)) continue;

        JInterfaceInvokeExpr interfaceInvokeExpr = (JInterfaceInvokeExpr) invokeExpr;
        if (!interfaceInvokeExpr.getMethodSignature().equals(toStringMethodSignature)) continue;

        SootMethod newToStringMethod =
            JavaSootMethod.JavaSootMethodBuilder.builder()
                .withSignature(new MethodSignature(objectClassType, toStringSubSignature))
                .withModifier(Collections.emptyList())
                .build();

        JVirtualInvokeExpr virtualInvokeExpr =
            Jimple.newVirtualInvokeExpr(
                interfaceInvokeExpr.getBase(), newToStringMethod.getSignature());

        JAssignStmt newAssignStmt =
            Jimple.newAssignStmt(lhs, virtualInvokeExpr, stmt.getPositionInfo());
        virtualInvokeCharSequence = newAssignStmt;
      } else if (stmt instanceof JInvokeStmt) {
        JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
        Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
        if (!invokeExprOpt.isPresent()) continue;

        AbstractInvokeExpr invokeExpr = invokeExprOpt.get();
        if (!(invokeExpr instanceof JInterfaceInvokeExpr)) continue;

        JInterfaceInvokeExpr interfaceInvokeExpr = (JInterfaceInvokeExpr) invokeExpr;
        if (!interfaceInvokeExpr.getMethodSignature().equals(toStringMethodSignature)) continue;

        SootMethod newToStringMethod =
            JavaSootMethod.JavaSootMethodBuilder.builder()
                .withSignature(new MethodSignature(objectClassType, toStringSubSignature))
                .withModifier(Collections.emptyList())
                .build();

        JVirtualInvokeExpr virtualInvokeExpr =
            Jimple.newVirtualInvokeExpr(
                interfaceInvokeExpr.getBase(), newToStringMethod.getSignature());

        JInvokeStmt newInvokeStmt = Jimple.newInvokeStmt(virtualInvokeExpr, stmt.getPositionInfo());
        virtualInvokeCharSequence = newInvokeStmt;
      }

      if (virtualInvokeCharSequence != null) {
        stmtGraph.replaceNode(stmt, virtualInvokeCharSequence);
        modified = true;
      }
    }

    if (modified) {
      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }
  }

  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
