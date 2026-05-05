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

abstract class PhoneLowerPanelsActivity extends PhoneTopLayoutActivity {
  protected void showTrainTab() {
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

  protected void showAboutTab() {
    showingAbout = true;
    contentContainer.removeAllViews();
    contentContainer.addView(aboutPanel());
    render();
  }

  protected LinearLayout enginePanel() {
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

  protected LinearLayout notesPanel() {
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

  protected LinearLayout punchPanel() {
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

  protected View sliderRow(
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
}

