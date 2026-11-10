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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.ui.ModernUi;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.SearchPreset;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.livedata.LiveSearchPresets;
import com.smouldering_durtles.wk.model.AdvancedSearchParameters;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.SearchUtil;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Activity for starting a self-study session.
 *
 * <p>
 *     This activity show the self-study configurations, allows it to modified,
 *     and launches te session when the user taps Start.
 * </p>
 */
public final class SelfStudyStartActivity extends AbstractActivity {
    private static final Session session = Session.getInstance();

    private final ViewProxy tutorialText = new ViewProxy();
    private final ViewProxy tutorialDismiss = new ViewProxy();
    private final ViewProxy presetSpinner = new ViewProxy();
    private final ViewProxy presetHeader = new ViewProxy();
    private final ViewProxy presetDivider = new ViewProxy();
    private final ViewProxy presetButton = new ViewProxy();
    private final ViewProxy presetDelete = new ViewProxy();
    private final ViewProxy searchForm = new ViewProxy();
    private final ViewProxy searchButton1 = new ViewProxy();
    private final ViewProxy searchButton2 = new ViewProxy();

    /**
     * The constructor.
     */
    public SelfStudyStartActivity() {
        super(R.layout.activity_self_study_start, R.menu.generic_options_menu);
    }

    private void updatePresetAdapter() {
        final List<String> names = LiveSearchPresets.getInstance().getNames();
        final @Nullable Object selection = presetSpinner.getSelection();
        int position = -1;
        if (selection instanceof String) {
            position = LiveSearchPresets.getInstance().getNames().indexOf(selection);
        }
        final SpinnerAdapter adapter = new ArrayAdapter<>(this, R.layout.browse_spinner_item_modern, names);
        presetSpinner.setAdapter(adapter);
        if (position >= 0) {
            presetSpinner.setSelection(position);
        }
    }

