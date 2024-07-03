package jnorm.core;

import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.scalar.UnusedLocalEliminator;
import soot.util.Chain;

import java.util.*;
import java.util.stream.Collectors;

public class RenameLocalBodyTransformer extends BodyTransformer {
    static int hashWindow;

    public static void addToSootConfig(int renamingWindow) {
        PackManager.v()
                .getPack("jap")
                .add(new Transform("jap.rename", new RenameLocalBodyTransformer()));
        Options.v().set_verbose(true);
        soot.options.Options.v().setPhaseOption("jap", "enabled:" + true);
        soot.options.Options.v().setPhaseOption("jap.rename", "enabled:" + true);
        hashWindow = renamingWindow;
    }

    @Override
    protected void internalTransform(Body body, String s, Map<String, String> map) {
        UnusedLocalEliminator.v().transform(body);
        renameLocals(body);
        SootMethod enumValues = body.getMethod().getDeclaringClass().getMethodByNameUnsafe("$values");
        if (enumValues != null) {
            renameLocals(enumValues.getActiveBody());
        }
        sortLocals(body);
    }

    private void renameLocals(Body body) {
        int hashWindow = RenameLocalBodyTransformer.hashWindow;

        UnitPatchingChain unitChain = body.getUnits();
        Body clonedBody = (Body) body.clone();
        List<Unit> clonedUnitsList = new ArrayList<>(clonedBody.getUnits());

        Chain<Local> localChain = body.getLocals();
        Map<Local, Integer> localHashes = new HashMap<>();

        int index = 0;
        for (Unit unit : unitChain) {
            List<Unit> hashUnits = new ArrayList<>();
            Local local = null;
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value lhs = assignStmt.getLeftOp();
                if (!(lhs instanceof Local)) continue;
                local = (Local) lhs;
            } else if (unit instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) unit;
                Value lhs = identityStmt.getLeftOp();
                if (!(lhs instanceof Local)) continue;
                local = (Local) lhs;
            }

            if (local == null) continue;

            Unit clonedUnit = clonedUnitsList.get(index);
            removeLocalNames(clonedUnit);
            hashUnits.add(clonedUnit);
            for (int i = 1; i <= hashWindow; i++) {
                if (index - i >= 0) {
                    Unit prevUnit = clonedUnitsList.get(index -i);
                    removeLocalNames(prevUnit);
                    hashUnits.add(prevUnit);
                }
                if (index + i < clonedUnitsList.size()) {
                    Unit nextUnit = clonedUnitsList.get(index + i);
                    removeLocalNames(nextUnit);
                    hashUnits.add(nextUnit);
                }
            }

            String hashUnitsString = hashUnits
                    .stream()
                    .map(u -> u.toString())
                    .collect(Collectors.joining(", "));

            localHashes.put(local, Math.abs(hashUnitsString.hashCode()));
            index++;
        }

        Map<String, Integer> collisions = new HashMap<>();
        for (Local local : localChain) {
            Integer localHash = localHashes.get(local);
            if (localHash != null) {
                String oldLocalName = local.getName();
                String newLocalNamePrefix = oldLocalName.startsWith("$") ? "$" + oldLocalName.charAt(1) : String.valueOf(oldLocalName.charAt(0));
                String newName = newLocalNamePrefix + localHash;
                collisions.put(newName, collisions.getOrDefault(newName, 0) + 1);

                local.setName(newName + "_" + collisions.get(newName));
            }
        }
    }

    private void removeLocalNames (Unit unit) {
        List<ValueBox> valueBoxes = unit.getUseAndDefBoxes();
        for (ValueBox vb : valueBoxes) {
            Value val = vb.getValue();
            if (val instanceof Local) {
                vb.setValue(Jimple.v().newLocal("local", RefType.v("java.lang.Object")));
            }
            List<ValueBox> innerVBs = val.getUseBoxes();
            for (ValueBox innerVB : innerVBs) {
                Value innerVal = innerVB.getValue();
                if (innerVal instanceof Local) {
                    innerVB.setValue(Jimple.v().newLocal("local", RefType.v("java.lang.Object")));
                }
            }
        }
        if (unit instanceof GotoStmt) {
            GotoStmt gotoStmt = (GotoStmt) unit;
            gotoStmt.setTarget(Jimple.v().newNopStmt());
        } else if (unit instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) unit;
            ifStmt.setTarget(Jimple.v().newNopStmt());
        } else if (unit instanceof SwitchStmt) {
            SwitchStmt switchStmt = (SwitchStmt) unit;
            switchStmt.setDefaultTarget(Jimple.v().newNopStmt());
            List<Unit> targets = switchStmt.getTargets();
            for (int i = 0; i < targets.size(); i++) {
                switchStmt.setTarget(i, Jimple.v().newNopStmt());
            }
        }
    }

    private void sortLocals(Body body) {
        Collection<Local> localCollection = body.getLocals().getElementsUnsorted();
        List<Local> localList = new ArrayList<>(localCollection);

        // Sort based on type first and then sort based on name
        localList.sort((o1, o2) -> {
            int typeComparison = o1.getType().toString().compareTo(o2.getType().toString());
            if (typeComparison > 0) return 1;
            if (typeComparison < 0) return -1;

            return o1.getName().compareTo(o2.getName());
        });

        body.getLocals().clear();
        body.getLocals().addAll(localList);
    }

