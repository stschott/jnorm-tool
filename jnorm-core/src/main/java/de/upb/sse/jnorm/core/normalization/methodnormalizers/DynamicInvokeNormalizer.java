package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.common.Immediate;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.signatures.PackageName;
import sootup.core.views.View;
import sootup.java.core.types.JavaClassType;

public class DynamicInvokeNormalizer implements MethodNormalizer, ModificationTracker {
  private static final String LAMBDA_METAFACTORY_SIGNATURE =
      "LambdaMetafactory: java.lang.invoke.CallSite metafactory";
  private static final String HASHTABLE_PUTALL_SIGNATURE =
      "java.util.Hashtable: void putAll(java.util.Map)";
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    for (Stmt stmt : stmtGraph.getStmts()) {
      // Look for dynamic invoke expressions
      for (Value use : stmt.getUses().toList()) {
        if (!(use instanceof JDynamicInvokeExpr dynamicInvoke)) continue;

        MethodSignature bootstrapSig = dynamicInvoke.getBootstrapMethodSignature();
        String bootstrapStr = bootstrapSig.toString();

        List<Immediate> args = dynamicInvoke.getBootstrapArgs();
        boolean modified = false;

        if (bootstrapStr.contains(LAMBDA_METAFACTORY_SIGNATURE)) {
          if (args.size() > 1 && args.get(1) instanceof MethodHandle originalMethodHandle) {
            MethodHandle potentiallyModifiedMethodHandle = originalMethodHandle;

            // Case 1: Handle Hashtable -> Properties conversion in the method handle's reference
            // signature
            if (originalMethodHandle
                .getReferenceSignature()
                .toString()
                .contains(HASHTABLE_PUTALL_SIGNATURE)) {
              MethodSignature newMethodSignature =
                  convertMethodSignature(
                      (MethodSignature) originalMethodHandle.getReferenceSignature());
              potentiallyModifiedMethodHandle =
                  new MethodHandle(
                      newMethodSignature,
                      originalMethodHandle.getKind(),
                      originalMethodHandle.getType());
              modified = true;
            }

            // Case 2: Handle REF_INVOKE_SPECIAL -> REF_INVOKE_VIRTUAL in the method handle's kind
            if (potentiallyModifiedMethodHandle.getKind() == MethodHandle.Kind.REF_INVOKE_SPECIAL) {
              potentiallyModifiedMethodHandle =
                  new MethodHandle(
                      potentiallyModifiedMethodHandle.getReferenceSignature(),
                      MethodHandle.Kind.REF_INVOKE_VIRTUAL,
                      potentiallyModifiedMethodHandle.getType());
              modified = true;
            }

            if (modified) {
              List<Immediate> newArgs = new ArrayList<>(args);
              newArgs.set(1, potentiallyModifiedMethodHandle);
              Stmt newStmt =
                  stmt.withNewUse(dynamicInvoke, dynamicInvoke.withBootstrapArgs(newArgs));
              if (newStmt != null) {
                stmtGraph.replaceNode(stmt, newStmt);
              } else {
                modified = false;
              }
            }
          }
        }

        if (modified) {
          tracker.recordModification(bodyBuilder.getMethodSignature().toString());
        }
      }
    }
  }

  private static MethodSignature convertMethodSignature(MethodSignature methodSig) {
    JavaClassType propertiesType = new JavaClassType("Properties", new PackageName("java.util"));

    MethodSubSignature subSig = methodSig.getSubSignature();
    return new MethodSignature(propertiesType, subSig);
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
