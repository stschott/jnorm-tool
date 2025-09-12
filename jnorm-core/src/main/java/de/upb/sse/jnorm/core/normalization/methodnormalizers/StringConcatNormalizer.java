package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import de.upb.sse.jnorm.core.model.StringBuilderConcat;
import de.upb.sse.jnorm.core.stats.ModifiedMethodTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.Immediate;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.PackageName;
import sootup.core.types.ArrayType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.views.View;
import sootup.java.core.types.JavaClassType;

public class StringConcatNormalizer implements MethodNormalizer, ModificationTracker {
  private final ModifiedMethodTracker tracker = new ModifiedMethodTracker();

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    final String STRINGBUILDER_TYPE = "java.lang.StringBuilder";
    final String STRINGBUILDER_INIT_SIGNATURE = "<java.lang.StringBuilder: void <init>()>";

    List<StringBuilderConcat> stringBuilderConcats = new ArrayList<>();
    MutableStmtGraph stmtGraph = bodyBuilder.getStmtGraph();

    for (Stmt stmt : stmtGraph.getStmts()) {
      // Check for the StringBuilder <init> call
      if (stmt instanceof JInvokeStmt) {
        JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
        Optional<AbstractInvokeExpr> invokeExprOpt = invokeStmt.getInvokeExpr();
        if (!invokeExprOpt.isPresent()) continue;

        AbstractInvokeExpr invokeExpr = invokeExprOpt.get();

        if (!(invokeExpr instanceof JSpecialInvokeExpr)) continue;
        JSpecialInvokeExpr specialInvokeExpr = (JSpecialInvokeExpr) invokeExpr;
        Local value = specialInvokeExpr.getBase();

        // Check if we have a StringBuilderCall
        Optional<StringBuilderConcat> sbc =
            stringBuilderConcats.stream().filter(sb -> sb.aliasContained(value)).findFirst();
        if (!sbc.isPresent()) continue;

        // specialinvoke of StringBuilder constructor
        if (!specialInvokeExpr.getMethodSignature().toString().equals(STRINGBUILDER_INIT_SIGNATURE))
          continue;
        sbc.get().addStringBuilderCall(stmt);
      }

      if (!(stmt instanceof JAssignStmt)) continue;
      // At this point the stmt is an AssignStmt
      JAssignStmt assignStmt = (JAssignStmt) stmt;

      // Check if lhs is a Local
      Value lhs = assignStmt.getLeftOp();
      if (!(lhs instanceof Local)) continue;
      Local lhsLocal = (Local) lhs;

      Value rhs = assignStmt.getRightOp();

      // Check for new StringBuilder call
      if (rhs instanceof JNewExpr) {
        JNewExpr newExpr = (JNewExpr) rhs;

        if (!newExpr.getType().getFullyQualifiedName().equals(STRINGBUILDER_TYPE)) continue;
        // We have a new StringBuilder call
        StringBuilderConcat sbc = new StringBuilderConcat(lhsLocal, stmt);
        stringBuilderConcats.add(sbc);
      }

      // Check for StringBuilder.append and StringBuilder.toString calls
      if (!(rhs instanceof JVirtualInvokeExpr)) continue;
      JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) rhs;
      MethodSignature sm = virtualInvokeExpr.getMethodSignature();

      if (!sm.getDeclClassType().getFullyQualifiedName().equals(STRINGBUILDER_TYPE)) continue;
      // We have a StringBuilder call
      virtualInvokeExpr.getBase();
      Local sbCallTarget = virtualInvokeExpr.getBase();

      // Now get the corresponding sbc Object
      Optional<StringBuilderConcat> sbcOptional =
          stringBuilderConcats.stream().filter(sb -> sb.aliasContained(sbCallTarget)).findFirst();
      if (!sbcOptional.isPresent()) continue;

      StringBuilderConcat sbc = sbcOptional.get();

