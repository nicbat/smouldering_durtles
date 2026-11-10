package com.smouldering_durtles.wk.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.PreferencesActivity;
import com.smouldering_durtles.wk.enums.SessionPriority;
import com.smouldering_durtles.wk.fragments.PreferencesFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionPrioritySettingsTest {
    @Test public void normalSessionSettingsExposeAndPersistPrioritiesWithoutAdvancedMode() {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String[] keys = {"enable_advanced", "lesson_order_priority", "review_order_priority",
                "lesson_level_up_first", "review_level_up_first"};
        Map<String, ?> original = prefs.getAll();
        prefs.edit().putBoolean("enable_advanced", false)
                .putBoolean("lesson_level_up_first", true).remove("review_level_up_first")
                .putString("lesson_order_priority", "NONE").putString("review_order_priority", "NONE").commit();
        Intent intent = new Intent(context, PreferencesActivity.class).putExtra("rootKey", "review_settings");
        try (ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = (PreferencesFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment);
                ListPreference lessons = fragment.findPreference("lesson_order_priority");
                ListPreference reviews = fragment.findPreference("review_order_priority");
                assertNotNull(lessons);
                assertNotNull(reviews);
                assertTrue(lessons.isVisible() && lessons.isEnabled());
                assertTrue(reviews.isVisible() && reviews.isEnabled());
                assertEquals("LEVEL_UP_FIRST", lessons.getValue());
                assertNotNull(fragment.findPreference("session_priority_help"));
                assertNull(fragment.findPreference("lesson_level_up_first"));
                assertNull(fragment.findPreference("review_level_up_first"));
                reviews.setValue("RADICALS_FIRST");
                lessons.setValue("NONE");
                assertEquals(SessionPriority.RADICALS_FIRST, GlobalSettings.AdvancedReview.getOrderPriority());
                assertEquals(SessionPriority.NONE, GlobalSettings.AdvancedLesson.getOrderPriority());
                assertFalse(prefs.getBoolean("enable_advanced", false));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = (PreferencesFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment);
                assertEquals("RADICALS_FIRST", ((ListPreference) fragment.findPreference("review_order_priority")).getValue());
                assertEquals("NONE", ((ListPreference) fragment.findPreference("lesson_order_priority")).getValue());
            });
        } finally {
            SharedPreferences.Editor edit = prefs.edit();
            for (String key : keys) {
                Object value = original.get(key);
                if (value == null) edit.remove(key);
                else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
                else edit.putString(key, (String) value);
            }
            edit.commit();
        }
    }
}
