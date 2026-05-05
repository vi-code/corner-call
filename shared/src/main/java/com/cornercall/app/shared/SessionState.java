package com.cornercall.app.shared;

import org.json.JSONException;
import org.json.JSONObject;

public final class SessionState {
  public final String sessionId;
  public final String origin;
  public final String action;
  public final String status;
  public final long startedAt;
  public final boolean workPhase;
  public final int currentRound;
  public final int remainingSeconds;

  public SessionState(
      String sessionId,
      String origin,
      String action,
      String status,
      long startedAt,
      boolean workPhase,
      int currentRound,
      int remainingSeconds) {
    this.sessionId = sessionId;
    this.origin = origin;
    this.action = action;
    this.status = status;
    this.startedAt = startedAt;
    this.workPhase = workPhase;
    this.currentRound = currentRound;
    this.remainingSeconds = remainingSeconds;
  }

  public byte[] toBytes() {
    return toJson().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  public JSONObject toJson() {
    JSONObject object = new JSONObject();
    try {
      object.put("sessionId", sessionId);
      object.put("origin", origin);
      object.put("action", action);
      object.put("status", status);
      object.put("startedAt", startedAt);
      object.put("workPhase", workPhase);
      object.put("currentRound", currentRound);
      object.put("remainingSeconds", remainingSeconds);
    } catch (JSONException ignored) {
    }
    return object;
  }

  public static SessionState fromBytes(byte[] payload) throws JSONException {
    return fromJson(new JSONObject(new String(payload, java.nio.charset.StandardCharsets.UTF_8)));
  }

  public static SessionState fromJson(JSONObject object) throws JSONException {
    return new SessionState(
        object.optString("sessionId", ""),
        object.optString("origin", ""),
        object.optString("action", ""),
        object.optString("status", WearPaths.STATUS_READY),
        object.optLong("startedAt", 0L),
        object.optBoolean("workPhase", true),
        object.optInt("currentRound", 1),
        object.optInt("remainingSeconds", 0));
  }
}
