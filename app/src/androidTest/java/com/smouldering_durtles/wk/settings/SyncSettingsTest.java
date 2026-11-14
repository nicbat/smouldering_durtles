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
    @Test public void statusTracksQueueAndNetworkChangesWithoutReopening() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        com.smouldering_durtles.wk.db.AppDatabase db = com.smouldering_durtles.wk.WkApplication.getDatabase();
        com.smouldering_durtles.wk.db.model.TaskDefinition upload = new com.smouldering_durtles.wk.db.model.TaskDefinition();
        upload.setId(2000000001);
        upload.setTaskClass(com.smouldering_durtles.wk.tasks.ReportSessionItemTask.class);
        upload.setData("0 1 1 REVIEW 0 0 false");
        Intent intent = new Intent(context, PreferencesActivity.class).putExtra("rootKey", "api_settings");
        try (ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(intent)) {
            // The test emulator stays physically offline. Only presentation events are simulated.
            com.smouldering_durtles.wk.livedata.LiveApiState.getInstance().post(com.smouldering_durtles.wk.api.ApiState.OK);
            com.smouldering_durtles.wk.livedata.LiveOnlineStatus.getInstance().post(com.smouldering_durtles.wk.enums.OnlineStatus.NO_CONNECTION);
            db.taskDefinitionDao().insertTaskDefinition(upload);
            awaitSummary(scenario, "sync_status", "Saved on this device");
            awaitSummary(scenario, "sync_queue", "1 saved updates");
            com.smouldering_durtles.wk.livedata.LiveOnlineStatus.getInstance().post(com.smouldering_durtles.wk.enums.OnlineStatus.UNMETERED);
            awaitSummary(scenario, "sync_status", "Uploading saved updates");
            db.taskDefinitionDao().deleteTaskDefinition(upload);
            awaitSummary(scenario, "sync_queue", "0 saved updates", "No pending sync");
            com.smouldering_durtles.wk.livedata.LiveOnlineStatus.getInstance().post(com.smouldering_durtles.wk.enums.OnlineStatus.NO_CONNECTION);
            awaitSummary(scenario, "sync_status", "Sync waiting for connection");
        } finally {
            db.taskDefinitionDao().deleteTaskDefinition(upload);
            com.smouldering_durtles.wk.livedata.LiveOnlineStatus.getInstance().forceUpdate();
            com.smouldering_durtles.wk.livedata.LiveApiState.getInstance().forceUpdate();
        }
    }

    private static void awaitSummary(ActivityScenario<PreferencesActivity> scenario, String key, String... expected) throws Exception {
        java.util.concurrent.atomic.AtomicReference<String> actual = new java.util.concurrent.atomic.AtomicReference<>("");
        long deadline = android.os.SystemClock.elapsedRealtime() + 5000;
        do {
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = (PreferencesFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag("preferencesFragment");
                actual.set(String.valueOf(fragment.findPreference(key).getSummary()));
            });
            for (String value : expected) if (actual.get().contains(value)) return;
            Thread.sleep(25);
        } while (android.os.SystemClock.elapsedRealtime() < deadline);
        fail("Unexpected " + key + ": " + actual.get());
    }

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
