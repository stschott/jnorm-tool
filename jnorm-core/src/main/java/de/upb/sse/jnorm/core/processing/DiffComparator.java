package de.upb.sse.jnorm.core.processing;

import de.upb.sse.jnorm.core.stats.ProcessingStats;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sootup.core.model.SootClass;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.core.JavaSootClass;

/** Compares Java classes between two versions and collects comparison statistics. */
@Slf4j
public class DiffComparator {
  private final Set<String> excludedClasses;

  /**
   * Creates a new DiffComparator.
   *
   * @param excludedClasses Set of class names to exclude from comparison
   */
  public DiffComparator(Set<String> excludedClasses) {
    this.excludedClasses = excludedClasses != null ? excludedClasses : new HashSet<>();
  }

  /** Result of comparing two sets of classes. */
  @Getter
  @Builder
  public static class ComparisonResult {
    private final ProcessingStats stats;
    private final Set<String> disjunctionClasses;
    private final Set<String> matchingClasses;
    private final Set<String> differingClasses;
    private final Set<String> errorClasses;
  }

  /**
   * Compare two streams of Java classes and collect statistics.
   *
   * @param classes1 First set of classes
   * @param classes2 Second set of classes
   * @param stats Statistics collector to record results
   * @return Result of the comparison
   */
  public ComparisonResult compare(
      Stream<JavaSootClass> classes1, Stream<JavaSootClass> classes2, ProcessingStats stats) {
    Set<JavaSootClass> classes1Set = classes1.collect(Collectors.toSet());
    Set<JavaSootClass> classes2Set = classes2.collect(Collectors.toSet());

    Set<String> disjunctionClasses = getDisjunction(classes1Set, classes2Set);
    Set<String> matchingClasses = new HashSet<>();
    Set<String> differingClasses = new HashSet<>();
    Set<String> errorClasses = new HashSet<>();

    // Record disjunction classes
    disjunctionClasses.forEach(
        className -> {
          log.info("Class in disjunction: {}", className);
          stats.recordDisjunctionClass(className);
        });

    // Compare classes present in both versions
    for (JavaSootClass clazz1 : classes1Set) {
      String clazz1Name = clazz1.getName();

      // Skip excluded classes
      if (excludedClasses.contains(clazz1Name)) {
        log.info("Skipping excluded class: {}", clazz1Name);
        continue;
      }

      Optional<JavaSootClass> clazz2Opt =
          classes2Set.stream().filter(clazz -> clazz.getName().equals(clazz1Name)).findFirst();

      if (clazz2Opt.isEmpty()) {
        log.error("Class {} not found in second set", clazz1Name);
        stats.recordProcessedClass(clazz1Name, false);
        errorClasses.add(clazz1Name);
        continue;
      }

      JavaSootClass clazz2 = clazz2Opt.get();

      try {
        String clazz1String = classToString(clazz1);
        String clazz2String = classToString(clazz2);

        if (!clazz1String.equals(clazz2String)) {
          log.info("Class {} differs", clazz1Name);
          stats.recordProcessedClass(clazz1Name, false);
          differingClasses.add(clazz1Name);
        } else {
          log.debug("Class {} matches", clazz1Name);
          stats.recordProcessedClass(clazz1Name, true);
          matchingClasses.add(clazz1Name);
        }
      } catch (IllegalArgumentException e) {
        log.error("Error comparing class {}: {}", clazz1Name, e.getMessage());
        stats.recordProcessedClass(clazz1Name, false);
        errorClasses.add(clazz1Name);
      }
    }

    return ComparisonResult.builder()
        .stats(stats)
        .disjunctionClasses(disjunctionClasses)
        .matchingClasses(matchingClasses)
        .differingClasses(differingClasses)
        .errorClasses(errorClasses)
        .build();
  }

  private Set<String> getDisjunction(Set<JavaSootClass> classes1, Set<JavaSootClass> classes2) {
    Set<String> disjunction = new HashSet<>();
    Set<String> names1 = classes1.stream().map(SootClass::getName).collect(Collectors.toSet());
    Set<String> names2 = classes2.stream().map(SootClass::getName).collect(Collectors.toSet());

    // Add classes unique to version 1
    names1.stream().filter(name -> !names2.contains(name)).forEach(disjunction::add);

    // Add classes unique to version 2
    names2.stream().filter(name -> !names1.contains(name)).forEach(disjunction::add);

    return disjunction;
  }

  private String classToString(JavaSootClass clazz) {
    JimplePrinter printer = new JimplePrinter(JimplePrinter.Option.Deterministic);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    printer.printTo(clazz, printWriter);
    printWriter.close();
    return stringWriter.toString();
  }
}
