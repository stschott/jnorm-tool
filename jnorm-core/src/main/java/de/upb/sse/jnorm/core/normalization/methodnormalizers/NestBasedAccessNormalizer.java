package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import de.upb.sse.jnorm.core.util.SootUpHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.*;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.Body;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.views.View;

public class NestBasedAccessNormalizer implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    final String NEST_BASED_ACCESS_PATTERN = "access\\$\\d+";

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    for (Stmt stmt : bodyBuilder.getStmts()) {
      Value currentStmt = null;
      Value newValue = null;

      if (stmt instanceof JAssignStmt) {
        JAssignStmt assignStmt = (JAssignStmt) stmt;
        currentStmt = assignStmt.getRightOp();
      } else if (stmt instanceof JInvokeStmt) {
        JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
        Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
        if (!invokeExprOpt.isPresent()) continue;
        currentStmt = invokeExprOpt.get();
      }

      // Stmt is neither AssignStmt nor InvokeStmt
      if (currentStmt == null) continue;
      if (!(currentStmt instanceof AbstractInvokeExpr)) continue;
      AbstractInvokeExpr invokeExpr = (AbstractInvokeExpr) currentStmt;

      if (!(invokeExpr instanceof JStaticInvokeExpr)) continue;
      JStaticInvokeExpr staticInvokeExpr = (JStaticInvokeExpr) invokeExpr;
      List<Immediate> staticInvokeArgs = invokeExpr.getArgs();
      MethodSignature sm = staticInvokeExpr.getMethodSignature();

      if (!sm.getName().matches(NEST_BASED_ACCESS_PATTERN)) continue;
      StmtGraph<?> callTargetStmtGraph =
          view.getMethod(sm)
              .orElseThrow(() -> new RuntimeException("Soot method was not found: " + sm))
              .getBody()
              .getStmtGraph();

      // Get all relevant stmts from the bridge method
      // Relevant stmts are all stmts between parameterRefs and the return statement
      List<Stmt> relevantCallTargetStmts = new ArrayList<>();
      boolean parametersChecked = false;
      for (Stmt callTargetStmt : callTargetStmtGraph) {
        if (SootUpHelper.isParameterRef(callTargetStmt)) {
          Stmt nextStmt = callTargetStmtGraph.successors(callTargetStmt).get(0);
          if (!SootUpHelper.isParameterRef(nextStmt)) {
            parametersChecked = true;
            continue;
          }
        } else {
          parametersChecked = true;
        }
        // There are still parameters left
        if (!parametersChecked) continue;
        // Do not add the return to the relevant stmts
        if (callTargetStmt instanceof JReturnStmt) break;
        if (callTargetStmt instanceof JReturnVoidStmt) break;

        relevantCallTargetStmts.add(callTargetStmt);
      }

      if (relevantCallTargetStmts.size() < 1) continue;
      Stmt relevantStmt = relevantCallTargetStmts.get(relevantCallTargetStmts.size() - 1);

      Value relevantExpression = null;
      boolean fieldAccess = false;
      if (relevantStmt instanceof JAssignStmt) {
        JAssignStmt relevantStmtAssignStmt = (JAssignStmt) relevantStmt;
        Value rightOp = relevantStmtAssignStmt.getRightOp();
        Value leftOp = relevantStmtAssignStmt.getLeftOp();
        if (rightOp instanceof Local) {
          relevantExpression = leftOp;
          fieldAccess = true;
        } else {
          relevantExpression = rightOp;
        }
      } else if (relevantStmt instanceof JInvokeStmt) {
        JInvokeStmt relevantStmtInvokeStmt = (JInvokeStmt) relevantStmt;
        Optional<AbstractInvokeExpr> invokeExprOpt = relevantStmtInvokeStmt.getInvokeExpr();
        if (!invokeExprOpt.isPresent()) continue;

        relevantExpression = invokeExprOpt.get();
      }

      if (relevantExpression instanceof JFieldRef) {
        // relevant stmt refers to a private class field
        JFieldRef relevantStmtInstanceFieldRef = (JFieldRef) relevantExpression;
        FieldSignature relevantFieldRef = relevantStmtInstanceFieldRef.getFieldSignature();

        if (relevantExpression instanceof JInstanceFieldRef) {
          newValue = Jimple.newInstanceFieldRef((Local) staticInvokeArgs.get(0), relevantFieldRef);
        } else if (relevantExpression instanceof JStaticFieldRef) {
          newValue = Jimple.newStaticFieldRef(relevantFieldRef);
        }
      } else if (relevantExpression instanceof AbstractInvokeExpr) {
        // relevant stmt refers to a private method
        AbstractInvokeExpr relevantInvokeExpr = (AbstractInvokeExpr) relevantExpression;
        MethodSignature relevantMethodRef = relevantInvokeExpr.getMethodSignature();

        if (relevantInvokeExpr instanceof JStaticInvokeExpr) {
          newValue = Jimple.newStaticInvokeExpr(relevantMethodRef, staticInvokeArgs);
        } else if (relevantInvokeExpr instanceof JVirtualInvokeExpr) {
          newValue =
              Jimple.newVirtualInvokeExpr(
                  (Local) staticInvokeArgs.get(0),
                  relevantMethodRef,
                  staticInvokeArgs.subList(1, staticInvokeArgs.size()));
        } else if (relevantExpression instanceof JSpecialInvokeExpr) {
          newValue =
              Jimple.newSpecialInvokeExpr(
                  (Local) staticInvokeArgs.get(0),
                  relevantMethodRef,
                  staticInvokeArgs.subList(1, staticInvokeArgs.size()));
        }
      }

      if (newValue == null) continue;

      if (stmt instanceof JAssignStmt) {
        // Current stmt is an AssignStmt
        JAssignStmt currentAssignStmt = (JAssignStmt) stmt;
        stmtGraph.replaceNode(currentAssignStmt, currentAssignStmt.withRValue(newValue));
        tracker.recordModification(bodyBuilder.getMethodSignature().toString());
      } else {
        // Current stmt is an InvokeStmt
        JInvokeStmt currentInvokeStmt = (JInvokeStmt) stmt;
        if (!fieldAccess) {
          assert newValue instanceof AbstractInvokeExpr;
          stmtGraph.replaceNode(
              currentInvokeStmt, currentInvokeStmt.withInvokeExpr((AbstractInvokeExpr) newValue));
        } else if (staticInvokeArgs.size() > 1) {
          Stmt newFieldAccess =
              Jimple.newAssignStmt(
                  (LValue) newValue,
                  staticInvokeArgs.get(1),
                  StmtPositionInfo.getNoStmtPositionInfo());
          stmtGraph.replaceNode(stmt, newFieldAccess);
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
