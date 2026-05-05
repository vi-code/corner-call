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

abstract class PhoneTopLayoutActivity extends PhoneViewHelpersActivity {
  protected View buildUi() {
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

  protected View header() {
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

  protected LinearLayout tabBar() {
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

  protected LinearLayout workoutCard() {
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

  protected LinearLayout controlPanel() {
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

  protected LinearLayout heartRatePanel() {
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

  protected LinearLayout formatPanel() {
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

  protected LinearLayout presetRow(int start, int end) {
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

  protected LinearLayout aboutPanel() {
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
}

