package com.cornercall.app;

import android.content.Intent;
import com.cornercall.app.shared.HeartRatePayload;
import com.cornercall.app.shared.HeartRateSummary;
import com.cornercall.app.shared.SessionState;
import com.cornercall.app.shared.WearPaths;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import org.json.JSONException;

public final class PhoneWearListenerService extends WearableListenerService {
  public static final String ACTION_WEAR_CONTROL = "com.cornercall.app.WEAR_CONTROL";
  public static final String ACTION_HR_SUMMARY = "com.cornercall.app.HR_SUMMARY";
  public static final String EXTRA_PAYLOAD = "payload";
  public static final String EXTRA_SAMPLE_COUNT = "sampleCount";
  public static final String EXTRA_MIN_BPM = "minBpm";
  public static final String EXTRA_AVG_BPM = "avgBpm";
  public static final String EXTRA_MAX_BPM = "maxBpm";
  public static final String EXTRA_CALORIES = "calories";
  public static final String EXTRA_SYNCED_AT = "syncedAt";

  @Override
  public void onMessageReceived(MessageEvent messageEvent) {
    if (!messageEvent.getPath().startsWith(WearPaths.CONTROL_PREFIX)) {
      return;
    }
    try {
      SessionState state = SessionState.fromBytes(messageEvent.getData());
      Intent intent = new Intent(ACTION_WEAR_CONTROL);
      intent.setPackage(getPackageName());
      intent.putExtra(EXTRA_PAYLOAD, state.toJson().toString());
      sendBroadcast(intent);
    } catch (JSONException ignored) {
    }
  }

  @Override
  public void onDataChanged(DataEventBuffer dataEvents) {
    for (DataEvent event : dataEvents) {
      if (event.getType() != DataEvent.TYPE_CHANGED) {
        continue;
      }
      String path = event.getDataItem().getUri().getPath();
      if (path == null || !path.startsWith(WearPaths.HEART_RATE_PREFIX)) {
        continue;
      }
      DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
      HeartRatePayload payload = HeartRatePayload.fromDataMap(map);
      HeartRateSummary summary = new HeartRateStore(this).savePayload(payload);
      broadcastSummary(summary);
    }
  }

  private void broadcastSummary(HeartRateSummary summary) {
    Intent intent = new Intent(ACTION_HR_SUMMARY);
    intent.setPackage(getPackageName());
    intent.putExtra(EXTRA_SAMPLE_COUNT, summary.sampleCount);
    intent.putExtra(EXTRA_MIN_BPM, summary.minBpm);
    intent.putExtra(EXTRA_AVG_BPM, summary.avgBpm);
    intent.putExtra(EXTRA_MAX_BPM, summary.maxBpm);
    intent.putExtra(EXTRA_CALORIES, summary.calories);
    intent.putExtra(EXTRA_SYNCED_AT, summary.lastSyncedAt);
    sendBroadcast(intent);
  }
}
