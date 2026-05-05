package com.cornercall.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class HeartRateNotification {
  private HeartRateNotification() {}

  static Notification create(Context context, String channelId) {
    Intent open = new Intent(context, MainActivity.class);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            open,
            Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
    Notification.Builder builder =
        Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(context, channelId)
            : new Notification.Builder(context);
    return builder
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Corner Call")
        .setContentText("Recording heart rate")
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build();
  }

  static void createChannel(Context context, String channelId) {
    if (Build.VERSION.SDK_INT < 26) {
      return;
    }
    NotificationChannel channel =
        new NotificationChannel(channelId, "Heart rate", NotificationManager.IMPORTANCE_LOW);
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager != null) {
      manager.createNotificationChannel(channel);
    }
  }
}
