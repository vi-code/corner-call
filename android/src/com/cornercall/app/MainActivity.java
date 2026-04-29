package com.cornercall.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
  private static final String PREFS = "corner_call_settings";
  private static final int DARK = 0xff08090d;
  private static final int PANEL = 0xff11151c;
  private static final int PANEL_SOFT = 0xff171d26;
  private static final int TEXT = 0xfff6f7fb;
  private static final int MUTED = 0xff9aa3b2;
  private static final int ACCENT = 0xfff14d42;
  private static final int GOLD = 0xfff8b84e;
  private static final int LINE = 0xff28303b;

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
  private TextView paceValue;
  private TextView lengthValue;
  private EditText roundsInput;
  private EditText minutesInput;
  private EditText restInput;
  private CheckBox defenseToggle;
  private CheckBox voiceToggle;
  private Button startButton;

  private final List<String> recentCombos = new ArrayList<>();

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

    loadSettings();
    phaseRemaining = roundSeconds;
    phaseDuration = roundSeconds;
    setContentView(buildUi());
    showCombo(false);
    render();
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

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(18), dp(24), dp(18), dp(28));
    scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

    TextView kicker = text("HEAVY BAG ROUNDS", 12, GOLD, Typeface.BOLD);
    root.addView(kicker);

    TextView title = text("Corner Call", 44, TEXT, Typeface.BOLD);
    root.addView(title);

    LinearLayout topPanel = panel();
    topPanel.setGravity(Gravity.CENTER);
    topPanel.setPadding(dp(20), dp(22), dp(20), dp(22));
    root.addView(topPanel);

    phaseLabel = text("Ready", 14, GOLD, Typeface.BOLD);
    phaseLabel.setGravity(Gravity.CENTER);
    topPanel.addView(phaseLabel);

    roundLabel = text("Round 1 / 5", 18, MUTED, Typeface.BOLD);
    roundLabel.setGravity(Gravity.CENTER);
    topPanel.addView(roundLabel);

    timeLabel = text("03:00", 72, TEXT, Typeface.BOLD);
    timeLabel.setGravity(Gravity.CENTER);
    timeLabel.setIncludeFontPadding(false);
    topPanel.addView(timeLabel);

    hintLabel = text("Pick a format and tap Start", 15, MUTED, Typeface.NORMAL);
    hintLabel.setGravity(Gravity.CENTER);
    topPanel.addView(hintLabel);

    comboLabel = text("1 - 2 - 1 - 2", 42, TEXT, Typeface.BOLD);
    comboLabel.setGravity(Gravity.CENTER);
    comboLabel.setPadding(0, dp(18), 0, 0);
    topPanel.addView(comboLabel);

    comboNamesLabel = text("jab, cross, jab, cross", 17, GOLD, Typeface.NORMAL);
    comboNamesLabel.setGravity(Gravity.CENTER);
    topPanel.addView(comboNamesLabel);

    LinearLayout controlRow = row();
    root.addView(controlRow);
    startButton = button("Start", true);
    controlRow.addView(startButton, weightParams());
    controlRow.addView(button("Skip", false), weightParams());
    controlRow.addView(button("Reset", false), weightParams());

    startButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            toggleWorkout();
          }
        });
    controlRow
        .getChildAt(1)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                showCombo(running);
                nextCallIn = paceSeconds;
                render();
              }
            });
    controlRow
        .getChildAt(2)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                resetWorkout();
              }
            });

    root.addView(formatPanel());
    root.addView(enginePanel());
    root.addView(notesPanel());
    root.addView(punchPanel());

    return scroll;
  }

  private LinearLayout formatPanel() {
    LinearLayout panel = panel();
    panel.addView(text("Round Format", 18, TEXT, Typeface.BOLD));

    LinearLayout presetRow = row();
    panel.addView(presetRow);
    for (final Preset preset : presets) {
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
      presetRow.addView(presetButton, weightParams());
    }

    LinearLayout customRow = row();
    panel.addView(customRow);
    roundsInput = numberInput(String.valueOf(rounds));
    minutesInput = numberInput(String.valueOf(roundSeconds / 60));
    restInput = numberInput(String.valueOf(restSeconds));
    customRow.addView(labeledInput("Rounds", roundsInput), weightParams());
    customRow.addView(labeledInput("Minutes", minutesInput), weightParams());
    customRow.addView(labeledInput("Rest sec", restInput), weightParams());

    Button apply = button("Apply", false);
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
    panel.addView(apply);
    return panel;
  }

  private LinearLayout enginePanel() {
    LinearLayout panel = panel();
    panel.addView(text("Combo Engine", 18, TEXT, Typeface.BOLD));

    paceValue = text("", 15, GOLD, Typeface.BOLD);
    panel.addView(sliderRow("Call pace", 2, 9, paceSeconds, paceValue, true));

    lengthValue = text("", 15, GOLD, Typeface.BOLD);
    panel.addView(sliderRow("Combo length", 2, 6, comboLength, lengthValue, false));

    defenseToggle = checkbox("Defensive moves", includeDefense);
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
    panel.addView(defenseToggle);

    voiceToggle = checkbox("Voice calls", voiceEnabled);
    voiceToggle.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            voiceEnabled = voiceToggle.isChecked();
            saveSettings();
          }
        });
    panel.addView(voiceToggle);
    return panel;
  }

  private LinearLayout notesPanel() {
    LinearLayout panel = panel();
    LinearLayout heading = row();
    heading.addView(text("Coach Notes", 18, TEXT, Typeface.BOLD), weightParams());
    callCountLabel = text("0 calls", 14, MUTED, Typeface.NORMAL);
    heading.addView(callCountLabel);
    panel.addView(heading);
    recentLabel = text("Start a round to build the call log", 16, MUTED, Typeface.NORMAL);
    recentLabel.setLineSpacing(dp(3), 1.0f);
    panel.addView(recentLabel);
    return panel;
  }

  private LinearLayout punchPanel() {
    LinearLayout panel = panel();
    panel.addView(text("Punch Key", 18, TEXT, Typeface.BOLD));
    for (Move punch : punches) {
      TextView item = text(punch.code + "   " + punch.name, 16, MUTED, Typeface.BOLD);
      item.setPadding(0, dp(5), 0, dp(5));
      panel.addView(item);
    }
    return panel;
  }

  private View sliderRow(
      String label, int min, int max, int value, final TextView valueLabel, final boolean pace) {
    LinearLayout wrap = new LinearLayout(this);
    wrap.setOrientation(LinearLayout.VERTICAL);
    wrap.setPadding(0, dp(8), 0, dp(8));
    LinearLayout labelRow = row();
    labelRow.addView(text(label, 14, MUTED, Typeface.NORMAL), weightParams());
    labelRow.addView(valueLabel);
    wrap.addView(labelRow);

    SeekBar seek = new SeekBar(this);
    seek.setMax(max - min);
    seek.setProgress(value - min);
    seek.getProgressDrawable().setTint(ACCENT);
    seek.getThumb().setTint(ACCENT);
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
    if (running) {
      stopTimer();
      render();
      return;
    }
    if (complete) {
      resetWorkout();
    }
    running = true;
    complete = false;
    if (workPhase) {
      showCombo(true);
      nextCallIn = paceSeconds;
    }
    handler.postDelayed(ticker, 1000);
    render();
  }

  private void stopTimer() {
    running = false;
    handler.removeCallbacks(ticker);
  }

  private void resetWorkout() {
    stopTimer();
    complete = false;
    workPhase = true;
    currentRound = 1;
    phaseRemaining = roundSeconds;
    phaseDuration = roundSeconds;
    nextCallIn = 0;
    comboCount = 0;
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
    phaseLabel.setText(complete ? "Complete" : running ? (workPhase ? "Work" : "Rest") : "Ready");
    roundLabel.setText("Round " + currentRound + " / " + rounds);
    timeLabel.setText(formatTime(phaseRemaining));
    hintLabel.setText(
        complete
            ? "Nice work"
            : running ? (workPhase ? "Hands up, chin down" : "Breathe") : "Pick a format and tap Start");
    startButton.setText(running ? "Pause" : complete ? "Start again" : "Start");
    callCountLabel.setText(comboCount + (comboCount == 1 ? " call" : " calls"));
    defenseToggle.setChecked(includeDefense);
    voiceToggle.setChecked(voiceEnabled);
    roundsInput.setText(String.valueOf(rounds));
    minutesInput.setText(String.valueOf(roundSeconds / 60));
    restInput.setText(String.valueOf(restSeconds));

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

  private LinearLayout panel() {
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(16), dp(16), dp(16), dp(16));
    panel.setBackgroundColor(PANEL);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(0, dp(14), 0, 0);
    panel.setLayoutParams(params);
    return panel;
  }

  private LinearLayout row() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(8), 0, 0);
    return row;
  }

  private TextView text(String value, int sp, int color, int style) {
    TextView text = new TextView(this);
    text.setText(value);
    text.setTextColor(color);
    text.setTextSize(sp);
    text.setTypeface(Typeface.DEFAULT, style);
    return text;
  }

  private Button button(String label, boolean primary) {
    Button button = new Button(this);
    button.setText(label);
    button.setAllCaps(false);
    button.setTextColor(primary ? DARK : TEXT);
    button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    button.setBackgroundColor(primary ? GOLD : PANEL_SOFT);
    button.setMinHeight(dp(48));
    return button;
  }

  private CheckBox checkbox(String label, boolean checked) {
    CheckBox box = new CheckBox(this);
    box.setText(label);
    box.setTextColor(MUTED);
    box.setTextSize(16);
    box.setChecked(checked);
    box.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
    return box;
  }

  private EditText numberInput(String value) {
    EditText input = new EditText(this);
    input.setText(value);
    input.setSingleLine(true);
    input.setTextColor(TEXT);
    input.setTextSize(17);
    input.setGravity(Gravity.CENTER);
    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    input.setBackgroundColor(PANEL_SOFT);
    input.setPadding(dp(8), 0, dp(8), 0);
    return input;
  }

  private LinearLayout labeledInput(String label, EditText input) {
    LinearLayout wrap = new LinearLayout(this);
    wrap.setOrientation(LinearLayout.VERTICAL);
    TextView labelView = text(label, 12, MUTED, Typeface.NORMAL);
    wrap.addView(labelView);
    wrap.addView(input, new LinearLayout.LayoutParams(-1, dp(46)));
    return wrap;
  }

  private LinearLayout.LayoutParams weightParams() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
    params.setMargins(dp(3), 0, dp(3), 0);
    return params;
  }

  private void updateSliderLabel(TextView label, int value, boolean pace) {
    label.setText(pace ? value + "s" : String.valueOf(value));
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
