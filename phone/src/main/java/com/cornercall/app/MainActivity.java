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

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
  private static final String PREFS = "corner_call_settings";
  private static final int DARK = 0xff07080c;
  private static final int SURFACE = 0xff0d1118;
  private static final int PANEL = 0xff121821;
  private static final int PANEL_STRONG = 0xff18212c;
  private static final int TEXT = 0xfff7f8fb;
  private static final int MUTED = 0xff9aa3b2;
  private static final int DIM = 0xff697485;
  private static final int ACCENT = 0xfff14d42;
  private static final int GOLD = 0xfff8b84e;
  private static final int LINE = 0xff293241;
  private static final int REST_BLUE = 0xff5fb4ff;
  private static final int GOOD = 0xff36d98a;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Random random = new Random();
  private final Runnable ticker =
      new Runnable() {
        @Override
        public void run() {
          tick();
          if (running) {
            handler.postDelayed(this, 1000);
          }
        }
      };

  private TextToSpeech textToSpeech;
  private ToneGenerator toneGenerator;
  private boolean speechReady;
  private boolean running;
  private boolean complete;
  private boolean workPhase = true;
  private boolean showingAbout;
  private int currentRound = 1;
  private int phaseRemaining;
  private int phaseDuration;
  private int nextCallIn;
  private int comboCount;

  private int rounds = 5;
  private int roundSeconds = 180;
  private int restSeconds = 60;
  private int paceSeconds = 4;
  private int comboLength = 4;
  private boolean includeDefense = true;
  private boolean voiceEnabled = true;

  private TextView phaseLabel;
  private TextView roundLabel;
  private TextView timeLabel;
  private TextView hintLabel;
  private TextView comboLabel;
  private TextView comboNamesLabel;
  private TextView callCountLabel;
  private TextView recentLabel;
  private TextView heartRateSummaryLabel;
  private TextView wearStatusLabel;
  private TextView paceValue;
  private TextView lengthValue;
  private TextView sessionMetaLabel;
  private ProgressBar phaseProgress;
  private EditText roundsInput;
  private EditText minutesInput;
  private EditText restInput;
  private CheckBox defenseToggle;
  private CheckBox voiceToggle;
  private Button startButton;
  private Button trainTab;
  private Button aboutTab;
  private LinearLayout contentContainer;
  private final List<String> recentCombos = new ArrayList<>();
  private HeartRateStore heartRateStore;
  private HeartRateSummary latestHeartRateSummary;
  private String sessionId = "";
  private long sessionStartedAt;
  private boolean wearBroadcastsRegistered;
  private final BroadcastReceiver wearReceiver =
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

  private static class Move {
    final String code;
    final String name;
    final int weight;

    Move(String code, String name, int weight) {
      this.code = code;
      this.name = name;
      this.weight = weight;
    }
  }

  private static class Preset {
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

  private final List<Preset> presets =
      Arrays.asList(
          new Preset("5 x 3:00", 5, 180, 60),
          new Preset("12 x 2:00", 12, 120, 60),
          new Preset("3 x 5:00", 3, 300, 90),
          new Preset("4 x 1:30", 4, 90, 45));

  private final List<Move> punches =
      Arrays.asList(
          new Move("1", "jab", 6),
          new Move("2", "cross", 6),
          new Move("3", "lead hook", 4),
          new Move("4", "rear hook", 3),
          new Move("5", "lead uppercut", 3),
          new Move("6", "rear uppercut", 3));

  private final List<Move> defense =
      Arrays.asList(
          new Move("duck", "duck", 2), new Move("slip", "slip", 2), new Move("roll", "roll", 1));

  private final String[][] signatureCombos = {
    {"1", "2", "1", "2"},
    {"1", "2", "3"},
    {"1", "duck", "3"},
    {"5", "6", "5", "6"},
    {"1", "1", "2"},
    {"2", "3", "2"},
    {"1", "2", "5", "2"},
    {"3", "2", "3"}
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    textToSpeech = new TextToSpeech(this, this);
    toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
    heartRateStore = new HeartRateStore(this);
    latestHeartRateSummary = heartRateStore.latestSummary();

    loadSettings();
    phaseRemaining = roundSeconds;
    phaseDuration = roundSeconds;
    setContentView(buildUi());
    showCombo(false);
    render();
  }

  @Override
  protected void onStart() {
    super.onStart();
    registerWearBroadcasts();
  }

  @Override
  protected void onStop() {
    unregisterWearBroadcasts();
    super.onStop();
  }

  @Override
  public void onInit(int status) {
    speechReady = status == TextToSpeech.SUCCESS;
    if (speechReady) {
      textToSpeech.setLanguage(Locale.US);
      textToSpeech.setSpeechRate(1.02f);
      textToSpeech.setPitch(0.9f);
    }
  }

  @Override
  protected void onDestroy() {
    stopTimer();
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
    if (toneGenerator != null) {
      toneGenerator.release();
    }
    super.onDestroy();
  }

  private View buildUi() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(DARK);

    LinearLayout root = vertical();
    root.setPadding(dp(18), dp(22), dp(18), dp(30));
    scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

    root.addView(header());
    root.addView(tabBar());
    contentContainer = vertical();
    root.addView(contentContainer);
    showTrainTab();
    return scroll;
  }

  private View header() {
    LinearLayout header = row();
    header.setPadding(0, 0, 0, dp(4));

    ImageView icon = new ImageView(this);
    icon.setImageResource(R.drawable.ic_launcher);
    icon.setBackground(rounded(PANEL, dp(18), LINE, 1));
    icon.setPadding(dp(7), dp(7), dp(7), dp(7));
    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(64), dp(64));
    iconParams.setMargins(0, 0, dp(14), 0);
    header.addView(icon, iconParams);

    LinearLayout copy = vertical();
    TextView kicker = text("HEAVY BAG ROUNDS", 12, GOLD, Typeface.BOLD);
    kicker.setLetterSpacing(0.08f);
    copy.addView(kicker);
    copy.addView(text("Corner Call", 36, TEXT, Typeface.BOLD));
    sessionMetaLabel = text("", 14, MUTED, Typeface.NORMAL);
    copy.addView(sessionMetaLabel);
    header.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

    TextView version = chip("v0.1", GOLD);
    header.addView(version);
    return header;
  }

  private LinearLayout tabBar() {
    LinearLayout tabs = row();
    tabs.setPadding(0, dp(16), 0, dp(2));
    trainTab = tabButton("Train");
    aboutTab = tabButton("About");
    trainTab.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            showTrainTab();
          }
        });
    aboutTab.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            showAboutTab();
          }
        });
    tabs.addView(trainTab, weightedButton());
    tabs.addView(aboutTab, weightedButton());
    return tabs;
  }

  private void showTrainTab() {
    showingAbout = false;
    contentContainer.removeAllViews();
    contentContainer.addView(workoutCard());
    contentContainer.addView(controlPanel());
    contentContainer.addView(heartRatePanel());
    contentContainer.addView(formatPanel());
    contentContainer.addView(enginePanel());
    contentContainer.addView(notesPanel());
    contentContainer.addView(punchPanel());
    render();
  }

  private void showAboutTab() {
    showingAbout = true;
    contentContainer.removeAllViews();
    contentContainer.addView(aboutPanel());
    render();
  }

  private LinearLayout workoutCard() {
    LinearLayout card = panel(true);
    card.setGravity(Gravity.CENTER);
    card.setPadding(dp(20), dp(22), dp(20), dp(24));

    LinearLayout chips = row();
    chips.setGravity(Gravity.CENTER);
    phaseLabel = chip("Ready", GOLD);
    roundLabel = chip("Round 1 / 5", MUTED);
    chips.addView(phaseLabel);
    chips.addView(roundLabel);
    card.addView(chips);

    timeLabel = text("03:00", 76, TEXT, Typeface.BOLD);
    timeLabel.setGravity(Gravity.CENTER);
    timeLabel.setIncludeFontPadding(false);
    card.addView(timeLabel, matchWrap());

    phaseProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    phaseProgress.setMax(1000);
    phaseProgress.setProgressTintList(ColorStateList.valueOf(ACCENT));
    phaseProgress.setProgressBackgroundTintList(ColorStateList.valueOf(PANEL_STRONG));
    LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(8));
    progressParams.setMargins(dp(8), dp(12), dp(8), dp(12));
    card.addView(phaseProgress, progressParams);

    hintLabel = text("Pick a format and tap Start", 15, MUTED, Typeface.NORMAL);
    hintLabel.setGravity(Gravity.CENTER);
    card.addView(hintLabel);

    TextView next = text("NEXT CALL", 12, DIM, Typeface.BOLD);
    next.setLetterSpacing(0.08f);
    next.setGravity(Gravity.CENTER);
    next.setPadding(0, dp(22), 0, dp(2));
    card.addView(next, matchWrap());

    comboLabel = text("1 - 2 - 1 - 2", 44, TEXT, Typeface.BOLD);
    comboLabel.setGravity(Gravity.CENTER);
    comboLabel.setIncludeFontPadding(false);
    card.addView(comboLabel, matchWrap());

    comboNamesLabel = text("jab, cross, jab, cross", 17, GOLD, Typeface.NORMAL);
    comboNamesLabel.setGravity(Gravity.CENTER);
    comboNamesLabel.setPadding(0, dp(8), 0, 0);
    card.addView(comboNamesLabel, matchWrap());
    return card;
  }

  private LinearLayout controlPanel() {
    LinearLayout controls = row();
    controls.setPadding(0, dp(18), 0, 0);
    startButton = button("Start", true);
    Button skip = button("Skip", false);
    Button reset = button("Reset", false);
    controls.addView(startButton, weightedButton());
    controls.addView(skip, weightedButton());
    controls.addView(reset, weightedButton());

    startButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            toggleWorkout();
          }
        });
    skip.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            showCombo(running);
            nextCallIn = paceSeconds;
            render();
          }
        });
    reset.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            resetWorkout();
          }
        });
    return controls;
  }

  private LinearLayout heartRatePanel() {
    LinearLayout panel = panel(false);
    LinearLayout heading = sectionHeading("Heart Rate", "Synced from Wear OS");
    wearStatusLabel = chip("Watch sync", REST_BLUE);
    heading.addView(wearStatusLabel);
    panel.addView(heading);
    heartRateSummaryLabel = text("", 16, MUTED, Typeface.NORMAL);
    heartRateSummaryLabel.setLineSpacing(dp(5), 1.0f);
    heartRateSummaryLabel.setPadding(0, dp(10), 0, 0);
    panel.addView(heartRateSummaryLabel);
    return panel;
  }

  private LinearLayout formatPanel() {
    LinearLayout panel = panel(false);
    panel.addView(sectionHeading("Round Format", "Common fight clocks"));
    panel.addView(presetRow(0, 2));
    panel.addView(presetRow(2, 4));

    LinearLayout customRow = row();
    customRow.setPadding(0, dp(12), 0, 0);
    roundsInput = numberInput(String.valueOf(rounds));
    minutesInput = numberInput(String.valueOf(roundSeconds / 60));
    restInput = numberInput(String.valueOf(restSeconds));
    customRow.addView(labeledInput("Rounds", roundsInput), weightParams());
    customRow.addView(labeledInput("Minutes", minutesInput), weightParams());
    customRow.addView(labeledInput("Rest", restInput), weightParams());
    panel.addView(customRow);

    Button apply = button("Apply custom timer", false);
    apply.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            rounds = clamp(readInt(roundsInput, rounds), 1, 15);
            roundSeconds = clamp(readInt(minutesInput, roundSeconds / 60), 1, 12) * 60;
            restSeconds = clamp(readInt(restInput, restSeconds), 15, 180);
            saveSettings();
            if (!running) {
              resetWorkout();
            }
            render();
          }
        });
    LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(-1, dp(52));
    applyParams.setMargins(dp(6), dp(16), dp(6), 0);
    panel.addView(apply, applyParams);
    return panel;
  }

  private LinearLayout presetRow(int start, int end) {
    LinearLayout presetRow = row();
    presetRow.setPadding(0, dp(10), 0, 0);
    for (int i = start; i < end; i += 1) {
      final Preset preset = presets.get(i);
      Button presetButton = button(preset.label, false);
      presetButton.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              rounds = preset.rounds;
              roundSeconds = preset.roundSeconds;
              restSeconds = preset.restSeconds;
              saveSettings();
              if (!running) {
                resetWorkout();
              }
              render();
            }
          });
      presetRow.addView(presetButton, presetButtonParams());
    }
    return presetRow;
  }

  private LinearLayout aboutPanel() {
    LinearLayout panel = panel(true);
    panel.setPadding(dp(20), dp(22), dp(20), dp(22));
    panel.addView(sectionHeading("About", "Built by vi-code"));

    TextView copy =
        text(
            "Corner Call is a native Android boxing timer built for real bag work. It keeps the screen simple, calls combinations out loud, and stays out of the way while you train.",
            16,
            MUTED,
            Typeface.NORMAL);
    copy.setLineSpacing(dp(5), 1.0f);
    copy.setPadding(0, dp(8), 0, dp(16));
    panel.addView(copy);

    TextView author =
        text(
            "Made by Vihar Patel. If the app helps your rounds, you can buy me a coffee or check out more of my work.",
            16,
            TEXT,
            Typeface.NORMAL);
    author.setLineSpacing(dp(5), 1.0f);
    panel.addView(author);

    Button venmo = button("Buy me a coffee on Venmo", true);
    LinearLayout.LayoutParams venmoParams = new LinearLayout.LayoutParams(-1, dp(56));
    venmoParams.setMargins(0, dp(18), 0, 0);
    panel.addView(venmo, venmoParams);
    venmo.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            openLink("https://venmo.com/u/vikoopat");
          }
        });

    Button website = button("vi-code.github.io", false);
    LinearLayout.LayoutParams siteParams = new LinearLayout.LayoutParams(-1, dp(56));
    siteParams.setMargins(0, dp(10), 0, 0);
    panel.addView(website, siteParams);
    website.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            openLink("https://vi-code.github.io");
          }
        });

    return panel;
  }

  private LinearLayout enginePanel() {
    LinearLayout panel = panel(false);
    panel.addView(sectionHeading("Combo Engine", "Control the coach in your headphones"));

    paceValue = text("", 15, GOLD, Typeface.BOLD);
    panel.addView(sliderRow("Call pace", 2, 9, paceSeconds, paceValue, true));

    lengthValue = text("", 15, GOLD, Typeface.BOLD);
    panel.addView(sliderRow("Combo length", 2, 6, comboLength, lengthValue, false));

    LinearLayout toggles = row();
    toggles.setPadding(0, dp(8), 0, 0);
    defenseToggle = checkbox("Defense", includeDefense);
    voiceToggle = checkbox("Voice", voiceEnabled);
    toggles.addView(defenseToggle, weightParams());
    toggles.addView(voiceToggle, weightParams());
    panel.addView(toggles);

    defenseToggle.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            includeDefense = defenseToggle.isChecked();
            saveSettings();
            showCombo(false);
            render();
          }
        });
    voiceToggle.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            voiceEnabled = voiceToggle.isChecked();
            saveSettings();
          }
        });
    return panel;
  }

  private LinearLayout notesPanel() {
    LinearLayout panel = panel(false);
    LinearLayout heading = sectionHeading("Coach Notes", "");
    callCountLabel = chip("0 calls", MUTED);
    heading.addView(callCountLabel);
    panel.addView(heading);

    recentLabel = text("Start a round to build the call log", 16, MUTED, Typeface.NORMAL);
    recentLabel.setLineSpacing(dp(5), 1.0f);
    recentLabel.setPadding(0, dp(10), 0, 0);
    panel.addView(recentLabel);
    return panel;
  }

  private LinearLayout punchPanel() {
    LinearLayout panel = panel(false);
    panel.addView(sectionHeading("Punch Key", "Orthodox numbers"));
    for (Move punch : punches) {
      LinearLayout row = row();
      row.setPadding(0, dp(7), 0, dp(7));
      TextView number = text(punch.code, 16, DARK, Typeface.BOLD);
      number.setGravity(Gravity.CENTER);
      number.setBackground(rounded(GOLD, dp(14), 0, 0));
      row.addView(number, new LinearLayout.LayoutParams(dp(34), dp(34)));
      TextView name = text(punch.name, 16, MUTED, Typeface.NORMAL);
      LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1);
      nameParams.setMargins(dp(12), 0, 0, 0);
      row.addView(name, nameParams);
      panel.addView(row);
    }
    return panel;
  }

  private View sliderRow(
      String label, int min, int max, int value, final TextView valueLabel, final boolean pace) {
    LinearLayout wrap = vertical();
    wrap.setPadding(0, dp(12), 0, dp(4));
    LinearLayout labelRow = row();
    labelRow.addView(text(label, 14, MUTED, Typeface.NORMAL), weightParams());
    labelRow.addView(valueLabel);
    wrap.addView(labelRow);

    SeekBar seek = new SeekBar(this);
    seek.setMax(max - min);
    seek.setProgress(value - min);
    seek.setProgressTintList(ColorStateList.valueOf(ACCENT));
    seek.setProgressBackgroundTintList(ColorStateList.valueOf(PANEL_STRONG));
    seek.setThumbTintList(ColorStateList.valueOf(GOLD));
    wrap.addView(seek);
    updateSliderLabel(valueLabel, value, pace);

    seek.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            int selected = min + progress;
            if (pace) {
              paceSeconds = selected;
            } else {
              comboLength = selected;
            }
            updateSliderLabel(valueLabel, selected, pace);
            saveSettings();
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    return wrap;
  }

  private void toggleWorkout() {
    toggleWorkout(true);
  }

  private void toggleWorkout(boolean notifyWear) {
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

  private void stopTimer() {
    running = false;
    handler.removeCallbacks(ticker);
  }

  private void resetWorkout() {
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

  private void tick() {
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

  private void advancePhase() {
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

  private void showCombo(boolean announce) {
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

  private List<String> buildCombo() {
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

  private List<String> trimCombo(List<String> selected) {
    List<String> out = new ArrayList<>();
    int limit = Math.min(comboLength, selected.size());
    for (int i = 0; i < limit; i += 1) {
      out.add(selected.get(i));
    }
    return out;
  }

  private Move weightedMove(List<Move> moves) {
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

  private boolean allPunches(String[] combo) {
    for (String code : combo) {
      if (!isPunch(code)) {
        return false;
      }
    }
    return true;
  }

  private boolean isPunch(String code) {
    for (Move punch : punches) {
      if (punch.code.equals(code)) {
        return true;
      }
    }
    return false;
  }

  private String moveName(String code) {
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

  private void speak(String text) {
    if (!voiceEnabled || !speechReady || textToSpeech == null) {
      return;
    }
    textToSpeech.stop();
    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "corner-call");
  }

  private void bell() {
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

  private void render() {
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

  private void renderHeartRateSummary() {
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

  private void handleWearControl(String payload) {
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

  private void completeFromWear() {
    stopTimer();
    complete = true;
    phaseRemaining = 0;
    speak("Workout complete.");
    render();
  }

  private void ensureSession() {
    if (!sessionId.isEmpty()) {
      return;
    }
    sessionStartedAt = System.currentTimeMillis();
    sessionId = "corner-" + sessionStartedAt;
    heartRateStore.ensureSession(sessionId, sessionStartedAt, WearPaths.STATUS_ACTIVE);
  }

  private void sendControlToWatch(String action) {
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
                  Wearable.getMessageClient(MainActivity.this)
                      .sendMessage(node.getId(), WearPaths.controlPath(action), state.toBytes());
                }
              }
            });
  }

  private void mirrorSessionState(SessionState state) {
    PutDataMapRequest request = PutDataMapRequest.create(WearPaths.SESSION_STATE);
    DataMap map = request.getDataMap();
    map.putString("payload", state.toJson().toString());
    map.putLong("updatedAt", System.currentTimeMillis());
    PutDataRequest dataRequest = request.asPutDataRequest();
    dataRequest.setUrgent();
    Wearable.getDataClient(this).putDataItem(dataRequest);
  }

  private String statusForAction(String action) {
    if (WearPaths.ACTION_END.equals(action)) {
      return WearPaths.STATUS_COMPLETE;
    }
    if (WearPaths.ACTION_PAUSE.equals(action)) {
      return WearPaths.STATUS_PAUSED;
    }
    return WearPaths.STATUS_ACTIVE;
  }

  private void registerWearBroadcasts() {
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

  private void unregisterWearBroadcasts() {
    if (!wearBroadcastsRegistered) {
      return;
    }
    unregisterReceiver(wearReceiver);
    wearBroadcastsRegistered = false;
  }

  private void loadSettings() {
    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
    rounds = prefs.getInt("rounds", rounds);
    roundSeconds = prefs.getInt("roundSeconds", roundSeconds);
    restSeconds = prefs.getInt("restSeconds", restSeconds);
    paceSeconds = prefs.getInt("paceSeconds", paceSeconds);
    comboLength = prefs.getInt("comboLength", comboLength);
    includeDefense = prefs.getBoolean("includeDefense", includeDefense);
    voiceEnabled = prefs.getBoolean("voiceEnabled", voiceEnabled);
  }

  private void saveSettings() {
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

  private LinearLayout sectionHeading(String title, String subtitle) {
    LinearLayout heading = row();
    heading.setPadding(0, 0, 0, dp(6));
    LinearLayout copy = vertical();
    copy.addView(text(title, 18, TEXT, Typeface.BOLD));
    if (!subtitle.isEmpty()) {
      copy.addView(text(subtitle, 13, DIM, Typeface.NORMAL));
    }
    heading.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
    return heading;
  }

  private LinearLayout panel(boolean hero) {
    LinearLayout panel = vertical();
    panel.setPadding(dp(16), dp(16), dp(16), dp(16));
    panel.setBackground(rounded(hero ? SURFACE : PANEL, dp(24), LINE, 1));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(0, dp(14), 0, 0);
    panel.setLayoutParams(params);
    return panel;
  }

  private LinearLayout vertical() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    return layout;
  }

  private LinearLayout row() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    return row;
  }

  private TextView text(String value, int sp, int color, int style) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextColor(color);
    view.setTextSize(sp);
    view.setTypeface(Typeface.DEFAULT, style);
    return view;
  }

  private TextView chip(String value, int color) {
    TextView chip = text(value, 13, color, Typeface.BOLD);
    chip.setGravity(Gravity.CENTER);
    chip.setPadding(dp(12), dp(7), dp(12), dp(7));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
    params.setMargins(dp(3), 0, dp(3), 0);
    chip.setLayoutParams(params);
    chip.setBackground(rounded(0xff151b24, dp(18), color, 1));
    return chip;
  }

  private Button button(String label, boolean primary) {
    Button button = new Button(this);
    button.setText(label);
    button.setAllCaps(false);
    button.setTextColor(primary ? DARK : TEXT);
    button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    button.setGravity(Gravity.CENTER);
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setIncludeFontPadding(false);
    button.setPadding(dp(10), 0, dp(10), 0);
    button.setBackground(rounded(primary ? GOLD : PANEL_STRONG, dp(16), primary ? GOLD : LINE, 1));
    button.setMinHeight(dp(52));
    return button;
  }

  private Button tabButton(String label) {
    Button button = new Button(this);
    button.setText(label);
    button.setAllCaps(false);
    button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    button.setGravity(Gravity.CENTER);
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setIncludeFontPadding(false);
    button.setPadding(dp(10), 0, dp(10), 0);
    button.setMinHeight(dp(48));
    return button;
  }

  private CheckBox checkbox(String label, boolean checked) {
    CheckBox box = new CheckBox(this);
    box.setText(label);
    box.setTextColor(MUTED);
    box.setTextSize(16);
    box.setChecked(checked);
    box.setButtonTintList(ColorStateList.valueOf(ACCENT));
    box.setBackground(rounded(SURFACE, dp(16), LINE, 1));
    box.setPadding(dp(12), 0, dp(12), 0);
    return box;
  }

  private EditText numberInput(String value) {
    EditText input = new EditText(this);
    input.setText(value);
    input.setSingleLine(true);
    input.setTextColor(TEXT);
    input.setTextSize(18);
    input.setGravity(Gravity.CENTER);
    input.setSelectAllOnFocus(true);
    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    input.setBackground(rounded(SURFACE, dp(14), LINE, 1));
    input.setPadding(dp(8), 0, dp(8), 0);
    return input;
  }

  private LinearLayout labeledInput(String label, EditText input) {
    LinearLayout wrap = vertical();
    TextView labelView = text(label, 12, MUTED, Typeface.NORMAL);
    labelView.setPadding(dp(2), 0, 0, dp(5));
    wrap.addView(labelView);
    wrap.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
    return wrap;
  }

  private LinearLayout.LayoutParams matchWrap() {
    return new LinearLayout.LayoutParams(-1, -2);
  }

  private LinearLayout.LayoutParams weightParams() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
    params.setMargins(dp(4), 0, dp(4), 0);
    return params;
  }

  private LinearLayout.LayoutParams weightedButton() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1);
    params.setMargins(dp(6), 0, dp(6), 0);
    return params;
  }

  private LinearLayout.LayoutParams presetButtonParams() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1);
    params.setMargins(dp(8), 0, dp(8), 0);
    return params;
  }

  private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    if (strokeWidth > 0) {
      drawable.setStroke(dp(strokeWidth), strokeColor);
    }
    return drawable;
  }

  private void updateSliderLabel(TextView label, int value, boolean pace) {
    label.setText(pace ? value + "s" : String.valueOf(value));
  }

  private void setTabSelected(Button button, boolean selected) {
    button.setTextColor(selected ? DARK : TEXT);
    button.setBackground(rounded(selected ? GOLD : PANEL_STRONG, dp(16), selected ? GOLD : LINE, 1));
  }

  private void openLink(String url) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
  }

  private void setInputText(EditText input, String value) {
    if (!input.hasFocus() && !input.getText().toString().equals(value)) {
      input.setText(value);
    }
  }

  private int readInt(EditText input, int fallback) {
    try {
      return Integer.parseInt(input.getText().toString().trim());
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private String formatTime(int totalSeconds) {
    int safeSeconds = Math.max(0, totalSeconds);
    int minutes = safeSeconds / 60;
    int seconds = safeSeconds % 60;
    return String.format(Locale.US, "%02d:%02d", minutes, seconds);
  }

  private String formatDuration(int totalSeconds) {
    int minutes = totalSeconds / 60;
    int seconds = totalSeconds % 60;
    return minutes + ":" + String.format(Locale.US, "%02d", seconds);
  }

  private String formatClock(long timestampMs) {
    if (timestampMs <= 0) {
      return "";
    }
    return new java.text.SimpleDateFormat("h:mm a", Locale.US).format(new java.util.Date(timestampMs));
  }

  private String join(List<String> values, String separator) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.size(); i += 1) {
      builder.append(values.get(i));
      if (i < values.size() - 1) {
        builder.append(separator);
      }
    }
    return builder.toString();
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }
}
