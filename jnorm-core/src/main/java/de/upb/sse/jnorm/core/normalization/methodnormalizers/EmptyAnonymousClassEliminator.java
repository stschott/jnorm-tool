package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.MethodModifier;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.Type;
import sootup.core.views.View;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.types.JavaClassType;

public class EmptyAnonymousClassEliminator implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    final String ANON_INNER_CLASS_PATTERN = "\\$\\d+";
    String declaringClassName =
        bodyBuilder.getMethodSignature().getDeclClassType().getFullyQualifiedName();

    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    for (Stmt stmt : stmtGraph.getStmts()) {
      if (!(stmt instanceof JInvokeStmt)) continue;
      JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
      Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
      if (!invokeExprOpt.isPresent()) continue;
      AbstractInvokeExpr invokeExpr = invokeExprOpt.get();

      if (!(invokeExpr instanceof JSpecialInvokeExpr)) continue;
      JSpecialInvokeExpr specialInvokeExpr = (JSpecialInvokeExpr) invokeExpr;
      MethodSignature invokeExprSignature = specialInvokeExpr.getMethodSignature();
      List<Type> parameterTypes = invokeExprSignature.getParameterTypes();

      if (parameterTypes.isEmpty()) continue;
      Type lastParameterType = parameterTypes.get(parameterTypes.size() - 1);

      JavaClassType classType =
          JavaIdentifierFactory.getInstance().getClassType(lastParameterType.toString());

      Optional<? extends SootClass> scOpt = view.getClass(classType);
      if (!scOpt.isPresent()) continue;
      SootClass sc = scOpt.get();

      // Check if the class is an inner class
      if (!sc.isInnerClass()) continue;
      // Check if inner class is anonymous

      String outerDeclaringClassName = declaringClassName.split("\\$")[0];

      String outerClassPattern = outerDeclaringClassName.replace("$", "\\$");
      if (!sc.getName().matches(outerClassPattern + ANON_INNER_CLASS_PATTERN)) continue;

      // Find and set new init call
      SootClass declaringInnerClass = view.getClassOrThrow(invokeExprSignature.getDeclClassType());
      Optional<? extends SootMethod> newInitCall =
          declaringInnerClass.getMethod(
              invokeExprSignature.getName(), parameterTypes.subList(0, parameterTypes.size() - 1));

      if (!newInitCall.isPresent()) continue;

      JInvokeStmt newInvokeStmt =
          Jimple.newInvokeStmt(
              Jimple.newSpecialInvokeExpr(
                  specialInvokeExpr.getBase(),
                  newInitCall.get().getSignature(),
                  specialInvokeExpr.getArgs().subList(0, specialInvokeExpr.getArgs().size() - 1)),
              StmtPositionInfo.getNoStmtPositionInfo());

      stmtGraph.replaceNode(stmt, newInvokeStmt);

      Set<MethodModifier> methodModifiers = new HashSet<>(bodyBuilder.getModifiers());
      methodModifiers.add(MethodModifier.PRIVATE);
      bodyBuilder.setModifiers(methodModifiers);

      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
