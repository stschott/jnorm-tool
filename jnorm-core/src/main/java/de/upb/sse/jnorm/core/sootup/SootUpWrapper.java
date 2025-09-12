package de.upb.sse.jnorm.core.sootup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.transform.BodyInterceptor;
import sootup.core.views.View;
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;
import sootup.java.core.views.MutableJavaView;

public class SootUpWrapper {

  public Optional<View> createViewFromDirectory(
      String inputDir, List<BodyInterceptor> interceptors) {
    Path inputDirPath = Paths.get(inputDir);
    if (isJarFile(inputDirPath)) {
      return Optional.of(createJavaView(inputDirPath, interceptors));
    } else if (Files.isDirectory(inputDirPath)) {
      if (containsJimpleFiles(inputDirPath)) {
        return Optional.of(createJavaView(inputDirPath, interceptors));
      } else if (containsClassFiles(inputDirPath)) {
        return Optional.of(createJavaView(inputDirPath, interceptors));
      }
    }

    return Optional.empty();
  }

  private boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && path.toString().endsWith(".jar");
  }

  private boolean containsJimpleFiles(Path dir) {
    try (Stream<Path> paths = Files.walk(dir)) {
      return paths.anyMatch(path -> path.toString().endsWith(".jimple"));
    } catch (IOException e) {
      return false;
    }
  }

  private boolean containsClassFiles(Path dir) {
    try (Stream<Path> paths = Files.walk(dir)) {
      return paths.anyMatch(path -> path.toString().endsWith(".class"));
    } catch (IOException e) {
      return false;
    }
  }

  private JavaView createJavaView(Path inputDir, List<BodyInterceptor> interceptors) {
    List<AnalysisInputLocation> inputLocations =
        Arrays.asList(
            new JavaClassPathAnalysisInputLocation(
                inputDir.toString(), SourceType.Application, interceptors),
            new DefaultRuntimeAnalysisInputLocation());
    return new MutableJavaView(inputLocations);
  }
}
