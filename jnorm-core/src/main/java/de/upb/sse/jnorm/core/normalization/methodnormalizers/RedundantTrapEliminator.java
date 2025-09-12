package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import sootup.core.graph.MutableBasicBlock;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.signatures.PackageName;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.core.types.JavaClassType;

public class RedundantTrapEliminator implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    final ClassType THROWABLE_CLASS_TYPE =
        new JavaClassType("Throwable", new PackageName("java.lang"));

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();
    Map<Stmt, ClassType> exceptionalEdgesToRemove = new HashMap<>();

    stmtGraph
        .getBlocks()
        .forEach(
            block -> {
              Stmt headStmt = block.getHead();
              Map<ClassType, MutableBasicBlock> exceptionalSuccessors =
                  (Map<ClassType, MutableBasicBlock>) block.getExceptionalSuccessors();

              exceptionalSuccessors.forEach(
                  (classType, exceptionHandlingBlock) -> {
                    if (!classType.equals(THROWABLE_CLASS_TYPE)) return;

                    Stmt exceptionHeadStmt = exceptionHandlingBlock.getHead();

                    // check if the exception handler block is the same as the handled block (by
                    // checking equality oh their
                    // head statements)
                    if (headStmt != exceptionHeadStmt) return;

                    // Check if the block head is a @caughtexception
                    if (!(exceptionHeadStmt instanceof JIdentityStmt identityStmt)) return;
                    if (!(identityStmt.getRightOp() instanceof JCaughtExceptionRef)) return;

                    for (int i = 1; i < exceptionHandlingBlock.getStmtCount(); i++) {
                      Stmt currStmt = exceptionHandlingBlock.getStmts().get(i);

                      if (!(currStmt instanceof JAssignStmt assignStmt)) return;
                      if (!(assignStmt.getLeftOp() instanceof Local)) return;
                      if (!(assignStmt.getRightOp() instanceof Local)) return;
                    }

                    //                List<MutableBasicBlock> predecessors =
                    // (List<MutableBasicBlock>) block.getPredecessors();
                    //                for (MutableBasicBlock predecessor : predecessors) {
                    //                    if (predecessor ==  exceptionHandlingBlock) continue;
                    //
                    //                    Map<ClassType, MutableBasicBlock>
                    // exceptionalSuccessorsOfPredecessor = predecessor.getExceptionalSuccessors();
                    //                    for (Map.Entry<ClassType, MutableBasicBlock> entry :
                    // exceptionalSuccessorsOfPredecessor.entrySet()) {
                    //                        if (entry.getKey().equals(THROWABLE_CLASS_TYPE) &&
                    // entry.getValue() == exceptionHandlingBlock) return;
                    //                    }
                    //                }

                    //                // Don't remove reflexive exceptional edges (pred and succ are
                    // in the same block)
                    //                Stmt successorStmt =
                    // stmtGraph.exceptionalSuccessors(stmt).get(THROWABLE_CLASS_TYPE);
                    //                if (stmt.equivTo(successorStmt)) return;

                    // If we reached here it means that we have an exception handler block where the
                    // first stmt is a
                    // @caughtexception and the remaining ones are just local to local assignments
                    // (l1 = l3)
                    exceptionalEdgesToRemove.put(exceptionHeadStmt, THROWABLE_CLASS_TYPE);
                  });
              //
            });

    exceptionalEdgesToRemove.forEach(
        (stmt, type) -> {
          stmtGraph.removeExceptionalEdge(stmt, type);
          tracker.recordModification(bodyBuilder.getMethodSignature().toString());
        });
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
