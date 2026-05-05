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

abstract class PhoneViewHelpersActivity extends PhoneStateActivity {
  protected LinearLayout sectionHeading(String title, String subtitle) {
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

  protected LinearLayout panel(boolean hero) {
    LinearLayout panel = vertical();
    panel.setPadding(dp(16), dp(16), dp(16), dp(16));
    panel.setBackground(rounded(hero ? SURFACE : PANEL, dp(24), LINE, 1));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(0, dp(14), 0, 0);
    panel.setLayoutParams(params);
    return panel;
  }

  protected LinearLayout vertical() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    return layout;
  }

  protected LinearLayout row() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    return row;
  }

  protected TextView text(String value, int sp, int color, int style) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextColor(color);
    view.setTextSize(sp);
    view.setTypeface(Typeface.DEFAULT, style);
    return view;
  }

  protected TextView chip(String value, int color) {
    TextView chip = text(value, 13, color, Typeface.BOLD);
    chip.setGravity(Gravity.CENTER);
    chip.setPadding(dp(12), dp(7), dp(12), dp(7));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
    params.setMargins(dp(3), 0, dp(3), 0);
    chip.setLayoutParams(params);
    chip.setBackground(rounded(0xff151b24, dp(18), color, 1));
    return chip;
  }

  protected Button button(String label, boolean primary) {
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

  protected Button tabButton(String label) {
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

  protected CheckBox checkbox(String label, boolean checked) {
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

  protected EditText numberInput(String value) {
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

  protected LinearLayout labeledInput(String label, EditText input) {
    LinearLayout wrap = vertical();
    TextView labelView = text(label, 12, MUTED, Typeface.NORMAL);
    labelView.setPadding(dp(2), 0, 0, dp(5));
    wrap.addView(labelView);
    wrap.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
    return wrap;
  }

  protected LinearLayout.LayoutParams matchWrap() {
    return new LinearLayout.LayoutParams(-1, -2);
  }

  protected LinearLayout.LayoutParams weightParams() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
    params.setMargins(dp(4), 0, dp(4), 0);
    return params;
  }

  protected LinearLayout.LayoutParams weightedButton() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1);
    params.setMargins(dp(6), 0, dp(6), 0);
    return params;
  }

  protected LinearLayout.LayoutParams presetButtonParams() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1);
    params.setMargins(dp(8), 0, dp(8), 0);
    return params;
  }

  protected GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    if (strokeWidth > 0) {
      drawable.setStroke(dp(strokeWidth), strokeColor);
    }
    return drawable;
  }

  protected void updateSliderLabel(TextView label, int value, boolean pace) {
    label.setText(pace ? value + "s" : String.valueOf(value));
  }

  protected void setTabSelected(Button button, boolean selected) {
    button.setTextColor(selected ? DARK : TEXT);
    button.setBackground(rounded(selected ? GOLD : PANEL_STRONG, dp(16), selected ? GOLD : LINE, 1));
  }

  protected void openLink(String url) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
  }

  protected void setInputText(EditText input, String value) {
    if (!input.hasFocus() && !input.getText().toString().equals(value)) {
      input.setText(value);
    }
  }

  protected int readInt(EditText input, int fallback) {
    try {
      return Integer.parseInt(input.getText().toString().trim());
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  protected int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  protected String formatTime(int totalSeconds) {
    int safeSeconds = Math.max(0, totalSeconds);
    int minutes = safeSeconds / 60;
    int seconds = safeSeconds % 60;
    return String.format(Locale.US, "%02d:%02d", minutes, seconds);
  }

  protected String formatDuration(int totalSeconds) {
    int minutes = totalSeconds / 60;
    int seconds = totalSeconds % 60;
    return minutes + ":" + String.format(Locale.US, "%02d", seconds);
  }

  protected String formatClock(long timestampMs) {
    if (timestampMs <= 0) {
      return "";
    }
    return new java.text.SimpleDateFormat("h:mm a", Locale.US).format(new java.util.Date(timestampMs));
  }

  protected String join(List<String> values, String separator) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.size(); i += 1) {
      builder.append(values.get(i));
      if (i < values.size() - 1) {
        builder.append(separator);
      }
    }
    return builder.toString();
  }

  protected int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }
}

