package com.smouldering_durtles.wk.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.FontSelectionActivity;
import com.smouldering_durtles.wk.activities.PreferencesActivity;
import com.smouldering_durtles.wk.activities.ThemeCustomizationActivity;
import com.smouldering_durtles.wk.components.MaterialSliderPreference;
import com.smouldering_durtles.wk.components.NumberRangePreference;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.fragments.PreferencesFragment;
import com.smouldering_durtles.wk.ui.ModernUi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SettingsCompletionTest {
    private SharedPreferences prefs;
    private Map<String, ?> before;
    private final String[] keys = {"modern_settings", "settings_dynamic_color", "selected_fonts", "anki_buttons_height"};

    @Before public void prepare() {
        prefs = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().getTargetContext());
        before = new HashMap<>(prefs.getAll());
        prefs.edit().putBoolean("modern_settings", true).putBoolean("settings_dynamic_color", false)
                .putString("selected_fonts", "[\"1\"]").putInt("anki_buttons_height", 50).commit();
    }
    @After public void restore() {
        SharedPreferences.Editor edit = prefs.edit();
        for (String key : keys) {
            Object value = before.get(key);
            if (value == null) edit.remove(key);
            else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) edit.putInt(key, (Integer) value);
            else edit.putString(key, (String) value);
        }
        edit.commit();
    }

    @Test public void sliderKeyboardPersistsAndRespectsChangeVetoAndDisabledState() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), PreferencesActivity.class);
        intent.putExtra("rootKey", "display_settings");
        try (ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = (PreferencesFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment);
                MaterialSliderPreference preference = fragment.findPreference("anki_buttons_height");
                assertNotNull(preference);
                View row = LayoutInflater.from(activity).inflate(preference.getLayoutResource(), null);
                PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(row);
                preference.onBindViewHolder(holder);
                Slider slider = row.findViewById(R.id.materialPreferenceSlider);
                assertNotNull(slider);
                assertEquals(10f, slider.getValueFrom(), 0f);
                assertEquals(250f, slider.getValueTo(), 0f);
                slider.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                slider.onKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
                assertEquals(51, preference.getValue());
                assertEquals(51, prefs.getInt("anki_buttons_height", 0));
                preference.setOnPreferenceChangeListener((p, value) -> false);
                slider.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                slider.onKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
                assertEquals(51, preference.getValue());
                assertEquals(51f, slider.getValue(), 0f);
                assertEquals("51", ((TextView) row.findViewById(androidx.preference.R.id.seekbar_value)).getText().toString());
                preference.setEnabled(false);
                preference.onBindViewHolder(holder);
                assertFalse(slider.isEnabled());
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                PreferencesFragment fragment = (PreferencesFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment);
                MaterialSliderPreference preference = fragment.findPreference("anki_buttons_height");
                assertEquals(51, preference.getValue());
            });
        }
    }

    @Test public void settingsControlsWrapAtDoubleTextSizeInDarkMode() {
        try (ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(PreferencesActivity.class)) {
            scenario.onActivity(activity -> {
                Configuration config = new Configuration(activity.getResources().getConfiguration());
                config.fontScale = 2f;
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
                ContextThemeWrapper context = new ContextThemeWrapper(activity.createConfigurationContext(config), R.style.SmokySettingsTheme);
                for (int resource : new int[] {R.layout.pref_number_range_modern, R.layout.pref_tagged_url_modern, R.layout.settings_slider}) {
                    View form = LayoutInflater.from(context).inflate(resource, null);
                    TextView message = form.findViewById(R.id.message);
                    if (message != null) message.setText("Choose the values for this setting. Leave a field empty to keep it unrestricted.");
                    if (resource == R.layout.settings_slider) {
                        ((TextView) form.findViewById(android.R.id.title)).setText("Vertical size of the Anki mode buttons");
                        ((TextView) form.findViewById(android.R.id.summary)).setText("Choose the height of the study controls");
                        Slider slider = form.findViewById(R.id.materialPreferenceSlider);
                        slider.setValueFrom(10); slider.setValueTo(250); slider.setValue(50);
                    }
                    int width = Math.round(320 * context.getResources().getDisplayMetrics().density);
                    form.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    form.layout(0, 0, width, form.getMeasuredHeight());
                    assertTextFits(form);
                    capture(activity, "settings-dark-large-" + activity.getResources().getResourceEntryName(resource), form);
                }
                com.smouldering_durtles.wk.views.FontSelectionRowView font = new com.smouldering_durtles.wk.views.FontSelectionRowView(context);
                font.setFontName("A long imported Japanese typeface name");
                int fontWidth = Math.round(320 * context.getResources().getDisplayMetrics().density);
                font.measure(View.MeasureSpec.makeMeasureSpec(fontWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                font.layout(0, 0, fontWidth, font.getMeasuredHeight());
                TextView name = font.findViewById(R.id.fontName);
                View toggle = font.findViewById(R.id.fontSwitch);
                assertTextFits(font);
                assertTrue("Long font name must not collide with its switch", name.getRight() <= toggle.getLeft());
                assertTrue("Switch stays inside the row", toggle.getRight() <= font.getWidth());
                capture(activity, "settings-font-row-dark-large", font);
            });
        }
    }

    private void assertTextFits(View view) {
        if (view instanceof TextView && view.getVisibility() == View.VISIBLE) {
            TextView text = (TextView) view;
            if (text.getLayout() != null) {
                for (int line = 0; line < text.getLineCount(); line++) assertEquals(0, text.getLayout().getEllipsisCount(line));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertTextFits(group.getChildAt(i));
        }
    }

    @Test public void twoFieldEditorsUseOutlinedInputsAndClassicKeepsOriginalLayout() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), PreferencesActivity.class);
        try (ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View range = activity.getLayoutInflater().inflate(R.layout.pref_number_range_modern, null);
                View url = activity.getLayoutInflater().inflate(R.layout.pref_tagged_url_modern, null);
                assertTrue(range.findViewById(R.id.minInput) instanceof TextInputEditText);
                assertTrue(range.findViewById(R.id.maxInput) instanceof TextInputEditText);
                assertTrue(url.findViewById(R.id.tagInput) instanceof TextInputEditText);
                assertTrue(url.findViewById(R.id.urlInput) instanceof TextInputEditText);
            });
        }
        prefs.edit().putBoolean("modern_settings", false).commit();
        try (ActivityScenario<PreferencesActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                NumberRangePreference preference = new NumberRangePreference(activity);
                assertEquals(R.layout.pref_number_range, preference.getDialogLayoutResource());
                MaterialSliderPreference slider = new MaterialSliderPreference(activity, null);
                assertNotEquals(R.layout.settings_slider, slider.getLayoutResource());
            });
        }
    }

    @Test public void fontSelectionsRemainIndependentAcrossRecreationAndClassicSwitch() {
        try (ActivityScenario<FontSelectionActivity> scenario = ActivityScenario.launch(FontSelectionActivity.class)) {
            scenario.onActivity(activity -> {
                ViewGroup rows = activity.findViewById(R.id.fontList);
                CompoundButton first = rows.getChildAt(1).findViewById(R.id.fontSwitch);
                CompoundButton second = rows.getChildAt(2).findViewById(R.id.fontSwitch);
                assertTrue(first instanceof MaterialSwitch);
                first.setChecked(false);
                second.setChecked(true);
                assertFalse(GlobalSettings.Font.isFontSelected("1"));
                assertTrue(GlobalSettings.Font.isFontSelected("2"));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                ViewGroup rows = activity.findViewById(R.id.fontList);
                assertFalse(((CompoundButton) rows.getChildAt(1).findViewById(R.id.fontSwitch)).isChecked());
                assertTrue(((CompoundButton) rows.getChildAt(2).findViewById(R.id.fontSwitch)).isChecked());
                capture(activity, "modern-font-selection", activity.getWindow().getDecorView());
            });
            prefs.edit().putBoolean("modern_settings", false).commit();
            scenario.recreate();
            scenario.onActivity(activity -> {
                ViewGroup rows = activity.findViewById(R.id.fontList);
                assertFalse(rows.getChildAt(1).findViewById(R.id.fontSwitch) instanceof MaterialSwitch);
                assertFalse(((CompoundButton) rows.getChildAt(1).findViewById(R.id.fontSwitch)).isChecked());
                assertTrue(((CompoundButton) rows.getChildAt(2).findViewById(R.id.fontSwitch)).isChecked());
            });
        }
    }

    @Test public void colorEditorKeepsSelectionAndCustomSemanticPaletteAcrossRecreation() {
        java.util.List<Integer> colors = new java.util.ArrayList<>(GlobalSettings.Display.getThemeCustomizations(ActiveTheme.getCurrentTheme()));
        try (ActivityScenario<ThemeCustomizationActivity> scenario = ActivityScenario.launch(ThemeCustomizationActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(ModernUi.isModern(activity));
                View guru = activity.findViewById(R.id.guruSample);
                assertNotNull(guru.getContentDescription());
                guru.performClick();
                assertTrue(((TextView) activity.findViewById(R.id.selectionDescription)).getText().toString().contains("Guru"));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(View.VISIBLE, activity.findViewById(R.id.guruSampleHighlight).getVisibility());
                assertTrue(((TextView) activity.findViewById(R.id.selectionDescription)).getText().toString().contains("Guru"));
                assertEquals(colors, GlobalSettings.Display.getThemeCustomizations(ActiveTheme.getCurrentTheme()));
                capture(activity, "modern-color-customizer", activity.getWindow().getDecorView());
            });
        }
    }

    private void capture(android.content.Context context, String name, View view) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) return;
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
        bitmap.recycle();
    }
}
