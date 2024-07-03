package jnorm.core;

import jnorm.core.model.JimpleClass;
import jnorm.core.model.NormalizationStatistics;
import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;
import soot.baf.BafASMBackend;
import soot.util.Chain;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class Normalizer {
    private final NormalizationStatistics statistics = new NormalizationStatistics();
    String inputDir = "input";
    String outputDir = "output";
    boolean applyOptimization = false;
    boolean applyNormalization = false;
    boolean applyAggressiveNormalization = false;
    boolean applyStandardization = false;
    boolean applyPrettyPrint = true;
    boolean applySimpleRenaming = true;
    int renamingWindow = 1;

    public Normalizer(String inputDir, String outputDir, boolean applyOptimization, boolean applyNormalization,
                      boolean applyAggressiveNormalization, boolean applyStandardization, boolean applyPrettyPrint, boolean applySimpleRenaming, int renamingWindow) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.applyOptimization = applyOptimization;
        this.applyNormalization = applyNormalization;
        this.applyAggressiveNormalization = applyAggressiveNormalization;
        this.applyStandardization = applyStandardization;
        this.applyPrettyPrint = applyPrettyPrint;
        this.applySimpleRenaming = applySimpleRenaming;
        this.renamingWindow = renamingWindow;
    }

    public void normalize() {
        FileHandler.makeDirs(outputDir);
        SootHandler sh = new SootHandler(applyOptimization, applyNormalization, applyAggressiveNormalization, applyStandardization, renamingWindow, statistics);
        sh.loadDir(inputDir);
        printJimple(sh);
    }

    public NormalizationStatistics getStatistics() {
        return this.statistics;
    }

    private void printJimple(SootHandler sh) {
        ArrayList<JimpleClass> jimpleClasses = sh.generateJimpleStrings();

        for (JimpleClass jimpleClass : jimpleClasses) {
            String jimple = jimpleClass.getJimple();
            if (applyPrettyPrint) {
                PrettyPrinter pp = new PrettyPrinter(jimple);
                jimple = pp.prettyPrint();
            }

            if (applySimpleRenaming) {
                Renamer re = new Renamer(jimple);
                jimple = re.rename();
            }

            FileHandler.generateFileFromString(jimple, String.format("%s/%s.jimple", outputDir, jimpleClass.getClassName()));
        }
    }

    private void generateClasses() {
        Chain<SootClass> classes = Scene.v().getClasses();
        int java_version = soot.options.Options.v().java_version();

        for (SootClass sc : classes) {
            if (!sc.isApplicationClass()) continue;
            String fileName = SourceLocator.v().getFileNameFor(sc, soot.options.Options.output_format_class);
            try {
                OutputStream streamOut = new FileOutputStream(fileName);
                BafASMBackend backend = new BafASMBackend(sc, java_version);
                backend.generateClassFile(streamOut);
                streamOut.close();
            } catch (Exception e) {
                System.out.println("Failed generating class: " + fileName);
                System.out.println(e.getMessage());
            }
        }
    }
}
