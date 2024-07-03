package jnorm.core;

import soot.*;
import soot.jimple.toolkits.scalar.DeadAssignmentEliminator;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.toolkits.scalar.UnusedLocalEliminator;
import soot.util.Chain;

import java.util.*;
import java.util.stream.Collectors;

public class LocalNormalizer {

    public void normalize(Body body) {
        NopEliminator.v().transform(body);
        UnusedLocalEliminator.v().transform(body);
        DeadAssignmentEliminator.v().transform(body);
        standardizeLocalsSimple(body);
        sortLocals(body);
    }

    private void standardizeLocalsSimple(Body body) {
        List<Local> renamedLocals = new ArrayList<>();
        Chain<Unit> unitChain = body.getUnits();
        int localCounter = 0;

        for (Unit unit : unitChain) {
            List<ValueBox> defBoxes = unit.getDefBoxes();
            for (ValueBox defBox : defBoxes) {
                Value def = defBox.getValue();
                if (!(def instanceof Local)) continue;
                Local defLocal = (Local) def;
                if(renamedLocals.contains(defLocal)) continue;

                defLocal.setName("v" + localCounter);
                renamedLocals.add(defLocal);
                localCounter++;
            }
        }

        for (Local local : body.getLocals()) {
            if (!renamedLocals.contains(local)) {
                local.setName("v" + localCounter);
                localCounter++;
            }
        }
    }

    private void standardizeLocals(Body body) {
        List<Local> renamedLocals = new ArrayList<>();
        Chain<Unit> unitChain = body.getUnits();
        Map<Character, Integer> localCounters = new HashMap<>();

        for (Unit unit : unitChain) {
            List<ValueBox> defBoxes = unit.getDefBoxes();
            for (ValueBox defBox : defBoxes) {
                Value def = defBox.getValue();
                if (!(def instanceof Local)) continue;
                Local defLocal = (Local) def;
                if(renamedLocals.contains(defLocal)) continue;
                String localName = defLocal.getName();

                if (localName.startsWith("$")) {
                    localName = localName.substring(1);
                }

                Character localChar = localName.charAt(0);
                int localCounter = localCounters.getOrDefault(localChar, 0);
                String newLocalName = String.valueOf(localChar) + localCounter;

                defLocal.setName(newLocalName);
                localCounters.put(localChar, localCounter + 1);
                renamedLocals.add(defLocal);
            }
        }
    }

    // Java 8 -> Java 11
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
}
