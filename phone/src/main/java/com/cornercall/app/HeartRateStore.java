package com.cornercall.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.cornercall.app.shared.HeartRatePayload;
import com.cornercall.app.shared.HeartRateSample;
import com.cornercall.app.shared.HeartRateSummary;
import com.cornercall.app.shared.WearPaths;

public final class HeartRateStore extends SQLiteOpenHelper {
  private static final String DB_NAME = "corner_call_workouts.db";
  private static final int DB_VERSION = 2;

  public HeartRateStore(Context context) {
    super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE workout_sessions ("
            + "session_id TEXT PRIMARY KEY,"
            + "started_at INTEGER NOT NULL,"
            + "status TEXT NOT NULL,"
            + "min_bpm REAL NOT NULL DEFAULT 0,"
            + "avg_bpm REAL NOT NULL DEFAULT 0,"
            + "max_bpm REAL NOT NULL DEFAULT 0,"
            + "calories REAL NOT NULL DEFAULT 0,"
            + "sample_count INTEGER NOT NULL DEFAULT 0,"
            + "last_synced_at INTEGER NOT NULL DEFAULT 0)");
    db.execSQL(
        "CREATE TABLE heart_rate_samples ("
            + "session_id TEXT NOT NULL,"
            + "timestamp_ms INTEGER NOT NULL,"
            + "bpm REAL NOT NULL,"
            + "calories REAL NOT NULL DEFAULT 0,"
            + "PRIMARY KEY(session_id, timestamp_ms))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      db.execSQL("ALTER TABLE workout_sessions ADD COLUMN calories REAL NOT NULL DEFAULT 0");
      db.execSQL("ALTER TABLE heart_rate_samples ADD COLUMN calories REAL NOT NULL DEFAULT 0");
    }
  }

  public synchronized void ensureSession(String sessionId, long startedAt, String status) {
    if (sessionId == null || sessionId.isEmpty()) {
      return;
    }
    ContentValues values = new ContentValues();
    values.put("session_id", sessionId);
    values.put("started_at", startedAt > 0 ? startedAt : System.currentTimeMillis());
    values.put("status", status == null ? WearPaths.STATUS_READY : status);
    getWritableDatabase()
        .insertWithOnConflict("workout_sessions", null, values, SQLiteDatabase.CONFLICT_IGNORE);
  }

  public synchronized HeartRateSummary savePayload(HeartRatePayload payload) {
    long syncedAt = System.currentTimeMillis();
    ensureSession(payload.sessionId, syncedAt, payload.isFinal ? WearPaths.STATUS_COMPLETE : WearPaths.STATUS_PAUSED);
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (HeartRateSample sample : payload.samples) {
        ContentValues values = new ContentValues();
        values.put("session_id", payload.sessionId);
        values.put("timestamp_ms", sample.timestampMs);
        values.put("bpm", sample.bpm);
        values.put("calories", sample.calories);
        db.insertWithOnConflict(
            "heart_rate_samples", null, values, SQLiteDatabase.CONFLICT_IGNORE);
      }
      HeartRateSummary summary = calculateSummary(db, payload.sessionId, syncedAt);
      ContentValues session = new ContentValues();
      session.put("status", payload.isFinal ? WearPaths.STATUS_COMPLETE : WearPaths.STATUS_PAUSED);
      session.put("min_bpm", summary.minBpm);
      session.put("avg_bpm", summary.avgBpm);
      session.put("max_bpm", summary.maxBpm);
      session.put("calories", summary.calories);
      session.put("sample_count", summary.sampleCount);
      session.put("last_synced_at", syncedAt);
      db.update("workout_sessions", session, "session_id = ?", new String[] {payload.sessionId});
      db.setTransactionSuccessful();
      return summary;
    } finally {
      db.endTransaction();
    }
  }

  public synchronized HeartRateSummary latestSummary() {
    SQLiteDatabase db = getReadableDatabase();
    Cursor cursor =
        db.query(
            "workout_sessions",
            new String[] {"session_id", "sample_count", "min_bpm", "avg_bpm", "max_bpm", "calories", "last_synced_at"},
            null,
            null,
            null,
            null,
            "last_synced_at DESC",
            "1");
    try {
      if (cursor.moveToFirst()) {
        return new HeartRateSummary(
            cursor.getString(0),
            cursor.getInt(1),
            cursor.getFloat(2),
            cursor.getFloat(3),
            cursor.getFloat(4),
            cursor.getFloat(5),
            cursor.getLong(6));
      }
    } finally {
      cursor.close();
    }
    return new HeartRateSummary("", 0, 0, 0, 0, 0);
  }

  private HeartRateSummary calculateSummary(SQLiteDatabase db, String sessionId, long syncedAt) {
    Cursor cursor =
        db.rawQuery(
            "SELECT COUNT(*), MIN(bpm), AVG(bpm), MAX(bpm), MAX(calories) FROM heart_rate_samples WHERE session_id = ?",
            new String[] {sessionId});
    try {
      if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
        return new HeartRateSummary(
            sessionId,
            cursor.getInt(0),
            cursor.getFloat(1),
            cursor.getFloat(2),
            cursor.getFloat(3),
            cursor.getFloat(4),
            syncedAt);
      }
    } finally {
      cursor.close();
    }
    return new HeartRateSummary(sessionId, 0, 0, 0, 0, syncedAt);
  }
}