      if (sm.getName().equals("append")) {

        //        // We want to skip append calls with empty strings as arguments
        //        if (virtualInvokeExpr.getArgCount() == 1 &&
        //                virtualInvokeExpr.getArg(0) instanceof StringConstant &&
        //                ((StringConstant) virtualInvokeExpr.getArg(0)).getValue().isEmpty()) {
        //          continue;
        //        }

        // We have a StringBuilder.append call
        sbc.addAlias(lhsLocal);
        sbc.addStringBuilderCall(stmt);

        // Parameter type of StringBuilder.append call
        Type sbParameterType = sm.getParameterTypes().get(0);
        Immediate sbArgument = virtualInvokeExpr.getArg(0);

        // arguments are checked here

        // if there is a "-" sign it should be handled like a variable string literal
        // this is apparently due to weird compiler stuff

        //                if (sbArgument instanceof StringConstant && !((StringConstant)
        // sbArgument).value.equals("-")) {

        boolean dynamicArgumentCase = false;
        if (sbArgument instanceof StringConstant) {
          // argument is a String constant
          StringConstant sbArgumentStringConstant = (StringConstant) sbArgument;
          if (sbArgumentStringConstant.getValue().isEmpty()) continue;

          if (sbArgumentStringConstant.getValue().equals("null")) {
            dynamicArgumentCase = true;
          } else {
            sbc.concat(sbArgumentStringConstant.getValue());
          }
        } else if (sbParameterType.equals(PrimitiveType.BooleanType.getInstance())
            && sbArgument instanceof IntConstant) {
          // argument type is boolean and argument is an IntConstant
          // in bytecode this is expressed as an IntConstant
          // Therefore boolean value has to be determined from the int
          IntConstant boolValue = (IntConstant) sbArgument;
          String boolValueString = boolValue.getValue() > 0 ? "true" : "false";
          sbc.concat(boolValueString);
        } else if (sbParameterType.equals(PrimitiveType.CharType.getInstance())
            && sbArgument instanceof IntConstant) {
          // argument type is char
          // this means that the int is used as index for a character
          IntConstant sbArgumentIntConstant = (IntConstant) sbArgument;
          char correspondingChar = (char) sbArgumentIntConstant.getValue();
          sbc.concat(String.valueOf(correspondingChar));
        } else if (sbParameterType.equals(PrimitiveType.IntType.getInstance())
            && sbArgument instanceof IntConstant) {
          IntConstant sbArgumentIntConstant = (IntConstant) sbArgument;
          sbc.concat(String.valueOf(sbArgumentIntConstant.getValue()));
        } else {
          // argument is dynamic
          dynamicArgumentCase = true;
        }

        if (dynamicArgumentCase) {
          handleDynamicArgument(sbc, stmtGraph, stmt, sbParameterType, sbArgument);
        }

      } else if (sm.getName().equals("toString")) {
        // We have a StringBuilder.toString call
        sbc.setEndStmt(stmt);

        // Construct a new dynamicinvoke expression
        List<Type> bootstrapTypes = new ArrayList<>();
        bootstrapTypes.add(
            new JavaClassType("MethodHandles$Lookup", new PackageName("java.lang.invoke")));
        bootstrapTypes.add(new JavaClassType("String", new PackageName("java.lang")));
        bootstrapTypes.add(new JavaClassType("MethodType", new PackageName("java.lang.invoke")));
        bootstrapTypes.add(new JavaClassType("String", new PackageName("java.lang")));
        bootstrapTypes.add(
            new ArrayType(new JavaClassType("Object", new PackageName("java.lang")), 1));

        Type stringType = new JavaClassType("String", new PackageName("java.lang"));

        MethodSignature bootstrap =
            new MethodSignature(
                new JavaClassType("StringConcatFactory", new PackageName("java.lang.invoke")),
                "makeConcatWithConstants",
                bootstrapTypes,
                new JavaClassType("CallSite", new PackageName("java.lang.invoke")));
        MethodSignature stringConcat =
            new MethodSignature(
                new JavaClassType("InvokeDynamic", new PackageName("sootup.dummy")),
                "makeConcatWithConstants",
                sbc.getDynamicTypes(),
                stringType);

        List<Immediate> bootstrapArgs = new ArrayList<>();
        bootstrapArgs.add(new StringConstant(sbc.getTemplate(), stringType));

        JAssignStmt dynamicStringConcat =
            Jimple.newAssignStmt(
                lhsLocal,
                Jimple.newDynamicInvokeExpr(
                    bootstrap, bootstrapArgs, stringConcat, sbc.getDynamicValues()),
                StmtPositionInfo.getNoStmtPositionInfo());

        sbc.setNewStringConcatCall(dynamicStringConcat);
      }
    }

    for (StringBuilderConcat sbc : stringBuilderConcats) {
      if (sbc.getEndStmt() == null) continue;
      if (sbc.getNewStringConcatCall() == null) continue;
      sbc.getStringBuilderCalls().forEach(stmtGraph::removeNode);
      stmtGraph.replaceNode(sbc.getEndStmt(), sbc.getNewStringConcatCall());
      tracker.recordModification(bodyBuilder.getMethodSignature().toString());
    }
  }

  public void handleDynamicArgument(
      StringBuilderConcat sbc,
      MutableStmtGraph stmtGraph,
      Stmt stmt,
      Type sbParameterType,
      Immediate sbArgument) {
    boolean intCast = false;
    if (sbParameterType.equals(PrimitiveType.IntType.getInstance())) {
      Stmt prevUnit = stmtGraph.predecessors(stmt).get(0);
      if (prevUnit instanceof JAssignStmt) {
        JAssignStmt prevUnitAssignStmt = (JAssignStmt) prevUnit;
        Value rOp = prevUnitAssignStmt.getRightOp();
        if (rOp instanceof JCastExpr) {
          intCast = true;
          // previous stmt is a typecast
          JCastExpr cast = (JCastExpr) rOp;
          Type originalType = cast.getOp().getType();
          sbc.concat("\u0001");
          sbc.addDynamicArgument(sbArgument, originalType);
        }
      }
    }

    if (!intCast) {
      sbc.concat("\u0001");
      // this means we also need to add the type and value of the dynamic value
      Type argumentType = sbArgument.getType();

      // If the appended type is java.lang.Throwable, the compiler converts it to
      // java.lang.Exception
      if (argumentType.equals(new JavaClassType("Throwable", new PackageName("java.lang")))) {
        argumentType = new JavaClassType("Exception", new PackageName("java.lang"));
      }

      sbc.addDynamicArgument(sbArgument, argumentType);
    }
  }

  @Override
  public Set<String> getModifiedMethods() {
    return tracker.getModifiedMethods();
  }
}
