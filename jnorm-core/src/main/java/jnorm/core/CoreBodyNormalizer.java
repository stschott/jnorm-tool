package jnorm.core;

import jnorm.core.helpers.SootHelper;
import jnorm.core.model.BufferCall;
import jnorm.core.model.NormalizationStatistics;
import jnorm.core.model.StringBuilderConcat;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JArrayRef;
import soot.jimple.toolkits.scalar.DefaultLocalCreation;
import soot.util.Chain;

import java.util.*;

public class CoreBodyNormalizer {
    private NormalizationStatistics statistics;

    public CoreBodyNormalizer(NormalizationStatistics statistics) {
        this.statistics = statistics;
    }

    public void normalize(Body body) {
        SootMethod method = body.getMethod();
        // Check if current method is synthetic and skip if it is
        // also add to a list of synthetic methods for removal later on
        if (isSynthetic(method)) {
            SootHandler.syntheticMethods.add(method);
            return;
        }
        try {
            normalizeArithmeticOperations(body);
            normalizeStringConcat(body);
            normalizePrivateMethodCalls(body);
            normalizeCharSequenceToStringInvoke(body);
            normalizeBufferMethodCalls(body);
            normalizeNullChecks(body);
            normalizeDuplicateTypeCast(body);
            normalizeEnum(body);
            removeRedundantTraps(body);
            normalizeTraps(body);
            normalizeInnerClassInits(body);
            normalizeNestBasedPrivateAccesses(body);
            normalizeInnerEnums(body);

            // belong to normalizeStringConcat
            normalizeDynamicStringConcatCasts(body);
            normalizeDynamicStringConstantConcat(body);
        } catch (Exception e) {
            // e.printStackTrace();
        }


    }

