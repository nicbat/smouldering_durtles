package com.smouldering_durtles.wk.settings;

import android.content.Context;
import android.content.Intent;
import androidx.preference.Preference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.smouldering_durtles.wk.activities.PreferencesActivity;
import com.smouldering_durtles.wk.fragments.PreferencesFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises the shared hub without sending API requests or downloading account data. */
@RunWith(AndroidJUnit4.class)
public class SyncSettingsTest {
    @Test public void syncAndDownloadsShareOneSettingsScreen() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, PreferencesActivity.class).putExtra("rootKey", "api_settings");
        try (ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = (PreferencesFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag("preferencesFragment");
                assertNotNull(fragment);
                for (String key : new String[]{"sync_status", "sync_queue", "sync_now", "download_audio",
                        "api_key", "network_rule", "audio_location", "auto_download_audio",
                        "enable_background_sync", "sync_on_open"}) {
                    assertNotNull(key, fragment.findPreference(key));
                }
                Preference download = fragment.findPreference("download_audio");
                assertNotNull(download.getOnPreferenceClickListener());
                Preference sync = fragment.findPreference("sync_now");
                assertNotNull(sync.getOnPreferenceClickListener());
                assertFalse(fragment.findPreference("sync_status").isSelectable());
                assertFalse(fragment.findPreference("sync_queue").isSelectable());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = (PreferencesFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag("preferencesFragment");
                assertNotNull(fragment.findPreference("sync_status").getSummary());
                assertNotNull(fragment.findPreference("sync_queue").getSummary());
            });
            android.graphics.Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(
                    new java.io.File(context.getFilesDir(), "sync-audio.png"))) {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
            }
            screenshot.recycle();
        }
    }
}
