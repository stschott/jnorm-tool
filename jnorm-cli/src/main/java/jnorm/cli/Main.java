package jnorm.cli;

import jnorm.core.Normalizer;
import org.apache.commons.cli.*;

public class Main {

    public static void main(String[] args){
        CliHandler cli = new CliHandler();
        Options options = cli.getOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = null;
        try {
            // Show help message
            if (options.hasOption("help") || options.hasOption("h") || args.length < 1) {
                cli.showHelpMessage(options);
            }
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed. Reason: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Running jNorm with the following configuration: " + String.join(" ", args));

        final String inputDir = cmdLine.getOptionValue(CliHandler.inputDirOpt);
        final String outputDir = cmdLine.getOptionValue(CliHandler.outputDirOpt) != null ? cmdLine.getOptionValue(CliHandler.outputDirOpt) : "output";
        final boolean applyOptimization = cmdLine.hasOption(CliHandler.optimizationOpt);
        final boolean applyPrettyPrint = cmdLine.hasOption(CliHandler.prettyPrintOpt);
        final boolean applyNormalization = cmdLine.hasOption(CliHandler.normalizationOpt);
        final int renamingWindow = cmdLine.getOptionValue(CliHandler.renameOpt) != null ? Integer.parseInt(cmdLine.getOptionValue(CliHandler.renameOpt)) : -1;
        final boolean applySimpleRenaming = cmdLine.hasOption(CliHandler.simpleRenameOpt);
        final boolean applyAggressiveNormalization = cmdLine.hasOption(CliHandler.aggressiveOpt);
        final boolean applyStandardization = cmdLine.hasOption(CliHandler.standardizationOpt);
        final boolean classFileGeneration = cmdLine.hasOption(CliHandler.classFileOpt);

        Normalizer normalizer = new Normalizer(inputDir, outputDir, applyOptimization, applyNormalization, applyAggressiveNormalization, applyStandardization, applyPrettyPrint, applySimpleRenaming, renamingWindow);
        normalizer.normalize();

//        System.out.println(normalizer.getStatistics());
    }
}