    // Java 8 -> Java 11
    private void normalizeDynamicStringConstantConcat(Body body) {
        try {
            final String MAKE_CONCAT_WITH_CONSTANTS = "makeConcatWithConstants";
            UnitPatchingChain unitChain = body.getUnits();
            List<Unit> unitsToAdd = new ArrayList<>();
            List<Unit> unitsToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                if (!(unit instanceof AssignStmt)) continue;
                AssignStmt assignStmt = (AssignStmt) unit;
                Value rhs = assignStmt.getRightOp();
                Value lhs = assignStmt.getLeftOp();

                if (!(rhs instanceof DynamicInvokeExpr)) continue;
                DynamicInvokeExpr dynamicInvokeExpr = (DynamicInvokeExpr) rhs;

                // check if we have a string concat call
                // sometimes throws concurrent modification exception
                SootMethod dynamicMethod = dynamicInvokeExpr.getMethod();

                if (!dynamicMethod.getName().equals(MAKE_CONCAT_WITH_CONSTANTS)) continue;
                List<Value> concatArgs = dynamicInvokeExpr.getArgs();

                if (concatArgs.size() < 2) continue;

                boolean allArgsStingConstant = true;
                for (Value concatArg : concatArgs) {
                    if (!(concatArg instanceof StringConstant)) {
                        allArgsStingConstant = false;
                        break;
                    }
                }

                // Check if all arguments are String Constants
                if (!allArgsStingConstant) continue;

                // Concatenate all args
                String concatenatedArgs = "";
                for (Value concatArg : concatArgs) {
                    StringConstant stringConstant = (StringConstant) concatArg;
                    concatenatedArgs += stringConstant.value;
                }

                List<Value> dynamicMethodArgs = new ArrayList<>();
                dynamicMethodArgs.add(StringConstant.v(concatenatedArgs));
                // Construct a new dynamicinvoke expression
                List<Type> bootstrapTypes = new ArrayList<>();
                bootstrapTypes.add(RefType.v("java.lang.invoke.MethodHandles$Lookup"));
                bootstrapTypes.add(RefType.v("java.lang.String"));
                bootstrapTypes.add(RefType.v("java.lang.invoke.MethodType"));
                bootstrapTypes.add(RefType.v("java.lang.String"));
                bootstrapTypes.add(ArrayType.v(RefType.v("java.lang.Object"), 1));

                SootMethodRefImpl bootstrap = new SootMethodRefImpl(new SootClass("java.lang.invoke.StringConcatFactory"), "makeConcatWithConstants", bootstrapTypes, RefType.v("java.lang.invoke.CallSite"), false);
                SootMethodRefImpl stringConcat = new SootMethodRefImpl(new SootClass("soot.dummy.InvokeDynamic"), "makeConcatWithConstants", new ArrayList<>(), RefType.v("java.lang.String"), false);

                AssignStmt dynamicStringConcat = Jimple.v().newAssignStmt(
                        lhs,
                        Jimple.v().newDynamicInvokeExpr(
                                bootstrap,
                                dynamicMethodArgs,
                                stringConcat,
                                new ArrayList<>()
                        )
                );

                unitsToAdd.add(dynamicStringConcat);
                unitsToRemove.add(unit);
            }

            for (int i = 0; i < unitsToAdd.size(); i++) {
                unitChain.swapWith(unitsToRemove.get(i), unitsToAdd.get((i)));
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Java 8 -> Java 11
    private void normalizeDynamicStringConcatCasts(Body body) {
        try {
            final String MAKE_CONCAT_WITH_CONSTANTS = "makeConcatWithConstants";
            UnitPatchingChain unitChain = body.getUnits();
            List<Unit> unitsToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                try {
                    if (!(unit instanceof AssignStmt)) continue;
                    AssignStmt assignStmt = (AssignStmt) unit;
                    Value rhs1 = assignStmt.getRightOp();

                    if (!(rhs1 instanceof DynamicInvokeExpr)) continue;
                    DynamicInvokeExpr dynamicInvokeExpr = (DynamicInvokeExpr) rhs1;

                    // check if we have a string concat call
                    // sometimes throws concurrent modification exception
                    SootMethod dynamicMethod = dynamicInvokeExpr.getMethod();

                    if (!dynamicMethod.getName().equals(MAKE_CONCAT_WITH_CONSTANTS)) continue;
                    Unit prevUnit = unitChain.getPredOf(unit);

                    if (!(prevUnit instanceof AssignStmt)) continue;
                    AssignStmt prevAssignStmt = (AssignStmt) prevUnit;
                    Value rhs2 = prevAssignStmt.getRightOp();
                    Value lhs2 = prevAssignStmt.getLeftOp();

                    if (!(rhs2 instanceof CastExpr)) continue;
                    CastExpr castExpr = (CastExpr) rhs2;
                    Value castOp = castExpr.getOp();

                    List<Value> dynamicArgs = dynamicInvokeExpr.getArgs();
                    if (!dynamicArgs.contains(lhs2)) continue;

                    // Swap the argument
                    int changedArgIndex = dynamicArgs.indexOf(lhs2);
                    dynamicInvokeExpr.setArg(changedArgIndex, castOp);
                    // Swap the parameter types
                    Type castOpOriginalType = castOp.getType();
                    List<Type> parameterTypes = dynamicInvokeExpr.getMethod().getParameterTypes();

                    if (castOpOriginalType instanceof RefType) {
                        parameterTypes.set(changedArgIndex, RefType.v("java.lang.Object"));
                    } else {
                        parameterTypes.set(changedArgIndex, castOpOriginalType);
                    }

                    SootClass dummyInvokeDynamic = Scene.v().getSootClass("soot.dummy.InvokeDynamic");
                    Type returnType = dynamicInvokeExpr.getMethod().getReturnType();
                    boolean methodAlreadyExisting = dummyInvokeDynamic.declaresMethod(MAKE_CONCAT_WITH_CONSTANTS, parameterTypes, returnType);

                    // check if method with desired signature already exists
                    SootMethod sm;
                    if (!methodAlreadyExisting) {
                        sm = new SootMethod(MAKE_CONCAT_WITH_CONSTANTS, parameterTypes, returnType);
                        sm.setDeclaringClass(dummyInvokeDynamic);
                        dummyInvokeDynamic.addMethod(sm);
                        //                dynamicMethod.setParameterTypes(parameterTypes);
                    } else {
                        sm = dummyInvokeDynamic.getMethod(MAKE_CONCAT_WITH_CONSTANTS, parameterTypes, dynamicInvokeExpr.getMethod().getReturnType());
                    }
                    dynamicInvokeExpr.setMethodRef(sm.makeRef());

                    unitsToRemove.add(prevUnit);
                } catch (Exception e) {
                    System.out.println("Error in DynamicStringConcatNormalization");
                    System.out.println(body.getMethod().getSignature());
                }
            }
            unitsToRemove.forEach(unitChain::remove);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Java 8 -> Java 11
    private void normalizeInnerEnums(Body body) {
        try {
            SootMethod sm = body.getMethod();

            if (!sm.isConstructor()) return;

            SootClass declaringClass = body.getMethod().getDeclaringClass();
            if (!declaringClass.isInnerClass()) return;
            if (!declaringClass.isEnum()) return;
            if (!declaringClass.isFinal()) return;

            // Check if method is already private
            if (sm.isPrivate()) return;
            sm.setModifiers(sm.getModifiers() + Modifier.PRIVATE);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Java 8 -> Java 11
    private void normalizeNestBasedPrivateAccesses(Body body) {
        try {
            final String NEST_BASED_ACCESS_PATTERN = "access\\$\\d+";
            UnitPatchingChain unitChain = body.getUnits();
            List<Unit> unitsToAdd = new ArrayList<>();
            List<Unit> unitsToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                Value currentUnit = null;
                Value newValue = null;

                if (unit instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) unit;
                    currentUnit = assignStmt.getRightOp();
                } else if (unit instanceof InvokeStmt) {
                    InvokeStmt invokeStmt = (InvokeStmt) unit;
                    currentUnit = invokeStmt.getInvokeExpr();
                }

                // Unit is neither AssignStmt nor InvokeStmt
                if (currentUnit == null) continue;
                if (!(currentUnit instanceof InvokeExpr)) continue;
                InvokeExpr invokeExpr = (InvokeExpr) currentUnit;

                if (!(invokeExpr instanceof StaticInvokeExpr)) continue;
                StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) invokeExpr;
                List<Value> staticInvokeArgs = invokeExpr.getArgs();
                SootMethod sm = staticInvokeExpr.getMethod();

                if (!sm.getName().matches(NEST_BASED_ACCESS_PATTERN)) continue;
                UnitPatchingChain callTargetUnits = sm.getActiveBody().getUnits();

                // Get all relevant units from the bridge method
                // Relevant units are all units between parameterRefs and the return statement
                List<Unit> relevantCallTargetUnits = new ArrayList<>();
                boolean parametersChecked = false;
                for (Unit callTargetUnit : callTargetUnits) {
                    if (SootHelper.isParameterRef(callTargetUnit)) {
                        Unit nextUnit = callTargetUnits.getSuccOf(callTargetUnit);
                        if (!SootHelper.isParameterRef(nextUnit)) {
                            parametersChecked = true;
                            continue;
                        }
                    } else {
                        parametersChecked = true;
                    }
                    // There are still parameters left
                    if (!parametersChecked) continue;
                    // Do not add the return to the relevant units
                    if (callTargetUnit instanceof ReturnStmt) break;
                    if (callTargetUnit instanceof ReturnVoidStmt) break;

                    relevantCallTargetUnits.add(callTargetUnit);
                }

                if (relevantCallTargetUnits.size() < 1) continue;
                Unit relevantUnit = relevantCallTargetUnits.get(relevantCallTargetUnits.size() - 1);

                Value relevantExpression = null;
                boolean fieldAccess = false;
                if (relevantUnit instanceof AssignStmt) {
                    AssignStmt relevantUnitAssignStmt = (AssignStmt) relevantUnit;
                    Value rightOp = relevantUnitAssignStmt.getRightOp();
                    Value leftOp = relevantUnitAssignStmt.getLeftOp();
                    if (rightOp instanceof Local) {
                        relevantExpression = leftOp;
                        fieldAccess = true;
                    } else {
                        relevantExpression = rightOp;
                    }
                } else if (relevantUnit instanceof InvokeStmt) {
                    InvokeStmt relevantUnitInvokeStmt = (InvokeStmt) relevantUnit;
                    relevantExpression = relevantUnitInvokeStmt.getInvokeExpr();
                }

                if (relevantExpression instanceof FieldRef) {
                    // relevant unit refers to a private class field
                    FieldRef relevantUnitInstanceFieldRef = (FieldRef) relevantExpression;
                    SootFieldRef relevantFieldRef = relevantUnitInstanceFieldRef.getFieldRef();

                    if (relevantExpression instanceof InstanceFieldRef) {
                        newValue = Jimple.v().newInstanceFieldRef(staticInvokeArgs.get(0), relevantFieldRef);
                    } else if (relevantExpression instanceof StaticFieldRef) {
                        newValue = Jimple.v().newStaticFieldRef(relevantFieldRef);
                    }
                } else if (relevantExpression instanceof InvokeExpr) {
                    // relevant unit refers to a private method
                    InvokeExpr relevantInvokeExpr = (InvokeExpr) relevantExpression;
                    SootMethodRef relevantMethodRef = relevantInvokeExpr.getMethodRef();

                    if (relevantInvokeExpr instanceof StaticInvokeExpr) {
                        newValue = Jimple.v().newStaticInvokeExpr(relevantMethodRef, staticInvokeArgs);
                    } else if (relevantInvokeExpr instanceof VirtualInvokeExpr) {
                        newValue = Jimple.v().newVirtualInvokeExpr((Local) staticInvokeArgs.get(0), relevantMethodRef, staticInvokeArgs.subList(1, staticInvokeArgs.size()));
                    } else if (relevantExpression instanceof SpecialInvokeExpr) {
                        newValue = Jimple.v().newSpecialInvokeExpr((Local) staticInvokeArgs.get(0), relevantMethodRef, staticInvokeArgs.subList(1, staticInvokeArgs.size()));
                        ;
                    }
                }

                if (newValue == null) continue;

                if (unit instanceof AssignStmt) {
                    // Current Unit is an AssignStmt
                    AssignStmt currentAssignStmt = (AssignStmt) unit;
                    currentAssignStmt.setRightOp(newValue);
                } else if (unit instanceof InvokeStmt) {
                    // Current Unit is an InvokeStmt
                    InvokeStmt currentInvokeStmt = (InvokeStmt) unit;
                    if (fieldAccess) {
                        if (staticInvokeArgs.size() > 1) {
                            Unit newFieldAccess = Jimple.v().newAssignStmt(
                                    newValue,
                                    staticInvokeArgs.get(1)
                            );
                            unitsToAdd.add(newFieldAccess);
                            unitsToRemove.add(unit);
                        } else if (relevantCallTargetUnits.size() > 1) {
                            // Bridge method contains more than one relevant statement
//                        LocalGenerator lg = new DefaultLocalGenerator(body);
//                        Unit newFieldAccess = Jimple.v().newAssignStmt(
//                                lg.generateLocal(newValue.getType()),
//                                newValue
//                        );
//                        unitsToAdd.add(newFieldAccess);
//                        unitsToRemove.add(unit);
//
//                        unitsToAdd.addAll(relevantCallTargetUnits.subList(0, relevantCallTargetUnits.size() - 1));
                        }
                    } else {
                        currentInvokeStmt.setInvokeExpr(newValue);
                    }
                }
            }

            for (int i = 0; i < unitsToAdd.size(); i++) {
                unitChain.swapWith(unitsToRemove.get(i), unitsToAdd.get(i));
                if (statistics != null) statistics.nestBasedAccessControl += 1;
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Java 8 -> Java 11
    private void normalizeInnerClassInits(Body body) {
        try {
            final String ANON_INNER_CLASS_PATTERN = "\\$\\d+";
            String declaringClassName = body.getMethod().getDeclaringClass().getName();

            Chain<SootClass> loadedClasses = Scene.v().getClasses();
            UnitPatchingChain unitChain = body.getUnits();

            List<Unit> unitsToRemove = new ArrayList<>();
            List<Unit> unitsToAdd = new ArrayList<>();
            List<SootClass> classesToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                if (!(unit instanceof InvokeStmt)) continue;
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                if (!(invokeExpr instanceof SpecialInvokeExpr)) continue;
                SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;
                SootMethod sm = specialInvokeExpr.getMethod();
                List<Type> parameterTypes = sm.getParameterTypes();

                if (parameterTypes.size() < 1) continue;
                Type lastParameterType = parameterTypes.get(parameterTypes.size() - 1);
                SootClass sc = Scene.v().getSootClass(lastParameterType.toString());

                // Check if the class is an inner class
                if (!sc.isInnerClass()) continue;
                // Check if inner class is anonymous

                String outerDeclaringClassName = declaringClassName.split("\\$")[0];
                if (!sc.getName().matches(outerDeclaringClassName + ANON_INNER_CLASS_PATTERN)) continue;

                // Find and set new init call
                SootClass declaringInnerClass = sm.getDeclaringClass();
                SootMethod newInitCall = declaringInnerClass.getMethodUnsafe(
                        sm.getName(),
                        parameterTypes.subList(0, parameterTypes.size() - 1),
                        sm.getReturnType()
                );

                if (newInitCall == null) continue;

                InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(
                        Jimple.v().newSpecialInvokeExpr(
                                (Local) specialInvokeExpr.getBase(),
                                newInitCall.makeRef(),
                                specialInvokeExpr.getArgs().subList(0, specialInvokeExpr.getArgs().size() - 1)
                        )
                );

                unitsToAdd.add(newInvokeStmt);
                unitsToRemove.add(unit);
                classesToRemove.add(sc);
                if (statistics != null) statistics.innerClassInstantiation += 1;
            }

            for (int i = 0; i < unitsToAdd.size(); i++) {
                unitChain.swapWith(unitsToRemove.get(i), unitsToAdd.get(i));
            }
//        further verify the removal of inner classes
//        classesToRemove.forEach(loadedClasses::remove);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Javac7 -> Javac8
    private void removeRedundantTraps(Body body) {
        // Pattern
        // 1. only for java.lang.Throwable
        // 2. always directly after a @caughtexception
        // 3. always when at least 2 exceptions are handled
        // 4. if multiple, it's always the first @caugtexception label that is removed
        // 5. remove all instances of this, even though compiler only removes one (do it to old and new version, so it does not matter)
        try {
            final String THROWABLE_EXCEPTION = "java.lang.Throwable";
            Chain<Trap> trapChain = body.getTraps();
            UnitPatchingChain unitChain = body.getUnits();
            List<Trap> trapsToRemove = new ArrayList<>();

            if (trapChain.size() < 2) return;

            for (Trap trap : trapChain) {
                Unit trapBegin = trap.getBeginUnit();
                Unit trapEnd = trap.getEndUnit();
                Unit unitBeforeTrapEnd = unitChain.getPredOf(trapEnd);

                // Check if there is only one statment in the catch block
                if (!trapBegin.equals(unitBeforeTrapEnd)) continue;
                // Check if the exception is of type java.lang.Throwable
                if (!trap.getException().getName().equals(THROWABLE_EXCEPTION)) continue;

                // Check if the single statement is a @caughtexception
                if (!(trapBegin instanceof IdentityStmt)) continue;
                IdentityStmt identityStmt = (IdentityStmt) trapBegin;
                if (!(identityStmt.getRightOp() instanceof CaughtExceptionRef)) continue;


                trapsToRemove.add(trap);
                if (statistics != null) statistics.emptyTryCatch += 1;
            }

            trapsToRemove.forEach(trapChain::remove);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Javac8 -> javac11
    private void normalizeTraps(Body body) {
        try {
            final String INPUTSTREAM_CLOSE_SIGNATURE = "<java.io.InputStream: void close()>";
            Chain<Unit> unitChain = body.getUnits();
            Chain<Trap> trapChain = body.getTraps();
            List<Trap> trapsToRemove = new ArrayList<>();


            for (Trap trap : trapChain) {
                Unit trapHandle = trap.getHandlerUnit();
                Unit trapEnd = trap.getEndUnit();
                Unit trapStart = trap.getBeginUnit();

                // remove overlapping traps
                if (unitChain.follows(trapEnd, trapHandle)) {
                    trapsToRemove.add(trap);
                    continue;
                }

                // remove traps that only cover <java.io.InputStream: void close()> statements
                if (!(trapStart instanceof InvokeStmt)) continue;
                InvokeStmt trapStartInvokeStmt = (InvokeStmt) trapStart;
                InvokeExpr trapStartInvokeExpr = trapStartInvokeStmt.getInvokeExpr();

                if (!trapStartInvokeExpr.getMethod().getName().equals("close")) continue;
                if (!unitChain.getPredOf(trapEnd).equals(trapStart)) continue;

                unitChain.remove(trapStart);
                trapsToRemove.add(trap);
                if (statistics != null) statistics.tryWithResources += 1;
            }

            trapsToRemove.forEach(trapChain::remove);
        } catch (Exception e) {
            // e.printStackTrace();
        }

//        if (!body.getMethod().getSignature().equals("<org.apache.commons.io.file.PathUtils: java.nio.file.Path copyFile(java.net.URL,java.nio.file.Path,java.nio.file.CopyOption[])>")) return;
//        System.out.println(trapChain);
//        System.out.println("-------------------");

//        final String ADDSUPPRESSED_METHOD_SIGNATURE = "<java.lang.Throwable: void addSuppressed(java.lang.Throwable)>";
//        InvokeStmt addSuppressedCall = null;
//        UnitPatchingChain unitChain = body.getUnits();
//        List<Unit> checkedTraps = new ArrayList<>();
//        boolean trapRemovedInLastIteration;
//
//        do {
//            boolean applyNormalization = false;
//            List<Unit> unitsToRemove = new ArrayList<>();
//
//            // Reset loop condition
//            trapRemovedInLastIteration = false;
//
//            // Look for a addSuppressed call
//            for (Unit unit : unitChain) {
//                if (!(unit instanceof InvokeStmt)) continue;
//                InvokeStmt invokeStmt = (InvokeStmt) unit;
//                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
//
//                if (!(invokeExpr instanceof VirtualInvokeExpr)) continue;
//                VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
//
//                if (!virtualInvokeExpr.getMethod().getSignature().equals(ADDSUPPRESSED_METHOD_SIGNATURE)) continue;
//                if (checkedTraps.contains(unit)) continue;
//                checkedTraps.add(unit);
//                addSuppressedCall = invokeStmt;
//                break;
//            }
//
//            if (addSuppressedCall == null) continue;
//            VirtualInvokeExpr addSuppressedCallInvoke = (VirtualInvokeExpr) addSuppressedCall.getInvokeExpr();
//            List<Value> trapBaseAliases = new ArrayList<>();
//            trapBaseAliases.add(addSuppressedCallInvoke.getBase());
//
//            for (Unit unit : unitChain) {
//                if (unit instanceof AssignStmt) {
//                    AssignStmt assignStmt = (AssignStmt) unit;
//                    Value lhs = assignStmt.getLeftOp();
//                    Value rhs = assignStmt.getRightOp();
//                    NullConstant nc = NullConstant.v();
//
//                    if (applyNormalization && trapBaseAliases.contains(lhs)) {
//                        unitsToRemove.add(unit);
//                        trapBaseAliases.add(rhs);
//                        Unit predUnit = unitChain.getPredOf(unit);
//                        if (predUnit instanceof IdentityStmt) {
//                            IdentityStmt identityStmt = (IdentityStmt) predUnit;
//                            Value predRhs = identityStmt.getRightOp();
//                            if (!(predRhs instanceof CaughtExceptionRef)) continue;
//                            unitsToRemove.add(predUnit);
//                        }
//                    } else if (trapBaseAliases.contains(lhs) && rhs.equals(nc)) {
//                        // Found a addSuppressedRef = null statement
//                        unitsToRemove.add(unit);
//                        applyNormalization = true;
//                    }
//                }
//
//                if (!applyNormalization) continue;
//
//                if (unit instanceof ThrowStmt) {
//                    ThrowStmt throwStmt = (ThrowStmt) unit;
//
//                    if (!trapBaseAliases.contains(throwStmt.getOp())) continue;
//                    unitsToRemove.add(unit);
//
//                    // Get the following caughtexception
//                    Unit followingUnit = unitChain.getSuccOf(unit);
//
//                    if (!(followingUnit instanceof IdentityStmt)) continue;
//                    IdentityStmt identityStmt = (IdentityStmt) followingUnit;
//                    Value followingLhs = identityStmt.getLeftOp();
//                    Value followingRhs = identityStmt.getRightOp();
//                    if (!(followingRhs instanceof CaughtExceptionRef)) continue;
//
//                    ((VirtualInvokeExpr) addSuppressedCall.getInvokeExpr()).setBase(followingLhs);
//
//                } else if (unit instanceof IfStmt) {
//                    IfStmt ifStmt = (IfStmt) unit;
//                    Value ifCondition = ifStmt.getCondition();
//                    Value leftOp = ifCondition.getUseBoxes().get(0).getValue();
//
//                    if (!trapBaseAliases.contains(leftOp)) continue;
//                    unitsToRemove.add(unit);
//
//                    // Track the target of the if statement
//                    Stmt ifTarget = ifStmt.getTarget();
//                    unitsToRemove.add(ifTarget);
//                }
//
//            }
//
//            if (unitsToRemove.size() > 0) {
//                unitsToRemove.forEach(unitChain::remove);
//                trapRemovedInLastIteration = true;
//            }
//        } while (trapRemovedInLastIteration);
//
//        // Filter all the unnecessary traps
//        Chain<Trap> trapChain = body.getTraps();
//        List<Trap> trapsToRemove = new ArrayList<>();
//
//
//        for (Trap trap : trapChain) {
//            Unit trapBegin = trap.getBeginUnit();
//            Unit trapHandler = trap.getHandlerUnit();
//            Unit trapEnd = trap.getEndUnit();
//
//
//            // Check trap begin
//            if (!(trapBegin instanceof IdentityStmt)) continue;
//            IdentityStmt trapBeginId = (IdentityStmt) trapBegin;
//            if (!(trapBeginId.getRightOp() instanceof CaughtExceptionRef)) continue;
//
//            // Check trap handler
//            if (!(trapHandler instanceof IdentityStmt)) continue;
//            IdentityStmt trapHandlerId = (IdentityStmt) trapHandler;
//            if (!(trapHandlerId.getRightOp() instanceof CaughtExceptionRef)) continue;
//            Value trapHandlerLhs = trapHandlerId.getLeftOp();
//
//            // Check trap end
//            if(!(trapEnd instanceof IfStmt)) continue;
//            IfStmt trapIf = (IfStmt) trapEnd;
//            Unit trapIfTarget = trapIf.getTarget();
//            if (!(trapIfTarget instanceof ThrowStmt)) continue;
//            ThrowStmt trapThrow = (ThrowStmt) trapIfTarget;
//            Value throwOp = trapThrow.getOp();
//            if (!throwOp.equals(trapHandlerLhs)) continue;
//
//            trapsToRemove.add(trap);
//        }
//
//        trapsToRemove.forEach(trapChain::remove);
//
//        // Get trap boundaries
//        Trap firstTrap = null;
//        Trap lastTrap = null;
//        for (Trap trap : trapChain) {
//            if (!trap.getException().getName().equals("java.lang.Throwable")) continue;
//            if (firstTrap == null) {
//                firstTrap = trap;
//            }
//            lastTrap = trap;
//        }
//        if (firstTrap == null) return;
//
//        // Filter all the = null checks which are between the remaining traps
//        List<Unit> nullChecksToRemove = new ArrayList<>();
//        for (Unit unit : unitChain) {
//            if (!(unit instanceof IfStmt)) continue;
//            IfStmt ifStmt = (IfStmt) unit;
//            Value ifCondition = ifStmt.getCondition();
//            Value lhs = ifCondition.getUseBoxes().get(0).getValue();
//            Value rhs = ifCondition.getUseBoxes().get(1).getValue();
//
//            if (!(rhs instanceof NullConstant)) continue;
//            if(!(lhs instanceof Local)) continue;
//            Local lhsLocal = (Local) lhs;
//            if(!lhsLocal.getName().contains("$")) continue;
//
//            if (!unitChain.follows(unit, firstTrap.getBeginUnit())) continue;
//            if (unitChain.follows(unit, lastTrap.getEndUnit())) break;
//
//            nullChecksToRemove.add(unit);
//        }
//        nullChecksToRemove.forEach(unitChain::remove);

        // remove duplicate @caughtexception and fix addSupressed base
//        unitChain.forEach(u -> System.out.println(u));
//        System.out.println("---------------------");
//        List<Unit> caughtexceptionsToRemove = new ArrayList<>();
//        for (Unit unit : unitChain) {
//            if (unitChain.getPredOf(unit) == null) continue;
//            Unit betweenUnit = unitChain.getPredOf(unit);
//
//            if (unitChain.getPredOf(betweenUnit) == null) continue;
//            Unit prevUnit = unitChain.getPredOf(betweenUnit);
//
//            if (!(unit instanceof IdentityStmt) || !(prevUnit instanceof IdentityStmt)) continue;
//            IdentityStmt idUnit = (IdentityStmt) unit;
//            IdentityStmt idPrevUnit = (IdentityStmt) prevUnit;
//            Value rhs = idUnit.getRightOp();
//            Value prevRhs = idPrevUnit.getRightOp();
//
//            if(!(rhs instanceof CaughtExceptionRef) || !(prevRhs instanceof CaughtExceptionRef)) continue;
//            caughtexceptionsToRemove.add(prevUnit);
//            Value newAddSuppressedBase = idUnit.getLeftOp();
//            VirtualInvokeExpr virtAddSuppressed = (VirtualInvokeExpr) addSuppressedCall.getInvokeExpr();
//            virtAddSuppressed.setBase(newAddSuppressedBase);
//        }
//        caughtexceptionsToRemove.forEach(unitChain::remove);
    }

    // Javac11 -> Javac17
    private void normalizeEnum(Body body) {
        try {
            SootMethod method = body.getMethod();
            SootClass declaringClass = method.getDeclaringClass();
            // Check if declaring class is enum
            if (!declaringClass.isEnum()) return;
            // Check if method is clinit
            if (!method.isStaticInitializer()) return;
            // Check if method already has moved out the enum values
            if (declaringClass.declaresMethodByName("$values")) return;

            SootField enumValues = declaringClass.getFieldByName("$VALUES");
            Type enumType = enumValues.getType();
            if (enumType instanceof ArrayType) {
                enumType = ((ArrayType) enumType).baseType;
            }

            UnitPatchingChain unitChain = body.getUnits();
            List<Unit> unitsToMove = new ArrayList<>();
            List<Unit> unitsToRemove = new ArrayList<>();
            AssignStmt enumInit = null;
            Value enumRef = null;
            int enumSize = 0;

            for (Unit unit : unitChain) {
                if (!(unit instanceof AssignStmt)) continue;
                AssignStmt assignStmt = (AssignStmt) unit;
                enumRef = assignStmt.getLeftOp();
                Value rhs = assignStmt.getRightOp();

                if (!(rhs instanceof NewArrayExpr)) continue;
                NewArrayExpr newArrayExpr = (NewArrayExpr) rhs;

                if (!newArrayExpr.getBaseType().equals(enumType)) continue;
                enumSize = ((IntConstant) newArrayExpr.getSize()).value;
                enumInit = assignStmt;

                // Enum init statement has been found if loop reaches this point
                // Break is intentional, as only the first enum init has to be moved
                break;
            }

            if (enumInit == null) return;
            unitsToRemove.add(enumInit);
            AssignStmt newEnumInit = Jimple.v().newAssignStmt((Value) enumInit.getLeftOp().clone(), (Value) enumInit.getRightOp().clone());
            unitsToMove.add(enumInit);

            // get all enum calls
            Unit currUnit = enumInit;
            for (int i = 0; i < enumSize * 2; i++) {
                currUnit = unitChain.getSuccOf(currUnit);

                if (!(currUnit instanceof AssignStmt)) continue;
                AssignStmt currAssign = (AssignStmt) currUnit;
                Value lhs = currAssign.getLeftOp();

                if (lhs instanceof ArrayRef) {
                    ArrayRef ref = (ArrayRef) lhs;
                    ref.setBase((Local) enumInit.getLeftOp());
                }

                unitsToRemove.add(currUnit);
                unitsToMove.add(currUnit);
            }

            // create new method that contains values
            SootMethod newEnumMethod = new SootMethod("$values", new ArrayList<>(), ArrayType.v(enumType, 1));
            newEnumMethod.setModifiers(Modifier.PRIVATE + Modifier.STATIC + Modifier.SYNTHETIC);

            // create new body for newly created method
            JimpleBody newEnumMethodBody = Jimple.v().newBody(newEnumMethod);
            newEnumMethod.setActiveBody(newEnumMethodBody);
            UnitPatchingChain newEnumMethodUnits = newEnumMethodBody.getUnits();
            newEnumMethodUnits.addAll(unitsToMove);
            newEnumMethodUnits.add(Jimple.v().newReturnStmt(enumInit.getLeftOp()));
            // Add newly created method to current class at second to last position
            newEnumMethod.setDeclaringClass(declaringClass);
            declaringClass.addMethod(newEnumMethod);
            List<SootMethod> methodsOfClass = declaringClass.getMethods();
            Collections.swap(methodsOfClass, methodsOfClass.size() - 1, methodsOfClass.size() - 2);

            // create new method invoke
            AssignStmt newEnumMethodInvoke = Jimple.v().newAssignStmt(
                    enumRef,
                    Jimple.v().newStaticInvokeExpr(newEnumMethod.makeRef())
            );
            unitChain.insertBefore(newEnumMethodInvoke, enumInit);

            // remove units from old method body
            unitsToRemove.forEach((unitChain::remove));

            // Create and add locals to newly created method
            for (Unit unit : unitsToMove) {
                if (!(unit instanceof AssignStmt)) continue;
                AssignStmt assignStmt = (AssignStmt) unit;
                Value lhs = assignStmt.getLeftOp();
                if (!(lhs instanceof Local)) continue;
                Local local = (Local) lhs;
                newEnumMethodBody.getLocals().add(local);
            }

            // Replace all previously existing locals by new ones, so changes to locals won't affect newly generated $values method
            DefaultLocalCreation dlc = new DefaultLocalCreation(body.getLocals());
            Map<String, Local> localsToReplace = new HashMap<>();
            for (Unit unit : unitChain) {
                List<ValueBox> valueBoxes = unit.getUseAndDefBoxes();
                for (ValueBox valueBox : valueBoxes) {
                    Value value = valueBox.getValue();
                    Local localToReplace = null;

                    if (value instanceof JArrayRef) {
                        JArrayRef ref = (JArrayRef) value;
                        Value base = ref.getBase();
                        if (base instanceof Local) {
                            localToReplace = (Local) base;
                        }
                    }

                    if (value instanceof Local) {
                        localToReplace = (Local) value;
                    }

                    if (localToReplace == null) continue;

                    Local newLocal = localsToReplace.getOrDefault(localToReplace.getName(), dlc.newLocal(localToReplace.getType()));

                    if (value instanceof JArrayRef) {
                        JArrayRef ref = (JArrayRef) value;
                        ref.setBase(newLocal);
                    } else {
                        valueBox.setValue(newLocal);
                    }

                    localsToReplace.put(newLocal.getName(), newLocal);
                    localsToReplace.put(localToReplace.getName(), newLocal);
                }
            }

            if (statistics != null) statistics.enumUsage += 1;
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Javac8 -> Javac11
    private void normalizeDuplicateTypeCast(Body body) {
        try {
            UnitPatchingChain unitChain = body.getUnits();
            List<Unit> unitsToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                if (!(unit instanceof AssignStmt)) continue;
                AssignStmt assignStmt1 = (AssignStmt) unit;
                Value lhs1 = assignStmt1.getLeftOp();
                Value rhs1 = assignStmt1.getRightOp();

                if (!(rhs1 instanceof CastExpr)) continue;
                Unit followingUnit = unitChain.getSuccOf(unit);

                if (!(followingUnit instanceof AssignStmt)) continue;
                AssignStmt assignStmt2 = (AssignStmt) followingUnit;
                Value lhs2 = assignStmt2.getLeftOp();
                Value rhs2 = assignStmt2.getRightOp();

                // TODO: Just removing the typecast for ref types kinda works well
                if (!(rhs2 instanceof CastExpr)) continue;
                CastExpr cast1 = (CastExpr) rhs1;
                CastExpr cast2 = (CastExpr) rhs2;

                if (!cast1.getCastType().equals(cast2.getCastType())) continue;
                if (!lhs1.equals(cast2.getOp())) continue;

                assignStmt1.setLeftOp(lhs2);
                unitsToRemove.add(followingUnit);
                if (statistics != null) statistics.duplicateCheckcast += 1;
            }

            unitsToRemove.forEach(unitChain::remove);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Target 6 - Target 7
    // Javac8 -> Javac11
    private void normalizeNullChecks(Body body) {
        try {
            final String REQUIRENONNULL_METHOD_SIGNATURE = "<java.util.Objects: java.lang.Object requireNonNull(java.lang.Object)>";

            UnitPatchingChain unitChain = body.getUnits();
            List<Unit> unitsToAdd = new ArrayList<>();
            List<Unit> unitsToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                if (!(unit instanceof InvokeStmt)) continue;
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                if (!(invokeExpr instanceof StaticInvokeExpr)) continue;
                StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) invokeExpr;
                String invokedMethodSignature = staticInvokeExpr.getMethodRef().getSignature();

                // Check if method signature is <java.util.Objects: java.lang.Object requireNonNull(java.lang.Object)>
                if (!(invokedMethodSignature.equals(REQUIRENONNULL_METHOD_SIGNATURE))) continue;

                // Create new normalized virtualinvoke call: virtualinvoke r2.<java.lang.Object: java.lang.Class getClass()>();
                SootMethod newRequireNonNullMethod = new SootMethod("getClass", new ArrayList<>(), RefType.v("java.lang.Class"));
                SootClass objectClass = new SootClass("java.lang.Object");
                objectClass.addMethod(newRequireNonNullMethod);
                VirtualInvokeExpr normalizedRequireNonNullMethod = Jimple.v().newVirtualInvokeExpr(
                        (Local) staticInvokeExpr.getArg(0),
                        newRequireNonNullMethod.makeRef()
                );
                InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(normalizedRequireNonNullMethod);

                unitsToAdd.add(newInvokeStmt);
                unitsToRemove.add(unit);
            }
            for (int i = 0; i < unitsToAdd.size(); i++) {
                unitChain.swapWith(unitsToRemove.get(i), unitsToAdd.get(i));
                if (statistics != null) statistics.methodRefOperator += 1;
                if (statistics != null) statistics.outerClassObjectCreation += 1;
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Javac8 -> Javac11
    private void normalizeBufferMethodCalls(Body body) {
        try {
            final String SUB_BUFFER_REGEX = "java\\.nio\\.[\\w]+Buffer";
            final String BUFFER_REGEX = "java\\.nio\\.[\\w]*Buffer";
            UnitPatchingChain unitChain = body.getUnits();

            List<Unit> unitsToAdd = new ArrayList<>();
            List<Unit> unitsToRemove = new ArrayList<>();

            List<BufferCall> bufferUsages = new ArrayList<>();

            for (Unit unit : unitChain) {
                if (unit instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) unit;
                    Value lhs = assignStmt.getLeftOp();
                    Value rhs = assignStmt.getRightOp();

                    if (!(rhs instanceof VirtualInvokeExpr)) continue;
                    VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) rhs;

                    // Check if the unit is a buffer method
                    if (!virtualInvokeExpr.getMethodRef().getDeclaringClass().toString().matches(SUB_BUFFER_REGEX))
                        continue;
                    String bufferName = virtualInvokeExpr.getMethodRef().getDeclaringClass().getName();
                    SootMethod bufferMethod = virtualInvokeExpr.getMethod();

                    if (!bufferMethod.getReturnType().toString().matches(BUFFER_REGEX)) continue;
                    SootMethod newBufferInvoke = new SootMethod(
                            virtualInvokeExpr.getMethod().getName(),
                            virtualInvokeExpr.getMethod().getParameterTypes(),
                            RefType.v(virtualInvokeExpr.getMethodRef().getDeclaringClass())
                    );
                    SootClass newBufferClass = new SootClass(bufferName);
                    newBufferClass.addMethod(newBufferInvoke);

                    Unit bufferInvoke = Jimple.v().newAssignStmt(
                            lhs,
                            Jimple.v().newVirtualInvokeExpr(
                                    (Local) virtualInvokeExpr.getBase(),
                                    newBufferInvoke.makeRef(),
                                    virtualInvokeExpr.getArgs()
                            )
                    );

                    unitsToRemove.add(unit);
                    unitsToAdd.add(bufferInvoke);
                    bufferUsages.add(new BufferCall((Local) lhs, bufferName));
                } else if (unit instanceof InvokeStmt) {
                    InvokeStmt invokeStmt = (InvokeStmt) unit;
                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                    if (!(invokeExpr instanceof VirtualInvokeExpr)) continue;

                    VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                    Value base = virtualInvokeExpr.getBase();

                    // No previous Buffer Assignment, but still a virtualinvoke of a buffer method
                    if (bufferUsages.size() < 1) {
                        // Check if the unit is a buffer method
                        if (!virtualInvokeExpr.getMethodRef().getDeclaringClass().toString().matches(SUB_BUFFER_REGEX))
                            continue;
                        String bufferName = virtualInvokeExpr.getMethodRef().getDeclaringClass().getName();
                        SootMethod bufferMethod = virtualInvokeExpr.getMethod();

                        if (!bufferMethod.getReturnType().toString().matches(BUFFER_REGEX)) continue;
                        SootMethod newBufferInvoke = new SootMethod(
                                virtualInvokeExpr.getMethod().getName(),
                                virtualInvokeExpr.getMethod().getParameterTypes(),
                                RefType.v(virtualInvokeExpr.getMethodRef().getDeclaringClass())
                        );
                        SootClass newBufferClass = new SootClass(bufferName);
                        newBufferClass.addMethod(newBufferInvoke);

                        Unit bufferInvoke = Jimple.v().newInvokeStmt(
                                Jimple.v().newVirtualInvokeExpr(
                                        (Local) virtualInvokeExpr.getBase(),
                                        newBufferInvoke.makeRef(),
                                        virtualInvokeExpr.getArgs()
                                )
                        );

                        unitsToRemove.add(unit);
                        unitsToAdd.add(bufferInvoke);
                    }

                    for (BufferCall bc : bufferUsages) {
                        if (!bc.getLocal().equals(base)) continue;

                        SootMethod newBufferInvoke = new SootMethod(
                                virtualInvokeExpr.getMethod().getName(),
                                virtualInvokeExpr.getMethod().getParameterTypes(),
                                RefType.v(bc.getBuffer())
                        );
                        SootClass newBufferClass = new SootClass(bc.getBuffer());
                        newBufferClass.addMethod(newBufferInvoke);

                        Unit bufferInvoke = Jimple.v().newInvokeStmt(
                                Jimple.v().newVirtualInvokeExpr(
                                        (Local) virtualInvokeExpr.getBase(),
                                        newBufferInvoke.makeRef(),
                                        virtualInvokeExpr.getArgs()
                                )
                        );

                        unitsToRemove.add(unit);
                        unitsToAdd.add(bufferInvoke);
                    }
                }
            }
            for (int i = 0; i < unitsToAdd.size(); i++) {
                unitChain.swapWith(unitsToRemove.get(i), unitsToAdd.get(i));
                if (statistics != null) statistics.bufferMethod += 1;
            }

            // fix locals
            for (BufferCall bc : bufferUsages) {
                bc.getLocal().setType(RefType.v(bc.getBuffer()));
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Java 8 -> Java 11
    private void normalizePrivateMethodCalls(Body body) {
        try {
            UnitPatchingChain unitChain = body.getUnits();

            List<Unit> unitsToAdd = new ArrayList<>();
            List<Unit> unitsToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                Unit specialInvokePrivate = null;
                try {
                    if (unit instanceof AssignStmt) {
                        // Unit is an AssignStmt
                        AssignStmt assign = (AssignStmt) unit;
                        Value lhs = assign.getLeftOp();
                        Value rhs = assign.getRightOp();

                        // Continue with loop if no virtualinvoke
                        if (!(rhs instanceof VirtualInvokeExpr)) continue;

                        VirtualInvokeExpr virtualInvoke = (VirtualInvokeExpr) rhs;
                        SootMethod calledMethod = ((VirtualInvokeExpr) rhs).getMethod();
                        // Check if called method is private
                        if (!calledMethod.isPrivate() && !SootHandler.privateMethods.contains(calledMethod)) continue;

                        // Create new AssignStmt that contains a specialInvoke
                        specialInvokePrivate = Jimple.v().newAssignStmt(
                                lhs,
                                Jimple.v().newSpecialInvokeExpr(
                                        (Local) virtualInvoke.getBase(),
                                        calledMethod.makeRef(),
                                        virtualInvoke.getArgs()
                                )
                        );
                    } else if (unit instanceof InvokeStmt) {
                        // Unit is an InvokeStmt
                        InvokeStmt invoke = (InvokeStmt) unit;
                        Expr expression = invoke.getInvokeExpr();

                        // Continue with loop if no virtualinvoke
                        if (!(expression instanceof VirtualInvokeExpr)) continue;

                        VirtualInvokeExpr virtualInvoke = (VirtualInvokeExpr) expression;
                        SootMethod calledMethod = virtualInvoke.getMethod();
                        // Check if called method is private
                        if (!calledMethod.isPrivate() && !SootHandler.privateMethods.contains(calledMethod)) continue;

                        // Create a new InvokeStmt that contains a specialinvoke
                        specialInvokePrivate = Jimple.v().newInvokeStmt(
                                Jimple.v().newSpecialInvokeExpr(
                                        (Local) virtualInvoke.getBase(),
                                        calledMethod.makeRef(),
                                        virtualInvoke.getArgs()
                                )
                        );
                    }

                    // Add new and old unit to list
                    if (specialInvokePrivate != null) {
                        unitsToAdd.add(specialInvokePrivate);
                        unitsToRemove.add(unit);
                    }
                } catch (Exception e) {
                    System.out.println(body.getMethod().getSignature());
                    System.out.println(unit);
                    System.out.println(e.getStackTrace());
                    System.out.println("-------------------------------------");
                }
            }

            // Replace old virtualinvoke by new specialinvoke
            for (int i = 0; i < unitsToAdd.size(); i++) {
                unitChain.swapWith(unitsToRemove.get(i), unitsToAdd.get(i));
                if (statistics != null) statistics.privateMethodInvocation += 1;
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Java 8 -> Java 11
    private void normalizeStringConcat(Body body) {
        try {
            final String STRINGBUILDER_TYPE = "java.lang.StringBuilder";
            final String STRINGBUILDER_INIT_SIGNATURE = "<java.lang.StringBuilder: void <init>()>";

            UnitPatchingChain unitChain = body.getUnits();
            List<StringBuilderConcat> stringBuilderConcats = new ArrayList<>();

            for (Unit unit : unitChain) {
                // Check for the StringBuilder <init> call
                if (unit instanceof InvokeStmt) {
                    InvokeStmt invokeStmt = (InvokeStmt) unit;
                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                    if (!(invokeExpr instanceof SpecialInvokeExpr)) continue;
                    SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;
                    Value value = specialInvokeExpr.getBase();

                    if (!(value instanceof Local)) continue;
                    Local local = (Local) value;

                    // Check if we have a StringBuilderCall
                    Optional<StringBuilderConcat> sbc = stringBuilderConcats.stream().filter(sb -> sb.aliasContained(local)).findFirst();
                    if (!sbc.isPresent()) continue;

                    // specialinvoke of StringBuilder constructor
                    if (!specialInvokeExpr.getMethodRef().getSignature().equals(STRINGBUILDER_INIT_SIGNATURE)) continue;
                    sbc.get().addStringBuilderCall(unit);
                }

                if (!(unit instanceof AssignStmt)) continue;
                // At this point the unit is an AssignStmt
                AssignStmt assignStmt = (AssignStmt) unit;

                // Check if lhs is a Local
                Value lhs = assignStmt.getLeftOp();
                if (!(lhs instanceof Local)) continue;
                Local lhsLocal = (Local) lhs;

                Value rhs = assignStmt.getRightOp();

                // Check for new StringBuilder call
                if (rhs instanceof NewExpr) {
                    NewExpr newExpr = (NewExpr) rhs;

                    if (!newExpr.getBaseType().toQuotedString().equals(STRINGBUILDER_TYPE)) continue;
                    // We have a new StringBuilder call
                    StringBuilderConcat sbc = new StringBuilderConcat(lhsLocal, unit);
                    stringBuilderConcats.add(sbc);
                }

                // Check for StringBuilder.append and StringBuilder.toString calls
                if (!(rhs instanceof VirtualInvokeExpr)) continue;
                VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) rhs;
                SootMethod sm = virtualInvokeExpr.getMethod();

                if (!sm.getDeclaringClass().getName().equals(STRINGBUILDER_TYPE)) continue;
                // We have a StringBuilder call
                if (!(virtualInvokeExpr.getBase() instanceof Local)) continue;
                Local sbCallTarget = (Local) virtualInvokeExpr.getBase();

                // Now get the corresponding sbc Object
                Optional<StringBuilderConcat> sbcOptional = stringBuilderConcats.stream().filter(sb -> sb.aliasContained(sbCallTarget)).findFirst();
                if (!sbcOptional.isPresent()) continue;

                StringBuilderConcat sbc = sbcOptional.get();

                if (sm.getName().equals("append")) {
                    // We have a StringBuilder.append call
                    sbc.addAlias(lhsLocal);
                    sbc.addStringBuilderCall(unit);

                    // Parameter type of StringBuilder.append call
                    Type sbParameterType = sm.getParameterType(0);
                    Value sbArgument = virtualInvokeExpr.getArg(0);

                    if (sbArgument == null) continue;

                    // arguments are checked here

                    // if there is a "-" sign it should be handled like a variable string literal
                    // this is apparently due to weird compiler stuff

//                if (sbArgument instanceof StringConstant && !((StringConstant) sbArgument).value.equals("-")) {
                    if (sbArgument instanceof StringConstant) {
                        // argument is a String constant
                        StringConstant sbArgumentStringConstant = (StringConstant) sbArgument;
                        sbc.concat(sbArgumentStringConstant.value);
                    } else if (sbParameterType.equals(BooleanType.v()) && sbArgument instanceof IntConstant) {
                        // argument type is boolean and argument is an IntConstant
                        // in bytecode this is expressed as an IntConstant
                        // Therefore boolean value has to be determined from the int
                        IntConstant boolValue = (IntConstant) sbArgument;
                        String boolValueString = boolValue.value > 0 ? "true" : "false";
                        sbc.concat(boolValueString);
                    } else if (sbParameterType.equals(CharType.v()) && sbArgument instanceof IntConstant) {
                        // argument type is char
                        // this means that the int is used as index for a character
                        IntConstant sbArgumentIntConstant = (IntConstant) sbArgument;
                        char correspondingChar = (char) sbArgumentIntConstant.value;
                        sbc.concat(String.valueOf(correspondingChar));
                    } else if (sbParameterType.equals(IntType.v()) && sbArgument instanceof IntConstant) {
                        IntConstant sbArgumentIntConstant = (IntConstant) sbArgument;
                        sbc.concat(String.valueOf(sbArgumentIntConstant.value));
                    } else {
                        // argument is dynamic
                        boolean intCast = false;
                        if (sbParameterType.equals(IntType.v())) {
                            Unit prevUnit = unitChain.getPredOf(unit);
                            if (prevUnit instanceof AssignStmt) {
                                AssignStmt prevUnitAssignStmt = (AssignStmt) prevUnit;
                                Value rOp = prevUnitAssignStmt.getRightOp();
                                if (rOp instanceof CastExpr) {
                                    intCast = true;
                                    // previous unit is a typecast
                                    CastExpr cast = (CastExpr) rOp;
                                    Type originalType = cast.getOp().getType();
                                    sbc.concat("\u0001");
                                    sbc.addDynamicArgument(sbArgument, originalType);
                                }
                            }
                        }

                        if (!intCast) {
                            sbc.concat("\u0001");
                            // this means we also need to add the type and value of the dynamic value
                            sbc.addDynamicArgument(sbArgument, sbArgument.getType());
                        }
                    }

                } else if (sm.getName().equals("toString")) {
                    // We have a StringBuilder.toString call
                    sbc.setEndUnit(unit);

                    // Construct a new dynamicinvoke expression
                    List<Type> bootstrapTypes = new ArrayList<>();
                    bootstrapTypes.add(RefType.v("java.lang.invoke.MethodHandles$Lookup"));
                    bootstrapTypes.add(RefType.v("java.lang.String"));
                    bootstrapTypes.add(RefType.v("java.lang.invoke.MethodType"));
                    bootstrapTypes.add(RefType.v("java.lang.String"));
                    bootstrapTypes.add(ArrayType.v(RefType.v("java.lang.Object"), 1));

                    SootMethodRefImpl bootstrap = new SootMethodRefImpl(new SootClass("java.lang.invoke.StringConcatFactory"), "makeConcatWithConstants", bootstrapTypes, RefType.v("java.lang.invoke.CallSite"), false);
                    SootMethodRefImpl stringConcat = new SootMethodRefImpl(new SootClass("soot.dummy.InvokeDynamic"), "makeConcatWithConstants", sbc.getDynamicTypes(), RefType.v("java.lang.String"), false);

                    List<Value> bootstrapArgs = new ArrayList<>();
                    bootstrapArgs.add(StringConstant.v(sbc.getTemplate()));

                    AssignStmt dynamicStringConcat = Jimple.v().newAssignStmt(
                            lhsLocal,
                            Jimple.v().newDynamicInvokeExpr(
                                    bootstrap,
                                    bootstrapArgs,
                                    stringConcat,
                                    sbc.getDynamicValues()
                            )
                    );

                    sbc.setNewStringConcatCall(dynamicStringConcat);
                }
            }

            for (StringBuilderConcat sbc : stringBuilderConcats) {
                if (sbc.getEndUnit() == null) continue;
                if (sbc.getNewStringConcatCall() == null) continue;
                unitChain.swapWith(sbc.getEndUnit(), sbc.getNewStringConcatCall());
                unitChain.removeAll(sbc.getStringBuilderCalls());
                if (statistics != null) statistics.stringConstantConcat += 1;
                if (statistics != null) statistics.dynamicStringConcat += 1;
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }

    }

    // Javac5 -> Javac6
    private void normalizeArithmeticOperations(Body body) {
        try {
            List<Unit> unitsToRemove = new ArrayList<>();
            UnitPatchingChain unitChain = body.getUnits();

            for (Unit unit : unitChain) {
                for (ValueBox vb : unit.getUseBoxes()) {
                    Value v = vb.getValue();

                    // Look for AddExpressions
                    if (!(v instanceof AddExpr)) continue;
                    AddExpr addition = (AddExpr) v;

                    // Get previous unit
                    Unit prevUnit = unitChain.getPredOf(unit);
                    if (prevUnit == null) continue;
                    if (!(prevUnit instanceof AssignStmt)) continue;
                    AssignStmt assignStmt = (AssignStmt) prevUnit;
                    Value rhs = assignStmt.getRightOp();
                    if (!(rhs instanceof CastExpr)) continue;
                    CastExpr cast = (CastExpr) rhs;

                    if (!(cast.getOp() instanceof IntConstant)) continue;
                    int op2Value = ((IntConstant) cast.getOp()).value;

                    if (op2Value >= 0) continue;
                    // Create a SubExpr and replace the old AddExpr with it
                    Value newOp2 = IntConstant.v(Math.abs(op2Value));
                    SubExpr subtraction = Jimple.v().newSubExpr(addition.getOp1(), newOp2);
                    vb.setValue(subtraction);

                    unitsToRemove.add(prevUnit);
                    if (statistics != null) statistics.arithmetic += 1;
                }
            }
            unitsToRemove.forEach(unitChain::remove);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Javac6 -> javac7
    private void normalizeCharSequenceToStringInvoke(Body body) {
        try {
            final String CHARSEQUENCE_CLASS = "java.lang.CharSequence";
            final String OBJECT_CLASS = "java.lang.Object";
            final String TOSTRING_METHOD = "toString";

            UnitPatchingChain unitChain = body.getUnits();

            List<Unit> unitsToAdd = new ArrayList<>();
            List<Unit> unitsToRemove = new ArrayList<>();

            for (Unit unit : unitChain) {
                Unit virtualInvokeCharSequence = null;

                if (unit instanceof AssignStmt) {
                    // Unit is an AssignStmt
                    AssignStmt assign = (AssignStmt) unit;
                    Value lhs = assign.getLeftOp();
                    Value rhs = assign.getRightOp();

                    // Continue with loop if no interfaceinvoke
                    if (!(rhs instanceof InterfaceInvokeExpr)) continue;

                    InterfaceInvokeExpr interfaceInvoke = (InterfaceInvokeExpr) rhs;
                    SootMethod calledMethod = ((InterfaceInvokeExpr) rhs).getMethod();

                    // Check if the called method is toString of class java.lang.CharSequence
                    if (!calledMethod.getDeclaringClass().getName().equals(CHARSEQUENCE_CLASS)) continue;
                    if (!calledMethod.getName().equals(TOSTRING_METHOD)) continue;

                    // Create new toString method of the java.lang.Object class
                    SootMethod toString = new SootMethod(TOSTRING_METHOD, new ArrayList<>(), RefType.v("java.lang.String"));
                    SootClass objectClass = new SootClass(OBJECT_CLASS);
                    objectClass.addMethod(toString);

                    // Create new AssignStmt that contains a virtualinvoke
                    virtualInvokeCharSequence = Jimple.v().newAssignStmt(
                            lhs,
                            Jimple.v().newVirtualInvokeExpr(
                                    (Local) interfaceInvoke.getBase(),
                                    toString.makeRef(),
                                    interfaceInvoke.getArgs()
                            )
                    );

                } else if (unit instanceof InvokeStmt) {
                    // Unit is an InvokeStmt
                    InvokeStmt invoke = (InvokeStmt) unit;
                    Expr expression = invoke.getInvokeExpr();

                    // Continue with loop if no interfaceinvoke
                    if (!(expression instanceof InterfaceInvokeExpr)) continue;

                    InterfaceInvokeExpr interfaceInvoke = (InterfaceInvokeExpr) expression;
                    SootMethod calledMethod = interfaceInvoke.getMethod();

                    // Check if the called method is toString of class java.lang.CharSequence
                    if (!calledMethod.getDeclaringClass().getName().equals(CHARSEQUENCE_CLASS)) continue;
                    if (!calledMethod.getName().equals(TOSTRING_METHOD)) continue;

                    // Create new toString method of the java.lang.Object class
                    SootMethod toString = new SootMethod(TOSTRING_METHOD, new ArrayList<>(), RefType.v("java.lang.String"));
                    SootClass objectClass = new SootClass(OBJECT_CLASS);
                    objectClass.addMethod(toString);

                    // Create a new InvokeStmt that contains a virtualinvoke
                    virtualInvokeCharSequence = Jimple.v().newInvokeStmt(
                            Jimple.v().newVirtualInvokeExpr(
                                    (Local) interfaceInvoke.getBase(),
                                    toString.makeRef(),
                                    interfaceInvoke.getArgs()
                            )
                    );
                }

                // Add new and old unit to list
                if (virtualInvokeCharSequence != null) {
                    unitsToAdd.add(virtualInvokeCharSequence);
                    unitsToRemove.add(unit);
                }
            }

            // Replace old interfaceinvoke by new virtualinvoke
            for (int i = 0; i < unitsToAdd.size(); i++) {
                unitChain.swapWith(unitsToRemove.get(i), unitsToAdd.get(i));
                if (statistics != null) statistics.charSequenceToString += 1;
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Javac5 -> Javac6
    private boolean isSynthetic(SootMethod method) {
        int methodModifiers = method.getModifiers();
        // Do not remove synthetic enum method just yet
        // Will be removed after necessary information is extracted
        if (method.getName().equals("$values")) return false;

        return Modifier.isSynthetic(methodModifiers);
    }
}
