package com.cornercall.app.shared;

public final class WearPaths {
  public static final String CONTROL_PREFIX = "/cornercall/control/";
  public static final String CONTROL_START = CONTROL_PREFIX + "start";
  public static final String CONTROL_PAUSE = CONTROL_PREFIX + "pause";
  public static final String CONTROL_RESUME = CONTROL_PREFIX + "resume";
  public static final String CONTROL_END = CONTROL_PREFIX + "end";
  public static final String SESSION_STATE = "/cornercall/session";
  public static final String HEART_RATE_PREFIX = "/cornercall/hr/";

  public static final String ACTION_START = "start";
  public static final String ACTION_PAUSE = "pause";
  public static final String ACTION_RESUME = "resume";
  public static final String ACTION_END = "end";

  public static final String STATUS_READY = "ready";
  public static final String STATUS_ACTIVE = "active";
  public static final String STATUS_PAUSED = "paused";
  public static final String STATUS_COMPLETE = "complete";

  public static final String ORIGIN_PHONE = "phone";
  public static final String ORIGIN_WATCH = "watch";

  private WearPaths() {}

  public static String controlPath(String action) {
    return CONTROL_PREFIX + action;
  }
}
