package de.upb.sse.jnorm.cli;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CliHandler {
  static final String inputDirOpt = "i";
  static final String outputDirOpt = "d";
  static final String optimizationOpt = "o";
  static final String normalizationOpt = "n";
  static final String aggressiveOpt = "a";

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

    Option optimization = new Option(optimizationOpt, "Apply Soot internal optimizations");
    Option normalization = new Option(normalizationOpt, "Apply normalizations");
    Option aggressive = new Option(aggressiveOpt, "Apply aggressive normalizations");

    options.addOption(filePath);
    options.addOption(outputDir);
    options.addOption(optimization);
    options.addOption(normalization);
    options.addOption(aggressive);
  }

  public void showHelpMessage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("DiffOptimizer", options);
  }

  public Options getOptions() {
    return this.options;
  }
}
