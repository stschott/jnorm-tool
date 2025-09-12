package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import java.util.ArrayList;
import java.util.List;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Immediate;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.signatures.PackageName;
import sootup.core.types.Type;
import sootup.core.views.View;
import sootup.java.core.language.JavaJimple;
import sootup.java.core.types.JavaClassType;

public class DynamicStringConcatNormalizer implements MethodNormalizer {
  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();
    List<Stmt> stmtsToAdd = new ArrayList<>();
    List<Stmt> stmtsToRemove = new ArrayList<>();

    for (Stmt stmt : stmtGraph.getNodes()) {
      if (!(stmt instanceof JAssignStmt assignStmt)) {
        continue;
      }

      Value rhs = assignStmt.getRightOp();
      if (!(rhs instanceof JDynamicInvokeExpr dynamicInvokeExpr)) {
        continue;
      }

      // Check if this is a string concatenation
      String bootstrapMethodName = dynamicInvokeExpr.getBootstrapMethodSignature().getName();
      if (!bootstrapMethodName.equals("makeConcatWithConstants")) {
        continue;
      }

      List<Immediate> concatArgs = new ArrayList<>(dynamicInvokeExpr.getArgs());

      // Check if we have any string constants in the args
      List<Integer> stringConstantIndices = new ArrayList<>();
      for (int i = 0; i < concatArgs.size(); i++) {
        Immediate arg = concatArgs.get(i);
        if (arg instanceof StringConstant) {
          stringConstantIndices.add(i);
        }
      }

      // If we have string constants, optimize the dynamic invoke
      if (!stringConstantIndices.isEmpty()) {
        // Get the template, args, and bootstrap args
        String template = ((StringConstant) dynamicInvokeExpr.getBootstrapArgs().get(0)).getValue();
        List<Immediate> args = new ArrayList<>(dynamicInvokeExpr.getArgs());
        List<Type> paramTypes =
            new ArrayList<>(dynamicInvokeExpr.getMethodSignature().getParameterTypes());
        List<Immediate> newBootstrapArgs = new ArrayList<>(dynamicInvokeExpr.getBootstrapArgs());

        // Find placeholders in template and replace with string constants
        StringBuilder newTemplate = new StringBuilder(template);
        List<Integer> placeholderPositions = new ArrayList<>();
        int pos = 0;
        while ((pos = newTemplate.indexOf("\u0001", pos)) != -1) {
          placeholderPositions.add(pos);
          pos++;
        }

        // Replace placeholders with string constants and remove those args
        for (int i = stringConstantIndices.size() - 1; i >= 0; i--) {
          int argIndex = stringConstantIndices.get(i);
          if (argIndex < placeholderPositions.size()) {
            String value = ((StringConstant) args.get(argIndex)).getValue();
            newTemplate.replace(
                placeholderPositions.get(argIndex), placeholderPositions.get(argIndex) + 1, value);
            args.remove(argIndex);
            paramTypes.remove(argIndex);
          }
        }

        // Update bootstrap args with new template
        newBootstrapArgs.set(
            0,
            new StringConstant(
                newTemplate.toString(), new JavaClassType("String", new PackageName("java.lang"))));

        // Create new method signature with updated parameter types
        MethodSignature oldSig = dynamicInvokeExpr.getMethodSignature();
        MethodSubSignature newSubSig =
            new MethodSubSignature(oldSig.getName(), paramTypes, oldSig.getType());
        MethodSignature newMethodSig = new MethodSignature(oldSig.getDeclClassType(), newSubSig);

        // Create new dynamic invoke with updated args
        JDynamicInvokeExpr newDynamicInvoke =
            JavaJimple.newDynamicInvokeExpr(
                dynamicInvokeExpr.getBootstrapMethodSignature(),
                newBootstrapArgs,
                newMethodSig,
                args);

        // Create new assignment with optimized dynamic invoke
        JAssignStmt newAssignStmt =
            JavaJimple.newAssignStmt(
                assignStmt.getLeftOp(), newDynamicInvoke, StmtPositionInfo.getNoStmtPositionInfo());

        stmtsToAdd.add(newAssignStmt);
        stmtsToRemove.add(assignStmt);
      }
    }

    // Apply changes
    for (int i = 0; i < stmtsToAdd.size(); i++) {
      Stmt oldStmt = stmtsToRemove.get(i);
      Stmt newStmt = stmtsToAdd.get(i);
      stmtGraph.replaceNode(oldStmt, newStmt);
    }
  }
}
