package com.cornercall.app.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class HeartRatePayloadTest {
  @Test
  public void chunksEmptyPayloadAsFinalChunk() {
    List<HeartRatePayload> chunks = HeartRatePayload.chunks("s1", "complete", true, new ArrayList<HeartRateSample>());

    assertEquals(1, chunks.size());
    assertTrue(chunks.get(0).isFinal);
    assertEquals(0, chunks.get(0).samples.size());
  }

  @Test
  public void chunksLargePayloadWithoutLosingSamples() {
    List<HeartRateSample> samples = new ArrayList<>();
    for (int i = 0; i < HeartRatePayload.MAX_SAMPLES_PER_CHUNK + 3; i += 1) {
      samples.add(new HeartRateSample(i, 120 + i, i * 0.5f));
    }

    List<HeartRatePayload> chunks = HeartRatePayload.chunks("s1", "pause", false, samples);

    assertEquals(2, chunks.size());
    assertEquals(HeartRatePayload.MAX_SAMPLES_PER_CHUNK, chunks.get(0).samples.size());
    assertEquals(3, chunks.get(1).samples.size());
    assertEquals(40f, chunks.get(1).samples.get(0).calories, 0.01f);
    assertFalse(chunks.get(1).isFinal);
  }

  @Test
  public void serializesCaloriesWithSamples() {
    List<HeartRateSample> samples = new ArrayList<>();
    samples.add(new HeartRateSample(10, 145, 22.5f));

    HeartRatePayload parsed =
        HeartRatePayload.fromDataMap(new HeartRatePayload("s1", "pause", 0, false, samples).toDataMap());

    assertEquals(1, parsed.samples.size());
    assertEquals(145, parsed.samples.get(0).bpm, 0.01f);
    assertEquals(22.5f, parsed.samples.get(0).calories, 0.01f);
  }
}
