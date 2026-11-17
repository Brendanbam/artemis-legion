package com.limelight.legion;

import android.app.Activity;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Artemis Legion: works out which screen the stream is on (tablet, TV or XR glasses)
 * so the right settings profile, resolution, refresh rate and bitrate are used automatically.
 */
public final class LegionScreen {

    public enum Type {
        TABLET("Tablet"),
        TV("TV"),
        GLASSES("Glasses");

        /** Name of the Artemis settings profile that is used automatically on this screen, if it exists. */
        public final String profileName;

        Type(String profileName) {
            this.profileName = profileName;
        }
    }

    // Same names Legion Switch Dock uses to recognise XR glasses (XREAL xbx a01, Air/One, VITURE, Rokid, RayNeo...)
    private static final Pattern GLASSES_NAME = Pattern.compile(
            "xreal|nreal|xbx|\\ba01\\b|viture|rokid|rayneo|tcl ?nxt|inmo|\\bair ?[0-9]?\\b|glasses|\\bar\\b|\\bxr\\b",
            Pattern.CASE_INSENSITIVE);

    private LegionScreen() {}

    /**
     * The display the stream should target: an explicit display ID from the launch intent if given,
     * otherwise the display this activity is actually running on (e.g. launched onto the TV by
     * Legion Switch Dock), otherwise the built-in screen.
     */
    public static Display resolveDisplay(Activity activity, Intent intent, String displayIdExtra) {
        DisplayManager dm = activity.getSystemService(DisplayManager.class);
        int requestedId = intent != null ? intent.getIntExtra(displayIdExtra, -1) : -1;
        if (requestedId != -1 && dm != null) {
            Display d = dm.getDisplay(requestedId);
            if (d != null) return d;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Display d = activity.getDisplay();
                if (d != null) return d;
            } catch (UnsupportedOperationException ignored) {
            }
        }
        return activity.getWindowManager().getDefaultDisplay();
    }

    public static Type classify(Display display) {
        if (display == null || display.getDisplayId() == Display.DEFAULT_DISPLAY) {
            return Type.TABLET;
        }
        String name = display.getName() == null ? "" : display.getName();
        return GLASSES_NAME.matcher(name).find() ? Type.GLASSES : Type.TV;
    }

    /**
     * Bitrate (kbps) for the resolution/frame rate actually being streamed, scaled up for picture quality
     * on a home network. qualityPercent: 100 = Moonlight default, 150 = high, 200 = max.
     */
    public static int autoBitrateKbps(int width, int height, float fps, int qualityPercent) {
        String res = width + "x" + height;
        String fpsStr = String.format(Locale.US, "%d", Math.round(fps));
        int base = com.limelight.preferences.PreferenceConfiguration.getDefaultBitrate(res, fpsStr);
        long scaled = (long) base * qualityPercent / 100L;
        // Keep within what Wi-Fi 6/7 and the decoder handle comfortably
        return (int) Math.max(5_000L, Math.min(scaled, 150_000L));
    }
}
