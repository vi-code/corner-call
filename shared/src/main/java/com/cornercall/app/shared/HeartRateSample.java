package com.cornercall.app.shared;

public final class HeartRateSample {
  public final long timestampMs;
  public final float bpm;
  public final float calories;

  public HeartRateSample(long timestampMs, float bpm) {
    this(timestampMs, bpm, 0);
  }

  public HeartRateSample(long timestampMs, float bpm, float calories) {
    this.timestampMs = timestampMs;
    this.bpm = bpm;
    this.calories = calories;
  }
}
