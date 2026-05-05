package com.cornercall.app.shared;

import com.google.android.gms.wearable.DataMap;
import java.util.ArrayList;
import java.util.List;

public final class HeartRatePayload {
  public static final int MAX_SAMPLES_PER_CHUNK = 80;

  public final String sessionId;
  public final String eventType;
  public final int chunkIndex;
  public final boolean isFinal;
  public final List<HeartRateSample> samples;

  public HeartRatePayload(
      String sessionId,
      String eventType,
      int chunkIndex,
      boolean isFinal,
      List<HeartRateSample> samples) {
    this.sessionId = sessionId;
    this.eventType = eventType;
    this.chunkIndex = chunkIndex;
    this.isFinal = isFinal;
    this.samples = samples;
  }

  public DataMap toDataMap() {
    DataMap map = new DataMap();
    long[] timestamps = new long[samples.size()];
    float[] bpms = new float[samples.size()];
    float[] calories = new float[samples.size()];
    for (int i = 0; i < samples.size(); i += 1) {
      HeartRateSample sample = samples.get(i);
      timestamps[i] = sample.timestampMs;
      bpms[i] = sample.bpm;
      calories[i] = sample.calories;
    }
    map.putString("sessionId", sessionId);
    map.putString("eventType", eventType);
    map.putInt("chunkIndex", chunkIndex);
    map.putBoolean("isFinal", isFinal);
    map.putLongArray("timestamps", timestamps);
    map.putFloatArray("bpms", bpms);
    map.putFloatArray("calories", calories);
    map.putLong("createdAt", System.currentTimeMillis());
    return map;
  }

  public static HeartRatePayload fromDataMap(DataMap map) {
    long[] timestamps = map.getLongArray("timestamps");
    float[] bpms = map.getFloatArray("bpms");
    float[] calories = map.getFloatArray("calories");
    List<HeartRateSample> samples = new ArrayList<>();
    if (timestamps != null && bpms != null) {
      int count = Math.min(timestamps.length, bpms.length);
      for (int i = 0; i < count; i += 1) {
        float calorieTotal = calories != null && i < calories.length ? calories[i] : 0;
        samples.add(new HeartRateSample(timestamps[i], bpms[i], calorieTotal));
      }
    }
    return new HeartRatePayload(
        map.getString("sessionId", ""),
        map.getString("eventType", ""),
        map.getInt("chunkIndex", 0),
        map.getBoolean("isFinal", false),
        samples);
  }

  public static List<HeartRatePayload> chunks(String sessionId, String eventType, boolean isFinal, List<HeartRateSample> samples) {
    List<HeartRatePayload> chunks = new ArrayList<>();
    if (samples.isEmpty()) {
      chunks.add(new HeartRatePayload(sessionId, eventType, 0, isFinal, new ArrayList<HeartRateSample>()));
      return chunks;
    }
    int chunkIndex = 0;
    for (int start = 0; start < samples.size(); start += MAX_SAMPLES_PER_CHUNK) {
      int end = Math.min(samples.size(), start + MAX_SAMPLES_PER_CHUNK);
      boolean finalChunk = isFinal && end == samples.size();
      chunks.add(new HeartRatePayload(sessionId, eventType, chunkIndex, finalChunk, new ArrayList<>(samples.subList(start, end))));
      chunkIndex += 1;
    }
    return chunks;
  }
}
