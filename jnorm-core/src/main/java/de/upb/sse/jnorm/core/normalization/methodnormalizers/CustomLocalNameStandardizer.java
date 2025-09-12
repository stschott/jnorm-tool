package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.*;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.common.LValue;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.views.View;

public class CustomLocalNameStandardizer implements MethodNormalizer {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    Map<Local, Local> localsToReplace = new HashMap<>();

    int counter = 0;
    Set<Local> locals = bodyBuilder.getLocals();
    Set<Local> defs = new HashSet<>();
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    // we want to name locals in definition order:
    // get all stmts in order and check if they define a local
    for (Stmt stmt : stmtGraph.getStmts()) {
      Optional<LValue> def = stmt.getDef();
      if (!def.isPresent()) continue;

      LValue defValue = def.get();
      if (!(defValue instanceof Local)) continue;

      Local oldLocal = (Local) defValue;
      if (!locals.contains(oldLocal)) continue;

      defs.add(oldLocal);
      Local newLocal = new Local("lc" + counter++, oldLocal.getType());
      localsToReplace.put(oldLocal, newLocal);
    }

    for (Stmt stmt : stmtGraph.getStmts()) {
      List<Value> uses = stmt.getUses().toList();
      for (Value use : uses) {
        if (!(use instanceof Local)) continue;
        Local oldLocal = (Local) use;

        if (defs.contains(oldLocal)) continue;
        if (localsToReplace.containsKey(oldLocal)) continue;
        if (!locals.contains(oldLocal)) continue;

        Local newLocal = new Local("lc" + counter++, use.getType());
        localsToReplace.put(oldLocal, newLocal);
      }
    }

    for (Map.Entry<Local, Local> entry : localsToReplace.entrySet()) {
      Local oldLocal = entry.getKey();
      Local newLocal = entry.getValue();
      bodyBuilder.replaceLocal(oldLocal, newLocal);
    }

    if (!localsToReplace.isEmpty()) {
      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }
  }

  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
