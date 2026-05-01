package com.cornercall.app.shared;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public final class HeartRateStatsTest {
  @Test
  public void summarizesEmptySamples() {
    HeartRateSummary summary = HeartRateStats.summarize("s1", Collections.<HeartRateSample>emptyList(), 55L);

    assertEquals(0, summary.sampleCount);
    assertEquals(0, summary.avgBpm, 0.01f);
  }

  @Test
  public void summarizesSingleAndMultipleSamples() {
    HeartRateSummary single =
        HeartRateStats.summarize("s1", Collections.singletonList(new HeartRateSample(1, 144, 12)), 55L);
    HeartRateSummary multiple =
        HeartRateStats.summarize(
            "s1",
            Arrays.asList(
                new HeartRateSample(1, 100, 8),
                new HeartRateSample(2, 130, 16),
                new HeartRateSample(3, 160, 22)),
            55L);

    assertEquals(144, single.minBpm, 0.01f);
    assertEquals(144, single.avgBpm, 0.01f);
    assertEquals(144, single.maxBpm, 0.01f);
    assertEquals(100, multiple.minBpm, 0.01f);
    assertEquals(130, multiple.avgBpm, 0.01f);
    assertEquals(160, multiple.maxBpm, 0.01f);
    assertEquals(22, multiple.calories, 0.01f);
  }
}
