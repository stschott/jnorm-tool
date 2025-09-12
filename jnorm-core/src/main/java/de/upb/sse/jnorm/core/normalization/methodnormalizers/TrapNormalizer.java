package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.*;
import sootup.core.graph.MutableBasicBlock;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.types.ClassType;
import sootup.core.views.View;

public class TrapNormalizer implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    ClassType THROWABLE = view.getIdentifierFactory().getClassType("java.lang.Throwable");

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();
    //        stmtGraph.getBlocks().forEach((b -> {
    //            MutableBasicBlock block = (MutableBasicBlock) b;
    //            if (!(block.getTail() instanceof JGotoStmt)) return;
    //            if (block.getSuccessors().size() != 1) return;
    //            MutableBasicBlock nextBlock = block.getSuccessors().get(0);
    //            if (nextBlock.getStmtCount() != 1) return;
    //            if (!(nextBlock.getTail() instanceof JGotoStmt)) return;
    //
    //            block.replaceSuccessorBlock(nextBlock, nextBlock.getSuccessors().get(0));
    //            nextBlock.removePredecessorBlock(block);
    //        }));

    for (Stmt stmt : bodyBuilder.getStmts()) {
      if (!(stmt instanceof JInvokeStmt invokeStmt)) continue;
      if (invokeStmt.getInvokeExpr().isEmpty()) continue;
      MethodSubSignature methodSubSignature =
          invokeStmt.getInvokeExpr().get().getMethodSignature().getSubSignature();
      if (!methodSubSignature.getName().equals("close")
          || !methodSubSignature.getType().toString().equals("void")) continue;

      MutableBasicBlock relevantPredBlock = (MutableBasicBlock) stmtGraph.getBlockOf(stmt);
      while (relevantPredBlock.getPredecessors().size() == 1
          && (relevantPredBlock.getPredecessors().get(0).getTail() instanceof JIfStmt ifStmt)
          && (ifStmt.getCondition().getOp1() instanceof Local)
          && (ifStmt.getCondition().getOp2() instanceof NullConstant)) {
        relevantPredBlock = relevantPredBlock.getPredecessors().get(0);
      }
      if (relevantPredBlock.getPredecessors().isEmpty()) continue;
      relevantPredBlock =
          relevantPredBlock.getPredecessors().get(relevantPredBlock.getPredecessors().size() - 1);

      if (relevantPredBlock.getExceptionalSuccessors().containsKey(THROWABLE)) {
        relevantPredBlock.removeExceptionalSuccessorBlock(THROWABLE);
      }

      // This is hacky. It removes all "close" invocations and links the block to itself. This
      // destroys the control flow.
      for (int i = 0; i < relevantPredBlock.getSuccessors().size(); i++) {
        relevantPredBlock.replaceSuccessorBlock(
            relevantPredBlock.getSuccessors().get(i), relevantPredBlock);
      }
    }

    //        int i = 1;
    //        for (BasicBlock<?> block : stmtGraph.getBlocks()) {
    //            MutableBasicBlock mutableBlock = (MutableBasicBlock) block;
    //
    //            if (mutableBlock.getStmtCount() > 1) continue;
    //            Stmt headStmt = mutableBlock.getHead();
    //
    //            if (!(headStmt instanceof JIfStmt ifStmt)) continue;
    //
    //            List<MutableBasicBlock> predecessors = mutableBlock.getPredecessors();
    //            List<MutableBasicBlock> successors = mutableBlock.getSuccessors();
    //
    //            if (predecessors.size() > 1) continue;
    //            MutableBasicBlock predecessorBlock = predecessors.get(0);
    //            Stmt predecessorStmt = predecessorBlock.getTail();
    //            if (!(predecessorStmt instanceof JIfStmt predIfStmt)) continue;
    //
    //            boolean onlyCloseMethodSuccessors = successors.stream().allMatch(successor -> {
    //                Stmt stmt = successor.getHead();
    //                if (!(stmt instanceof JInvokeStmt invokeStmt)) return false;
    //
    //                var exprOpt = invokeStmt.getInvokeExpr();
    //                if (exprOpt.isEmpty()) return false;
    //
    //                var expr = exprOpt.get();
    //                if (!(expr instanceof JVirtualInvokeExpr virtualInvokeExpr)) return false;
    //
    //                return
    // virtualInvokeExpr.getMethodSignature().equals(INPUTSTREAM_CLOSE_METHOD);
    //            });
    //
    //            if (!onlyCloseMethodSuccessors) continue;
    //
    //            // If we reach here, we have an if stmt, where the predecessor is also an if
    // statement, and all
    //            // successors start with a "close" method call
    //            predecessorBlock.replaceSuccessorBlock(mutableBlock, successors.get(1));
    ////
    //            for (MutableBasicBlock successor : successors) {
    //                successor.replacePredecessorBlock(mutableBlock, predecessorBlock);
    //            }

    //            if (i == 0) {
    //                Map<ClassType, MutableBasicBlock> exceptionalPredecessors2 =
    // predecessorBlock.getExceptionalPredecessors();
    //                if (exceptionalPredecessors2.isEmpty()) continue;
    //                MutableBasicBlock exceptionalPredecessor2Block =
    // exceptionalPredecessors2.get(THROWABLE);
    //                if (exceptionalPredecessor2Block == null) continue;
    //                Map<ClassType, MutableBasicBlock> exceptionalPredecessors3 =
    // exceptionalPredecessor2Block.getExceptionalPredecessors();
    //                if (exceptionalPredecessors3.isEmpty()) continue;
    //                MutableBasicBlock exceptionalPredecessor3Block =
    // exceptionalPredecessors3.get(THROWABLE);
    //                if (exceptionalPredecessor3Block == null) continue;
    //
    //                exceptionalPredecessor3Block.removeExceptionalSuccessorBlock(THROWABLE);
    //
    //
    ////                exceptionalPredecessor3Block.linkExceptionalSuccessorBlock(THROWABLE,
    // predecessorBlock);
    ////                blocks2Remove.add(exceptionalPredecessor2Block);
    //
    //            }
    //
    ////            blocks2Remove.add(mutableBlock);
    //            i = i == 1 ? 0 : 1;
    //        }

    //        for (MutableBasicBlock block : blocks2Remove) {
    //            bodyBuilder.getStmtGraph().removeBlock(block);
    //        }

    //        stmtGraph.getBlocks().forEach(block -> {
    //            Stmt headStmt = block.getHead();
    //            Map<ClassType, MutableBasicBlock> exceptionalSuccessors = (Map<ClassType,
    // MutableBasicBlock>) block.getExceptionalSuccessors();
    //
    //            exceptionalSuccessors.forEach((classType, exceptionHandlingBlock) -> {
    ////                Stmt exceptionHeadStmt = exceptionHandlingBlock.getHead();
    //
    //                // check if the exception handler block is the same as the handled block (by
    // checking equality oh their
    //                // head statements)
    ////                if (headStmt != exceptionHeadStmt) return;
    //
    //                // Check if the block head is a close method call
    //                if (!(headStmt instanceof JInvokeStmt invokeStmt)) return;
    //                Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
    //                if (invokeExprOpt.isEmpty()) return;
    //                if (!(invokeExprOpt.get() instanceof JVirtualInvokeExpr virtualInvokeExpr))
    // return;
    //                if (!virtualInvokeExpr.getMethodSignature().equals(INPUTSTREAM_CLOSE_METHOD))
    // return;
    //
    ////                for (int i = 1; i < exceptionHandlingBlock.getStmtCount(); i++) {
    ////                    Stmt currStmt = exceptionHandlingBlock.getStmts().get(i);
    ////
    ////                    if (!(currStmt instanceof JAssignStmt assignStmt)) return;
    ////                    if (!(assignStmt.getLeftOp() instanceof Local)) return;
    ////                    if (!(assignStmt.getRightOp() instanceof Local)) return;
    ////                }
    //
    //                exceptionalEdgesToRemove.put(headStmt, classType);
    //            });
    ////
    //        });
    //
    ////        exceptionalEdgesToRemove.forEach((stmt, type) -> {
    ////            stmtGraph.removeExceptionalEdge(stmt, type);
    ////            tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    ////        });
    //
    //        for (Stmt stmt : stmtGraph.getStmts()) {
    //            if (!(stmt instanceof JIfStmt ifStmt)) continue;
    //            AbstractConditionExpr condition = ifStmt.getCondition();
    //            if (!(condition instanceof JEqExpr eqExpr)) continue;
    //            Immediate op1 = eqExpr.getOp1();
    //            Immediate op2 = eqExpr.getOp2();
    //            if (!(op1 instanceof NullConstant)) continue;
    //            if (!(op2 instanceof NullConstant)) continue;
    //
    //            branchingStmtsToRemove.add(ifStmt);
    //        }
    //
    //        for (BranchingStmt stmt : branchingStmtsToRemove) {
    //            MutableBasicBlock blockOfStmt = (MutableBasicBlock) stmtGraph.getBlockOf(stmt);
    //            List<MutableBasicBlock> predecessors = blockOfStmt.getPredecessors();
    //
    //            MutableBasicBlock successor2replace = blockOfStmt.getSuccessors().get(1);
    //            for (MutableBasicBlock predecessor : predecessors) {
    //                predecessor.replaceSuccessorBlock(blockOfStmt, successor2replace);
    //
    //                // need to update the predecessors as well
    //                for (MutableBasicBlock successor : blockOfStmt.getSuccessors()) {
    //                    successor.replacePredecessorBlock(blockOfStmt, predecessor);
    //                }
    //            }
    //
    //            stmtGraph.removeBlock(blockOfStmt);
    //        }

    //        List<BasicBlock<?>> blocks = new ArrayList<>(stmtGraph.getBlocks());
    //        List<BasicBlock<?>> blocksToRemove = new ArrayList<>();
    //        Map<BasicBlock<?>, Map<ClassType, BasicBlock<?>>> exceptionalEdgesToRemove = new
    // HashMap<>();
    //
    //        for (BasicBlock<?> block : blocks) {
    //            if (block.getStmts().size() != 1) {
    //                continue;
    //            }
    //
    //            Stmt stmt = block.getTail();
    //            if (!(stmt instanceof JInvokeStmt invokeStmt)) {
    //                continue;
    //            }
    //
    //            // Check if this is a close() method call
    //            Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
    //            if (invokeExprOpt.isEmpty() ||
    // !invokeExprOpt.get().getMethodSignature().getName().equals("close")) {
    //                continue;
    //            }
    //
    //            Map<? extends ClassType, ? extends BasicBlock<?>> exceptionalSuccs = new
    // HashMap<>(block.getExceptionalSuccessors());
    //            if (exceptionalSuccs.isEmpty() || block.getSuccessors().size() != 1) {
    //                continue;
    //            }
    //
    //            // Only remove if this block is at the end of a trap region
    //            // This means it should only have one successor and that successor should be the
    // handler's successor
    //            BasicBlock<?> successor = block.getSuccessors().iterator().next();
    //            boolean isLastInTrapRegion = true;
    //            for (BasicBlock<?> handlerBlock : exceptionalSuccs.values()) {
    //                if (!handlerBlock.getSuccessors().contains(successor)) {
    //                    isLastInTrapRegion = false;
    //                    break;
    //                }
    //            }
    //
    //            if (!isLastInTrapRegion) {
    //                continue;
    //            }
    //
    //            // Collect block for removal
    //            blocksToRemove.add(block);
    //            tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    //
    //            // Collect exceptional edges that need to be removed
    //            Map<ClassType, BasicBlock<?>> edgesToRemove = new HashMap<>();
    //            exceptionalSuccs.forEach((exceptionType, handlerBlock) -> {
    //                if (handlerBlock.getExceptionalPredecessors().size() == 1) {
    //                    edgesToRemove.put((ClassType) exceptionType, handlerBlock);
    //                }
    //            });
    //            if (!edgesToRemove.isEmpty()) {
    //                exceptionalEdgesToRemove.put(block, edgesToRemove);
    //            }
    //        }
    //
    //        // Apply removals after iteration is complete
    //        exceptionalEdgesToRemove.forEach((block, edges) -> {
    //            edges.forEach((exceptionType, handlerBlock) -> {
    //                System.out.printf("Removing Edge: %s <-> %s\n", exceptionType,
    // handlerBlock.getHead());
    //                stmtGraph.removeExceptionalEdge(block.getHead(), exceptionType);
    //            });
    //        });
    //
    //        // Remove blocks after edges are removed
    //        blocksToRemove.forEach(stmtGraph::removeBlock);
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
