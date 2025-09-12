package de.upb.sse.jnorm.core.normalization;

import de.upb.sse.jnorm.core.normalization.methodnormalizers.MethodNormalizer;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.ModificationTracker;
import de.upb.sse.jnorm.core.stats.StatisticsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sootup.core.model.Body;
import sootup.core.transform.BodyInterceptor;
import sootup.core.views.View;

@Slf4j
@RequiredArgsConstructor
public class MethodNormalizerWrapper implements MethodNormalizer {
  private final BodyInterceptor bodyInterceptor;
  private final StatisticsCollector statsCollector;

  @Override
  public void interceptBody(Body.BodyBuilder bodyBuilder, View view) {
    try {
      String className =
          bodyBuilder.getMethodSignature().getDeclClassType().getFullyQualifiedName();

      // Only track modifications for normalizers in the methodnormalizers package
      int initialModifiedCount = 0;
      int finalModifiedCount = 0;

      if (bodyInterceptor instanceof ModificationTracker) {
        initialModifiedCount = ((ModificationTracker) bodyInterceptor).getModifiedMethods().size();
      }

      bodyInterceptor.interceptBody(bodyBuilder, view);

      if (bodyInterceptor instanceof ModificationTracker) {
        finalModifiedCount = ((ModificationTracker) bodyInterceptor).getModifiedMethods().size();
        if (finalModifiedCount > initialModifiedCount) {
          String fullName = bodyInterceptor.getClass().getName();
          String simpleName = bodyInterceptor.getClass().getSimpleName();
          statsCollector.recordNormalization(className, fullName, true, null);
          statsCollector.getStatistics().recordAppliedNormalizer(className, simpleName);
        }
      }
    } catch (Exception e) {
      String normalizerName = bodyInterceptor.getClass().getSimpleName();
      String methodSignature = bodyBuilder.getMethodSignature().toString();
      String className =
          bodyBuilder.getMethodSignature().getDeclClassType().getFullyQualifiedName();

      log.warn("Exception when applying {} to method {}", normalizerName, methodSignature, e);
      statsCollector.recordNormalization(
          className, bodyInterceptor.getClass().getName(), false, e.getMessage());
    }
  }
}
