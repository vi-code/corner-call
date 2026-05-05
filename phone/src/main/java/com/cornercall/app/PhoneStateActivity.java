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

abstract class PhoneStateActivity extends Activity implements TextToSpeech.OnInitListener {
  protected abstract void tick();
  protected abstract void render();
  protected abstract void handleWearControl(String payload);
  protected abstract void toggleWorkout();
  protected abstract void toggleWorkout(boolean notifyWear);
  protected abstract void resetWorkout();
  protected abstract void showCombo(boolean announce);
  protected abstract void showTrainTab();
  protected abstract void showAboutTab();
  protected abstract void saveSettings();
  protected abstract void ensureSession();
  protected abstract void sendControlToWatch(String action);

  protected static final String PREFS = "corner_call_settings";
  protected static final int DARK = 0xff07080c;
  protected static final int SURFACE = 0xff0d1118;
  protected static final int PANEL = 0xff121821;
  protected static final int PANEL_STRONG = 0xff18212c;
  protected static final int TEXT = 0xfff7f8fb;
  protected static final int MUTED = 0xff9aa3b2;
  protected static final int DIM = 0xff697485;
  protected static final int ACCENT = 0xfff14d42;
  protected static final int GOLD = 0xfff8b84e;
  protected static final int LINE = 0xff293241;
  protected static final int REST_BLUE = 0xff5fb4ff;
  protected static final int GOOD = 0xff36d98a;

  protected final Handler handler = new Handler(Looper.getMainLooper());
  protected final Random random = new Random();
  protected final Runnable ticker =
      new Runnable() {
        @Override
        public void run() {
          tick();
          if (running) {
            handler.postDelayed(this, 1000);
          }
        }
      };

  protected TextToSpeech textToSpeech;
  protected ToneGenerator toneGenerator;
  protected boolean speechReady;
  protected boolean running;
  protected boolean complete;
  protected boolean workPhase = true;
  protected boolean showingAbout;
  protected int currentRound = 1;
  protected int phaseRemaining;
  protected int phaseDuration;
  protected int nextCallIn;
  protected int comboCount;

  protected int rounds = 5;
  protected int roundSeconds = 180;
  protected int restSeconds = 60;
  protected int paceSeconds = 4;
  protected int comboLength = 4;
  protected boolean includeDefense = true;
  protected boolean voiceEnabled = true;

  protected TextView phaseLabel;
  protected TextView roundLabel;
  protected TextView timeLabel;
  protected TextView hintLabel;
  protected TextView comboLabel;
  protected TextView comboNamesLabel;
  protected TextView callCountLabel;
  protected TextView recentLabel;
  protected TextView heartRateSummaryLabel;
  protected TextView wearStatusLabel;
  protected TextView paceValue;
  protected TextView lengthValue;
  protected TextView sessionMetaLabel;
  protected ProgressBar phaseProgress;
  protected EditText roundsInput;
  protected EditText minutesInput;
  protected EditText restInput;
  protected CheckBox defenseToggle;
  protected CheckBox voiceToggle;
  protected Button startButton;
  protected Button trainTab;
  protected Button aboutTab;
  protected LinearLayout contentContainer;
  protected final List<String> recentCombos = new ArrayList<>();
  protected HeartRateStore heartRateStore;
  protected HeartRateSummary latestHeartRateSummary;
  protected String sessionId = "";
  protected long sessionStartedAt;
  protected boolean wearBroadcastsRegistered;
  protected final BroadcastReceiver wearReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (PhoneWearListenerService.ACTION_WEAR_CONTROL.equals(intent.getAction())) {
            handleWearControl(intent.getStringExtra(PhoneWearListenerService.EXTRA_PAYLOAD));
          } else if (PhoneWearListenerService.ACTION_HR_SUMMARY.equals(intent.getAction())) {
            latestHeartRateSummary =
                new HeartRateSummary(
                    sessionId,
                    intent.getIntExtra(PhoneWearListenerService.EXTRA_SAMPLE_COUNT, 0),
                    intent.getFloatExtra(PhoneWearListenerService.EXTRA_MIN_BPM, 0),
                    intent.getFloatExtra(PhoneWearListenerService.EXTRA_AVG_BPM, 0),
                    intent.getFloatExtra(PhoneWearListenerService.EXTRA_MAX_BPM, 0),
                    intent.getFloatExtra(PhoneWearListenerService.EXTRA_CALORIES, 0),
                    intent.getLongExtra(PhoneWearListenerService.EXTRA_SYNCED_AT, 0));
            render();
          }
        }
      };

  protected static class Move {
    final String code;
    final String name;
    final int weight;

    Move(String code, String name, int weight) {
      this.code = code;
      this.name = name;
      this.weight = weight;
    }
  }

  protected static class Preset {
    final String label;
    final int rounds;
    final int roundSeconds;
    final int restSeconds;

    Preset(String label, int rounds, int roundSeconds, int restSeconds) {
      this.label = label;
      this.rounds = rounds;
      this.roundSeconds = roundSeconds;
      this.restSeconds = restSeconds;
    }
  }

  protected final List<Preset> presets =
      Arrays.asList(
          new Preset("5 x 3:00", 5, 180, 60),
          new Preset("12 x 2:00", 12, 120, 60),
          new Preset("3 x 5:00", 3, 300, 90),
          new Preset("4 x 1:30", 4, 90, 45));

  protected final List<Move> punches =
      Arrays.asList(
          new Move("1", "jab", 6),
          new Move("2", "cross", 6),
          new Move("3", "lead hook", 4),
          new Move("4", "rear hook", 3),
          new Move("5", "lead uppercut", 3),
          new Move("6", "rear uppercut", 3));

  protected final List<Move> defense =
      Arrays.asList(
          new Move("duck", "duck", 2), new Move("slip", "slip", 2), new Move("roll", "roll", 1));

  protected final String[][] signatureCombos = {
    {"1", "2", "1", "2"},
    {"1", "2", "3"},
    {"1", "duck", "3"},
    {"5", "6", "5", "6"},
    {"1", "1", "2"},
    {"2", "3", "2"},
    {"1", "2", "5", "2"},
    {"3", "2", "3"}
  };
}

