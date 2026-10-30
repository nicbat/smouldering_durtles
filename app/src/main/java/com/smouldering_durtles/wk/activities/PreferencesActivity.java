/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.activities;

import android.os.Bundle;
import android.content.res.Resources;
import android.content.res.ColorStateList;
import android.view.Menu;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.GlobalSettings;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.fragments.PreferencesFragment;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Activity for the preferences.
 */
public final class PreferencesActivity extends AbstractActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    /**
     * The constructor.
     */
    public PreferencesActivity() {
        super(R.layout.activity_preferences, R.menu.generic_options_menu);
    }

    @Override
    protected void customizeActivityTheme(final Resources.Theme theme) {
        theme.applyStyle(R.style.SmokyClassicSettingsOverlay, true);
        if (GlobalSettings.Display.getModernSettings()) {
            theme.applyStyle(R.style.SmokySettingsTheme, true);
            if (GlobalSettings.Display.getSettingsDynamicColor()) {
                DynamicColors.applyToActivityIfAvailable(this);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if (GlobalSettings.Display.getModernSettings()) {
            final int color = MaterialColors.getColor(this,
                    com.google.android.material.R.attr.colorOnSurface, "settings toolbar");
            for (int i = 0; i < menu.size(); i++) {
                // Legacy assets contain white paths; tint must follow this screen's surface.
                MenuItemCompat.setIconTintList(menu.getItem(i), ColorStateList.valueOf(color));
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        if (GlobalSettings.Display.getModernSettings() && getToolbar() != null) {
            getToolbar().setElevation(0);
            final int surface = MaterialColors.getColor(this,
                    com.google.android.material.R.attr.colorSurface, "settings surface");
            final WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(
                    getWindow(), getWindow().getDecorView());
            bars.setAppearanceLightStatusBars(MaterialColors.isColorLight(surface));
            bars.setAppearanceLightNavigationBars(MaterialColors.isColorLight(surface));
        }
        if (savedInstanceState == null) {
            final FragmentManager manager = getSupportFragmentManager();
            @Nullable PreferencesFragment fragment = (PreferencesFragment) manager.findFragmentByTag("preferencesFragment");
            if (fragment == null) {
                fragment = new PreferencesFragment();
            }

            final @Nullable String rootKey = getIntent().getStringExtra("rootKey");
            if (!isEmpty(rootKey)) {
                final Bundle args = new Bundle();
                args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, rootKey);
                fragment.setArguments(args);
            }

            final FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.fragment, fragment, "preferencesFragment");
            transaction.commitNow();
        }
    }

    @Override
    protected void onResumeLocal() {
        //
    }

    @Override
    protected void onPauseLocal() {
        //
    }

    @Override
    protected void enableInteractionLocal() {
        //
    }

    @Override
    protected void disableInteractionLocal() {
        //
    }

    @Override
    protected boolean showWithoutApiKey() {
        return true;
    }

    @Override
    public boolean onPreferenceStartScreen(final PreferenceFragmentCompat caller, final PreferenceScreen pref) {
        safe(() -> {
            final PreferencesFragment fragment = new PreferencesFragment();
            final Bundle args = new Bundle();
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
            fragment.setArguments(args);

            final FragmentManager manager = getSupportFragmentManager();
            final FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.fragment, fragment, pref.getKey());
            transaction.setReorderingAllowed(true);
            transaction.addToBackStack(pref.getKey());
            transaction.commit();
        });
        return true;
    }
}
