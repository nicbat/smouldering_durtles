package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.ui.ModernUi;
import java.text.NumberFormat;

/** Exact new arrivals by hour. Noninteractive rows keep time, bar and count aligned. */
public final class HourlyReviewChart extends LinearLayout {
    public HourlyReviewChart(Context context, String[] hours, int[] counts, int availableNow) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(ModernUi.dp(this, 24), ModernUi.dp(this, 8), ModernUi.dp(this, 24), ModernUi.dp(this, 8));
        if (availableNow > 0) {
            TextView available = label(getContext().getString(R.string.forecast_available_now, availableNow));
            available.setPadding(0, 0, 0, ModernUi.dp(this, 12));
            addView(available, new LayoutParams(-1, -2));
        }
        int max = 1;
        for (int count : counts) max = Math.max(max, count);
        // Reserve enough label space for the widest localized hour and exact count.
        float hourWidth = 0, countWidth = 0;
        TextView sample = label("");
        for (int i = 0; i < hours.length; i++) {
            hourWidth = Math.max(hourWidth, sample.getPaint().measureText(hours[i]));
            countWidth = Math.max(countWidth, sample.getPaint().measureText(NumberFormat.getIntegerInstance().format(counts[i])));
        }
        for (int i = 0; i < hours.length; i++) {
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, ModernUi.dp(this, 5), 0, ModernUi.dp(this, 5));
            TextView hour = label(hours[i]);
            row.addView(hour, new LayoutParams((int) Math.ceil(hourWidth) + ModernUi.dp(this, 8), -2));
            LoadBar bar = new LoadBar(context, counts[i], max);
            LayoutParams barParams = new LayoutParams(0, ModernUi.dp(this, 6), 1);
            barParams.setMarginEnd(ModernUi.dp(this, 8));
            row.addView(bar, barParams);
            TextView count = label(NumberFormat.getIntegerInstance().format(counts[i]));
            count.setGravity(Gravity.END);
            row.addView(count, new LayoutParams((int) Math.ceil(countWidth), -2));
            row.setContentDescription(getContext().getString(R.string.forecast_hour_count, hours[i], counts[i]));
            for (int n = 0; n < row.getChildCount(); n++) row.getChildAt(n).setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(row, new LayoutParams(-1, -2));
        }
    }
    private TextView label(String text) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        return view;
    }
    private static final class LoadBar extends View {
        final int count, max;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LoadBar(Context context, int count, int max) { super(context); this.count = count; this.max = max; }
        @Override protected void onDraw(Canvas canvas) {
            float radius = getHeight() / 2f;
            paint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHighest));
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, paint);
            if (count > 0) {
                paint.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
                canvas.drawRoundRect(0, 0, Math.max(getHeight(), getWidth() * count / (float) max), getHeight(), radius, radius, paint);
            }
        }
    }
}
