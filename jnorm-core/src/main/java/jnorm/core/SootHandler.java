package jnorm.core;

import jnorm.core.model.JimpleClass;
import jnorm.core.model.NormalizationStatistics;
import soot.*;
import soot.util.Chain;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class SootHandler {
    static List<SootMethod> syntheticMethods = new ArrayList<>();
    static List<SootMethod> privateMethods = new ArrayList<>();
    private NormalizationStatistics statistics;

    public SootHandler(boolean applyOptimization, boolean applyNormalization, boolean applyAggressiveNormalization,
                       boolean applyStandardization, int renamingWindow, NormalizationStatistics statistics) {

        G.reset();
        soot.options.Options.v().set_num_threads(1);
        soot.options.Options.v().set_debug(false);
        soot.options.Options.v().set_debug_resolver(true);
        soot.options.Options.v().set_prepend_classpath(true);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_output_format(soot.options.Options.output_format_none);
        soot.options.Options.v().set_allow_phantom_refs(true);
        soot.options.Options.v().set_omit_excepting_unit_edges(true);
        soot.options.Options.v().setPhaseOption("jb.sils", "enabled:false");
        soot.options.Options.v().setPhaseOption("jb", "use-original-names:false");

        this.statistics = statistics;
        CoreBodyNormalizer cbn = new CoreBodyNormalizer(this.statistics);
        LocalNormalizer ln = new LocalNormalizer();

        if (applyNormalization) {
            // apply normalization
            NormalizeBodyTransformer normalizeBodyTransformer = new NormalizeBodyTransformer(cbn, ln);
            normalizeBodyTransformer.addToSootConfig();
        }

        if (applyAggressiveNormalization) {
            // apply aggressive normalization
            AggressiveBodyTransformer aggressiveBodyTransformer = new AggressiveBodyTransformer(this.statistics);
            aggressiveBodyTransformer.addToSootConfig();
        }

        if (applyOptimization) {
            // apply optimizations
            soot.options.Options.v().setPhaseOption("jb.ule", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jb.dae", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jb.cp-ule", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop.cp", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop.cpf", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop.dae", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop.ubf1", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop.ubf2", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop.ule", "enabled:" + true);
            soot.options.Options.v().setPhaseOption("jop.nce", "enabled:" + true);
        }

        if (renamingWindow > -1) {
            // apply local renaming
            RenameLocalBodyTransformer.addToSootConfig(renamingWindow);
        }

        if (applyStandardization) {
            StandardizeBodyTransformer standardizeBodyTransformer = new StandardizeBodyTransformer(ln);
            standardizeBodyTransformer.addToSootConfig();
        }
    }

    public void loadDir(String filepath) {
        soot.options.Options.v().set_process_dir(Collections.singletonList(filepath));

        Scene.v().loadNecessaryClasses();

        // get private methods before the information is lost for some reason
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            for (SootMethod sm : sc.getMethods()) {
                if (!sm.isPrivate()) continue;
                privateMethods.add(sm);
            }
        }

        PackManager.v().runPacks();
        this.statistics.amountOfClasses = Scene.v().getApplicationClasses().size();
        this.statistics.amountOfMethods = Scene.v().getApplicationClasses().stream().mapToInt(c -> c.getMethods().size()).sum();
    }


    public ArrayList<JimpleClass> generateJimpleStrings() {
        return this.generateJimpleStrings(new String[]{});
    }

    public ArrayList<JimpleClass> generateJimpleStrings(String[] relevantClasses) {
        Chain<SootClass> classes = Scene.v().getClasses();
        // Remove synthetic methods
        filterSyntheticMethods();

        ArrayList<JimpleClass> jimpleClasses = new ArrayList<>();
        for (SootClass sc : classes) {
            if (!sc.isApplicationClass()) continue;

            // remove final keyword from inner classes
            removeFinalKeyword(sc);

            if (relevantClasses.length < 1 || Arrays.asList(relevantClasses).contains(sc.getName())) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writerOut = new PrintWriter(stringWriter);
                Printer.v().printTo(sc, writerOut);
                writerOut.flush();

                jimpleClasses.add(new JimpleClass(stringWriter.toString(), sc.getName()));
            }
        }
        return jimpleClasses;
    }

    private void filterSyntheticMethods() {
        for (SootMethod sm : syntheticMethods) {
            try {
                int smModifiers = sm.getModifiers();

                if (!Modifier.isSynthetic(smModifiers)) continue;
                if (sm.getDeclaringClass() == null) continue;
//                if (!sm.getName().equals("sort")) continue;

                sm.getDeclaringClass().removeMethod(sm);
                if (this.statistics != null) this.statistics.sortMethod += 1;
            } catch (RuntimeException e) {
//                e.printStackTrace();
            }

        }
    }

    private void removeFinalKeyword(SootClass sc) {
//        final String NESTED_CLASS_REGEX = "[\\w.]*\\$[\\d]+";
        String[] splitClassName = sc.getName().split("\\$");
        if (splitClassName.length < 1) return;
        if (!splitClassName[splitClassName.length - 1].matches("\\d+")) return;
        if (!sc.isInnerClass()) return;
        if (!sc.isFinal()) return;

        sc.setModifiers(sc.getModifiers() - Modifier.FINAL);
    }

}
