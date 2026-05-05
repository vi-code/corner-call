package com.cornercall.app;

import com.cornercall.app.shared.HeartRatePayload;
import com.cornercall.app.shared.HeartRateSample;
import com.google.android.gms.wearable.DataMap;

final class HeartRatePayloadMapper {
  private HeartRatePayloadMapper() {}

  static void write(DataMap map, HeartRatePayload payload) {
    long[] timestamps = new long[payload.samples.size()];
    float[] bpms = new float[payload.samples.size()];
    float[] calories = new float[payload.samples.size()];
    for (int i = 0; i < payload.samples.size(); i += 1) {
      HeartRateSample sample = payload.samples.get(i);
      timestamps[i] = sample.timestampMs;
      bpms[i] = sample.bpm;
      calories[i] = sample.calories;
    }
    map.putString("sessionId", payload.sessionId);
    map.putString("eventType", payload.eventType);
    map.putInt("chunkIndex", payload.chunkIndex);
    map.putBoolean("isFinal", payload.isFinal);
    map.putLongArray("timestamps", timestamps);
    map.putFloatArray("bpms", bpms);
    map.putFloatArray("calories", calories);
    map.putLong("createdAt", System.currentTimeMillis());
  }
}
