package com.cornercall.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import androidx.health.services.client.ExerciseClient;
import androidx.health.services.client.ExerciseUpdateCallback;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.data.Availability;
import androidx.health.services.client.data.CumulativeDataPoint;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.ExerciseConfig;
import androidx.health.services.client.data.ExerciseEvent;
import androidx.health.services.client.data.ExerciseLapSummary;
import androidx.health.services.client.data.ExerciseType;
import androidx.health.services.client.data.ExerciseUpdate;
import androidx.health.services.client.data.SampleDataPoint;
import com.cornercall.app.shared.HeartRatePayload;
import com.cornercall.app.shared.HeartRateSample;
import com.cornercall.app.shared.SessionState;
import com.cornercall.app.shared.WearPaths;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HeartRateService extends Service {
  public static final String ACTION_STATE = "com.cornercall.app.WATCH_STATE";
  public static final String EXTRA_ACTIVE = "active";
  public static final String EXTRA_PAUSED = "paused";
  public static final String EXTRA_BPM = "bpm";
  public static final String EXTRA_CALORIES = "calories";
  public static final String EXTRA_SYNC = "sync";
  public static final String EXTRA_FROM_PHONE = "fromPhone";
  public static final String EXTRA_SESSION_ID = "sessionId";
  public static final String EXTRA_STARTED_AT = "startedAt";

  private static final String CHANNEL_ID = "corner_call_hr";
  private static final int NOTIFICATION_ID = 42;

  private final List<HeartRateSample> pendingSamples = new ArrayList<>();
  private ExerciseClient exerciseClient;
  private boolean active;
  private boolean paused;
  private int latestBpm;
  private float latestCalories;
  private int chunkSeed;
  private String sessionId = "";
  private long sessionStartedAt;
  private String syncStatus = "Waiting for phone";

  private final ExerciseUpdateCallback callback =
      new ExerciseUpdateCallback() {
        @Override
        public void onRegistered() {}

        @Override
        public void onRegistrationFailed(Throwable throwable) {
          syncStatus = "Health Services unavailable";
          broadcastState();
        }

        @Override
        public void onExerciseUpdateReceived(ExerciseUpdate update) {
          CumulativeDataPoint<Double> caloriePoint =
              update.getLatestMetrics().getData(DataType.CALORIES_TOTAL);
          if (caloriePoint != null) {
            latestCalories = caloriePoint.getTotal().floatValue();
          }
          List<SampleDataPoint<Double>> points =
              update.getLatestMetrics().getData(DataType.HEART_RATE_BPM);
          Instant bootInstant =
              Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());
          for (SampleDataPoint<Double> point : points) {
            float bpm = point.getValue().floatValue();
            latestBpm = Math.round(bpm);
            pendingSamples.add(
                new HeartRateSample(
                    point.getTimeInstant(bootInstant).toEpochMilli(), bpm, latestCalories));
          }
          broadcastState();
        }

        @Override
        public void onAvailabilityChanged(
            DataType<?, ?> dataType, Availability availability) {}

        @Override
        public void onExerciseEventReceived(ExerciseEvent event) {}

        @Override
        public void onLapSummaryReceived(ExerciseLapSummary lapSummary) {}
      };

  @Override
  public void onCreate() {
    super.onCreate();
    createNotificationChannel();
    exerciseClient = HealthServices.getClient(this).getExerciseClient();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    ensureForeground();
    if (intent == null || intent.getAction() == null) {
      return START_STICKY;
    }
    if (intent.hasExtra(EXTRA_SESSION_ID)) {
      sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
      sessionStartedAt = intent.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis());
    }
    boolean fromPhone = intent.getBooleanExtra(EXTRA_FROM_PHONE, false);
    String action = intent.getAction();
    if (WearPaths.ACTION_START.equals(action)) {
      startRecording(fromPhone);
    } else if (WearPaths.ACTION_PAUSE.equals(action)) {
      pauseRecording(fromPhone);
    } else if (WearPaths.ACTION_RESUME.equals(action)) {
      resumeRecording(fromPhone);
    } else if (WearPaths.ACTION_END.equals(action)) {
      endRecording(fromPhone);
    }
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void startRecording(boolean fromPhone) {
    ensureSession();
    active = true;
    paused = false;
    syncStatus = "Recording";
    try {
      exerciseClient.setUpdateCallback(callback);
      Set<DataType<?, ?>> dataTypes = new HashSet<>();
      dataTypes.add(DataType.HEART_RATE_BPM);
      dataTypes.add(DataType.CALORIES_TOTAL);
      ExerciseConfig config =
          ExerciseConfig.builder(ExerciseType.BOXING)
              .setDataTypes(dataTypes)
              .setIsGpsEnabled(false)
              .setIsAutoPauseAndResumeEnabled(false)
              .build();
      exerciseClient.startExerciseAsync(config);
    } catch (SecurityException exception) {
      syncStatus = "Heart-rate permission needed";
    }
    if (!fromPhone) {
      sendControlToPhone(WearPaths.ACTION_START);
    }
    broadcastState();
  }

  private void pauseRecording(boolean fromPhone) {
    if (!active && !paused) {
      return;
    }
    active = false;
    paused = true;
    syncStatus = "Syncing pause";
    try {
      exerciseClient.pauseExerciseAsync();
    } catch (RuntimeException ignored) {
    }
    syncSamples("pause", false);
    if (!fromPhone) {
      sendControlToPhone(WearPaths.ACTION_PAUSE);
    }
    broadcastState();
  }

  private void resumeRecording(boolean fromPhone) {
    ensureSession();
    active = true;
    paused = false;
    syncStatus = "Recording";
    try {
      exerciseClient.resumeExerciseAsync();
    } catch (RuntimeException ignored) {
      startRecording(true);
    }
    if (!fromPhone) {
      sendControlToPhone(WearPaths.ACTION_RESUME);
    }
    broadcastState();
  }

  private void endRecording(boolean fromPhone) {
    if (!active && !paused) {
      return;
    }
    active = false;
    paused = false;
    syncStatus = "Syncing complete";
    try {
      exerciseClient.endExerciseAsync();
      exerciseClient.clearUpdateCallbackAsync(callback);
    } catch (RuntimeException ignored) {
    }
    syncSamples("complete", true);
    if (!fromPhone) {
      sendControlToPhone(WearPaths.ACTION_END);
    }
    broadcastState();
    stopForeground(true);
    stopSelf();
  }

  private void syncSamples(String eventType, boolean finalSync) {
    ensureSession();
    List<HeartRateSample> snapshot = new ArrayList<>(pendingSamples);
    pendingSamples.clear();
    List<HeartRatePayload> chunks = HeartRatePayload.chunks(sessionId, eventType, finalSync, snapshot);
    long syncedAt = System.currentTimeMillis();
    for (HeartRatePayload payload : chunks) {
      PutDataMapRequest request =
          PutDataMapRequest.create(
              WearPaths.HEART_RATE_PREFIX + sessionId + "/" + chunkSeed + "-" + payload.chunkIndex);
      DataMap map = request.getDataMap();
      writePayload(map, payload);
      map.putLong("syncedAt", syncedAt);
      PutDataRequest dataRequest = request.asPutDataRequest();
      dataRequest.setUrgent();
      Wearable.getDataClient(this).putDataItem(dataRequest);
    }
    chunkSeed += 1;
    syncStatus = snapshot.isEmpty() ? "No new samples" : "Synced " + snapshot.size() + " samples";
  }

  private void writePayload(DataMap map, HeartRatePayload payload) {
    long[] timestamps = new long[payload.samples.size()];
    float[] bpms = new float[payload.samples.size()];
    float[] calories = new float[payload.samples.size()];
    for (int i = 0; i < payload.samples.size(); i += 1) {
      HeartRateSample sample = payload.samples.get(i);
      timestamps[i] = sample.timestampMs;
      bpms[i] = sample.bpm;
      calories[i] = sample.calories;
    }
    map.putString("sessionId", payload.sessionId);
    map.putString("eventType", payload.eventType);
    map.putInt("chunkIndex", payload.chunkIndex);
    map.putBoolean("isFinal", payload.isFinal);
    map.putLongArray("timestamps", timestamps);
    map.putFloatArray("bpms", bpms);
    map.putFloatArray("calories", calories);
    map.putLong("createdAt", System.currentTimeMillis());
  }

  private void sendControlToPhone(String action) {
    final SessionState state =
        new SessionState(
            sessionId,
            WearPaths.ORIGIN_WATCH,
            action,
            statusForAction(action),
            sessionStartedAt,
            true,
            1,
            0);
    mirrorSessionState(state);
    Wearable.getNodeClient(this)
        .getConnectedNodes()
        .addOnSuccessListener(
            new com.google.android.gms.tasks.OnSuccessListener<List<Node>>() {
              @Override
              public void onSuccess(List<Node> nodes) {
                for (Node node : nodes) {
                  Wearable.getMessageClient(HeartRateService.this)
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

  private void ensureSession() {
    if (!sessionId.isEmpty()) {
      return;
    }
    sessionStartedAt = System.currentTimeMillis();
    sessionId = "corner-" + sessionStartedAt;
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

  private void broadcastState() {
    Intent intent = new Intent(ACTION_STATE);
    intent.setPackage(getPackageName());
    intent.putExtra(EXTRA_ACTIVE, active);
    intent.putExtra(EXTRA_PAUSED, paused);
    intent.putExtra(EXTRA_BPM, latestBpm);
    intent.putExtra(EXTRA_CALORIES, latestCalories);
    intent.putExtra(EXTRA_SYNC, syncStatus);
    sendBroadcast(intent);
  }

  private void ensureForeground() {
    startForeground(NOTIFICATION_ID, notification());
  }

  private Notification notification() {
    Intent open = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            open,
            Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
    Notification.Builder builder =
        Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
    return builder
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Corner Call")
        .setContentText("Recording heart rate")
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT < 26) {
      return;
    }
    NotificationChannel channel =
        new NotificationChannel(CHANNEL_ID, "Heart rate", NotificationManager.IMPORTANCE_LOW);
    NotificationManager manager = getSystemService(NotificationManager.class);
    if (manager != null) {
      manager.createNotificationChannel(channel);
    }
  }
}
