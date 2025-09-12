package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.model.BufferCall;
import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.Body;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.core.views.View;

public class BufferMethodCallNormalizer implements MethodNormalizer {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    final String BUFFER = "java.nio.Buffer";
    final String SUB_BUFFER_REGEX = "java\\.nio\\.[\\w]+Buffer";
    final String BUFFER_REGEX = "java\\.nio\\.[\\w]*Buffer";

    List<BufferCall> bufferUsages = new ArrayList<>();

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();
    boolean modified = false;

    for (Stmt stmt : stmtGraph.getStmts()) {
      if (stmt instanceof JAssignStmt) {
        JAssignStmt assignStmt = (JAssignStmt) stmt;
        sootup.core.jimple.common.LValue lhs = assignStmt.getLeftOp();
        sootup.core.jimple.common.Value rhs = assignStmt.getRightOp();

        if (!(rhs instanceof JVirtualInvokeExpr)) continue;
        JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) rhs;

        // Check if the stmt is a buffer method
        if (!virtualInvokeExpr
            .getMethodSignature()
            .getDeclClassType()
            .toString()
            .matches(BUFFER_REGEX)) continue;
        ClassType bufferClass = virtualInvokeExpr.getMethodSignature().getDeclClassType();
        MethodSignature bufferMethod = virtualInvokeExpr.getMethodSignature();

        if (!bufferMethod.getType().toString().matches(BUFFER_REGEX)) continue;

        MethodSignature oldBufferMethodSignature = virtualInvokeExpr.getMethodSignature();
        String methodName = oldBufferMethodSignature.getName();
        List<Type> parameterTypes = oldBufferMethodSignature.getParameterTypes();
        ClassType declClassType = oldBufferMethodSignature.getDeclClassType();

        Stmt bufferInvoke =
            Jimple.newAssignStmt(
                lhs,
                Jimple.newVirtualInvokeExpr(
                    virtualInvokeExpr.getBase(),
                    new MethodSignature(
                        declClassType,
                        methodName,
                        parameterTypes,
                        view.getIdentifierFactory().getType(BUFFER)),
                    virtualInvokeExpr.getArgs()),
                StmtPositionInfo.getNoStmtPositionInfo());

        stmtGraph.replaceNode(stmt, bufferInvoke);
        bufferUsages.add(new BufferCall((Local) lhs, bufferClass));
        modified = true;
      } else if (stmt instanceof JInvokeStmt) {
        JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
        Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
        if (!invokeExprOpt.isPresent()) continue;
        AbstractInvokeExpr invokeExpr = invokeExprOpt.get();
        if (!(invokeExpr instanceof JVirtualInvokeExpr)) continue;

        JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) invokeExpr;
        Value base = virtualInvokeExpr.getBase();

        MethodSignature oldBufferMethodSignature = virtualInvokeExpr.getMethodSignature();
        String methodName = oldBufferMethodSignature.getName();
        List<Type> parameterTypes = oldBufferMethodSignature.getParameterTypes();
        ClassType declClassType = oldBufferMethodSignature.getDeclClassType();

        // No previous Buffer Assignment, but still a virtualinvoke of a buffer method
        if (bufferUsages.isEmpty()) {
          // Check if the stmt is a buffer method
          if (!virtualInvokeExpr
              .getMethodSignature()
              .getDeclClassType()
              .toString()
              .matches(SUB_BUFFER_REGEX)) continue;
          MethodSignature bufferMethod = virtualInvokeExpr.getMethodSignature();

          if (!bufferMethod.getType().toString().matches(BUFFER_REGEX)) continue;
          Stmt bufferInvoke =
              Jimple.newInvokeStmt(
                  Jimple.newVirtualInvokeExpr(
                      virtualInvokeExpr.getBase(),
                      new MethodSignature(
                          declClassType,
                          methodName,
                          parameterTypes,
                          view.getIdentifierFactory().getType(BUFFER)),
                      virtualInvokeExpr.getArgs()),
                  StmtPositionInfo.getNoStmtPositionInfo());

          stmtGraph.replaceNode(stmt, bufferInvoke);
          modified = true;
        }

        for (BufferCall bc : bufferUsages) {
          if (!bc.getLocal().equals(base)) continue;

          Stmt bufferInvoke =
              Jimple.newInvokeStmt(
                  Jimple.newVirtualInvokeExpr(
                      virtualInvokeExpr.getBase(),
                      virtualInvokeExpr.getMethodSignature(),
                      virtualInvokeExpr.getArgs()),
                  StmtPositionInfo.getNoStmtPositionInfo());

          stmtGraph.replaceNode(stmt, bufferInvoke);
          modified = true;
        }
      }
    }

    // fix locals
    for (BufferCall bc : bufferUsages) {
      bodyBuilder.replaceLocal(bc.getLocal(), bc.getLocal().withType(bc.getBuffer()));
    }

    if (modified) {
      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }
  }

  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
