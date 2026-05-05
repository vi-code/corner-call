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

public class MainActivity extends PhoneWearActivity {
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
}

