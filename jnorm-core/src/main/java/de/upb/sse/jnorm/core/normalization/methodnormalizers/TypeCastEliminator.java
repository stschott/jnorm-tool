package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.normalization.AggressiveMethodNormalizer;
import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.*;
import sootup.core.graph.BasicBlock;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JNopStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.ReplaceUseStmtVisitor;
import sootup.core.model.Body;
import sootup.core.types.PrimitiveType;
import sootup.core.views.View;

public class TypeCastEliminator implements AggressiveMethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    Map<Local, Local> localAliases = new HashMap<>();
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    // Find type cast statements and build local aliases
    List<Stmt> typeCastStmts = new ArrayList<>();
    for (Stmt stmt : stmtGraph.getStmts()) {
      if (!(stmt instanceof JAssignStmt assignStmt)) continue;

      Value lhs = assignStmt.getLeftOp();
      Value rhs = assignStmt.getRightOp();
      if (!(rhs instanceof JCastExpr castExpr)) continue;
      if (castExpr.getType() instanceof PrimitiveType) continue;

      Value castOp = castExpr.getOp();
      if (lhs instanceof Local oldLocal && castOp instanceof Local newLocal) {
        // Handle transitive aliases (a = b, b = c -> a = c)
        Local targetLocal = localAliases.getOrDefault(newLocal, newLocal);
        localAliases.put(oldLocal, targetLocal);
        typeCastStmts.add(stmt);
      }
    }

    Set<Stmt> stmtsToRemove = new HashSet<>();
    for (Stmt typeCastStmt : typeCastStmts) {
      // Remove the exceptional block if it no longer handles any statements
      if (stmtGraph.getBlockOf(typeCastStmt).getStmts().size() == 1)
        stmtGraph
            .exceptionalSuccessors(typeCastStmt)
            .values()
            .forEach(
                s -> {
                  BasicBlock<?> excBlock = stmtGraph.getBlockOf(s);
                  stmtsToRemove.addAll(excBlock.getStmts());

                  // Remove blocks that are reachable only from the exceptional block being removed.
                  stmtsToRemove.addAll(
                      stmtGraph.getBlocks().stream()
                          .filter(
                              block -> {
                                List<?> predecessors = block.getPredecessors();
                                return predecessors.size() == 1
                                    && predecessors.get(0).equals(excBlock);
                              })
                          .flatMap(succBlock -> succBlock.getStmts().stream())
                          .toList());
                });

      // Add the type cast statements to the remove list
      stmtsToRemove.add(typeCastStmt);
    }

    for (BasicBlock<?> block : stmtGraph.getBlocks()) {
      if (block.isEmpty()) stmtGraph.removeBlock(block);
    }

    // Replace uses and definitions of type-casted locals
    Map<Stmt, Stmt> stmtReplacementPairs = new HashMap<>();
    for (Map.Entry<Local, Local> entry : localAliases.entrySet()) {
      Local oldLocal = entry.getKey();
      Local newLocal = entry.getValue();

      for (Stmt stmt : stmtGraph.getStmts()) {
        // Get latest version of statement from previous replacements
        Stmt currentStmt = stmtReplacementPairs.getOrDefault(stmt, stmt);

        // Replace uses
        ReplaceUseStmtVisitor replaceUseStmtVisitor = new ReplaceUseStmtVisitor(oldLocal, newLocal);
        currentStmt.accept(replaceUseStmtVisitor);
        Stmt stmtWithReplacedUses = replaceUseStmtVisitor.getResult();

        // Check if this statement also defines the old local
        Stmt finalStmt = stmtWithReplacedUses;
        if (stmtWithReplacedUses instanceof JAssignStmt assignStmt
            && assignStmt.getLeftOp().equals(oldLocal)) {
          finalStmt = assignStmt.withNewDef(newLocal);
        }

        if (!finalStmt.equals(stmt)) {
          stmtReplacementPairs.put(stmt, finalStmt);
          tracker.recordModification(bodyBuilder.getMethodSignature().toString());
        }
      }
    }

    for (Map.Entry<Stmt, Stmt> entry : stmtReplacementPairs.entrySet()) {
      Stmt oldStmt = entry.getKey();
      Stmt newStmt = entry.getValue();
      stmtGraph.replaceNode(oldStmt, newStmt);

      if (stmtsToRemove.contains(oldStmt)) {
        stmtsToRemove.remove(oldStmt);
        stmtsToRemove.add(newStmt);
      }
    }

    // Handle statement removals
    for (Stmt stmt : stmtGraph.getStmts()) {
      if (!(stmt instanceof JAssignStmt assignStmt)) continue;
      if (!stmtsToRemove.contains(assignStmt)) continue;

      bodyBuilder.removeDefLocalsOf(assignStmt);

      if (stmtGraph.getBlockOf(assignStmt).getStmts().size() == 1) {
        stmtGraph.replaceNode(assignStmt, new JNopStmt(StmtPositionInfo.getNoStmtPositionInfo()));
      } else {
        stmtGraph.removeNode(assignStmt);
      }
    }
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
