package com.limelight.legion;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.limelight.AppView;
import com.limelight.Game;
import com.limelight.PcView;
import com.limelight.ShortcutTrampoline;

/**
 * Artemis Legion: one-tap "Play PC". Opened by Legion Switch Dock on the TV / glasses (or from the
 * tablet), it jumps straight back into the last game or desktop stream on that screen. Waking the PC
 * and resuming a running game are handled by Artemis' ShortcutTrampoline.
 */
public class LegionPlayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences last = LegionSession.lastStream(this);
        String pcUuid = last.getString("pc_uuid", null);
        int appId = last.getInt("app_id", 0);

        Intent next;
        if (pcUuid != null && appId != 0) {
            next = new Intent(this, ShortcutTrampoline.class);
            next.setAction(Intent.ACTION_DEFAULT);
            next.putExtra(AppView.UUID_EXTRA, pcUuid);
            next.putExtra(AppView.NAME_EXTRA, last.getString("pc_name", null));
            next.putExtra(Game.EXTRA_APP_NAME, last.getString("app_name", null));
            next.putExtra(Game.EXTRA_APP_UUID, last.getString("app_uuid", null));
            next.putExtra(Game.EXTRA_APP_ID, String.valueOf(appId));
        } else {
            // Nothing streamed yet: open the normal PC list
            next = new Intent(this, PcView.class);
        }
        startActivity(next);
        finish();
    }
}
