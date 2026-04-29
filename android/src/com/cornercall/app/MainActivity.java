package com.cornercall.app;

import android.app.Activity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private TextToSpeech textToSpeech;
  private boolean speechReady;
  private ToneGenerator toneGenerator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    textToSpeech = new TextToSpeech(this, this);
    toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);

    WebView webView = new WebView(this);
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setMediaPlaybackRequiresUserGesture(false);

    webView.setWebViewClient(new WebViewClient());
    webView.addJavascriptInterface(new CoachBridge(), "AndroidCoach");
    webView.loadUrl("file:///android_asset/www/index.html");
    setContentView(webView);
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
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
    if (toneGenerator != null) {
      toneGenerator.release();
    }
    super.onDestroy();
  }

  public class CoachBridge {
    @JavascriptInterface
    public void speak(final String text) {
      if (text == null || text.trim().isEmpty()) {
        return;
      }
      mainHandler.post(
          new Runnable() {
            @Override
            public void run() {
              if (speechReady && textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "corner-call");
              }
            }
          });
    }

    @JavascriptInterface
    public void bell() {
      mainHandler.post(
          new Runnable() {
            @Override
            public void run() {
              if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                mainHandler.postDelayed(
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
            }
          });
    }
  }
}
