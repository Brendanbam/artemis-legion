package com.limelight.legion;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Display;

import androidx.preference.PreferenceManager;

/**
 * Artemis Legion: one-time "best settings" for a Legion Y700 streaming from an Apollo host
 * over home Wi-Fi to the tablet, a TV dock or XR glasses.
 */
public final class LegionDefaults {
    private static final String APPLIED_KEY = "legion_defaults_v1";

    private LegionDefaults() {}

    public static void applyOnce(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(APPLIED_KEY, false)) {
            return;
        }

        SharedPreferences.Editor e = prefs.edit();

        // Picture: AV1 at a fixed 100 Mbps (auto bitrate off so the fixed value is used everywhere)
        e.putString("video_format", "forceav1");
        e.putInt("seekbar_bitrate_kbps", 100_000);
        e.putBoolean("checkbox_legion_auto_bitrate", false);

        // Latency: lowest-latency frame pacing + Snapdragon ultra low latency decoder mode
        e.putString("frame_pacing", "latency");
        e.putBoolean("checkbox_ultra_low_latency", true);
        e.putBoolean("checkbox_unlock_fps", true);

        // Apollo builds a virtual display that matches each screen's resolution / refresh rate
        e.putBoolean("checkbox_use_virtual_display", true);

        // Tablet screen: native 16:10 look, capped at 2560 wide and 120 fps to keep decode + Wi-Fi comfortable
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (display != null) {
            Display.Mode mode = display.getMode();
            int w = Math.max(mode.getPhysicalWidth(), mode.getPhysicalHeight());
            int h = Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight());
            if (w > 2560) {
                h = Math.round(h * 2560f / w / 8f) * 8;
                w = 2560;
            }
            float maxHz = 60f;
            for (Display.Mode m : display.getSupportedModes()) {
                maxHz = Math.max(maxHz, m.getRefreshRate());
            }
            int fps = Math.min(120, Math.round(maxHz));
            e.putString("list_resolution", w + "x" + h);
            e.putString("list_fps", String.valueOf(fps));
        }

        e.putBoolean(APPLIED_KEY, true);
        e.apply();
    }
}
