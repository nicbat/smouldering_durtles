package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.view.View;
import androidx.core.view.ViewCompat;
import com.smouldering_durtles.wk.Actment;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.ui.LearningColors;
import com.smouldering_durtles.wk.ui.ModernUi;
import java.util.List;
import androidx.arch.core.util.Function;

/** Latest subjects in two compact rows on a typical phone; complete names stay accessible. */
public final class RecentSubjectGrid extends android.view.ViewGroup {
    private int columns = 1;
    private int cellWidth;
    private int rowHeight;
    private final int gap;
    public RecentSubjectGrid(Context context, List<Subject> subjects, Actment actment,
                             Function<Subject, String> extra) {
        super(context);
        setId(R.id.recentSubjectGrid);
        gap = ModernUi.dp(this, 8);
        long[] ids = new long[subjects.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = subjects.get(i).getId();
        for (Subject subject : subjects) {
            View glyph;
            if (subject.getType().isVocabulary()) {
                android.widget.TextView text = new android.widget.TextView(context);
                text.setText(subject.getCharacters());
                text.setTextSize(16);
                text.setTextLocale(java.util.Locale.JAPANESE);
                text.setMaxLines(2);
                text.setEllipsize(android.text.TextUtils.TruncateAt.END);
                text.setGravity(android.view.Gravity.CENTER);
                text.setTextColor(LearningColors.ink(context, subject.getTextColor()));
                glyph = text;
            } else {
                SubjectInfoButtonView title = new SubjectInfoButtonView(context);
                title.setSubject(subject);
                title.setSizeSp(24);
                title.setMaxSize(Integer.MAX_VALUE, Math.round(24 * getResources().getDisplayMetrics().scaledDensity));
                title.setTransparent(true);
                title.setTextShadowEnabled(false);
                glyph = title;
            }
            glyph.setMinimumHeight(ModernUi.dp(this, 48));
            glyph.setPadding(ModernUi.dp(this, 6), ModernUi.dp(this, 10), ModernUi.dp(this, 6), ModernUi.dp(this, 10));
            android.widget.FrameLayout cell = new android.widget.FrameLayout(context);
            cell.setMinimumHeight(ModernUi.dp(this, 48));
            cell.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(
                    com.google.android.material.color.MaterialColors.getColor(cell, com.google.android.material.R.attr.colorControlHighlight)),
                    LearningColors.surface(cell, subject.getBackgroundColor(), 8), null));
            cell.setFocusable(true);
            String characters = subject.getCharacters();
            String description = subject.getType().getDescription() + ": "
                    + (characters == null || characters.isEmpty() ? subject.getOneMeaning() : characters + ", " + subject.getOneMeaning())
                    + ", " + extra.apply(subject);
            cell.setContentDescription(description);
            ViewCompat.setTooltipText(cell, description);
            cell.setOnClickListener(v -> actment.goToSubjectInfo(subject.getId(), ids, FragmentTransitionAnimation.RTL));
            glyph.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            glyph.setClickable(false);
            glyph.setFocusable(false);
            cell.addView(glyph, new android.widget.FrameLayout.LayoutParams(-1, -1, android.view.Gravity.CENTER));
            addView(cell);
        }
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = View.MeasureSpec.getSize(widthSpec);
        columns = Math.max(1, Math.min(5, (int) (width / (64 * getResources().getDisplayMetrics().density
                * Math.max(1, getResources().getConfiguration().fontScale)))));
        cellWidth = Math.max(1, (width - (columns - 1) * gap) / columns);
        rowHeight = ModernUi.dp(this, 48);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
        int rows = (getChildCount() + columns - 1) / columns;
        setMeasuredDimension(width, rows * rowHeight + Math.max(0, rows - 1) * gap);
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            int column = i % columns;
            int x = column * (cellWidth + gap);
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getWidth() - x - cellWidth;
            int y = (i / columns) * (rowHeight + gap);
            getChildAt(i).layout(x, y, x + cellWidth, y + rowHeight);
        }
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-2, -2); }
}
