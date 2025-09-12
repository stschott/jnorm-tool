package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.exceptions.ArithmeticInterceptionException;
import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.Set;
import java.util.stream.Collectors;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.views.View;

public class ArithmeticNormalizer implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    for (Stmt stmt : stmtGraph.getStmts()) {
      for (sootup.core.jimple.common.Value use : stmt.getUses().collect(Collectors.toList())) {
        if (!(use instanceof JAddExpr)) continue;
        JAddExpr addition = (JAddExpr) use;

        for (Stmt predStmt : bodyBuilder.getStmtGraph().predecessors(stmt)) {
          if (!(predStmt instanceof JAssignStmt)) continue;
          JAssignStmt predAssignStmt = (JAssignStmt) predStmt;

          Value rhs = predAssignStmt.getRightOp();
          if (!(rhs instanceof JCastExpr)) continue;

          JCastExpr rhsCast = (JCastExpr) rhs;
          if (!(rhsCast.getOp() instanceof IntConstant)) continue;
          int op2Value = ((IntConstant) rhsCast.getOp()).getValue();

          if (op2Value >= 0) continue;

          // Create a SubExpr and replace the old AddExpr with it
          IntConstant newOp2 = IntConstant.getInstance(Math.abs(op2Value));
          JSubExpr subtraction = new JSubExpr(addition.getOp1(), newOp2);

          Stmt newStmt;
          if (stmt instanceof JAssignStmt) {
            newStmt = ((JAssignStmt) stmt).withRValue(subtraction);
          } else {
            newStmt = stmt.withNewUse(addition, subtraction);
          }

          if (newStmt == null) {
            throw new ArithmeticInterceptionException(stmt, addition, subtraction);
          }

          stmtGraph.removeNode(predStmt);
          stmtGraph.replaceNode(stmt, newStmt);
          tracker.recordModification(bodyBuilder.getMethodSignature().toString());
        }
      }
    }
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
