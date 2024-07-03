package jnorm.core.helpers;

import soot.Unit;
import soot.Value;
import soot.jimple.*;
public class SootHelper {

    public static boolean isParameterRef (Unit unit) {
        if (unit instanceof IdentityStmt) {
            IdentityStmt identityStmt = (IdentityStmt) unit;
            Value rightOp = identityStmt.getRightOp();
            if (rightOp instanceof ParameterRef) {
                return true;
            }
        }
        return false;
    }
}