//    private void renameLocals(Body body) {
//        // TODO: In next version do not remove goto targets
//        // TODO: Use a sencond scene in FutureSoot
//        Map<Local, String> localAndStatements = new HashMap<>();
//        UnitPatchingChain unitChain = body.getUnits();
//        Chain<Local> localChain = body.getLocals();
//        List<Unit> clonedUnits = new ArrayList<>();
//
//        for (Unit unit : unitChain) {
//            clonedUnits.add((Unit) unit.clone());
//        }
//
//        for (Unit unit : clonedUnits) {
//            Set<Local> usedLocals = removeLocalNames(unit);
//            if (unit instanceof GotoStmt) {
//                GotoStmt gotoStmt = (GotoStmt) unit;
//                gotoStmt.setTarget(Jimple.v().newNopStmt());
////                Set<Local> targetLocals = removeLocalNames(gotoStmt.getTarget());
////                usedLocals.addAll(targetLocals);
//            } else if (unit instanceof IfStmt) {
//                IfStmt ifStmt = (IfStmt) unit;
//                ifStmt.setTarget(Jimple.v().newNopStmt());
////                Unit target = ifStmt.getTarget();
////                if (target instanceof GotoStmt) continue;
////                Set<Local> targetLocals = removeLocalNames(ifStmt.getTarget());
////                usedLocals.addAll(targetLocals);
//            } else if (unit instanceof SwitchStmt) {
//                SwitchStmt switchStmt = (SwitchStmt) unit;
//                switchStmt.setDefaultTarget(Jimple.v().newNopStmt());
//                List<Unit> targets = switchStmt.getTargets();
//                for (int i = 0; i < targets.size(); i++) {
//                    switchStmt.setTarget(i, Jimple.v().newNopStmt());
//                }
//            }
//
//            for (Local local : usedLocals) {
//                String prevStatement = localAndStatements.getOrDefault(local, "");
//                localAndStatements.put(local, prevStatement +"\n" + unit);
//            }
//        }
//
//        Map<String, Integer> collisions = new HashMap<>();
//        for (Local local : localChain) {
//            String localStatement = localAndStatements.get(local);
//            if (localStatement != null) {
//                String oldLocalName = local.getName();
//                String newLocalNamePrefix = oldLocalName.startsWith("$") ? "$" + oldLocalName.charAt(1) : String.valueOf(oldLocalName.charAt(0));
//                String newName = newLocalNamePrefix + Math.abs(localStatement.hashCode());
//                collisions.put(newName, collisions.getOrDefault(newName, 0) + 1);
//
//                local.setName(newName + "_" + collisions.get(newName));
//            }
//        }
//
//    }

//    private Set<Local> removeLocalNames (Unit unit) {
//        Set<Local> usedLocals = new HashSet<>();
//        List<ValueBox> valueBoxes = unit.getUseAndDefBoxes();
//        for (ValueBox vb : valueBoxes) {
//            Value val = vb.getValue();
//            if (val instanceof Local) {
//                usedLocals.add((Local) val);
//                vb.setValue(Jimple.v().newLocal("local", RefType.v("java.lang.Object")));
//            }
//            List<ValueBox> innerVBs = val.getUseBoxes();
//            for (ValueBox innerVB : innerVBs) {
//                Value innerVal = innerVB.getValue();
//                if (innerVal instanceof Local) {
//                    usedLocals.add((Local) innerVal);
//                    innerVB.setValue(Jimple.v().newLocal("local", RefType.v("java.lang.Object")));
//                }
//            }
//        }
//        return usedLocals;
//    }
}
