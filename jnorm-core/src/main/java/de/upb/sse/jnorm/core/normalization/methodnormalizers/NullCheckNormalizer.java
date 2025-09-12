package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
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

public class NullCheckNormalizer implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    JavaClassType objectClass = new JavaClassType("Object", new PackageName("java.lang"));
    JavaClassType classClass = new JavaClassType("Class", new PackageName("java.lang"));

    final MethodSignature requireNonNullMethodSignature =
        new MethodSignature(
            new JavaClassType("Objects", new PackageName("java.util")),
            new MethodSubSignature(
                "requireNonNull", Collections.singletonList(objectClass), objectClass));

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    for (Stmt stmt : stmtGraph.getStmts()) {
      if (!(stmt instanceof JInvokeStmt)) continue;
      JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
      Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
      if (!invokeExprOpt.isPresent()) continue;
      AbstractInvokeExpr invokeExpr = invokeExprOpt.get();

      if (!(invokeExpr instanceof JStaticInvokeExpr)) continue;
      JStaticInvokeExpr staticInvokeExpr = (JStaticInvokeExpr) invokeExpr;
      MethodSignature invokedMethodSignature = staticInvokeExpr.getMethodSignature();

      if (!(invokedMethodSignature.equals(requireNonNullMethodSignature))) continue;

      SootMethod newRequireNonNullMethod =
          JavaSootMethod.JavaSootMethodBuilder.builder()
              .withSignature(
                  new MethodSignature(
                      new JavaClassType("Object", new PackageName("java.lang")),
                      new MethodSubSignature("getClass", Collections.emptyList(), classClass)))
              .withModifier(Collections.emptyList())
              .build();

      JVirtualInvokeExpr normalizedRequireNonNullMethod =
          Jimple.newVirtualInvokeExpr(
              (Local) staticInvokeExpr.getArg(0), newRequireNonNullMethod.getSignature());
      JInvokeStmt newInvokeStmt =
          Jimple.newInvokeStmt(normalizedRequireNonNullMethod, stmt.getPositionInfo());

      stmtGraph.replaceNode(stmt, newInvokeStmt);
      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
