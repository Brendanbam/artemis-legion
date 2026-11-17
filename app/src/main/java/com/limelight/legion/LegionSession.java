package com.limelight.legion;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;

import com.limelight.Game;
import com.limelight.LimeLog;

/**
 * Artemis Legion: remembers the last stream (for the one-tap "Play PC" tile) and restarts the
 * stream on another screen when the TV / glasses are plugged in or unplugged mid-game.
 */
public final class LegionSession {
    private static final String PREFS = "legion_session";
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private LegionSession() {}

    public static void saveLastStream(Context context, Intent gameIntent) {
        String pcUuid = gameIntent.getStringExtra(Game.EXTRA_PC_UUID);
        int appId = gameIntent.getIntExtra(Game.EXTRA_APP_ID, 0);
        if (pcUuid == null || appId == 0) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("pc_uuid", pcUuid)
                .putString("pc_name", gameIntent.getStringExtra(Game.EXTRA_PC_NAME))
                .putString("app_name", gameIntent.getStringExtra(Game.EXTRA_APP_NAME))
                .putString("app_uuid", gameIntent.getStringExtra(Game.EXTRA_APP_UUID))
                .putInt("app_id", appId)
                .apply();
    }

    public static SharedPreferences lastStream(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** A real plugged-in screen (TV / glasses), not a screen recorder or other private virtual display. */
    public static boolean isExternalScreen(Display d) {
        if (d == null || d.getDisplayId() == Display.DEFAULT_DISPLAY) return false;
        if (d.getState() == Display.STATE_OFF) return false;
        int flags = d.getFlags();
        return (flags & Display.FLAG_PRIVATE) == 0 && (flags & Display.FLAG_PRESENTATION) != 0;
    }

    public static Display findExternalScreen(Context context) {
        DisplayManager dm = context.getSystemService(DisplayManager.class);
        if (dm == null) return null;
        for (Display d : dm.getDisplays()) {
            if (isExternalScreen(d)) return d;
        }
        return null;
    }

    /**
     * Restarts the same stream on targetDisplayId once the current stream activity has closed.
     * The game keeps running on the PC; Apollo reconnects at the new screen's resolution/refresh.
     */
    public static void relaunchOn(Context context, Intent originalGameIntent, int targetDisplayId) {
        final Context app = context.getApplicationContext();
        final Intent next = new Intent(originalGameIntent);
        next.setClass(app, Game.class);
        next.putExtra(Game.EXTRA_DISPLAY_ID, targetDisplayId);
        next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        LimeLog.info("Artemis Legion: switching stream to display " + targetDisplayId);
        attemptRelaunch(app, next, targetDisplayId, 0);
    }

    private static void attemptRelaunch(Context app, Intent next, int targetDisplayId, int attempt) {
        handler.postDelayed(() -> {
            // Wait for the old stream to fully close (up to ~4s) before starting the new one
            if (Game.instance != null && attempt < 8) {
                attemptRelaunch(app, next, targetDisplayId, attempt + 1);
                return;
            }
            try {
                ActivityOptions opts = ActivityOptions.makeBasic();
                opts.setLaunchDisplayId(targetDisplayId);
                app.startActivity(next, opts.toBundle());
            } catch (Exception e) {
                LimeLog.warning("Artemis Legion: relaunch failed: " + e);
            }
        }, attempt == 0 ? 1200 : 500);
    }
}
