# jNorm

*jNorm* is a tool that *normalizes* Java bytecode generated in different compilation environments.
Bytecode that originates from the same source code will be transformed into a representation that is independent from
the used JDK with most compilation differences removed. This allows for a subsequent comparison of bytecode, no matter the compilation environment it originates from.

## Table of Contents

1. [Requirements](#requirements)
2. [Supported compilers and settings](#supported-compilers-and-settings)
3. [Build](#build)
4. [Usage](#usage)
5. [List of all available command line parameters](#list-of-all-available-command-line-parameters)

## Requirements

[Java 8](https://www.oracle.com/de/java/technologies/javase/javase8-archive-downloads.html) or higher needs to be installed on your system to run jNorm.
If you want to build jNorm yourself, you need to clone and install the current [SootUp](https://github.com/soot-oss/SootUp) snapshot first.

## Supported compilers and settings

jNorm supports the default compilers of Oracle's JDK and OpenJDK (javac).
More specifically it is designed to remove differences introduced by JDK versions 5-8, 11 and 17.

jNorm additionally employs a normalization of target level differences.
The designed difference removal range supports Java versions 5-8 and 11.

## Build

Run

```
mvn package
```

## Usage

### Command Line Interface

The simplest way to invoke jNorm is to invoke its CLI in the following way:

```
java -jar jnorm-cli-2.0.0.jar -i path/to/bytecode -o -n -a -d path/to/output
```

This command will start jNorm and apply all optimization, normalizations and aggressive normalizations to the files in the path/to/bytecode directory and output the normalized Jimple files to the path/to/output directory.

### Programmatic Invocation

jNorm can also be invoked programmatically in the following way.
```
ProcessingConfig config = new ProcessingConfig(applyOptimizations, applyNormalizations, applyAggressiveNormalizations);
SourceProcessor sp = SourceProcessorFactory.createSourceProcessor(config);
Stream<JavaSootClass> normalizedClasses = sp.process("path/to/bytecode");
```

### Usage with SootUp

If you are already using SootUp in your project, you can simply provide the normalizing BodyInterceptors:
```
List<BodyInterceptor> interceptors = Arrays.asList(
            // Optimizations:
            new NopEliminator(),
            new EmptySwitchEliminator(),
            new CastAndReturnInliner(),
            new LocalSplitter(),
            new Aggregator(),
            new ConstantPropagatorAndFolder(),
            new CustomTypeAssigner(),
            new DeadAssignmentEliminator(),
            new UnreachableCodeEliminator()));
            
            // Normalizations:
            new ArithmeticNormalizer(),
            new BufferMethodCallNormalizer(),
            new CharSequenceToStringNormalizer(),
            new DuplicateTypeCastEliminator(),
            new EmptyAnonymousClassEliminator(),
            new EnumNormalizer(),
            new NestBasedAccessNormalizer(),
            new NullCheckNormalizer(),
            new PrivateInvokeNormalizer(),
            new RedundantTrapEliminator(),
            new TrapNormalizer(),
            new DynamicInvokeNormalizer(),
            new StringConcatNormalizer(),
            new ConcurrentHashMapNormalizer(),
            
            // Aggressive Normalization:
            new TypeCastEliminator(),
            
            // Post-processing:
            new DeadAssignmentEliminator(),
            new UnreachableCodeEliminator(),
            new CustomUnusedLocalNormalizer(),
            new CustomLocalNameStandardizer());

AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation("path/to/bytecode", SourceType.Application, interceptors);            
JavaView sootupJavaView = new JavaView(inputLocation);
```



## List of all available command line parameters for the CLI

| Parameter | Functionality                                                 |
|-----------|---------------------------------------------------------------|
| -i \<dir> | Set input directory                                           |
| -d \<dir> | Set output directory                                          |
| -o        | Apply optimizations                                           |
| -n        | Apply normalizations                                          |
| -a        | Apply aggressive normalizations                               |

## Publication

The paper ["Java Bytecode Normalization for Code Similarity Analysis"](https://drops.dagstuhl.de/entities/document/10.4230/LIPIcs.ECOOP.2024.37) provides detailed explanations and evaluation of jNorm.

If you use jNorm in your research work, feel free to cite it:
```
@InProceedings{schott_et_al:LIPIcs.ECOOP.2024.37,
  author =	{Schott, Stefan and Ponta, Serena Elisa and Fischer, Wolfram and Klauke, Jonas and Bodden, Eric},
  title =	{{Java Bytecode Normalization for Code Similarity Analysis}},
  booktitle =	{38th European Conference on Object-Oriented Programming (ECOOP 2024)},
  pages =	{37:1--37:29},
  series =	{Leibniz International Proceedings in Informatics (LIPIcs)},
  ISBN =	{978-3-95977-341-6},
  ISSN =	{1868-8969},
  year =	{2024},
  volume =	{313},
  editor =	{Aldrich, Jonathan and Salvaneschi, Guido},
  publisher =	{Schloss Dagstuhl -- Leibniz-Zentrum f{\"u}r Informatik},
  address =	{Dagstuhl, Germany},
  URL =		{https://drops.dagstuhl.de/entities/document/10.4230/LIPIcs.ECOOP.2024.37},
  URN =		{urn:nbn:de:0030-drops-208865},
  doi =		{10.4230/LIPIcs.ECOOP.2024.37},
  annote =	{Keywords: Bytecode, Java Compiler, Code Similarity Analysis}
}
```
