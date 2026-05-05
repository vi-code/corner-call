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

abstract class PhoneWorkoutActivity extends PhoneLowerPanelsActivity {
  protected void toggleWorkout() {
    toggleWorkout(true);
  }

  protected void toggleWorkout(boolean notifyWear) {
    if (running) {
      stopTimer();
      if (notifyWear) {
        sendControlToWatch(WearPaths.ACTION_PAUSE);
      }
      render();
      return;
    }
    if (complete) {
      resetWorkout();
    }
    boolean freshSession = sessionId.isEmpty();
    ensureSession();
    running = true;
    complete = false;
    if (workPhase) {
      showCombo(true);
      nextCallIn = paceSeconds;
    }
    handler.postDelayed(ticker, 1000);
    if (notifyWear) {
      sendControlToWatch(freshSession ? WearPaths.ACTION_START : WearPaths.ACTION_RESUME);
    }
    render();
  }

  protected void stopTimer() {
    running = false;
    handler.removeCallbacks(ticker);
  }

  protected void resetWorkout() {
    if (!complete && !sessionId.isEmpty()) {
      sendControlToWatch(WearPaths.ACTION_END);
    }
    stopTimer();
    complete = false;
    workPhase = true;
    currentRound = 1;
    phaseRemaining = roundSeconds;
    phaseDuration = roundSeconds;
    nextCallIn = 0;
    comboCount = 0;
    sessionId = "";
    sessionStartedAt = 0;
    recentCombos.clear();
    showCombo(false);
    render();
  }

  protected void tick() {
    if (!running) {
      return;
    }
    if (workPhase) {
      nextCallIn -= 1;
      if (nextCallIn <= 0) {
        showCombo(true);
        nextCallIn = paceSeconds;
      }
    }
    phaseRemaining -= 1;
    if (phaseRemaining <= 0) {
      advancePhase();
    }
    render();
  }

  protected void advancePhase() {
    bell();
    if (workPhase && currentRound < rounds) {
      workPhase = false;
      phaseRemaining = restSeconds;
      phaseDuration = restSeconds;
      speak("Rest. Breathe and reset.");
      return;
    }
    if (!workPhase) {
      currentRound += 1;
      workPhase = true;
      phaseRemaining = roundSeconds;
      phaseDuration = roundSeconds;
      nextCallIn = 0;
      speak("Round " + currentRound + ". Work.");
      return;
    }
    complete = true;
    stopTimer();
    phaseRemaining = 0;
    speak("Workout complete.");
    sendControlToWatch(WearPaths.ACTION_END);
  }

  protected void showCombo(boolean announce) {
    List<String> combo = buildCombo();
    List<String> names = new ArrayList<>();
    for (String code : combo) {
      names.add(moveName(code));
    }
    String comboText = join(combo, " - ");
    comboLabel.setText(comboText);
    comboNamesLabel.setText(join(names, ", "));

    if (announce) {
      comboCount += 1;
      recentCombos.add(0, comboText);
      while (recentCombos.size() > 7) {
        recentCombos.remove(recentCombos.size() - 1);
      }
      speak(join(combo, ", "));
    }
  }

  protected List<String> buildCombo() {
    if (random.nextFloat() < 0.45f) {
      List<String[]> available = new ArrayList<>();
      for (String[] combo : signatureCombos) {
        if (includeDefense || allPunches(combo)) {
          available.add(combo);
        }
      }
      if (!available.isEmpty()) {
        String[] selected = available.get(random.nextInt(available.size()));
        return trimCombo(Arrays.asList(selected));
      }
    }

    List<Move> pool = new ArrayList<>(punches);
    if (includeDefense) {
      pool.addAll(defense);
    }

    List<String> combo = new ArrayList<>();
    while (combo.size() < comboLength) {
      Move move = weightedMove(pool);
      String previous = combo.isEmpty() ? "" : combo.get(combo.size() - 1);
      if (!isPunch(previous) && !isPunch(move.code)) {
        continue;
      }
      combo.add(move.code);
    }
    return combo;
  }

  protected List<String> trimCombo(List<String> selected) {
    List<String> out = new ArrayList<>();
    int limit = Math.min(comboLength, selected.size());
    for (int i = 0; i < limit; i += 1) {
      out.add(selected.get(i));
    }
    return out;
  }

