package com.smouldering_durtles.wk.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.PreferencesActivity;
import com.smouldering_durtles.wk.enums.LessonOrder;
import com.smouldering_durtles.wk.enums.SessionPriority;
import com.smouldering_durtles.wk.enums.SpecialButtonBehavior;
import com.smouldering_durtles.wk.fragments.PreferencesFragment;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UnifiedSettingsTest {
    private SharedPreferences prefs;
    private Map<String, ?> original;
    @Before public void before() {
        prefs = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().getTargetContext());
        original = prefs.getAll();
    }
    @After public void restore() {
        Set<String> keys = new HashSet<>(prefs.getAll().keySet());
        keys.addAll(original.keySet());
        SharedPreferences.Editor edit = prefs.edit();
        for (String key : keys) {
            if (!(key.startsWith("lesson_") || key.startsWith("review_") || key.startsWith("self_study_")
                    || key.startsWith("anki_") || key.startsWith("font_size_") || key.startsWith("max_font_size_")
                    || key.equals("enable_advanced") || key.equals("unified_study_settings_v1")
                    || key.equals("quiz_question_view_height") || key.equals("kanji_mode_onkun")
                    || key.equals("overdue_threshold") || key.equals("undo_button") || key.equals("ignore_button")
                    || key.equals("special_button_3") || key.equals("dont_know_button")
                    || key.equals("shake_on_matching_kanji") || key.equals("full_session_log")
                    || key.equals("indicate_kanji_reading_type"))) continue;
            Object value = original.get(key);
            if (value == null) edit.remove(key);
            else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) edit.putInt(key, (Integer) value);
            else if (value instanceof String) edit.putString(key, (String) value);
        }
        edit.commit();
    }

    @Test public void disabledAdvancedMigrationPreservesEffectiveBehaviorAndRunsOnlyOnce() {
        prefs.edit().remove("unified_study_settings_v1").putBoolean("enable_advanced", false)
                .putString("lesson_order", "SHUFFLE").putBoolean("lesson_reading_first", true)
                .putBoolean("anki_mode_review_meaning", true).putInt("review_subjects_current_min", 15)
                .putString("undo_button", "UNDO_AND_RETRY_NOW").putBoolean("shake_on_matching_kanji", true)
                .putString("lesson_order_priority", "LEVEL_UP_FIRST").commit();
        GlobalSettings.migrateUnifiedStudySettings();
        assertEquals(LessonOrder.LEVEL_THEN_TYPE, GlobalSettings.AdvancedLesson.getOrder());
        assertFalse(GlobalSettings.AdvancedLesson.getReadingFirst());
        assertFalse(GlobalSettings.getAnkiMode(com.smouldering_durtles.wk.enums.SessionType.REVIEW,
                com.smouldering_durtles.wk.enums.QuestionType.WANIKANI_KANJI_MEANING));
        assertEquals(-1, prefs.getInt("review_subjects_current_min", 0));
        assertEquals(SpecialButtonBehavior.DISABLE, GlobalSettings.AdvancedOther.getSpecialButton1Behavior());
        assertFalse(GlobalSettings.AdvancedOther.getShakeOnMatchingKanji());
        assertEquals(SessionPriority.LEVEL_UP_FIRST, GlobalSettings.AdvancedLesson.getOrderPriority());
        assertFalse(prefs.contains("enable_advanced"));
        GlobalSettings.setQuestionOrder("lesson", "READING_FIRST");
        GlobalSettings.AdvancedReview.setAnkiModeMeaning(true);
        GlobalSettings.migrateUnifiedStudySettings();
        assertTrue(GlobalSettings.AdvancedLesson.getReadingFirst());
        assertTrue(GlobalSettings.getAnkiMode(com.smouldering_durtles.wk.enums.SessionType.REVIEW,
                com.smouldering_durtles.wk.enums.QuestionType.WANIKANI_KANJI_MEANING));
    }

    @Test public void enabledAdvancedMigrationPreservesStoredChoicesAndBooleanFormats() {
        prefs.edit().remove("unified_study_settings_v1").putBoolean("enable_advanced", true)
                .putBoolean("lesson_reading_first", true).putBoolean("lesson_meaning_first", true)
                .putBoolean("anki_mode_review_meaning", true).putInt("review_subjects_current_min", 15)
                .putString("undo_button", "UNDO_AND_RETRY_NOW").commit();
        GlobalSettings.migrateUnifiedStudySettings();
        assertEquals("COMBINED", GlobalSettings.getQuestionOrder("lesson"));
        assertTrue(GlobalSettings.getAnkiMode(com.smouldering_durtles.wk.enums.SessionType.REVIEW,
                com.smouldering_durtles.wk.enums.QuestionType.WANIKANI_KANJI_MEANING));
        assertEquals(15, prefs.getInt("review_subjects_current_min", 0));
        assertEquals(SpecialButtonBehavior.UNDO_AND_RETRY_NOW, GlobalSettings.AdvancedOther.getSpecialButton1Behavior());
        GlobalSettings.setQuestionOrder("lesson", "MEANING_FIRST");
        assertEquals(Boolean.FALSE, prefs.getAll().get("lesson_reading_first"));
        assertEquals(Boolean.TRUE, prefs.getAll().get("lesson_meaning_first"));
        assertFalse(prefs.contains("lesson_question_order"));
    }

    @Test public void importingAnOlderBackupHonorsItsDisabledGateAgain() {
        prefs.edit().putBoolean("unified_study_settings_v1", true)
                .putBoolean("anki_mode_review_meaning", true).commit();
        java.util.Map<String, Object> backup = new java.util.HashMap<>();
        backup.put("enable_advanced", false);
        backup.put("anki_mode_review_meaning", true);
        backup.put("review_reading_first", true);
        GlobalSettings.setAllSettings(backup);
        assertFalse(GlobalSettings.getAnkiMode(com.smouldering_durtles.wk.enums.SessionType.REVIEW,
                com.smouldering_durtles.wk.enums.QuestionType.WANIKANI_KANJI_MEANING));
        assertFalse(GlobalSettings.AdvancedReview.getReadingFirst());
        assertTrue(prefs.getBoolean("unified_study_settings_v1", false));
        assertFalse(prefs.contains("enable_advanced"));
    }

    @Test public void studyAndAnkiSectionsExposeSettingsWithoutAnAdvancedToggle() {
        try (ActivityScenario<PreferencesActivity> scenario = launch(null)) {
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = fragment(activity);
                assertNull(fragment.findPreference("enable_advanced"));
                assertNull(fragment.findPreference("advanced"));
                assertNotNull(fragment.findPreference("lesson_session_settings"));
                assertNotNull(fragment.findPreference("review_session_settings"));
                assertNotNull(fragment.findPreference("self_study_session_settings"));
                PreferenceGroup anki = fragment.findPreference("anki_settings");
                for (String key : new String[]{"anki_mode_lesson_meaning", "anki_mode_lesson_reading",
                        "anki_mode_review_meaning", "anki_mode_review_reading", "anki_mode_self_study_meaning",
                        "anki_mode_self_study_reading", "anki_buttons_height", "swap_anki_buttons",
                        "autoplay_anki_reveal", "font_size_anki_answer"}) assertNotNull(anki.findPreference(key));
            });
        }
        try (ActivityScenario<PreferencesActivity> scenario = launch("lesson_session_settings")) {
            scenario.onActivity(activity -> {
                ListPreference order = fragment(activity).findPreference("lesson_question_order");
                assertNotNull(order);
                assertFalse(order.isPersistent());
                assertTrue(order.callChangeListener("MEANING_FIRST"));
                order.setValue("MEANING_FIRST");
                assertFalse(GlobalSettings.AdvancedLesson.getReadingFirst());
                assertTrue(GlobalSettings.AdvancedLesson.getMeaningFirst());
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals("MEANING_FIRST",
                    ((ListPreference) fragment(activity).findPreference("lesson_question_order")).getValue()));
        }
    }

    @Test public void heightDropdownPreservesCustomIntegerAndStoresPresetsAsIntegers() {
        prefs.edit().putInt("quiz_question_view_height", 7).commit();
        try (ActivityScenario<PreferencesActivity> scenario = launch("font_settings")) {
            scenario.onActivity(activity -> {
                ListPreference height = fragment(activity).findPreference("quiz_question_height_preset");
                assertEquals("7", height.getValue());
                assertTrue(height.getEntry().toString().contains("safe minimum"));
                assertEquals(Integer.valueOf(7), prefs.getAll().get("quiz_question_view_height"));
                assertTrue(height.callChangeListener("160"));
                height.setValue("160");
                assertEquals(Integer.valueOf(160), prefs.getAll().get("quiz_question_view_height"));
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals("160",
                    ((ListPreference) fragment(activity).findPreference("quiz_question_height_preset")).getValue()));
        }
    }

    private ActivityScenario<PreferencesActivity> launch(String root) {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), PreferencesActivity.class);
        if (root != null) intent.putExtra("rootKey", root);
        return ActivityScenario.launch(intent);
    }
    private PreferencesFragment fragment(PreferencesActivity activity) {
        return (PreferencesFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment);
    }
}
