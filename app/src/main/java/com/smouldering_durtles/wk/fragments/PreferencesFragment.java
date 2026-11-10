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

package com.smouldering_durtles.wk.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import android.content.Context;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smouldering_durtles.wk.settings.MaterialPreferenceDialogs;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.smouldering_durtles.wk.Actment;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.AboutActivity;
import com.smouldering_durtles.wk.activities.AbstractActivity;
import com.smouldering_durtles.wk.activities.BackupActivity;
import com.smouldering_durtles.wk.activities.DataImportExportActivity;
import com.smouldering_durtles.wk.activities.FontImportActivity;
import com.smouldering_durtles.wk.activities.FontSelectionActivity;
import com.smouldering_durtles.wk.activities.KeyboardHelpActivity;
import com.smouldering_durtles.wk.activities.DownloadAudioActivity;
import com.smouldering_durtles.wk.jobs.SyncNowJob;
import com.smouldering_durtles.wk.api.ApiState;
import com.smouldering_durtles.wk.components.NumberRangePreference;
import com.smouldering_durtles.wk.components.NumberRangePreferenceDialogFragment;
import com.smouldering_durtles.wk.components.TaggedUrlPreference;
import com.smouldering_durtles.wk.components.TaggedUrlPreferenceDialogFragment;
import com.smouldering_durtles.wk.jobs.ResetDatabaseJob;
import com.smouldering_durtles.wk.livedata.LiveApiState;
import com.smouldering_durtles.wk.services.JobRunnerService;
import com.smouldering_durtles.wk.util.AudioUtil;
import com.smouldering_durtles.wk.util.DbLogger;

import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.API_KEY_PERMISSION_NOTICE;
import static com.smouldering_durtles.wk.Constants.EXPERIMENTAL_PREFERENCE_STATUS_NOTICE;
import static com.smouldering_durtles.wk.Constants.RESET_DATABASE_WARNING;
import static com.smouldering_durtles.wk.Constants.RESET_TUTORIALS_WARNING;
import static com.smouldering_durtles.wk.Constants.SUBJECT_SELECTION_NOTICE;
import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.TextUtil.renderHtml;

/**
 * Fragment for preferences.
 */
public final class PreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final @Nullable Bundle savedInstanceState, final @Nullable String rootKey) {
        GlobalSettings.migrateUnifiedStudySettings();
        GlobalSettings.migrateSessionPriorities();
        setPreferencesFromResource(R.xml.preferences, rootKey);
        forecastPreferences();
        questionOrderPreferences();
        fontPreferences();
        removeIconSpace(getPreferenceScreen());
        for (String key : new String[] {"learning_colors"}) {
            Preference appearance = findPreference(key);
            if (appearance != null) appearance.setOnPreferenceChangeListener((preference, value) -> {
                // The change listener runs before persistence. Recreate after it returns.
                requireView().post(() -> { if (isAdded()) requireActivity().recreate(); });
                return true;
            });
        }

        // Add listener for always_hide_wk_mnemonic changes
        final @Nullable TwoStatePreference alwaysHideWKPref = findPreference("always_hide_wk_mnemonic");
        final @Nullable TwoStatePreference notAlwaysHideWKPref = findPreference("not_always_hide_wk_mnemonic");

