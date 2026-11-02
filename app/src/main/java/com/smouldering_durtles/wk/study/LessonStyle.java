package com.smouldering_durtles.wk.study;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.ui.ModernUi;
import com.smouldering_durtles.wk.views.SubjectInfoButtonView;
import com.smouldering_durtles.wk.views.SubjectInfoHeadlineView;
import com.smouldering_durtles.wk.views.SubjectInfoView;
import java.util.ArrayList;
import java.util.List;
import static com.smouldering_durtles.wk.util.ViewUtil.getNearestEnclosingViewOfType;

/** Lesson-only presentation. Existing views, visibility rules, and listeners own study behavior. */
public final class LessonStyle {
    private LessonStyle() { }
    private static int dp(View view, int value) { return ModernUi.dp(view, value); }
    private static boolean isLesson(SubjectInfoView view) {
        return ModernUi.isModern(view.getContext())
                && view.getContainerType() == SubjectInfoView.ContainerType.LESSON_PRESENTATION;
    }

    public static void navigation(View root, int position, int total) {
        if (!ModernUi.isModern(root.getContext())) return;
        LinearLayout page = (LinearLayout) root;
        Button previous = root.findViewById(R.id.previousButton);
        Button next = root.findViewById(R.id.nextButton);
        TextView progress = root.findViewById(R.id.progress);
        LinearLayout footer = (LinearLayout) next.getParent();
        root.findViewById(R.id.subjectInfo).setPadding(dp(root, 16), dp(root, 8), dp(root, 16), dp(root, 16));
        if (footer.getId() != R.id.lessonNavigation) {
            footer.removeAllViews();
            footer.setId(R.id.lessonNavigation);
            footer.setPadding(dp(root, 16), dp(root, 8), dp(root, 16), dp(root, 8));
            footer.setGravity(Gravity.CENTER_VERTICAL);
            footer.setBackgroundColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurfaceContainerLow));
            LinearLayout.LayoutParams previousParams = new LinearLayout.LayoutParams(0, -2, 1);
            previousParams.setMarginEnd(dp(root, 8));
            footer.addView(previous, previousParams);
            footer.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
            page.removeView(footer);
            page.addView(footer, new LinearLayout.LayoutParams(-1, -2));
            progress.setPadding(dp(root, 16), dp(root, 8), dp(root, 16), dp(root, 4));
            progress.setTextColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurfaceVariant));
            progress.setGravity(Gravity.START);
            page.addView(progress, 0, new LinearLayout.LayoutParams(-1, -2));
        }
        ModernUi.button(previous, ModernUi.ButtonRole.TEXT);
        ModernUi.button(next, ModernUi.ButtonRole.PRIMARY);
        previous.setSingleLine(false);
        next.setSingleLine(false);
        previous.setMinWidth(0);
        next.setMinWidth(0);
        progress.setText(root.getResources().getString(R.string.lesson_position, position, total));
    }

    /** Called after every headline bind, including a reveal or same-subject refresh. */
    public static void headline(SubjectInfoHeadlineView view, Subject subject, int maxFontSize) {
        SubjectInfoView parent = getNearestEnclosingViewOfType(view, SubjectInfoView.class);
        if (parent == null || !isLesson(parent)) return;
        SubjectInfoButtonView glyph = view.findViewById(R.id.button);
        TextView title = view.findViewById(R.id.title);
        TextView meaning = view.findViewById(R.id.meaning);
        TextView reading = view.findViewById(R.id.reading);
        LinearLayout actions = view.findViewById(R.id.buttonsColumn);
        List<View> pitchViews = new ArrayList<>();
        for (int i = 0; i < view.getChildCount(); i++) {
            View child = view.getChildAt(i);
            if (child != glyph && child != title && child != meaning && child != reading && child != actions) pitchViews.add(child);
        }
        view.removeAllViews();
        view.setPadding(dp(view, 12), dp(view, 12), dp(view, 12), dp(view, 12));
        glyph.setPadding(dp(view, 8), dp(view, 8), dp(view, 8), dp(view, 8));
        glyph.setTextShadowEnabled(false);
        title.setTextColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant));
        // Preserve the headline's accepted/primary reading spans. The body lists all readings.
        LinearLayout text = new LinearLayout(view.getContext());
        text.setOrientation(LinearLayout.VERTICAL);
        for (TextView label : new TextView[]{title, meaning, reading}) {
            label.setGravity(Gravity.START);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = label == title ? 0 : dp(view, 4);
            text.addView(label, params);
        }
        for (View pitch : pitchViews) text.addView(pitch, new LinearLayout.LayoutParams(-1, -2));
        AdaptiveHeadlineRow row = new AdaptiveHeadlineRow(view.getContext(), glyph, text, Math.min(maxFontSize, 72));
        row.setId(R.id.lessonHeadlineRow);
        LinearLayout content = new LinearLayout(view.getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(row, new LinearLayout.LayoutParams(-1, -2));
        actions.setOrientation(LinearLayout.VERTICAL);
        boolean visible = false;
        int audioCount = 0;
        for (int i = 0; i < actions.getChildCount(); i++) if (actions.getChildAt(i).getId() != R.id.revealButton) audioCount++;
        for (int i = 0; i < actions.getChildCount(); i++) {
            Button action = (Button) actions.getChildAt(i);
            boolean reveal = action.getId() == R.id.revealButton;
            ModernUi.button(action, reveal ? ModernUi.ButtonRole.SECONDARY : ModernUi.ButtonRole.TEXT);
            action.setSingleLine(false);
            for (android.graphics.drawable.Drawable[] icons : new android.graphics.drawable.Drawable[][] {
                    action.getCompoundDrawables(), action.getCompoundDrawablesRelative()}) {
                for (android.graphics.drawable.Drawable icon : icons) {
                    if (icon != null) androidx.core.graphics.drawable.DrawableCompat.setTintList(icon.mutate(), action.getTextColors());
                }
            }
            action.setGravity(reveal ? Gravity.CENTER : Gravity.START | Gravity.CENTER_VERTICAL);
            action.setCompoundDrawablePadding(dp(view, 8));
            if (!reveal && audioCount == 1) {
                action.setContentDescription(view.getResources().getString(R.string.lesson_listen_subject, subject.getCharacters()));
                action.setText(R.string.lesson_listen);
            }
            action.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            visible |= action.getVisibility() == View.VISIBLE;
        }
        actions.setVisibility(visible ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = dp(view, 8);
        text.addView(actions, actionParams);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, -2);
        params.startToStart = params.endToEnd = params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        view.addView(content, params);
    }

    /** Metadata disclosure is applied after the existing binding establishes permitted visibility. */
    public static void details(SubjectInfoView view) {
        if (!isLesson(view)) return;
        for (int i = 0; i < view.getChildCount(); i++) {
            View child = view.getChildAt(i);
            if (child instanceof com.smouldering_durtles.wk.views.DividerView) {
                child.setBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutlineVariant));
            }
        }
        View table = view.findViewById(R.id.detailsTable);
        boolean available = table.getVisibility() == View.VISIBLE;
        view.setTag(R.id.lessonDetailsAvailable, available);
        Button toggle = view.findViewById(R.id.lessonDetailsToggle);
        if (toggle == null) {
            toggle = new Button(view.getContext());
            toggle.setId(R.id.lessonDetailsToggle);
            view.addView(toggle, view.indexOfChild(table), new LinearLayout.LayoutParams(-1, -2));
            toggle.setOnClickListener(button -> restoreDetails(view, !isDetailsExpanded(view)));
        }
        toggle.setVisibility(available ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.detailsDivider).setVisibility(View.GONE);
        restoreDetails(view, isDetailsExpanded(view));
        TextView add = view.findViewById(R.id.addSynonymButton);
        add.setText(R.string.lesson_add);
        add.setTextColor(MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary));
        StudyStyle.semanticSurface(add, Color.TRANSPARENT, 12, true);
        add.setPadding(dp(view, 12), 0, dp(view, 12), 0);
        // Keep the synonyms TextView visible: addSynonym updates it in place after its dialog.
    }

    public static boolean isDetailsExpanded(View root) {
        SubjectInfoView info = root instanceof SubjectInfoView ? (SubjectInfoView) root : root.findViewById(R.id.subjectInfo);
        return info != null && Boolean.TRUE.equals(info.getTag(R.id.lessonDetailsExpanded));
    }

    public static void restoreDetails(View root, boolean expanded) {
        SubjectInfoView info = root instanceof SubjectInfoView ? (SubjectInfoView) root : root.findViewById(R.id.subjectInfo);
        if (info == null || !isLesson(info)) return;
        info.setTag(R.id.lessonDetailsExpanded, expanded);
        Button toggle = info.findViewById(R.id.lessonDetailsToggle);
        if (toggle == null) return;
        toggle.setText(expanded ? R.string.lesson_hide_details : R.string.lesson_details);
        ModernUi.button(toggle, ModernUi.ButtonRole.TEXT);
        toggle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        toggle.setActivated(expanded);
        androidx.core.view.ViewCompat.setStateDescription(toggle, info.getResources().getString(
                expanded ? R.string.lesson_expanded : R.string.lesson_collapsed));
        info.findViewById(R.id.detailsTable).setVisibility(expanded
                && Boolean.TRUE.equals(info.getTag(R.id.lessonDetailsAvailable)) ? View.VISIBLE : View.GONE);
    }

    /** Uses actual measured space rather than display width; long glyphs and large text stack. */
    private static final class AdaptiveHeadlineRow extends LinearLayout {
        private final SubjectInfoButtonView glyph;
        private final LinearLayout text;
        private final int fontLimit;
        private int glyphWidthLimit = -1;
        AdaptiveHeadlineRow(Context context, SubjectInfoButtonView glyph, LinearLayout text, int fontLimit) {
            super(context);
            this.glyph = glyph;
            this.text = text;
            this.fontLimit = fontLimit;
            setGravity(Gravity.CENTER_VERTICAL);
            addView(glyph, new LayoutParams(-2, -2));
            addView(text, new LayoutParams(0, -2, 1));
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int bound = Math.max(1, width - glyph.getPaddingLeft() - glyph.getPaddingRight());
            if (glyphWidthLimit != bound) {
                glyphWidthLimit = bound;
                glyph.setMaxSize(bound, Math.round(fontLimit * getResources().getDisplayMetrics().scaledDensity));
            }
            int glyphWidth = 0;
            if (glyph.getVisibility() != GONE) {
                glyph.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                glyphWidth = glyph.getMeasuredWidth();
            }
            int gap = glyphWidth == 0 ? 0 : dp(this, 12);
            int readableText = Math.round(dp(this, 136) * getResources().getConfiguration().fontScale);
            boolean inline = glyphWidth + gap + readableText <= width;
            setOrientation(inline ? HORIZONTAL : VERTICAL);
            LayoutParams params = (LayoutParams) text.getLayoutParams();
            params.width = inline ? 0 : LayoutParams.MATCH_PARENT;
            params.weight = inline ? 1 : 0;
            params.setMarginStart(inline ? gap : 0);
            params.topMargin = inline ? 0 : dp(this, 8);
            params.resolveLayoutDirection(getLayoutDirection());
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}