  protected Move weightedMove(List<Move> moves) {
    int total = 0;
    for (Move move : moves) {
      total += move.weight;
    }
    int target = random.nextInt(total);
    for (Move move : moves) {
      target -= move.weight;
      if (target < 0) {
        return move;
      }
    }
    return moves.get(moves.size() - 1);
  }

  protected boolean allPunches(String[] combo) {
    for (String code : combo) {
      if (!isPunch(code)) {
        return false;
      }
    }
    return true;
  }

  protected boolean isPunch(String code) {
    for (Move punch : punches) {
      if (punch.code.equals(code)) {
        return true;
      }
    }
    return false;
  }

  protected String moveName(String code) {
    for (Move punch : punches) {
      if (punch.code.equals(code)) {
        return punch.name;
      }
    }
    for (Move move : defense) {
      if (move.code.equals(code)) {
        return move.name;
      }
    }
    return code;
  }

  protected void speak(String text) {
    if (!voiceEnabled || !speechReady || textToSpeech == null) {
      return;
    }
    textToSpeech.stop();
    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "corner-call");
  }

  protected void bell() {
    if (toneGenerator == null) {
      return;
    }
    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            if (toneGenerator != null) {
              toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
            }
          }
        },
        180);
  }

  protected void render() {
    if (trainTab != null && aboutTab != null) {
      setTabSelected(trainTab, !showingAbout);
      setTabSelected(aboutTab, showingAbout);
    }
    if (showingAbout) {
      return;
    }
    int phaseColor = complete ? GOOD : running ? (workPhase ? ACCENT : REST_BLUE) : GOLD;
    phaseLabel.setText(complete ? "Complete" : running ? (workPhase ? "Work" : "Rest") : "Ready");
    phaseLabel.setTextColor(phaseColor);
    phaseLabel.setBackground(rounded(0xff151b24, dp(18), phaseColor, 1));
    roundLabel.setText("Round " + currentRound + " / " + rounds);
    timeLabel.setText(formatTime(phaseRemaining));
    hintLabel.setText(
        complete
            ? "Nice work"
            : running ? (workPhase ? "Hands up, chin down" : "Breathe") : "Pick a format and tap Start");
    startButton.setText(running ? "Pause" : complete ? "Start again" : "Start");
    callCountLabel.setText(comboCount + (comboCount == 1 ? " call" : " calls"));
    sessionMetaLabel.setText(rounds + " rounds  " + formatDuration(roundSeconds) + " work  " + restSeconds + "s rest");
    defenseToggle.setChecked(includeDefense);
    voiceToggle.setChecked(voiceEnabled);
    setInputText(roundsInput, String.valueOf(rounds));
    setInputText(minutesInput, String.valueOf(roundSeconds / 60));
    setInputText(restInput, String.valueOf(restSeconds));

    phaseProgress.setProgressTintList(ColorStateList.valueOf(phaseColor));
    int progress = phaseDuration <= 0 ? 0 : (int) (((phaseDuration - phaseRemaining) / (float) phaseDuration) * 1000);
    phaseProgress.setProgress(clamp(progress, 0, 1000));

    if (recentCombos.isEmpty()) {
      recentLabel.setText("Start a round to build the call log");
    } else {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < recentCombos.size(); i += 1) {
        builder.append(i + 1).append(". ").append(recentCombos.get(i));
        if (i < recentCombos.size() - 1) {
          builder.append("\n");
        }
      }
      recentLabel.setText(builder.toString());
    }
    renderHeartRateSummary();
  }

  protected void renderHeartRateSummary() {
    if (heartRateSummaryLabel == null || wearStatusLabel == null) {
      return;
    }
    if (latestHeartRateSummary == null || !latestHeartRateSummary.hasSamples()) {
      heartRateSummaryLabel.setText("Pair a Wear OS watch and start a workout to collect heart rate and calories.");
      wearStatusLabel.setText("No samples");
      return;
    }
    heartRateSummaryLabel.setText(
        "Avg "
            + Math.round(latestHeartRateSummary.avgBpm)
            + " bpm  Max "
            + Math.round(latestHeartRateSummary.maxBpm)
            + "  Min "
            + Math.round(latestHeartRateSummary.minBpm)
            + "  Cal "
            + Math.round(latestHeartRateSummary.calories)
            + "\n"
            + latestHeartRateSummary.sampleCount
            + " samples synced "
            + formatClock(latestHeartRateSummary.lastSyncedAt));
    wearStatusLabel.setText("Synced");
  }
}

