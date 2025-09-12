package de.upb.sse.jnorm.core.exceptions;

import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.stmt.Stmt;

public class ArithmeticInterceptionException extends BodyInterceptionException {

  public ArithmeticInterceptionException(
      Stmt failedStatement, JAddExpr addition, JSubExpr subtraction) {
    super(
        String.format(
            "Failed to replace addition expression (%s) in statement (%s) with subtraction expression (%s).",
            addition, failedStatement, subtraction));
  }
}
