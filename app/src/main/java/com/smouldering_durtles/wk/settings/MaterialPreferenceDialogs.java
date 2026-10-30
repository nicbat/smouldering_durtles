package com.smouldering_durtles.wk.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smouldering_durtles.wk.GlobalSettings;

import javax.annotation.Nullable;

/** Material presentation with AndroidX still owning values, listeners and saved state. */
public final class MaterialPreferenceDialogs {
    private MaterialPreferenceDialogs() {}

    private static AlertDialog.Builder builder(PreferenceDialogFragmentCompat fragment,
                                               @Nullable View view) {
        DialogPreference preference = fragment.getPreference();
        // Back/outside-tap must never commit an edit or a choice after recreation.
        fragment.onClick(null, DialogInterface.BUTTON_NEGATIVE);
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(preference.getDialogTitle())
                .setIcon(preference.getDialogIcon())
                .setPositiveButton(preference.getPositiveButtonText(), fragment)
                .setNegativeButton(preference.getNegativeButtonText(), fragment);
        if (view == null) builder.setMessage(preference.getDialogMessage());
        else builder.setView(view);
        return builder;
    }

    public static ListDialog list(String key) {
        ListDialog dialog = new ListDialog();
        Bundle args = new Bundle();
        args.putString("key", key);
        dialog.setArguments(args);
        return dialog;
    }

    public static EditDialog edit(String key) {
        EditDialog dialog = new EditDialog();
        Bundle args = new Bundle();
        args.putString("key", key);
        dialog.setArguments(args);
        return dialog;
    }

    public static final class ListDialog extends ListPreferenceDialogFragmentCompat {
        @Override
        public Dialog onCreateDialog(@Nullable Bundle state) {
            AlertDialog.Builder builder = builder(this, null);
            // Retain AndroidX's immediate selection, change-listener veto and persistence.
            onPrepareDialogBuilder(builder);
            return builder.create();
        }
    }

    public static final class EditDialog extends EditTextPreferenceDialogFragmentCompat {
        @Override
        public Dialog onCreateDialog(@Nullable Bundle state) {
            View view = onCreateDialogView(requireContext());
            if (view != null) onBindDialogView(view);
            AlertDialog.Builder builder = builder(this, view);
            onPrepareDialogBuilder(builder);
            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            return dialog;
        }
    }

    /** Shared shell for the existing two-field editors; their data handling is unchanged. */
    public abstract static class CustomDialog extends PreferenceDialogFragmentCompat {
        @Override
        public Dialog onCreateDialog(@Nullable Bundle state) {
            if (!GlobalSettings.Display.getModernSettings()) return super.onCreateDialog(state);
            View view = onCreateDialogView(requireContext());
            if (view != null) onBindDialogView(view);
            AlertDialog.Builder builder = builder(this, view);
            onPrepareDialogBuilder(builder);
            return builder.create();
        }
    }
}
