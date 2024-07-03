# jNorm
*jNorm* is a tool that *normalizes* Java bytecode generated in different compilation environments.
Bytecode that originates from the same source code will be transformed into a representation that is independent from the used JDK with most compilation differences removed.

# Table of Contents
1. [Requirements](#requirements)
2. [Usage](#usage)
3. [Supported compilers and settings](#supported-compilers-and-settings)
3. [List of all available command line parameters](#list-of-all-available-command-line-parameters)

# Requirements
[Java 8](https://www.oracle.com/de/java/technologies/javase/javase8-archive-downloads.html) or higher needs to be installed on your system to run jNorm.

# Supported compilers and settings
jNorm supports the default compilers of Oracle's JDK and OpenJDK (javac).
More specifically it is designed to remove differences introduced by JDK versions 5-8, 11 and 17.

jNorm additionally employs a normalization of target level differences.
The designed difference removal range supports Java versions 5-8 and 11.

# Building jNorm
To build jNorm from its sourc code run the following command:
```
mvn package assembly:single
```
This command will create an executable JAR. This jar can be found in `jnorm-cli/target` and is named `jnorm-jar-with-dependencies.jar`.

# Usage
The simplest way to invoke jNorm is the following:
```
java -jar jnorm-jar-with-dependencies.jar -i path/to/bytecode -n
```
This command will start jNorm and normalize the files in the path/to/bytecode directory and output the normalized Jimple files to the default **output** directory.

If you want to apply a heavier normalization, use the following command:
```
java -jar jnorm-jar-with-dependencies.jar -i path/to/bytecode -o -n -s -a -p -d path/to/output
```
This command will optimize, normalize and standardize the bytecode and additionally remove all typechecks to remove further differences.
The normalized files will be formatted and output to the specified directory.


# List of all available command line parameters
|Parameter                      | Functionality |
| --------------- | ------------- |
| -i \<dir>       | Set input directory |
| -d \<dir>       | Set output directory|
| -p              | Apply pretty printing for output |
| -o              | Apply optimizations |
| -s              | Apply standardization |
| -n              | Apply normalizations |
| -a              | Apply aggressive normalizations |
| -r \<int>       | Apply renaming with specified hash look-ahead window |
| -r2             | Apply simple renaming (cannot be used in combination with -r) |