        if (alwaysHideWKPref != null && notAlwaysHideWKPref != null) {
            alwaysHideWKPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean alwaysHide = (Boolean) newValue;
                notAlwaysHideWKPref.setChecked(!alwaysHide);
                return true;
            });

            // Set initial state
            notAlwaysHideWKPref.setChecked(!alwaysHideWKPref.isChecked());
        }
    }

    private void questionOrderPreferences() {
        for (String prefix : new String[]{"lesson", "review", "self_study"}) {
            ListPreference preference = findPreference(prefix + "_question_order");
            if (preference == null) continue;
            String current = GlobalSettings.getQuestionOrder(prefix);
            // Keep an existing conflicting pair intact until the user chooses a new order.
            if ("COMBINED".equals(current)) {
                preference.setEntries(new String[]{"Any order", "Reading before meaning", "Meaning before reading", "Keep existing combined order"});
                preference.setEntryValues(new String[]{"ANY", "READING_FIRST", "MEANING_FIRST", "COMBINED"});
            } else {
                preference.setEntries(new String[]{"Any order", "Reading before meaning", "Meaning before reading"});
                preference.setEntryValues(new String[]{"ANY", "READING_FIRST", "MEANING_FIRST"});
            }
            preference.setValue(current);
            preference.setOnPreferenceChangeListener((p, value) -> {
                GlobalSettings.setQuestionOrder(prefix, value.toString());
                return true;
            });
        }
    }

    private void fontPreferences() {
        ListPreference height = findPreference("quiz_question_height_preset");
        if (height != null) {
            int current = GlobalSettings.Display.getQuizQuestionViewHeight();
            java.util.ArrayList<String> labels = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "Automatic", "Compact (112 dp)", "Comfortable (160 dp)", "Large (224 dp)"));
            java.util.ArrayList<String> values = new java.util.ArrayList<>(java.util.Arrays.asList("0", "112", "160", "224"));
            if (!values.contains(Integer.toString(current))) {
                labels.add("Custom (" + current + " dp; safe minimum applies)");
                values.add(Integer.toString(current));
            }
            height.setEntries(labels.toArray(new String[0]));
            height.setEntryValues(values.toArray(new String[0]));
            height.setValue(Integer.toString(current));
            height.setSummaryProvider(preference -> ((ListPreference) preference).getEntry()
                    + " · Reserves space for the main review question. Automatic grows with its text.");
            height.setOnPreferenceChangeListener((p, value) -> {
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                        .putInt("quiz_question_view_height", Integer.parseInt(value.toString())).apply();
                return true;
            });
        }
        Preference reset = findPreference("reset_font_sizes");
        if (reset != null) reset.setOnPreferenceClickListener(preference -> {
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                    .putInt("max_font_size_lesson", 128).putInt("max_font_size_quiz", 28)
                    .putInt("max_font_size_browse", 64).putInt("max_font_size_quiz_text", 100)
                    .putInt("font_size_question_edit", 14).putInt("font_size_subject_info", 14)
                    .putInt("font_size_live_subject_table", 14).putInt("font_size_anki_answer", 18)
                    .putInt("quiz_question_view_height", 0).apply();
            Toast.makeText(requireContext(), "Text sizes reset", Toast.LENGTH_SHORT).show();
            requireActivity().recreate();
            return true;
        });
    }

    private void forecastPreferences() {
        ListPreference horizon = findPreference("timeline_chart_size");
        if (horizon != null) {
            horizon.setTitle(R.string.forecast_horizon_title);
            horizon.setSummaryProvider(preference -> getString(R.string.forecast_horizon_summary,
                    GlobalSettings.Dashboard.getTimeLineChartSize()));
        }
    }

    private void onViewCreatedBase(final View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface));

        setOnPreferenceClick("reset_database", preference -> safe(false, () -> {
            dialogBuilder(preference.getContext())
                    .setTitle("Reset database?")
                    .setMessage(renderHtml(RESET_DATABASE_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {})
                    .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                        JobRunnerService.schedule(ResetDatabaseJob.class, "");
                        goToMainActivity();
                    })).create().show();
            return true;
        }));



        setOnPreferenceClick("reset_tutorials", preference -> safe(false, () -> {
            dialogBuilder(preference.getContext())
                    .setTitle("Reset confirmations and tutorials?")
                    .setMessage(renderHtml(RESET_TUTORIALS_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {})
                    .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                        GlobalSettings.resetConfirmationsAndTutorials();
                        Toast.makeText(preference.getContext(), "Confirmations and tutorials reset", Toast.LENGTH_LONG).show();
                    })).create().show();
            return true;
        }));

        final @Nullable Preference grantNotificationPref = findPreference("grant_notification_permission");
        if (grantNotificationPref != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                grantNotificationPref.setOnPreferenceClickListener(preference -> safe(false, () -> {
                    final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                    startActivity(intent);
                    return true;
                }));
            } else {
                grantNotificationPref.setVisible(false);
            }
        }

        setOnPreferenceClick("backup_settings", preference -> {
            dialogBuilder(preference.getContext())
                    .setTitle("Backup settings?")
                    .setMessage("Are you sure you want to backup your settings?")
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {})
                    .setPositiveButton("Yes", (dialog, which) -> {
                        try {
                            startBackup();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).create().show();
            return true;
        });

        setOnPreferenceClick("restore_settings", preference -> {
            dialogBuilder(preference.getContext())
                    .setTitle("Restore settings?")
                    .setMessage("Are you sure you want to restore your settings?")
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {})
                    .setPositiveButton("Yes", (dialog, which) -> {
                        try {
                            startRestore();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).create().show();
            return true;
        });

        final @Nullable ListPreference audioLocation = findPreference("audio_location");
        if (audioLocation != null) {
            final List<String> locationValues = AudioUtil.getLocationValues();
            final List<String> locations = AudioUtil.getLocations(locationValues);
            audioLocation.setEntries(locations.toArray(new String[] {}));
            audioLocation.setEntryValues(locationValues.toArray(new String[] {}));
            audioLocation.setVisible(true);
        }

        setVisibility("api_key_help", LiveApiState.getInstance().get() != ApiState.OK);
        setVisibility("ime_hint_reading", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        setVisibility("ime_hint_meaning", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        setVisibility("web_password", true);

        setSummaryHtml("api_key_help", API_KEY_PERMISSION_NOTICE);
        setSummaryHtml("experimental_status", EXPERIMENTAL_PREFERENCE_STATUS_NOTICE);
        setSummaryHtml("lesson_subject_selection", SUBJECT_SELECTION_NOTICE);
        setSummaryHtml("review_subject_selection", SUBJECT_SELECTION_NOTICE);
        setSummaryHtml("self_study_subject_selection", SUBJECT_SELECTION_NOTICE);

        setNumberInputType("overdue_threshold");
        setNumberInputType("max_lesson_session_size");
        setNumberInputType("max_review_session_size");
        setNumberInputType("max_self_study_session_size");
        setDecimalNumberInputType("next_button_delay");

        setOnClickGoToActivity("about_this_app", AboutActivity.class);
        setOnClickGoToActivity("download_audio", DownloadAudioActivity.class);
        Preference flushTasks = findPreference("flush_tasks");
        if (flushTasks != null) flushTasks.setOnPreferenceClickListener(preference -> {
            dialogBuilder(requireContext()).setTitle("Discard pending sync tasks?")
                    .setMessage(renderHtml(com.smouldering_durtles.wk.Constants.FLUSH_TASKS_WARNING))
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Discard tasks", (dialog, which) ->
                            JobRunnerService.schedule(com.smouldering_durtles.wk.jobs.FlushTasksJob.class, ""))
                    .show();
            return true;
        });
        Preference syncNow = findPreference("sync_now");
        if (syncNow != null) syncNow.setOnPreferenceClickListener(preference -> {
            JobRunnerService.schedule(SyncNowJob.class, "");
            Toast.makeText(requireContext(), "Sync requested", Toast.LENGTH_SHORT).show();
            return true;
        });
        com.smouldering_durtles.wk.livedata.LiveTaskCounts.getInstance().observe(getViewLifecycleOwner(), counts -> {
            Preference queue = findPreference("sync_queue");
            if (queue != null && counts != null) queue.setSummary(counts.isEmpty()
                    ? "No pending sync or download tasks"
                    : counts.getApiCount() + " API tasks · " + counts.getAudioCount()
                    + " audio files · " + counts.getPitchInfoCount() + " pitch files pending");
        });
        LiveApiState.getInstance().observe(getViewLifecycleOwner(), state -> {
            Preference status = findPreference("sync_status");
            if (status != null) {
                String message;
                switch (state) {
                    case OK: message = "Connected to WaniKani"; break;
                    case API_KEY_MISSING: message = "Add your WaniKani API token to connect"; break;
                    case API_KEY_REJECTED: message = "Token rejected — check the token and its permissions"; break;
                    case ERROR: message = "Connection failed — check your network, then tap Sync now"; break;
                    default: message = "Ready to refresh — tap Sync now";
                }
                status.setSummary(message);
            }
            setVisibility("api_key_help", state != ApiState.OK);
        });
        setOnClickGoToActivity("font_selection", FontSelectionActivity.class);
        setOnClickGoToActivity("font_import", FontImportActivity.class);
        setOnClickGoToActivity("keyboard_help", KeyboardHelpActivity.class);
        setOnClickGoToActivity("data_import_export", DataImportExportActivity.class);
    }

    @Override
    public void onViewCreated(final View view, final @Nullable Bundle savedInstanceState) {
        safe(() -> onViewCreatedBase(view, savedInstanceState));
    }

    @Override
    public void onResume() {
        safe(() -> {
            super.onResume();
            final @Nullable Bundle args = getArguments();
            @Nullable CharSequence title = null;
            if (args != null) {
                final @Nullable String key = args.getString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT);
                if (key != null) {
                    final @Nullable Preference preference = findPreference(key);
                    if (preference != null) {
                        title = preference.getTitle();
                    }
                }
            }
            if (title == null) {
                title = "Settings";
            }
            final @Nullable Activity activity = getActivity();
            if (activity instanceof Actment) {
                final @Nullable Toolbar toolbar = ((Actment) activity).getToolbar();
                if (toolbar != null) {
                    toolbar.setTitle(title);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onDisplayPreferenceDialog(final Preference preference) {
        safe(() -> {
            if (preference instanceof TaggedUrlPreference) {
                if (getParentFragmentManager().findFragmentByTag("TaggedUrlPreference") != null) {
                    return;
                }
                final DialogFragment f = TaggedUrlPreferenceDialogFragment.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                f.show(getParentFragmentManager(), "TaggedUrlPreference");
                return;
            }
            if (preference instanceof NumberRangePreference) {
                if (getParentFragmentManager().findFragmentByTag("NumberRangePreference") != null) {
                    return;
                }
                final DialogFragment f = NumberRangePreferenceDialogFragment.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                f.show(getParentFragmentManager(), "NumberRangePreference");
                return;
            }

            if (preference instanceof ListPreference || preference instanceof EditTextPreference) {
                final String tag = "materialPreferenceDialog";
                if (getParentFragmentManager().findFragmentByTag(tag) != null) return;
                final DialogFragment dialog = preference instanceof ListPreference
                        ? MaterialPreferenceDialogs.list(preference.getKey())
                        : MaterialPreferenceDialogs.edit(preference.getKey());
                dialog.setTargetFragment(this, 0);
                dialog.show(getParentFragmentManager(), tag);
                return;
            }
            super.onDisplayPreferenceDialog(preference);
        });
    }

    private AlertDialog.Builder dialogBuilder(Context context) {
        return new MaterialAlertDialogBuilder(context);
    }

    private void removeIconSpace(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            preference.setIconSpaceReserved(false);
            if (preference instanceof PreferenceGroup) removeIconSpace((PreferenceGroup) preference);
        }
    }

    private void goToActivity(final Class<? extends AbstractActivity> clas) {
        final @Nullable Activity a = getActivity();
        if (a instanceof Actment) {
            ((Actment) a).goToActivity(clas);
        }
    }

    private void goToMainActivity() {
        final @Nullable Activity a = getActivity();
        if (a instanceof Actment) {
            ((Actment) a).goToMainActivity();
        }
    }

    private void setOnPreferenceClick(final CharSequence key, final @Nullable Preference.OnPreferenceClickListener listener) {
        safe(() -> {
            final @Nullable Preference pref = findPreference(key);
            if (pref != null) {
                pref.setOnPreferenceClickListener(listener);
            }
        });
    }

    private void setOnClickGoToActivity(final CharSequence key, final Class<? extends AbstractActivity> clas) {
        safe(() -> setOnPreferenceClick(key, preference -> {
            safe(() -> goToActivity(clas));
            return true;
        }));
    }

    private void startBackup() {
        safe(() -> {
            Intent intent = new Intent(getActivity(), BackupActivity.class);
            intent.setAction("com.smouldering_durtles.wk.BACKUP");
            startActivity(intent);
        });
    }

    private void startRestore() {
        safe(() -> {
            Intent intent = new Intent(getActivity(), BackupActivity.class);
            intent.setAction("com.smouldering_durtles.wk.RESTORE");
            startActivity(intent);
        });
    }


    private void setNumberInputType(final CharSequence key) {
        safe(() -> {
            final @Nullable EditTextPreference pref = findPreference(key);
            if (pref != null) {
                pref.setOnBindEditTextListener(editText -> safe(() -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.setSelection(editText.getText().length());
                }));
            }
        });
    }

    private void setSummaryHtml(final CharSequence key, final String html) {
        safe(() -> {
            final @Nullable Preference pref = findPreference(key);
            if (pref != null) {
                pref.setSummary(renderHtml(html));
            }
        });
    }

    private void setDecimalNumberInputType(@SuppressWarnings("SameParameterValue") final CharSequence key) {
        safe(() -> {
            final @Nullable EditTextPreference pref = findPreference(key);
            if (pref != null) {
                pref.setOnBindEditTextListener(editText -> safe(() -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    editText.setSelection(editText.getText().length());
                }));
            }
        });
    }

    private void setVisibility(final CharSequence key, final boolean visible) {
        safe(() -> {
            final @Nullable Preference pref = findPreference(key);
            if (pref != null) {
                pref.setVisible(visible);
            }
        });

    }
    // In PreferencesFragment
    public void updateTheme() {
        View view = requireView();
        view.setBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface));
    }


}
