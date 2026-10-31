package com.smouldering_durtles.wk.study;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.Gravity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.helper.widget.Flow;
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
        reflowQuestionActions(view);
        prepareSummaryActions(view);
        final View info = view.findViewById(R.id.subjectInfo);
        if (info != null) info.setPadding(dp(info, 16), dp(info, 16), dp(info, 16), dp(info, 24));
    }

    private static void margins(View view, int start, int top, int end, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.setMargins(dp(view, start), dp(view, top), dp(view, end), dp(view, bottom));
            view.setLayoutParams(params);
        }
    }

    private static void action(Button button, boolean primary) {
        ModernUi.button(button, primary ? ModernUi.ButtonRole.PRIMARY : ModernUi.ButtonRole.TEXT);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setSingleLine(false);
    }

    /** Keep question content free of action overlays; bindings still target the same Views and IDs. */
    private static void reflowQuestionActions(View root) {
        final View question = root.findViewById(R.id.questionView);
        if (!(question instanceof ViewGroup) || !(question.getParent() instanceof LinearLayout)) return;
        final LinearLayout page = (LinearLayout) question.getParent();
        final View inputFrame = root.findViewById(R.id.questionEditFrame);
        final View feedback = root.findViewById(R.id.questionEdit);
        final View anchor = inputFrame != null ? inputFrame : feedback != null ? feedback : root.findViewById(R.id.questionType);
        if (anchor == null || anchor.getParent() != page) return;
        final LinearLayout actions = new LinearLayout(root.getContext());
        actions.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(dp(root, 16), 0, dp(root, 16), dp(root, 8));
        page.addView(actions, page.indexOfChild(anchor) + 1, rowParams);
        addActionRow(root, actions, new int[]{R.id.dontKnowButton, R.id.submitButton, R.id.nextButton});
        addActionRow(root, actions, new int[]{R.id.specialButton3, R.id.specialButton2, R.id.specialButton1});
        final View glyph = root.findViewById(R.id.questionText);
        if (glyph != null) {
            // The quiz renderer already reserves 28dp above and 24dp below the glyph.
            // Extra padding here would exceed compact user-configured question heights.
            glyph.setPadding(0, 0, 0, 0);
            if (glyph instanceof com.smouldering_durtles.wk.views.SubjectInfoButtonView)
                ((com.smouldering_durtles.wk.views.SubjectInfoButtonView) glyph).setTextShadowEnabled(false);
        }
        final Button bottomNext = root.findViewById(R.id.nextButton2);
        if (bottomNext != null) {
            action(bottomNext, true);
            bottomNext.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            margins(bottomNext, 16, 8, 16, 12);
        }
        if (inputFrame != null && page == root) {
            // The IME and large text can leave less vertical room than the configured question height.
            // Scroll the whole prompt/input/actions together instead of clipping the submit controls.
            final LinearLayout content = new LinearLayout(root.getContext());
            content.setOrientation(LinearLayout.VERTICAL);
            while (page.getChildCount() > 0) {
                final View child = page.getChildAt(0);
                page.removeViewAt(0);
                content.addView(child);
            }
            final androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(root.getContext());
            scroll.setFillViewport(true);
            scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private static void addActionRow(View root, LinearLayout container, int[] ids) {
        final LinearLayout row = new LinearLayout(root.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int id : ids) {
            final View view = root.findViewById(id);
            if (!(view instanceof Button)) continue;
            ((ViewGroup) view.getParent()).removeView(view);
            action((Button) view, id == R.id.submitButton);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (row.getChildCount() > 0) params.leftMargin = dp(root, 8);
            row.addView(view, params);
        }
        if (row.getChildCount() > 0) container.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static void prepareSummaryActions(View root) {
        final Button finish = root.findViewById(R.id.finishButton);
        if (finish == null || !(finish.getParent() instanceof ConstraintLayout)) return;
        final ConstraintLayout container = (ConstraintLayout) finish.getParent();
        container.setPadding(dp(root, 16), dp(root, 8), dp(root, 16), dp(root, 16));
        int[] ids = {R.id.specialButton3, R.id.specialButton2, R.id.specialButton1, R.id.showButton, R.id.finishButton};
        for (int id : ids) action(root.findViewById(id), id == R.id.finishButton);
        final Flow flow = new Flow(root.getContext());
        flow.setId(View.generateViewId());
        flow.setReferencedIds(ids);
        flow.setWrapMode(Flow.WRAP_CHAIN);
        flow.setHorizontalStyle(Flow.CHAIN_PACKED);
        flow.setHorizontalBias(1);
        flow.setHorizontalGap(dp(root, 8));
        flow.setVerticalGap(dp(root, 8));
        final ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        container.addView(flow, params);
        final View progress = root.findViewById(R.id.finishProgressBar);
        final ConstraintLayout.LayoutParams progressParams = (ConstraintLayout.LayoutParams) progress.getLayoutParams();
        progressParams.topToBottom = flow.getId();
        progress.setLayoutParams(progressParams);
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
            semanticSurface(question, subjectColor, 16, false);
            question.setClipToOutline(true);
            final ViewGroup.LayoutParams lp = question.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMargins(dp(question, 16), dp(question, 8), dp(question, 16), dp(question, 4));
                question.setLayoutParams(lp);
            }
        }
        final TextView type = root.findViewById(R.id.questionType);
        if (type != null) {
            // Reading/meaning remains explicit text; avoid a stark white/black capsule in either theme.
            type.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            type.setTextColor(MaterialColors.getColor(type, com.google.android.material.R.attr.colorOnSurfaceVariant));
            type.setElevation(0);
            type.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            type.setPadding(dp(type, 16), dp(type, 8), dp(type, 16), dp(type, 4));
            type.setTextSize(14);
            type.setTypeface(type.getTypeface(), Typeface.BOLD);
            type.setLabelFor(R.id.questionEdit);
        }
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
            semanticSurface(table, MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerLow), 12, false);
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
            semanticSurface(add, MaterialColors.getColor(view, com.google.android.material.R.attr.colorSecondaryContainer), 12, true);
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
        semanticSurface(answer, background, 12, false);
        margins(answer, 16, 8, 16, 8);
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
            // Summary status is an accent, not a second page-sized subject-colored surface.
            MaterialShapeDrawable surface = ModernUi.surface(panel,
                    MaterialColors.getColor(panel, com.google.android.material.R.attr.colorSurfaceContainerLow), 12);
            surface.setStroke(dp(panel, 2), color);
            panel.setBackground(surface);
            panel.setPadding(dp(panel, 16), dp(panel, 12), dp(panel, 16), dp(panel, 12));
            contrastText(panel, MaterialColors.getColor(panel, com.google.android.material.R.attr.colorOnSurface));
            if (panel instanceof android.widget.TableLayout) {
                final android.widget.TableLayout table = (android.widget.TableLayout) panel;
                for (int i = 0; i < table.getChildCount(); i++) {
                    if (!(table.getChildAt(i) instanceof android.widget.TableRow)) continue;
                    final ViewGroup old = (ViewGroup) table.getChildAt(i);
                    final LinearLayout row = new LinearLayout(root.getContext());
                    final boolean large = root.getResources().getConfiguration().fontScale >= 1.5f;
                    row.setOrientation(large ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, dp(panel, 6), 0, dp(panel, 6));
                    while (old.getChildCount() > 0) {
                        final TextView field = (TextView) old.getChildAt(0);
                        old.removeViewAt(0);
                        final boolean label = row.getChildCount() == 0;
                        field.setGravity(label || large ? Gravity.START : Gravity.END);
                        field.setPadding(0, 0, 0, 0);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                large ? ViewGroup.LayoutParams.MATCH_PARENT : label ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, !large && label ? 1 : 0);
                        if (!label) {
                            if (large) params.topMargin = dp(panel, 4);
                            else params.leftMargin = dp(panel, 16);
                        }
                        row.addView(field, params);
                    }
                    table.removeViewAt(i);
                    table.addView(row, i, new android.widget.TableLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        semanticSurface(row, color, 12, row.hasOnClickListeners());
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
        semanticSurface(row, color, 12, true);
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

    private static void flattenSubjectGlyphs(View view) {
        if (view instanceof com.smouldering_durtles.wk.views.SubjectInfoButtonView)
            ((com.smouldering_durtles.wk.views.SubjectInfoButtonView) view).setTextShadowEnabled(false);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) flattenSubjectGlyphs(group.getChildAt(i));
        }
    }

    public static void headline(SubjectInfoHeadlineView view) {
        if (!ModernUi.isModern(view.getContext())) return;
        semanticSurface(view, MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerLow), 12, false);
        view.setPadding(dp(view, 16), dp(view, 16), dp(view, 16), dp(view, 16));
        flattenSubjectGlyphs(view);
        View buttons = view.findViewById(R.id.buttonsColumn);
        if (buttons != null) ModernUi.style(buttons);
    }
}
