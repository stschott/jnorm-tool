package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.*;
import java.util.stream.Stream;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.ReplaceUseStmtVisitor;
import sootup.core.model.Body;
import sootup.core.views.View;

public class CustomUnusedLocalNormalizer implements MethodNormalizer {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    Set<Local> usedLocals = new HashSet<>();
    //        Set<Local> unusedLocals = new HashSet<>();
    Set<Stmt> redundantStmts = new HashSet<>();
    Map<Local, Local> localMap = new HashMap<>();

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    stmtGraph
        .getStmts()
        .forEach(
            stmt -> {
              if (!(stmt instanceof JAssignStmt assignStmt)) return;
              if (!(assignStmt.getLeftOp() instanceof Local leftLocal)) return;
              if (!(assignStmt.getRightOp() instanceof Local rightLocal)) return;

              localMap.put(leftLocal, rightLocal);
              redundantStmts.add(assignStmt);
            });

    // remove redundant l1 = l2 stmts
    redundantStmts.forEach(stmtGraph::removeNode);

    // Replace uses
    stmtGraph
        .getStmts()
        .forEach(
            stmt -> {
              localMap.forEach(
                  (l1, l2) -> {
                    ReplaceUseStmtVisitor replaceUseStmtVisitor = new ReplaceUseStmtVisitor(l1, l2);
                    stmt.accept(replaceUseStmtVisitor);
                    Stmt stmtWithReplacedUses = replaceUseStmtVisitor.getResult();
                    if (!stmtGraph.containsNode(stmt)) return;
                    stmtGraph.replaceNode(stmt, stmtWithReplacedUses);
                    //                unusedLocals.add(l1);
                  });
            });

    // Remove locals
    //        unusedLocals.forEach(local -> bodyBuilder.getLocals().remove(local));

    // remove unused locals
    stmtGraph
        .getStmts()
        .forEach(
            stmt -> {
              Stream<Value> usesAndDefs = stmt.getUsesAndDefs();
              usesAndDefs.forEach(
                  use -> {
                    if (!(use instanceof Local)) return;
                    usedLocals.add((Local) use);
                  });
            });

    if (!usedLocals.equals(bodyBuilder.getLocals())) {
      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }

    bodyBuilder.setLocals(usedLocals);
  }

  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
