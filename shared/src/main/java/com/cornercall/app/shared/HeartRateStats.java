package com.cornercall.app.shared;

import java.util.List;

public final class HeartRateStats {
  private HeartRateStats() {}

  public static HeartRateSummary summarize(String sessionId, List<HeartRateSample> samples, long syncedAt) {
    if (samples == null || samples.isEmpty()) {
      return new HeartRateSummary(sessionId, 0, 0, 0, 0, syncedAt);
    }
    float min = Float.MAX_VALUE;
    float max = 0;
    float total = 0;
    float calories = 0;
    for (HeartRateSample sample : samples) {
      min = Math.min(min, sample.bpm);
      max = Math.max(max, sample.bpm);
      total += sample.bpm;
      calories = Math.max(calories, sample.calories);
    }
    return new HeartRateSummary(sessionId, samples.size(), min, total / samples.size(), max, calories, syncedAt);
  }
}
