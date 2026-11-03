package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.ui.LearningColors;

/** Decorative proportions; the adjacent accessible legend retains exact counts and actions. */
public final class SrsDistributionBarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] values = new int[5];
    private int[] customColors;
    public void setColors(int[] colors) { customColors = colors == null ? null : colors.clone(); invalidate(); }
    public SrsDistributionBarView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setValues(int[] counts) { values = counts.clone(); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int track = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHighest);
        paint.setColor(track);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        long total = 0;
        for (int count : values) total += Math.max(0, count);
        if (total == 0) return;
        int[] colors = customColors == null ? ActiveTheme.getShallowStageBucketColors5() : customColors;
        long done = 0;
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        for (int i = 0; i < Math.min(values.length, colors.length); i++) {
            float start = (float) ((double) done * getWidth() / total);
            done += Math.max(0, values[i]);
            float end = (float) ((double) done * getWidth() / total);
            paint.setColor(LearningColors.accent(getContext(), colors[i]));
            canvas.drawRect(rtl ? getWidth() - end : start, 0,
                    rtl ? getWidth() - start : end, getHeight(), paint);
            if (start > 0 && end > start && "neutral".equals(GlobalSettings.Display.getLearningColors())) {
                // Snap the thin divider to whole pixels so neutral boundaries stay crisp.
                int dividerStart = Math.round(start);
                int dividerEnd = Math.min(Math.round(end), dividerStart
                        + Math.max(1, Math.round(getResources().getDisplayMetrics().density)));
                paint.setColor(track);
                canvas.drawRect(rtl ? getWidth() - dividerEnd : dividerStart, 0,
                        rtl ? getWidth() - dividerStart : dividerEnd, getHeight(), paint);
            }
        }
    }
}
