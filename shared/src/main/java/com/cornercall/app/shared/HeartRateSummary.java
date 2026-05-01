package com.cornercall.app.shared;

public final class HeartRateSummary {
  public final String sessionId;
  public final int sampleCount;
  public final float minBpm;
  public final float avgBpm;
  public final float maxBpm;
  public final float calories;
  public final long lastSyncedAt;

  public HeartRateSummary(
      String sessionId, int sampleCount, float minBpm, float avgBpm, float maxBpm, long lastSyncedAt) {
    this(sessionId, sampleCount, minBpm, avgBpm, maxBpm, 0, lastSyncedAt);
  }

  public HeartRateSummary(
      String sessionId,
      int sampleCount,
      float minBpm,
      float avgBpm,
      float maxBpm,
      float calories,
      long lastSyncedAt) {
    this.sessionId = sessionId;
    this.sampleCount = sampleCount;
    this.minBpm = minBpm;
    this.avgBpm = avgBpm;
    this.maxBpm = maxBpm;
    this.calories = calories;
    this.lastSyncedAt = lastSyncedAt;
  }

  public boolean hasSamples() {
    return sampleCount > 0;
  }
}
