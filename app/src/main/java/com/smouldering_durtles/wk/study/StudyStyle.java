package com.smouldering_durtles.wk.study;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.ui.ModernUi;
import com.smouldering_durtles.wk.views.SubjectInfoHeadlineView;
import com.smouldering_durtles.wk.views.SubjectInfoView;

/** Presentation only: never changes answers, callbacks, visibility, or the session model. */
public final class StudyStyle {
    private StudyStyle() {}

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    public static void page(View view) {
        if (!ModernUi.isModern(view.getContext())) return;
        ModernUi.style(view);
        view.setBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface));
        final View input = view.findViewById(R.id.questionEdit);
        if (input instanceof android.widget.EditText) {
            input.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            if (input.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) input.getLayoutParams()).setMargins(dp(input, 16), dp(input, 8), dp(input, 16), dp(input, 12));
            }
        }
        final View info = view.findViewById(R.id.subjectInfo);
        if (info != null) info.setPadding(dp(info, 16), dp(info, 16), dp(info, 16), dp(info, 24));
    }

    /** Round a semantic surface without replacing its established foreground/background palette. */
    public static void semanticSurface(View view, int color, int radius, boolean ripple) {
        if (view == null || !ModernUi.isModern(view.getContext())) return;
        final MaterialShapeDrawable shape = new MaterialShapeDrawable(ShapeAppearanceModel.builder()
                .setAllCornerSizes(dp(view, radius)).build());
        shape.setFillColor(ColorStateList.valueOf(color));
        if (ripple) {
            final int highlight = MaterialColors.getColor(view, com.google.android.material.R.attr.colorControlHighlight);
            view.setBackground(new RippleDrawable(ColorStateList.valueOf(highlight), shape, null));
        } else view.setBackground(shape);
    }

    public static void question(View root, int subjectColor) {
        if (!ModernUi.isModern(root.getContext())) return;
        final View question = root.findViewById(R.id.questionView);
        if (question != null) {
            semanticSurface(question, subjectColor, 24, false);
            question.setClipToOutline(true);
            final ViewGroup.LayoutParams lp = question.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMargins(dp(question, 12), dp(question, 8), dp(question, 12), dp(question, 8));
                question.setLayoutParams(lp);
            }
        }
        final View type = root.findViewById(R.id.questionType);
        roundCurrentColor(type, 12, false);
    }

    public static void roundCurrentColor(View view, int radius, boolean ripple) {
        if (view == null || !ModernUi.isModern(view.getContext())) return;
        final Drawable background = view.getBackground();
        if (background instanceof ColorDrawable) semanticSurface(view, ((ColorDrawable) background).getColor(), radius, ripple);
    }

    /** Reflow metadata into stacked records so dates and translated labels can wrap at large text sizes. */
    public static void prepareDetails(SubjectInfoView view) {
        if (!ModernUi.isModern(view.getContext())) return;
        view.setBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface));
        final android.widget.TableLayout table = view.findViewById(R.id.detailsTable);
        if (table != null) {
            for (int i = 0; i < table.getChildCount(); i++) {
                final View rowView = table.getChildAt(i);
                if (!(rowView instanceof android.widget.TableRow)) continue;
                final ViewGroup row = (ViewGroup) rowView;
                final android.widget.LinearLayout record = new android.widget.LinearLayout(view.getContext());
                record.setOrientation(android.widget.LinearLayout.VERTICAL);
                record.setPadding(0, dp(view, 8), 0, dp(view, 8));
                while (row.getChildCount() > 0) {
                    final View field = row.getChildAt(0);
                    row.removeViewAt(0);
                    final android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (record.getChildCount() > 0) params.topMargin = dp(view, 4);
                    if (field instanceof TextView) ((TextView) field).setGravity(android.view.Gravity.START);
                    record.addView(field, params);
                }
                table.removeViewAt(i);
                table.addView(record, i, new android.widget.TableLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            semanticSurface(table, MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerLow), 16, false);
            table.setPadding(dp(view, 16), dp(view, 8), dp(view, 16), dp(view, 8));
        }
        final TextView synonyms = view.findViewById(R.id.synonyms);
        final TextView add = view.findViewById(R.id.addSynonymButton);
        if (synonyms != null && add != null && synonyms.getParent() instanceof android.widget.LinearLayout) {
            final View row = (View) synonyms.getParent();
            row.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            synonyms.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            add.setMinWidth(dp(view, 48));
            add.setMinHeight(dp(view, 48));
            add.setGravity(android.view.Gravity.CENTER);
            add.setContentDescription(view.getContext().getString(R.string.study_add_synonym));
            add.setTextColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSecondaryContainer));
            semanticSurface(add, MaterialColors.getColor(view, com.google.android.material.R.attr.colorSecondaryContainer), 20, true);
            add.setFocusable(true);
        }
    }

    private static int contrastInk(int color) {
        return androidx.core.graphics.ColorUtils.calculateContrast(android.graphics.Color.BLACK, color)
                >= androidx.core.graphics.ColorUtils.calculateContrast(android.graphics.Color.WHITE, color)
                ? android.graphics.Color.BLACK : android.graphics.Color.WHITE;
    }

    public static void answerFeedback(TextView answer, int background) {
        if (!ModernUi.isModern(answer.getContext())) return;
        semanticSurface(answer, background, 16, false);
        answer.setTextColor(contrastInk(background));
        answer.setShadowLayer(0, 0, 0, 0);
        answer.setGravity(android.view.Gravity.CENTER);
        answer.setPadding(dp(answer, 16), dp(answer, 12), dp(answer, 16), dp(answer, 12));
        answer.setMinimumHeight(dp(answer, 48));
    }

    public static void summary(View root) {
        if (!ModernUi.isModern(root.getContext())) return;
        for (int id : new int[]{R.id.correctSummary, R.id.incorrectSummary}) {
            final View panel = root.findViewById(id);
            if (panel == null || !(panel.getBackground() instanceof ColorDrawable)) continue;
            final int color = ((ColorDrawable) panel.getBackground()).getColor();
            semanticSurface(panel, color, 24, false);
            panel.setPadding(dp(panel, 16), dp(panel, 12), dp(panel, 16), dp(panel, 12));
            contrastText(panel, contrastInk(color));
            if (panel instanceof android.widget.TableLayout) {
                ((android.widget.TableLayout) panel).setColumnShrinkable(0, true);
                ((android.widget.TableLayout) panel).setColumnStretchable(0, true);
                final ViewGroup rows = (ViewGroup) panel;
                for (int i = 0; i < rows.getChildCount(); i++) {
                    if (!(rows.getChildAt(i) instanceof ViewGroup)) continue;
                    final ViewGroup row = (ViewGroup) rows.getChildAt(i);
                    row.setPadding(0, dp(panel, 4), 0, dp(panel, 4));
                    if (row.getChildCount() > 1) {
                        ((TextView) row.getChildAt(0)).setGravity(android.view.Gravity.START);
                        row.getChildAt(1).setPadding(dp(panel, 16), 0, 0, 0);
                    }
                }
            }
        }
    }

    private static void contrastText(View view, int ink) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(ink);
            ((TextView) view).setShadowLayer(0, 0, 0, 0);
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) contrastText(group.getChildAt(i), ink);
        }
    }

    /** Log entries keep subject colors while their status and event text follow actual fill contrast. */
    public static void logRow(View row, int color) {
        if (!ModernUi.isModern(row.getContext())) return;
        semanticSurface(row, color, 16, row.hasOnClickListeners());
        row.setElevation(0);
        row.setPadding(dp(row, 12), dp(row, 12), dp(row, 12), dp(row, 12));
        row.setMinimumHeight(dp(row, 56));
        row.setFocusable(row.hasOnClickListeners());
        for (int id : new int[]{R.id.questionMark, R.id.question1Status, R.id.question2Status,
                R.id.question3Status, R.id.question4Status, R.id.age, R.id.details}) {
            final TextView text = row.findViewById(id);
            if (text == null) continue;
            text.setTextColor(contrastInk(color));
            text.setShadowLayer(0, 0, 0, 0);
            text.setTextSize(Math.max(14, text.getTextSize() / row.getResources().getDisplayMetrics().scaledDensity));
        }
    }

    public static void logHeader(View row) {
        if (!ModernUi.isModern(row.getContext())) return;
        final int color = MaterialColors.getColor(row, com.google.android.material.R.attr.colorSurfaceContainerLow);
        semanticSurface(row, color, 16, true);
        row.setPadding(dp(row, 8), dp(row, 12), dp(row, 12), dp(row, 12));
        row.setMinimumHeight(dp(row, 56));
        row.setFocusable(true);
        final int ink = MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface);
        for (int id : new int[]{R.id.title, R.id.details}) ((TextView) row.findViewById(id)).setTextColor(ink);
        final TextView title = row.findViewById(R.id.title);
        // A weighted title can wrap instead of pushing the count beyond the screen.
        final android.widget.LinearLayout.LayoutParams titleParams = (android.widget.LinearLayout.LayoutParams) title.getLayoutParams();
        titleParams.width = 0;
        titleParams.weight = 1;
        title.setLayoutParams(titleParams);
        final View details = row.findViewById(R.id.details);
        final android.widget.LinearLayout.LayoutParams detailParams = (android.widget.LinearLayout.LayoutParams) details.getLayoutParams();
        detailParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        detailParams.weight = 0;
        details.setLayoutParams(detailParams);
    }

    public static void details(SubjectInfoView view) {
        if (!ModernUi.isModern(view.getContext())) return;
        // Keep semantic rich-text spans, custom font size, subject grids, and pitch/stroke drawings intact.
        for (int i = 0; i < view.getChildCount(); i++) {
            final View child = view.getChildAt(i);
            if (child instanceof TextView) {
                final TextView text = (TextView) child;
                text.setLineSpacing(dp(view, 3), 1f);
                if (text.getTypeface() != null && text.getTypeface().isBold()) {
                    text.setTypeface(text.getTypeface(), Typeface.BOLD);
                    text.setTextSize(Math.max(18, text.getTextSize() / view.getResources().getDisplayMetrics().scaledDensity));
                    if (text.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) text.getLayoutParams()).topMargin = dp(view, 20);
                    }
                }
            }
        }
        for (int id : new int[]{R.id.addSynonymButton, R.id.resurrectButton, R.id.burnButton}) {
            View button = view.findViewById(id);
            if (button != null) ModernUi.style(button);
        }
    }

    public static void headline(SubjectInfoHeadlineView view) {
        if (!ModernUi.isModern(view.getContext())) return;
        semanticSurface(view, MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerLow), 24, false);
        view.setPadding(dp(view, 16), dp(view, 16), dp(view, 16), dp(view, 16));
        View buttons = view.findViewById(R.id.buttonsColumn);
        if (buttons != null) ModernUi.style(buttons);
    }
}
