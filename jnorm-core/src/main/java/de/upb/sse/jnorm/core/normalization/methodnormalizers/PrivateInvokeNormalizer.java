package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.Optional;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.LValue;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.Expr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.Body;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.views.View;

public class PrivateInvokeNormalizer implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    for (Stmt stmt : bodyBuilder.getStmts()) {
      Stmt specialInvokePrivate = null;
      if (stmt instanceof JAssignStmt) {
        JAssignStmt assign = (JAssignStmt) stmt;
        LValue lhs = assign.getLeftOp();
        Value rhs = assign.getRightOp();

        if (!(rhs instanceof JVirtualInvokeExpr)) continue;

        JVirtualInvokeExpr virtualInvoke = (JVirtualInvokeExpr) rhs;
        MethodSignature calledMethodSignature = ((JVirtualInvokeExpr) rhs).getMethodSignature();

        Optional<? extends SootMethod> calledMethodOpt = view.getMethod(calledMethodSignature);

        if (!calledMethodOpt.isPresent()) continue;

        SootMethod calledMethod = calledMethodOpt.get();

        // Check if called method is private
        if (!calledMethod.isPrivate()) continue;

        // Create new AssignStmt that contains a specialInvoke
        specialInvokePrivate =
            Jimple.newAssignStmt(
                lhs,
                Jimple.newSpecialInvokeExpr(
                    virtualInvoke.getBase(), calledMethodSignature, virtualInvoke.getArgs()),
                StmtPositionInfo.getNoStmtPositionInfo());
      } else if (stmt instanceof JInvokeStmt) {
        JInvokeStmt invoke = (JInvokeStmt) stmt;
        Optional<AbstractInvokeExpr> invokeExprOpt = invoke.getInvokeExpr();
        if (!invokeExprOpt.isPresent()) continue;

        Expr expression = invokeExprOpt.get();

        // Continue with loop if no virtualinvoke
        if (!(expression instanceof JVirtualInvokeExpr)) continue;

        JVirtualInvokeExpr virtualInvoke = (JVirtualInvokeExpr) expression;
        MethodSignature calledMethodSignature = virtualInvoke.getMethodSignature();
        Optional<? extends SootMethod> calledMethodOpt = view.getMethod(calledMethodSignature);

        if (!calledMethodOpt.isPresent()) continue;
        SootMethod calledMethod = calledMethodOpt.get();

        // Check if called method is private
        if (!calledMethod.isPrivate()) continue;

        // Create a new InvokeStmt that contains a specialinvoke
        specialInvokePrivate =
            Jimple.newInvokeStmt(
                Jimple.newSpecialInvokeExpr(
                    virtualInvoke.getBase(), calledMethodSignature, virtualInvoke.getArgs()),
                StmtPositionInfo.getNoStmtPositionInfo());
      }

      if (specialInvokePrivate != null) {
        stmtGraph.replaceNode(stmt, specialInvokePrivate);
        tracker.recordModification(bodyBuilder.getMethodSignature().toString());
      }
    }
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
