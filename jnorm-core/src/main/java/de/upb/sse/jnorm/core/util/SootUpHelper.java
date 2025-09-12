package de.upb.sse.jnorm.core.util;

import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;

public class SootUpHelper {
  public static boolean isParameterRef(Stmt stmt) {
    if (!(stmt instanceof JIdentityStmt)) return false;

    JIdentityStmt identityStmt = (JIdentityStmt) stmt;
    Value rightOp = identityStmt.getRightOp();
    return rightOp instanceof JParameterRef;
  }
}
