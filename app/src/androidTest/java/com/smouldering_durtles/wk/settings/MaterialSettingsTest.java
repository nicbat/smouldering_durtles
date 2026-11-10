package com.smouldering_durtles.wk.settings;

import android.content.Context;
import android.content.Intent;
import com.smouldering_durtles.wk.components.NumberRangePreference;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.InputType;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowCompat;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.components.TaggedUrlPreference;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.PreferencesActivity;
import com.smouldering_durtles.wk.fragments.PreferencesFragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** No credentials or study sessions: real preference rows, dialogs and persistence. */
@RunWith(AndroidJUnit4.class)
public class MaterialSettingsTest {
    private SharedPreferences prefs;
    private final Map<String, Object> original = new HashMap<>();
    private static final String[] KEYS = {"modern_settings", "settings_dynamic_color", "learning_colors",
            "modern_dashboard", "dashboard_dynamic_color", "network_rule", "max_lesson_session_size",
            "previous_level_progression_limit_amount", "previous_level_progression_limit"};

    @Before public void setUp() {
        prefs = PreferenceManager.getDefaultSharedPreferences(context());
        for (String key : KEYS) original.put(key, prefs.getAll().get(key));
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
                .putBoolean("modern_dashboard", true).putBoolean("dashboard_dynamic_color", true)
                .putString("network_rule", "DOWNLOAD_WIFI_ONLY")
                .putString("max_lesson_session_size", "5")
                .putBoolean("previous_level_progression_limit", true)
                .putString("previous_level_progression_limit_amount", "1").commit();
    }

