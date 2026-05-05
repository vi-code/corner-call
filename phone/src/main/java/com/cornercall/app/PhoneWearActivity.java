package com.cornercall.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.cornercall.app.shared.HeartRateSummary;
import com.cornercall.app.shared.SessionState;
import com.cornercall.app.shared.WearPaths;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

abstract class PhoneWearActivity extends PhoneWorkoutActivity {
  protected void handleWearControl(String payload) {
    if (payload == null || payload.isEmpty()) {
      return;
    }
    try {
      SessionState state = SessionState.fromJson(new JSONObject(payload));
      if (WearPaths.ORIGIN_PHONE.equals(state.origin)) {
        return;
      }
      if (!state.sessionId.isEmpty()) {
        sessionId = state.sessionId;
        sessionStartedAt = state.startedAt;
        heartRateStore.ensureSession(sessionId, sessionStartedAt, state.status);
      }
      if (WearPaths.ACTION_START.equals(state.action) || WearPaths.ACTION_RESUME.equals(state.action)) {
        if (!running) {
          toggleWorkout(false);
        }
      } else if (WearPaths.ACTION_PAUSE.equals(state.action)) {
        if (running) {
          stopTimer();
          render();
        }
      } else if (WearPaths.ACTION_END.equals(state.action)) {
        completeFromWear();
      }
    } catch (JSONException ignored) {
    }
  }

  protected void completeFromWear() {
    stopTimer();
    complete = true;
    phaseRemaining = 0;
    speak("Workout complete.");
    render();
  }

  protected void ensureSession() {
    if (!sessionId.isEmpty()) {
      return;
    }
    sessionStartedAt = System.currentTimeMillis();
    sessionId = "corner-" + sessionStartedAt;
    heartRateStore.ensureSession(sessionId, sessionStartedAt, WearPaths.STATUS_ACTIVE);
  }

  protected void sendControlToWatch(String action) {
    ensureSession();
    final SessionState state =
        new SessionState(
            sessionId,
            WearPaths.ORIGIN_PHONE,
            action,
            statusForAction(action),
            sessionStartedAt,
            workPhase,
            currentRound,
            phaseRemaining);
    mirrorSessionState(state);
    Wearable.getNodeClient(this)
        .getConnectedNodes()
        .addOnSuccessListener(
            new com.google.android.gms.tasks.OnSuccessListener<List<Node>>() {
              @Override
              public void onSuccess(List<Node> nodes) {
                for (Node node : nodes) {
                  Wearable.getMessageClient(PhoneWearActivity.this)
                      .sendMessage(node.getId(), WearPaths.controlPath(action), state.toBytes());
                }
              }
            });
  }

  protected void mirrorSessionState(SessionState state) {
    PutDataMapRequest request = PutDataMapRequest.create(WearPaths.SESSION_STATE);
    DataMap map = request.getDataMap();
    map.putString("payload", state.toJson().toString());
    map.putLong("updatedAt", System.currentTimeMillis());
    PutDataRequest dataRequest = request.asPutDataRequest();
    dataRequest.setUrgent();
    Wearable.getDataClient(this).putDataItem(dataRequest);
  }

  protected String statusForAction(String action) {
    if (WearPaths.ACTION_END.equals(action)) {
      return WearPaths.STATUS_COMPLETE;
    }
    if (WearPaths.ACTION_PAUSE.equals(action)) {
      return WearPaths.STATUS_PAUSED;
    }
    return WearPaths.STATUS_ACTIVE;
  }

  protected void registerWearBroadcasts() {
    if (wearBroadcastsRegistered) {
      return;
    }
    IntentFilter filter = new IntentFilter();
    filter.addAction(PhoneWearListenerService.ACTION_WEAR_CONTROL);
    filter.addAction(PhoneWearListenerService.ACTION_HR_SUMMARY);
    if (Build.VERSION.SDK_INT >= 33) {
      registerReceiver(wearReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    } else {
      registerReceiver(wearReceiver, filter);
    }
    wearBroadcastsRegistered = true;
  }

  protected void unregisterWearBroadcasts() {
    if (!wearBroadcastsRegistered) {
      return;
    }
    unregisterReceiver(wearReceiver);
    wearBroadcastsRegistered = false;
  }

  protected void loadSettings() {
    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
    rounds = prefs.getInt("rounds", rounds);
    roundSeconds = prefs.getInt("roundSeconds", roundSeconds);
    restSeconds = prefs.getInt("restSeconds", restSeconds);
    paceSeconds = prefs.getInt("paceSeconds", paceSeconds);
    comboLength = prefs.getInt("comboLength", comboLength);
    includeDefense = prefs.getBoolean("includeDefense", includeDefense);
    voiceEnabled = prefs.getBoolean("voiceEnabled", voiceEnabled);
  }

  protected void saveSettings() {
    getSharedPreferences(PREFS, MODE_PRIVATE)
        .edit()
        .putInt("rounds", rounds)
        .putInt("roundSeconds", roundSeconds)
        .putInt("restSeconds", restSeconds)
        .putInt("paceSeconds", paceSeconds)
        .putInt("comboLength", comboLength)
        .putBoolean("includeDefense", includeDefense)
        .putBoolean("voiceEnabled", voiceEnabled)
        .apply();
  }
}

