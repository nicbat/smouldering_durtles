package com.smouldering_durtles.wk.study;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.ui.ModernUi;

import java.util.Locale;

/** Modern summary presentation only. The fragment remains the owner of session counts and actions. */
public final class SummaryPresentation {
    private static final int[] CORRECT_IDS = {R.id.correctRadicals, R.id.correctKanji,
            R.id.correctVocabulary, R.id.correctKanaVocabulary};
    private static final int[] INCORRECT_IDS = {R.id.incorrectRadicals, R.id.incorrectKanji,
            R.id.incorrectVocabulary, R.id.incorrectKanaVocabulary};
    private static final String[] SUBJECTS = {"Radicals", "Kanji", "Vocabulary", "Kana vocabulary"};

    private SummaryPresentation() {}

    private static final class State {
        TextView percentage;
        TextView count;
        LinearProgressIndicator bar;
        LinearLayout breakdown;
        final View[] rows = new View[4];
        final TextView[] correct = new TextView[4];
        final TextView[] errors = new TextView[4];
    }

    /** Reuses the bound count views, so existing IDs and the Show items visibility contract survive. */
    public static void prepare(View root) {
        if (!ModernUi.isModern(root.getContext())) return;
        final TableLayout panel = root.findViewById(R.id.correctSummary);
        if (panel == null || panel.getTag(R.id.summaryPresentationState) != null) return;
        final Context context = panel.getContext();
        final boolean large = context.getResources().getConfiguration().fontScale >= 1.5f;
        final State state = new State();
        state.percentage = root.findViewById(R.id.correctPercentage);
        detach(state.percentage);
        for (int i = 0; i < 4; i++) {
            state.correct[i] = root.findViewById(CORRECT_IDS[i]);
            state.errors[i] = root.findViewById(INCORRECT_IDS[i]);
            detach(state.correct[i]);
            detach(state.errors[i]);
        }
        panel.removeAllViews();
        panel.setTag(R.id.summaryPresentationState, state);
        panel.setBackground(ModernUi.surface(panel, color(panel,
                com.google.android.material.R.attr.colorSurfaceContainerLow), 16));
        panel.setPadding(dp(panel, 12), dp(panel, 12), dp(panel, 12), dp(panel, 12));
        panel.addView(label(context, root.getResources().getString(R.string.summary_accuracy), 14, true));
        styleText(state.percentage, 32, false);
        state.percentage.setText("—");
        panel.addView(state.percentage, new TableLayout.LayoutParams(-1, -2));
        state.count = label(context, root.getResources().getString(R.string.summary_empty), 14, true);
        state.count.setId(R.id.summaryItemCount);
        panel.addView(state.count);
        state.bar = new LinearProgressIndicator(context);
        state.bar.setId(R.id.summaryAccuracyBar);
        state.bar.setMax(100);
        state.bar.setTrackThickness(dp(panel, 6));
        state.bar.setTrackCornerRadius(dp(panel, 3));
        state.bar.setIndicatorColor(color(panel, androidx.appcompat.R.attr.colorPrimary));
        state.bar.setTrackColor(color(panel, com.google.android.material.R.attr.colorSurfaceContainerHighest));
        state.bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        final TableLayout.LayoutParams barParams = new TableLayout.LayoutParams(-1, dp(panel, 6));
        barParams.topMargin = dp(panel, 12);
        barParams.bottomMargin = dp(panel, 12);
        panel.addView(state.bar, barParams);
        state.breakdown = new LinearLayout(context);
        state.breakdown.setId(R.id.summaryBreakdown);
        state.breakdown.setOrientation(LinearLayout.VERTICAL);
        panel.addView(state.breakdown);
        if (!large) {
            final LinearLayout header = row(context);
            header.addView(label(context, context.getString(R.string.summary_subject), 14, true), weighted(2));
            header.addView(label(context, context.getString(R.string.summary_correct), 14, true), weighted(1));
            header.addView(label(context, context.getString(R.string.summary_errors), 14, true), weighted(1));
            state.breakdown.addView(header);
        }
        for (int i = 0; i < 4; i++) {
            final LinearLayout subjectRow = row(context);
            subjectRow.setOrientation(large ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            subjectRow.setPadding(0, dp(panel, 6), 0, dp(panel, 6));
            subjectRow.addView(label(context, SUBJECTS[i], 16, false), large
                    ? new LinearLayout.LayoutParams(-1, -2) : weighted(2));
            styleText(state.correct[i], 16, false);
            styleText(state.errors[i], 16, false);
            if (large) {
                final LinearLayout values = row(context);
                values.addView(labeledCount(context, R.string.summary_correct, state.correct[i]), weighted(1));
                values.addView(labeledCount(context, R.string.summary_errors, state.errors[i]), weighted(1));
                subjectRow.addView(values);
            } else {
                subjectRow.addView(state.correct[i], weighted(1));
                subjectRow.addView(state.errors[i], weighted(1));
            }
            subjectRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            for (int j = 0; j < subjectRow.getChildCount(); j++) {
                subjectRow.getChildAt(j).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
            state.rows[i] = subjectRow;
            state.breakdown.addView(subjectRow);
        }
        root.findViewById(R.id.incorrectSummary).setVisibility(View.GONE);
        for (int id : new int[]{R.id.correctHeader, R.id.incorrectHeader, R.id.resurrectIncorrectButton}) {
            final View view = root.findViewById(id);
            final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) view.getLayoutParams();
            params.width = 0;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            view.setLayoutParams(params);
        }
        // Existing optional bulk actions must also fit when large text is selected.
        for (int id : new int[]{R.id.correctStarSpinnerRow, R.id.incorrectStarSpinnerRow}) {
            final LinearLayout row = root.findViewById(id);
            row.setOrientation(large ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) row.getLayoutParams();
            params.width = 0;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            row.setLayoutParams(params);
        }
    }

    /** Counts follow the legacy rule: any wrong answer marks the entire subject as having errors. */
    public static void bind(View root, int[] correct, int[] totals) {
        if (!ModernUi.isModern(root.getContext())) return;
        prepare(root);
        final View panel = root.findViewById(R.id.correctSummary);
        if (panel == null) return;
        final State state = (State) panel.getTag(R.id.summaryPresentationState);
        int total = 0;
        int correctTotal = 0;
        for (int i = 0; i < 4; i++) {
            total += totals[i];
            correctTotal += correct[i];
            final int errors = totals[i] - correct[i];
            state.correct[i].setText(String.valueOf(correct[i]));
            state.errors[i].setText(String.valueOf(errors));
            state.rows[i].setVisibility(totals[i] == 0 ? View.GONE : View.VISIBLE);
            state.rows[i].setContentDescription(root.getResources().getString(
                    R.string.summary_count_accessibility, SUBJECTS[i], correct[i], errors));
        }
        final int percentage = total == 0 ? 0 : Math.round(100f * correctTotal / total);
        state.percentage.setText(total == 0 ? "—" : String.format(Locale.ROOT, "%d%%", percentage));
        state.count.setText(total == 0 ? root.getResources().getString(R.string.summary_empty)
                : root.getResources().getQuantityString(R.plurals.summary_count, total, correctTotal, total, total - correctTotal));
        state.bar.setProgressCompat(percentage, false);
        state.bar.setVisibility(total == 0 ? View.GONE : View.VISIBLE);
        state.breakdown.setVisibility(total == 0 ? View.GONE : View.VISIBLE);
    }

    private static LinearLayout labeledCount(Context context, int label, TextView count) {
        final LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(SummaryPresentation.label(context, context.getString(label), 14, true));
        column.addView(count);
        return column;
    }

    private static LinearLayout row(Context context) {
        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private static LinearLayout.LayoutParams weighted(int weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    private static TextView label(Context context, String text, int sp, boolean muted) {
        final TextView view = new TextView(context);
        view.setText(text);
        styleText(view, sp, muted);
        return view;
    }

    private static void styleText(TextView view, int sp, boolean muted) {
        view.setTextSize(sp);
        view.setTextColor(color(view, muted ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : com.google.android.material.R.attr.colorOnSurface));
        view.setShadowLayer(0, 0, 0, 0);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        view.setPadding(0, 0, dp(view, 4), 0);
    }

    private static void detach(View view) {
        ((ViewGroup) view.getParent()).removeView(view);
    }

    private static int color(View view, int attr) { return MaterialColors.getColor(view, attr); }
    private static int dp(View view, int dp) { return Math.round(view.getResources().getDisplayMetrics().density * dp); }
}
