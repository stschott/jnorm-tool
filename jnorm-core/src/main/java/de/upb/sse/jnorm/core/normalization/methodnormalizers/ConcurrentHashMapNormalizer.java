package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.*;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.basic.LocalGenerator;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.ReplaceUseStmtVisitor;
import sootup.core.model.Body;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.views.View;

/**
 * This normalizer normalizes invokes of java.util.concurrent.ConcurrentHashMap:
 * java.util.concurrent.ConcurrentHashMap$KeySetView keySet() and
 * java.util.concurrent.ConcurrentHashMap$KeySetView: java.util.Iterator iterator() to use the old
 * API This has been changed from JDK7 -> JDK8
 */
public class ConcurrentHashMapNormalizer implements MethodNormalizer {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder builder, View view) {
    MethodSignature CONCURRENT_HASH_MAP_KEY_SET_METHOD =
        view.getIdentifierFactory()
            .parseMethodSignature(
                "<java.util.concurrent.ConcurrentHashMap: java.util.concurrent.ConcurrentHashMap$KeySetView keySet()>");
    MethodSignature CONCURRENT_HASH_MAP_KEY_SET_METHOD_PREV =
        view.getIdentifierFactory()
            .parseMethodSignature(
                "<java.util.concurrent.ConcurrentHashMap: java.util.Set keySet()>");
    MethodSignature CONCURRENT_HASH_MAP_ITERATOR_METHOD =
        view.getIdentifierFactory()
            .parseMethodSignature(
                "<java.util.concurrent.ConcurrentHashMap$KeySetView: java.util.Iterator iterator()>");
    MethodSignature CONCURRENT_HASH_MAP_ITERATOR_METHOD_PREV =
        view.getIdentifierFactory()
            .parseMethodSignature("<java.util.Set: java.util.Iterator iterator()>");

    ClassType CONCURRENT_HASH_MAP_TYPE =
        view.getIdentifierFactory().getClassType("java.util.ConcurrentHashMap");
    ClassType SET_TYPE = view.getIdentifierFactory().getClassType("java.util.Set");

    LocalGenerator localGen = new LocalGenerator(builder.getLocals());
    Map<Stmt, Stmt> stmtsToReplace = new HashMap<>();
    Map<Value, Value> valuesToReplace = new HashMap<>();
    MutableStmtGraph stmtGraph = builder.getStmtGraph();
    for (Stmt stmt : stmtGraph.getStmts()) {
      if (!(stmt instanceof JAssignStmt assignStmt)) continue;
      Optional<AbstractInvokeExpr> invokeExprOpt = assignStmt.getInvokeExpr();

      if (invokeExprOpt.isEmpty()) continue;
      AbstractInvokeExpr invokeExpr = invokeExprOpt.get();

      if (!(invokeExpr instanceof JVirtualInvokeExpr virtualInvokeExpr)) continue;

      if (virtualInvokeExpr.getMethodSignature().equals(CONCURRENT_HASH_MAP_KEY_SET_METHOD)) {
        Local newLocal = localGen.generateLocal(SET_TYPE);
        JVirtualInvokeExpr virtualInvokeExprNew =
            virtualInvokeExpr.withMethodSignature(CONCURRENT_HASH_MAP_KEY_SET_METHOD_PREV);
        JAssignStmt assignStmtNew =
            new JAssignStmt(
                newLocal, virtualInvokeExprNew, StmtPositionInfo.getNoStmtPositionInfo());
        stmtGraph.replaceNode(stmt, assignStmtNew);

        valuesToReplace.put(assignStmt.getLeftOp(), newLocal);
      } else if (virtualInvokeExpr
          .getMethodSignature()
          .equals(CONCURRENT_HASH_MAP_ITERATOR_METHOD)) {
        JInterfaceInvokeExpr interfaceInvokeExprNew =
            new JInterfaceInvokeExpr(
                virtualInvokeExpr.getBase(),
                CONCURRENT_HASH_MAP_ITERATOR_METHOD_PREV,
                virtualInvokeExpr.getArgs());
        JAssignStmt assignStmtNew =
            new JAssignStmt(
                assignStmt.getLeftOp(),
                interfaceInvokeExprNew,
                StmtPositionInfo.getNoStmtPositionInfo());
        stmtGraph.replaceNode(stmt, assignStmtNew);
      }
    }

    // we create a new local and need to replace all uses of the old one
    for (Map.Entry<Value, Value> entry : valuesToReplace.entrySet()) {
      ReplaceUseStmtVisitor visitor = new ReplaceUseStmtVisitor(entry.getKey(), entry.getValue());
      for (Stmt stmt : stmtGraph.getStmts()) {
        stmt.accept(visitor);
        Stmt newStmt = visitor.getResult();
        stmtsToReplace.put(stmt, newStmt);
      }
    }

    for (Map.Entry<Stmt, Stmt> entry : stmtsToReplace.entrySet()) {
      stmtGraph.replaceNode(entry.getKey(), entry.getValue());
    }
  }
}