    @After public void restore() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : KEYS) {
            Object value = original.get(key);
            if (value == null) editor.remove(key);
            else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else editor.putString(key, (String) value);
        }
        editor.commit();
    }

    @Test public void appearanceContainsOnlySupportedMaterialChoices() {
        try (ActivityScenario<PreferencesActivity> scenario = launch("display_settings")) {
            scenario.onActivity(activity -> {
                assertNull(fragment(activity).findPreference("modern_settings"));
                assertNull(fragment(activity).findPreference("modern_dashboard"));
                assertNull(fragment(activity).findPreference("settings_dynamic_color"));
                assertNull(fragment(activity).findPreference("dashboard_dynamic_color"));
                assertNotNull(fragment(activity).findPreference("learning_colors"));
            });
        }
    }

    @Test public void learningPalettePersistsAndRecreatesCurrentScreen() throws Exception {
        prefs.edit().putString("learning_colors", "soft").commit();
        try (ActivityScenario<PreferencesActivity> scenario = launch("display_settings")) {
            final PreferencesActivity[] before = new PreferencesActivity[1];
            scenario.onActivity(activity -> {
                before[0] = activity;
                fragment(activity).scrollToPreference("learning_colors");
            });
            idle();
            onView(withText(R.string.learning_colors_title)).perform(click());
            screenshot("settings-learning-colors");
            onView(withText("Neutral — use the Material palette")).inRoot(isDialog()).perform(click());
            idle();
            assertEquals("neutral", prefs.getString("learning_colors", ""));
            scenario.onActivity(activity -> {
                assertNotSame(before[0], activity);
                ListPreference palette = fragment(activity).findPreference("learning_colors");
                assertNotNull(palette);
                assertEquals("neutral", palette.getValue());
            });
        }
    }

    @Test public void obsoleteAppearanceFlagsCannotRestoreClassicOrWallpaperColors() {
        prefs.edit().putBoolean("modern_settings", false).putBoolean("settings_dynamic_color", true).commit();
        try (ActivityScenario<PreferencesActivity> scenario = launch("display_settings")) {
            scenario.onActivity(activity -> {
                assertTrue(com.smouldering_durtles.wk.ui.ModernUi.isModern(activity));
                android.util.TypedValue primary = new android.util.TypedValue();
                activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, primary, true);
                boolean dark = (activity.getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                assertEquals(activity.getColor(dark ? R.color.smoky_primary_dark : R.color.smoky_primary_light), primary.data);
            });
        }
    }

    @Test public void choiceSurvivesRecreationAndCommitsSelection() throws Exception {
        try (ActivityScenario<PreferencesActivity> scenario = launch("api_settings")) {
            onView(withText("Network access rule")).perform(click());
            scenario.recreate();
            idle();
            scenario.onActivity(activity -> assertTrue(dialog(activity) instanceof MaterialPreferenceDialogs.ListDialog));
            screenshot("settings-choice");
            onData(anything()).inAdapterView(isAssignableFrom(ListView.class)).atPosition(0).perform(click());
            scenario.onActivity(activity -> {
                androidx.preference.ListPreference pref = fragment(activity).findPreference("network_rule");
                assertEquals(pref.getEntryValues()[0].toString(), prefs.getString("network_rule", ""));
            });
        }
    }

    @Test public void choiceCancelAndListenerVetoDoNotPersist() {
        try (ActivityScenario<PreferencesActivity> scenario = launch("api_settings")) {
            onView(withText("Network access rule")).perform(click());
            pressBack();
            assertEquals("DOWNLOAD_WIFI_ONLY", prefs.getString("network_rule", ""));
            scenario.onActivity(activity -> fragment(activity).findPreference("network_rule")
                    .setOnPreferenceChangeListener((preference, value) -> false));
            onView(withText("Network access rule")).perform(click());
            onData(anything()).inAdapterView(isAssignableFrom(ListView.class)).atPosition(0).perform(click());
            assertEquals("DOWNLOAD_WIFI_ONLY", prefs.getString("network_rule", ""));
        }
    }

    @Test public void numericEditKeepsInputTypeDraftAndCancelSemantics() throws Exception {
        try (ActivityScenario<PreferencesActivity> scenario = launch("lesson_session_settings")) {
            onView(withText("Max. # of lessons per session")).perform(click());
            scenario.onActivity(activity -> {
                EditText input = dialog(activity).requireDialog().findViewById(android.R.id.edit);
                assertTrue(input instanceof TextInputEditText);
                assertEquals(InputType.TYPE_CLASS_NUMBER, input.getInputType() & InputType.TYPE_MASK_CLASS);
            });
            onView(withId(android.R.id.edit)).inRoot(isDialog()).perform(replaceText("17"));
            scenario.recreate();
            onView(withId(android.R.id.edit)).inRoot(isDialog()).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("17")));
            screenshot("settings-edit");
            onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click());
            assertEquals("5", prefs.getString("max_lesson_session_size", ""));
            onView(withText("Max. # of lessons per session")).perform(click());
            onView(withId(android.R.id.edit)).inRoot(isDialog()).perform(replaceText("12"));
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click());
            assertEquals("12", prefs.getString("max_lesson_session_size", ""));
        }
    }

    @Test public void boundedNumberStillRejectsInvalidValue() {
        try (ActivityScenario<PreferencesActivity> scenario = launch("dashboard_settings")) {
            scenario.onActivity(activity -> fragment(activity).onDisplayPreferenceDialog(
                    fragment(activity).findPreference("previous_level_progression_limit_amount")));
            scenario.onActivity(activity -> activity.getSupportFragmentManager().executePendingTransactions());
            idle();
            onView(withId(android.R.id.edit)).inRoot(isDialog()).perform(replaceText("61"));
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click());
            assertEquals("1", prefs.getString("previous_level_progression_limit_amount", ""));
        }
    }

    @Test public void nestedNavigationSurvivesRecreation() throws Exception {
        prefs.edit().putBoolean("settings_dynamic_color", true).commit();
        try (ActivityScenario<PreferencesActivity> scenario = launch(null)) {
            screenshot("settings-root");
            onView(withText("Display settings")).perform(click());
            scenario.recreate();
            onView(withText(R.string.learning_colors_title)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            pressBack();
            onView(withText("Sync & audio")).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
        }
    }

    @Test public void darkThemeHasReadableSurfaceAndMenus() throws Exception {
        int previousMode = AppCompatDelegate.getDefaultNightMode();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
        try (ActivityScenario<PreferencesActivity> scenario = launch("display_settings")) {
            scenario.onActivity(activity -> {
                int surface = MaterialColors.getColor(activity, com.google.android.material.R.attr.colorSurface, "test");
                int text = MaterialColors.getColor(activity, com.google.android.material.R.attr.colorOnSurface, "test");
                assertTrue(ColorUtils.calculateContrast(text, surface) >= 4.5);
                assertTrue(ColorUtils.calculateLuminance(surface) < 0.1);
                assertFalse(WindowCompat.getInsetsController(activity.getWindow(),
                        activity.getWindow().getDecorView()).isAppearanceLightStatusBars());
            });
            screenshot("settings-dark");
            androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu(context());
            screenshot("settings-overflow-dark");
            pressBack();
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    AppCompatDelegate.setDefaultNightMode(previousMode));
        }
    }

    @Test public void customUrlEditorKeepsDraftAcrossRecreationAndCancels() {
        try (ActivityScenario<PreferencesActivity> scenario = launch("other_settings")) {
            String[] before = new String[2];
            scenario.onActivity(activity -> {
                TaggedUrlPreference preference = fragment(activity).findPreference("search_engine_1");
                before[0] = preference.getTag();
                before[1] = preference.getUrl();
                fragment(activity).onDisplayPreferenceDialog(preference);
                activity.getSupportFragmentManager().executePendingTransactions();
            });
            onView(withId(R.id.tagInput)).inRoot(isDialog()).perform(replaceText("Test engine"));
            onView(withId(R.id.urlInput)).inRoot(isDialog()).perform(replaceText("https://example.com/%s"));
            scenario.recreate();
            onView(withId(R.id.tagInput)).inRoot(isDialog()).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("Test engine")));
            onView(withId(R.id.urlInput)).inRoot(isDialog()).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("https://example.com/%s")));
            onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click());
            scenario.onActivity(activity -> {
                TaggedUrlPreference preference = fragment(activity).findPreference("search_engine_1");
                assertEquals(before[0], preference.getTag());
                assertEquals(before[1], preference.getUrl());
            });
        }
    }

    @Test public void customNumberRangeKeepsDraftAcrossRecreationAndCancels() {
        try (ActivityScenario<PreferencesActivity> scenario = launch("lesson_session_settings")) {
            int[] before = new int[2];
            scenario.onActivity(activity -> {
                NumberRangePreference preference = fragment(activity).findPreference("lesson_subjects_current");
                before[0] = preference.getMin();
                before[1] = preference.getMax();
                fragment(activity).onDisplayPreferenceDialog(preference);
                activity.getSupportFragmentManager().executePendingTransactions();
            });
            onView(withId(R.id.minInput)).inRoot(isDialog()).perform(replaceText("2"));
            onView(withId(R.id.maxInput)).inRoot(isDialog()).perform(replaceText("8"));
            scenario.recreate();
            onView(withId(R.id.minInput)).inRoot(isDialog()).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("2")));
            onView(withId(R.id.maxInput)).inRoot(isDialog()).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("8")));
            onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click());
            scenario.onActivity(activity -> {
                NumberRangePreference preference = fragment(activity).findPreference("lesson_subjects_current");
                assertEquals(before[0], preference.getMin());
                assertEquals(before[1], preference.getMax());
            });
        }
    }

    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static ActivityScenario<PreferencesActivity> launch(String root) {
        Intent intent = new Intent(context(), PreferencesActivity.class);
        if (root != null) intent.putExtra("rootKey", root);
        ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(intent);
        idle();
        return scenario;
    }
    private static void idle() { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); }
    private static PreferencesFragment fragment(PreferencesActivity activity) {
        return (PreferencesFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment);
    }
    private static DialogFragment dialog(PreferencesActivity activity) {
        return (DialogFragment) activity.getSupportFragmentManager().findFragmentByTag("materialPreferenceDialog");
    }
    private static void screenshot(String name) throws Exception {
        idle();
        // Let window/system-bar transitions settle before capturing their visual state.
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(500, 5000);
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try (FileOutputStream out = new FileOutputStream(new File(context().getFilesDir(), name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }
}