    @Override
    protected void customizeActivityTheme(final android.content.res.Resources.Theme theme) {
        com.smouldering_durtles.wk.ui.ModernUi.applyTheme(this, theme);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        com.smouldering_durtles.wk.ui.ModernUi.style(findViewById(android.R.id.content));
        if (!session.isInactive()) {
            finish();
            return;
        }

        tutorialText.setDelegate(this, R.id.tutorialText);
        tutorialDismiss.setDelegate(this, R.id.tutorialDismiss);
        presetSpinner.setDelegate(this, R.id.presetSpinner);
        presetHeader.setDelegate(this, R.id.presetHeader);
        presetDivider.setDelegate(this, R.id.presetDivider);
        presetButton.setDelegate(this, R.id.presetButton);
        presetDelete.setDelegate(this, R.id.presetDelete);
        searchForm.setDelegate(this, R.id.searchForm);
        searchButton1.setDelegate(this, R.id.searchButton1);
        searchButton2.setDelegate(this, R.id.searchButton2);

        final View.OnClickListener listener = v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            if (!((com.smouldering_durtles.wk.views.AdvancedSearchFormView) findViewById(R.id.searchForm)).validateLevels()) return;
            disableInteraction();

            final @Nullable AdvancedSearchParameters searchParameters = searchForm.extractParameters();
            if (searchParameters != null) {
                startSession(2, Converters.getObjectMapper().writeValueAsString(searchParameters), true);
            }
        });

        searchButton1.setOnClickListener(listener);
        searchButton2.setOnClickListener(listener);

        final @Nullable SearchPreset selfStudyPreset = LiveSearchPresets.getInstance().getByName("\u0000SELF_STUDY_DEFAULT");
        if (selfStudyPreset != null) {
            safe(() -> {
                final AdvancedSearchParameters parameters = Converters.getObjectMapper().readValue(selfStudyPreset.data, AdvancedSearchParameters.class);
                searchForm.injectParameters(parameters);
            });
        }

        long[] practiceIds = getIntent().getLongArrayExtra("practiceIds");
        if (savedInstanceState != null) practiceIds = savedInstanceState.getLongArray("previewIds");
        if (practiceIds != null) {
            java.util.List<Long> ids = new java.util.ArrayList<>(); for (long id : practiceIds) ids.add(id);
            runAsync(this, () -> WkApplication.getDatabase().subjectCollectionsDao().getByIds(ids), this::showPracticePreview);
        }
        searchForm.setSearchButtonLabel("Start");
        searchForm.setSortOrderVisibility(false);
        ((com.smouldering_durtles.wk.views.AdvancedSearchFormView) findViewById(R.id.searchForm)).compact();

        updatePresetAdapter();

        if (savedInstanceState != null) {
            safe(() -> searchForm.injectParameters(Converters.getObjectMapper().readValue(savedInstanceState.getString("quizFilters", "{}"), AdvancedSearchParameters.class)));
            final @Nullable String selection = savedInstanceState.getString("presetSelection");
            if (selection != null) {
                final int position = LiveSearchPresets.getInstance().getNames().indexOf(selection);
                if (position >= 0) {
                    presetSpinner.setSelection(position);
                }
            }
        }

        safe(() -> LiveSearchPresets.getInstance().observe(this, t -> safe(() -> {
            if (LiveSearchPresets.getInstance().getNames().isEmpty()) {
                presetHeader.setVisibility(false);
                presetDivider.setVisibility(false);
                presetSpinner.setVisibility(false);
                presetButton.setParentVisibility(false);
            }
            else {
                updatePresetAdapter();
                presetHeader.setVisibility(true);
                presetDivider.setVisibility(true);
                presetSpinner.setVisibility(true);
                presetButton.setParentVisibility(true);
            }
        })));

        presetButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();

            final @Nullable Object selection = presetSpinner.getSelection();
            if (selection instanceof String) {
                final @Nullable SearchPreset preset = LiveSearchPresets.getInstance().getByName((String) selection);
                if (preset != null) {
                    startSession(preset.type, preset.data, false);
                }
            }
        }));

        presetDelete.setOnClickListener(v -> safe(() -> {
            final @Nullable Object selection = presetSpinner.getSelection();
            if (selection instanceof String) {
                final String name = (String) selection;
                com.smouldering_durtles.wk.ui.ModernUi.dialog(v.getContext())
                        .setTitle("Delete preset?")
                        .setMessage(String.format(Locale.ROOT, "Are you sure you want to delete the preset named '%s'?", name))
                        .setIcon(R.drawable.ic_baseline_warning_24px)
                        .setNegativeButton("No", (dialog, which) -> {})
                        .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                            runAsync(() -> WkApplication.getDatabase().searchPresetDao().deletePreset(name));
                            Toast.makeText(v.getContext(), "Preset deleted", Toast.LENGTH_SHORT).show();
                        })).create().show();
            }
        }));
    }

    @Override
    protected void onResumeLocal() {
        if (!session.isInactive()) {
            finish();
        }

        if (GlobalSettings.Tutorials.getStartSelfStudyDismissed()) {
            tutorialText.setParentVisibility(false);
        }
        else {
            tutorialText.setText("From this screen you can start a self-study quiz. The subjects for this quiz can be selected"
                    + " by filling out the search form below. If you have any search presets defined, you can also start a self-study"
                    + " based on those directly. Go to 'Browse/search...' from the menu to define presets.\n\n"
                    + "Results from a self-study quiz are not reported to WaniKani. They don't affect your progress"
                    + " and are only for your own self-study purposes.");
            tutorialText.setParentVisibility(true);
            tutorialDismiss.setOnClickListener(v -> safe(() -> {
                GlobalSettings.Tutorials.setStartSelfStudyDismissed(true);
                tutorialText.setParentVisibility(false);
            }));
        }
    }

    @Override
    protected void onPauseLocal() {
        //
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        safe(() -> {
            super.onSaveInstanceState(outState);
            outState.putLongArray("previewIds", previewIds);
            outState.putString("quizFilters", Converters.getObjectMapper().writeValueAsString(searchForm.extractParameters()));
            final @Nullable Object selection = presetSpinner.getSelection();
            if (selection instanceof String) {
                outState.putString("presetSelection", (String) selection);
            }
        });
    }

    @Override
    protected void enableInteractionLocal() {
        searchButton1.enableInteraction();
        searchButton2.enableInteraction();
        presetButton.enableInteraction();
    }

    @Override
    protected void disableInteractionLocal() {
        searchButton1.disableInteraction();
        searchButton2.disableInteraction();
        presetButton.disableInteraction();
    }

    @Override
    protected boolean showWithoutApiKey() {
        return false;
    }

    private long[] previewIds;
    private void showPracticePreview(List<Subject> subjects) {
        enableInteraction();
        if (subjects == null || subjects.isEmpty()) {
            Toast.makeText(this, "No items match these filters", Toast.LENGTH_SHORT).show(); return;
        }
        previewIds = new long[subjects.size()]; for (int i = 0; i < subjects.size(); i++) previewIds[i] = subjects.get(i).getId();
        android.widget.LinearLayout preview = new android.widget.LinearLayout(this); preview.setOrientation(android.widget.LinearLayout.VERTICAL);
        int gutter = ModernUi.dp(preview, 16); preview.setPadding(gutter, gutter, gutter, gutter);
        android.widget.TextView summary = new android.widget.TextView(this);
        int max = GlobalSettings.Review.getMaxSelfStudySessionSize();
        summary.setText(subjects.size() + " matching items · up to " + Math.min(subjects.size(), max) + " in this quiz\nPractice only · no SRS changes"); preview.addView(summary);
        for (Subject subject : subjects.subList(0, Math.min(12, subjects.size()))) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(this); row.setPadding(0, gutter / 2, 0, gutter / 2);
            android.widget.TextView japanese = new android.widget.TextView(this); japanese.setText(subject.getCharacters() == null ? subject.getSlug() : subject.getCharacters());
            japanese.setTextColor(com.smouldering_durtles.wk.ui.LearningColors.accent(this, subject.getBackgroundColor()));
            android.widget.TextView english = new android.widget.TextView(this); english.setText(subject.getOneMeaning()); english.setGravity(android.view.Gravity.END);
            row.addView(japanese, new android.widget.LinearLayout.LayoutParams(0, -2, 1)); row.addView(english, new android.widget.LinearLayout.LayoutParams(0, -2, 1)); preview.addView(row);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this); scroll.addView(preview);
        androidx.appcompat.app.AlertDialog dialog = ModernUi.dialog(this).setTitle("Custom quiz").setView(scroll)
                .setNegativeButton("Back", null).setPositiveButton("Start quiz", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            dialog.getButton(-1).setEnabled(false);
            runAsync(this, () -> {
                if (!session.isInactive() || WkApplication.getDatabase().propertiesDao().getSessionType() != com.smouldering_durtles.wk.enums.SessionType.NONE) return false;
                session.startNewSelfStudySession(subjects); return true;
            }, started -> {
                if (Boolean.TRUE.equals(started)) { dialog.dismiss(); goToActivity(SessionActivity.class); }
                else { dialog.getButton(-1).setEnabled(true); Toast.makeText(this, "The previous session is still finishing. Try again shortly.", Toast.LENGTH_SHORT).show(); }
            });
        })); dialog.setOnDismissListener(d -> previewIds = null); dialog.show();
    }

    private void startSession(final int searchType, final String searchParameters, final boolean saveSelfStudyPreset) {
        runAsync(this, () -> {
            if (saveSelfStudyPreset && searchType == 2) {
                WkApplication.getDatabase().searchPresetDao().setPreset("\u0000SELF_STUDY_DEFAULT", 2, searchParameters);
            }
            final List<Subject> subjects = SearchUtil.searchSubjects(searchType, searchParameters);
            return subjects;
        }, result -> showPracticePreview(result));
    }
}
