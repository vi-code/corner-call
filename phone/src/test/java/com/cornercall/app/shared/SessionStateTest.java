package com.cornercall.app.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SessionStateTest {
  @Test
  public void roundTripsThroughJsonBytes() throws Exception {
    SessionState state =
        new SessionState(
            "corner-1",
            WearPaths.ORIGIN_PHONE,
            WearPaths.ACTION_PAUSE,
            WearPaths.STATUS_PAUSED,
            123L,
            true,
            2,
            57);

    SessionState parsed = SessionState.fromBytes(state.toBytes());

    assertEquals("corner-1", parsed.sessionId);
    assertEquals(WearPaths.ORIGIN_PHONE, parsed.origin);
    assertEquals(WearPaths.ACTION_PAUSE, parsed.action);
    assertEquals(WearPaths.STATUS_PAUSED, parsed.status);
    assertEquals(123L, parsed.startedAt);
    assertTrue(parsed.workPhase);
    assertEquals(2, parsed.currentRound);
    assertEquals(57, parsed.remainingSeconds);
  }
}
