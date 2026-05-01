package com.cornercall.app;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.cornercall.app.shared.WearPaths;

public final class MainActivity extends Activity {
  private static final int DARK = 0xff07080c;
  private static final int SURFACE = 0xff111821;
  private static final int PANEL = 0xff18212c;
  private static final int TEXT = 0xfff7f8fb;
  private static final int MUTED = 0xff9aa3b2;
  private static final int GOLD = 0xfff8b84e;
  private static final int ACCENT = 0xfff14d42;
  private static final int LINE = 0xff293241;

  private TextView statusLabel;
  private TextView heartRateLabel;
  private TextView caloriesLabel;
  private TextView syncLabel;
  private Button startButton;
  private Button pauseButton;
  private Button endButton;
  private boolean active;
  private boolean paused;

  private final BroadcastReceiver stateReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (!HeartRateService.ACTION_STATE.equals(intent.getAction())) {
            return;
          }
          active = intent.getBooleanExtra(HeartRateService.EXTRA_ACTIVE, false);
          paused = intent.getBooleanExtra(HeartRateService.EXTRA_PAUSED, false);
          int bpm = intent.getIntExtra(HeartRateService.EXTRA_BPM, 0);
          float calories = intent.getFloatExtra(HeartRateService.EXTRA_CALORIES, 0);
          String sync = intent.getStringExtra(HeartRateService.EXTRA_SYNC);
          statusLabel.setText(active ? "Recording" : paused ? "Paused" : "Ready");
          heartRateLabel.setText(bpm > 0 ? bpm + "\nbpm" : "--\nbpm");
          caloriesLabel.setText(Math.round(calories) + " cal");
          syncLabel.setText(sync == null ? "Waiting for phone" : sync);
          startButton.setText(active ? "Resume" : "Start");
          pauseButton.setEnabled(active || paused);
          endButton.setEnabled(active || paused);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(buildUi());
    requestSensorPermissions();
  }

  @Override
  protected void onStart() {
    super.onStart();
    IntentFilter filter = new IntentFilter(HeartRateService.ACTION_STATE);
    if (Build.VERSION.SDK_INT >= 33) {
      registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    } else {
      registerReceiver(stateReceiver, filter);
    }
  }

  @Override
  protected void onStop() {
    unregisterReceiver(stateReceiver);
    super.onStop();
  }

  private View buildUi() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(DARK);

    LinearLayout root = vertical();
    root.setGravity(Gravity.CENTER_HORIZONTAL);
    root.setPadding(dp(16), dp(20), dp(16), dp(20));
    scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

    TextView title = text("Corner Call", 20, GOLD, Typeface.BOLD);
    title.setGravity(Gravity.CENTER);
    root.addView(title, matchWrap());

    statusLabel = text("Ready", 14, MUTED, Typeface.BOLD);
    statusLabel.setGravity(Gravity.CENTER);
    statusLabel.setPadding(0, dp(4), 0, dp(10));
    root.addView(statusLabel, matchWrap());

    heartRateLabel = text("--\nbpm", 46, TEXT, Typeface.BOLD);
    heartRateLabel.setGravity(Gravity.CENTER);
    heartRateLabel.setIncludeFontPadding(false);
    heartRateLabel.setBackground(rounded(SURFACE, dp(96), ACCENT, 2));
    root.addView(heartRateLabel, new LinearLayout.LayoutParams(dp(150), dp(150)));

    caloriesLabel = text("0 cal", 16, GOLD, Typeface.BOLD);
    caloriesLabel.setGravity(Gravity.CENTER);
    caloriesLabel.setPadding(0, dp(10), 0, 0);
    root.addView(caloriesLabel, matchWrap());

    syncLabel = text("Waiting for phone", 13, MUTED, Typeface.NORMAL);
    syncLabel.setGravity(Gravity.CENTER);
    syncLabel.setPadding(0, dp(12), 0, dp(12));
    root.addView(syncLabel, matchWrap());

    startButton = button("Start", true);
    pauseButton = button("Pause", false);
    endButton = button("End", false);
    pauseButton.setEnabled(false);
    endButton.setEnabled(false);
    root.addView(startButton, buttonParams());
    root.addView(pauseButton, buttonParams());
    root.addView(endButton, buttonParams());

    startButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            startServiceAction(paused ? WearPaths.ACTION_RESUME : WearPaths.ACTION_START, false);
          }
        });
    pauseButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            startServiceAction(WearPaths.ACTION_PAUSE, false);
          }
        });
    endButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            startServiceAction(WearPaths.ACTION_END, false);
          }
        });
    return scroll;
  }

  private void startServiceAction(String action, boolean fromPhone) {
    Intent intent = new Intent(this, HeartRateService.class);
    intent.setAction(action);
    intent.putExtra(HeartRateService.EXTRA_FROM_PHONE, fromPhone);
    if (Build.VERSION.SDK_INT >= 26) {
      startForegroundService(intent);
    } else {
      startService(intent);
    }
  }

  private void requestSensorPermissions() {
    if (Build.VERSION.SDK_INT < 23) {
      return;
    }
    java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
    if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
      permissions.add(Manifest.permission.BODY_SENSORS);
    }
    if (Build.VERSION.SDK_INT >= 29
        && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
      permissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
    }
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      permissions.add(Manifest.permission.POST_NOTIFICATIONS);
    }
    if (!permissions.isEmpty()) {
      requestPermissions(permissions.toArray(new String[0]), 7);
    }
  }

  private LinearLayout vertical() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    return layout;
  }

  private TextView text(String value, int sp, int color, int style) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextColor(color);
    view.setTextSize(sp);
    view.setTypeface(Typeface.DEFAULT, style);
    return view;
  }

  private Button button(String label, boolean primary) {
    Button button = new Button(this);
    button.setText(label);
    button.setAllCaps(false);
    button.setTextColor(primary ? DARK : TEXT);
    button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    button.setBackground(rounded(primary ? GOLD : PANEL, dp(18), primary ? GOLD : LINE, 1));
    return button;
  }

  private LinearLayout.LayoutParams matchWrap() {
    return new LinearLayout.LayoutParams(-1, -2);
  }

  private LinearLayout.LayoutParams buttonParams() {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
    params.setMargins(0, dp(7), 0, 0);
    return params;
  }

  private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    drawable.setStroke(dp(strokeWidth), strokeColor);
    return drawable;
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }
}
