package jnorm.core;

import soot.*;
import soot.options.Options;
import soot.util.Chain;

import java.util.*;

public class StandardizeBodyTransformer extends BodyTransformer {
    LocalNormalizer ln;

    public StandardizeBodyTransformer(LocalNormalizer ln) {
        this.ln = ln;
    }

    public void addToSootConfig() {
        PackManager.v()
                .getPack("jap")
                .add(new Transform("jap.standardize", this));
        Options.v().set_verbose(true);
        Options.v().setPhaseOption("jap", "enabled:" + true);
        Options.v().setPhaseOption("jap.standardize", "enabled:" + true);
    }



    @Override
    protected void internalTransform(Body body, String s, Map<String, String> map) {
        ln.normalize(body);
        Chain<SootClass> allClasses = Scene.v().getApplicationClasses();
        for (SootClass sc : allClasses) {
            List<SootMethod> methods = sc.getMethods();
            for (SootMethod sm : methods) {
                if (sm.getName().equals("$values")) {
                    ln.normalize(sm.getActiveBody());
                }
            }
        }
    }

}
