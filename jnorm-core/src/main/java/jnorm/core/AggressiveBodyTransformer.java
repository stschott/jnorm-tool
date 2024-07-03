package jnorm.core;

import jnorm.core.model.NormalizationStatistics;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.options.Options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggressiveBodyTransformer extends BodyTransformer {
    private NormalizationStatistics statistics;

    public AggressiveBodyTransformer(NormalizationStatistics statistics) {
        this.statistics = statistics;
    }

    public void addToSootConfig() {
        PackManager.v()
                .getPack("jtp")
                .add(new Transform("jtp.aggressive", this));
        Options.v().set_verbose(true);
        soot.options.Options.v().setPhaseOption("jtp", "enabled:" + true);
        soot.options.Options.v().setPhaseOption("jtp.aggressive", "enabled:" + true);
    }

    @Override
    protected void internalTransform(Body body, String s, Map<String, String> map) {
        try {
            removeTypeCasts(body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeTypeCasts(Body body) {
        UnitPatchingChain unitChain = body.getUnits();
        Map<Local, Local> localAliases = new HashMap<>();
        List<Unit> unitsToRemove = new ArrayList<>();

        for (Unit unit : unitChain) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value lhs = assignStmt.getLeftOp();
                Value rhs = assignStmt.getRightOp();

                if (rhs instanceof CastExpr) {
                    // Unit is a type cast
                    CastExpr castExpr = (CastExpr) rhs;
                    Value castOp = castExpr.getOp();

                    if (lhs instanceof Local && castOp instanceof Local) {
                        Local oldLocal = (Local) lhs;
                        Local newLocal = (Local) castOp;
                        unitsToRemove.add(unit);
                        localAliases.putIfAbsent(oldLocal, newLocal);
                        continue;
                    }
                }
            }

            // Check if unit makes use of a type cast local
            List<ValueBox> valueBoxes = unit.getUseAndDefBoxes();
            for (ValueBox vb : valueBoxes) {
                Value value = vb.getValue();

                if (!(value instanceof Local)) continue;
                Local currentLocal = (Local) value;

                if(!localAliases.containsKey(currentLocal)) continue;
                Local newLocal = localAliases.get(currentLocal);

                vb.setValue(newLocal);
                if (statistics != null) statistics.typecheck += 1;
            }
        }
        unitsToRemove.forEach(unitChain::remove);
    }
}
