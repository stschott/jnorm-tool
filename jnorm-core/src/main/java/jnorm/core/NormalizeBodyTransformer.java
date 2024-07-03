package jnorm.core;

import soot.*;
import soot.options.Options;

import java.util.*;

public class NormalizeBodyTransformer extends BodyTransformer {
    CoreBodyNormalizer cbn;
    LocalNormalizer ln;

    public NormalizeBodyTransformer(CoreBodyNormalizer cbn, LocalNormalizer ln) {
        this.cbn = cbn;
        this.ln = ln;
    }

    public void addToSootConfig() {
        PackManager.v()
                .getPack("jtp")
                .add(new Transform("jtp.normalize", this));
        Options.v().set_verbose(true);
        soot.options.Options.v().setPhaseOption("jtp", "enabled:" + true);
        soot.options.Options.v().setPhaseOption("jtp.normalize", "enabled:" + true);
    }

    @Override
    protected void internalTransform(Body body, String s, Map<String, String> map) {
        cbn.normalize(body);
    }

}
