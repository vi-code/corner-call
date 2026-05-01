package com.cornercall.app;

import android.content.Intent;
import android.os.Build;
import com.cornercall.app.shared.SessionState;
import com.cornercall.app.shared.WearPaths;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import org.json.JSONException;
import org.json.JSONObject;

public final class WatchWearListenerService extends WearableListenerService {
  @Override
  public void onMessageReceived(MessageEvent messageEvent) {
    if (!messageEvent.getPath().startsWith(WearPaths.CONTROL_PREFIX)) {
      return;
    }
    try {
      handleState(SessionState.fromBytes(messageEvent.getData()));
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
      if (!WearPaths.SESSION_STATE.equals(path)) {
        continue;
      }
      String payload = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("payload", "");
      try {
        handleState(SessionState.fromJson(new JSONObject(payload)));
      } catch (JSONException ignored) {
      }
    }
  }

  private void handleState(SessionState state) {
    if (!WearPaths.ORIGIN_PHONE.equals(state.origin)) {
      return;
    }
    Intent intent = new Intent(this, HeartRateService.class);
    intent.setAction(state.action);
    intent.putExtra(HeartRateService.EXTRA_FROM_PHONE, true);
    intent.putExtra(HeartRateService.EXTRA_SESSION_ID, state.sessionId);
    intent.putExtra(HeartRateService.EXTRA_STARTED_AT, state.startedAt);
    if (Build.VERSION.SDK_INT >= 26) {
      startForegroundService(intent);
    } else {
      startService(intent);
    }
  }
}
