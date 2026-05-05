package com.cornercall.app;

import android.media.ToneGenerator;
import android.os.Handler;
import android.speech.tts.TextToSpeech;

final class PhoneAudioHelper {
  private PhoneAudioHelper() {}

  static void speakComboOrStatus(
      TextToSpeech textToSpeech, boolean voiceEnabled, boolean speechReady, String text) {
    if (!voiceEnabled || !speechReady || textToSpeech == null) {
      return;
    }
    textToSpeech.stop();
    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "corner-call");
  }

  static void playRoundBell(final ToneGenerator toneGenerator, Handler handler) {
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

  static void playTenSecondClapper(final ToneGenerator toneGenerator, Handler handler) {
    if (toneGenerator == null) {
      return;
    }
    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 70);
    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            if (toneGenerator != null) {
              toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 70);
            }
          }
        },
        110);
    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            if (toneGenerator != null) {
              toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 90);
            }
          }
        },
        230);
  }
}
