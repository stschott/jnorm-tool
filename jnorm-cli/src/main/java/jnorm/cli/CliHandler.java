package jnorm.cli;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CliHandler {
    static final String inputDirOpt = "i";
    static final String optimizationOpt = "o";
    static final String prettyPrintOpt = "p";
    static final String renameOpt = "r";
    static final String simpleRenameOpt = "r2";
    static final String normalizationOpt = "n";
    static final String outputDirOpt = "d";
    static final String aggressiveOpt = "a";
    static final String classFileOpt = "c";
    static final String standardizationOpt = "s";

    Options options;

    public CliHandler() {
        options = new Options();

        Option filePath =
                Option.builder(inputDirOpt)
                        .argName(inputDirOpt)
                        .hasArg()
                        .desc("Input directory")
                        .required(true)
                        .build();

        Option outputDir =
                Option.builder(outputDirOpt)
                        .argName(outputDirOpt)
                        .hasArg()
                        .desc("Output directory")
                        .required(false)
                        .build();

        Option renaming =
                Option.builder(renameOpt)
                        .argName(renameOpt)
                        .hasArg()
                        .desc("Rename each identifier deterministically in order to minimize propagated diffs, receives the hashing window as parameter")
                        .required(false)
                        .type(Number.class)
                        .build();

        Option optimization = new Option(optimizationOpt, "Apply Soot internal optimizations");
        Option prettyPrint = new Option(prettyPrintOpt, "Apply additional pretty-printing");
        Option normalization = new Option(normalizationOpt, "Apply normalizations");
        Option simpleRenaming = new Option(simpleRenameOpt, "Strip the numbers from each identifier");
        Option aggressive = new Option(aggressiveOpt, "Apply aggressive normalizations");
        Option standardization = new Option(standardizationOpt, "Apply local name standardization");
        Option classFile = new Option(classFileOpt, "Generate .class files instead of .jimple files");

        options.addOption(filePath);
        options.addOption(outputDir);
        options.addOption(optimization);
        options.addOption(prettyPrint);
        options.addOption(normalization);
        options.addOption(simpleRenaming);
        options.addOption(renaming);
        options.addOption(aggressive);
        options.addOption(standardization);
        options.addOption(classFile);
    }

    public void showHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DiffOptimizer", options);
    }

    public Options getOptions() {
        return this.options;
    }
}
