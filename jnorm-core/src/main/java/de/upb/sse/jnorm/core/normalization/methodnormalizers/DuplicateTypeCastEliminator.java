package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.views.View;

public class DuplicateTypeCastEliminator implements MethodNormalizer {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();
    boolean modified = false;

    for (Stmt stmt : stmtGraph.getStmts()) {
      if (!(stmt instanceof JAssignStmt)) continue;
      JAssignStmt assignStmt = (JAssignStmt) stmt;

      Value rhs = assignStmt.getRightOp();
      if (!(rhs instanceof JCastExpr)) continue;

      JCastExpr rhsCast = (JCastExpr) rhs;
      Value castOp = rhsCast.getOp();
      if (!(castOp instanceof JCastExpr)) continue;

      JCastExpr innerCast = (JCastExpr) castOp;
      if (!rhsCast.getType().equals(innerCast.getType())) continue;

      JAssignStmt newAssignStmt = assignStmt.withRValue(innerCast.getOp());
      stmtGraph.replaceNode(stmt, newAssignStmt);
      modified = true;
    }

    if (modified) {
      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }
  }

  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
