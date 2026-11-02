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

package com.smouldering_durtles.wk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.LifecycleOwner;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.dashboard.DashboardStyle;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smouldering_durtles.wk.ui.LearningColors;
import com.smouldering_durtles.wk.enums.SubjectType;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.livedata.LiveVacationMode;
import com.smouldering_durtles.wk.model.SrsSystem;
import com.smouldering_durtles.wk.model.TimeLine;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.FONT_SIZE_NORMAL;
import static com.smouldering_durtles.wk.Constants.HOUR;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.TextUtil.formatShortTimeForDisplay;

/**
 * Custom bar chart for the timeline.
 */
public final class TimeLineBarChart extends View implements GestureDetector.OnGestureListener {
    private final List<BarEntry> entries = new ArrayList<>();
    private int[] segmentColors = new int[0];
    private String[] legendLabels = new String[0];
    private int maxBarCount = 0;
    private int totalCount = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect rect = new Rect();
    private int colorPrimary = 0;
    private int colorPrimaryTonedDown = 0;
    private int colorWaterfall = 0;
    private float density = 1.0f;
    private float labelDensity = 1.0f;
    private boolean modern;
    // Separate measured text bands keep labels out of the plot, including at 2x font scale.
    private final List<RectF> modernTextBounds = new ArrayList<>();
    private final List<DayEntry> days = new ArrayList<>();
    private final List<RectF> dayBounds = new ArrayList<>();
    private static final int OVERVIEW_DAYS = 7;
    private float originX = 0;
    private float originY = 0;
    private float axisSizeX = 0;
    private float axisSizeY = 0;
    private float pixelsPerUnit = 0;
    private float pixelsPerUnitCumulative = 0;
    private float pixelsPerUnitGrid = 0;
    private int axisIntervalY = 0;
    private float barAdvance = 0;
    private int numShownBars = 24;
    private float scrollOffset = 0;
    private @Nullable GestureDetectorCompat gestureDetector = null;
    private @Nullable Scroller scroller = null;
    private float prevVerticalScrollRawY = 0;
    private long firstSlot = 0;
    private @Nullable Drawable arrowIcon = null;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public TimeLineBarChart(final Context context) {
        this(context, null, 0);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public TimeLineBarChart(final Context context, final @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     * @param defStyle default style
     */
    public TimeLineBarChart(final Context context, final @Nullable AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        safe(() -> {
            colorPrimary = ThemeUtil.getColor(getContext(), androidx.appcompat.R.attr.colorPrimary);
            colorPrimaryTonedDown = ThemeUtil.getColor(getContext(), R.attr.colorPrimaryTonedDown);
            colorWaterfall = ThemeUtil.getColor(getContext(), R.attr.colorWaterfallLine);
            density = context.getResources().getDisplayMetrics().density;
            modern = DashboardStyle.isModern(context);
            labelDensity = modern ? context.getResources().getDisplayMetrics().scaledDensity : density;
            if (modern) {
                colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
                colorPrimaryTonedDown = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
            }
            arrowIcon = ContextCompat.getDrawable(getContext(), ActiveTheme.getCurrentTheme().getLevelUpArrowDrawableId());
            if (modern && arrowIcon != null) arrowIcon.mutate().setTint(colorPrimaryTonedDown);
            init();
            if (modern) {
                setClickable(true);
                setFocusable(true);
                setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        });
    }

    /**
     * Initialize the view.
     */
    private void init() {
        safe(() -> {
            setBackgroundResource(R.drawable.main_activity_view_background);
            setPadding(0, (int) (4 * density), 0, (int) (4 * density));
            setNestedScrollingEnabled(true);
        });
    }

    /**
     * Set the lifecycle owner for this view, to hook LiveData observers to.
     *
     * @param lifecycleOwner the lifecycle owner
     */
    public void setLifecycleOwner(final LifecycleOwner lifecycleOwner) {
        safe(() -> LiveTimeLine.getInstance().observe(lifecycleOwner, t -> safe(() -> update(t))));
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        safe(() -> {
            if (modern) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                if (width == 0) width = (int) (320 * density);
                setMeasuredDimension(width, resolveSize(modernHeight(width), heightMeasureSpec));
                return;
            }
            setMeasuredDimension(0, 0);

            final int w;
            final int h;
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
                w = MeasureSpec.getSize(widthMeasureSpec);
                h = MeasureSpec.getSize(heightMeasureSpec);
            }
            else if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
                w = Math.min(
                        MeasureSpec.getSize(widthMeasureSpec),
                        getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight()
                );
                h = Math.min(
                        MeasureSpec.getSize(heightMeasureSpec),
                        getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom()
                );
            }
            else {
                w = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
                h = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
            }

            setMeasuredDimension(w, h);
        });
    }

    /**
     * Round up an interval to the nearest 'pretty' number: an even power of ten,
     * times 1, 2, 3, 4 or 5.
     *
     * @param value the value to round up
     * @return the rounded value
     */
    private static int roundUpInterval(final int value) {
        if (value <= 0) {
            return 1;
        }
        if (value < 5) {
            return value;
        }
        if (value < 10) {
            return 5;
        }
        return roundUpInterval((value + 9) / 10) * 10;
    }

    /**
     * Get the time label for a bar, identified by its 0-based index.
     *
     * @param index the timeslot
     * @return the formatted label
     */
    @SuppressLint("NewApi")
    private String getBarLabel(final int index) {
        return formatShortTimeForDisplay(firstSlot + index * HOUR, index >= 24);
    }

    /**
     * Draw the background grid for the chart, including axis labels.
     *
     * @param canvas the canvas to draw on
     */
    private void drawGrid(final Canvas canvas) {
        paint.setColor(colorPrimary);
        paint.setStrokeWidth(0);
        canvas.drawLine(originX, originY, originX, originY - axisSizeY, paint);

        paint.setTextSize(labelDensity * 10);
        paint.setTextAlign(Paint.Align.RIGHT);

        if (axisIntervalY == 0) {
            canvas.drawLine(originX, originY, originX + axisSizeX, originY, paint);
        }
        else {
            int level = 0;
            while (true) {
                final float offset = level * pixelsPerUnitGrid;
                if (offset >= axisSizeY) {
                    break;
                }
                canvas.drawLine(originX, originY - offset, originX + axisSizeX, originY - offset, paint);
                if (level > 0) {
                    canvas.drawText(Integer.toString(level), originX - density * 4, originY - offset - (paint.ascent() + paint.descent()) / 2, paint);
                }
                level += axisIntervalY;
            }
        }
    }

    @SuppressLint("NewApi")
    private List<BarEntry> getSortedEntries() {
        final List<BarEntry> sortedEntries = new ArrayList<>(entries);
        Collections.sort(sortedEntries,
                Comparator.<BarEntry>comparingInt(entry -> entry.barCount)
                        .thenComparingInt(entry -> entry.index));
        return sortedEntries;
    }

    /**
     * Draw the actual bars for the chart, including labels, level-up markers, and the waterfall line.
     *
     * @param canvas the canvas to draw on
     */
    private void drawBars(final Canvas canvas) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0);
        paint.setTextSize(labelDensity * 10);
        paint.setTextAlign(Paint.Align.CENTER);

        // The actual bars
        for (int i=0; i<entries.size(); i++) {
            final BarEntry entry = entries.get(i);
            final float left = Math.max(originX, originX + barAdvance * i + barAdvance * 0.075f - scrollOffset);
            final float right = Math.min(originX + axisSizeX, originX + barAdvance * i + barAdvance * 0.925f - scrollOffset);
            if (right < originX || left > originX + axisSizeX || left > right) {
                continue;
            }
            float bottom = originY;
            for (int j=0; j<entry.values.length; j++) {
                paint.setColor(segmentColors[j]);
                final float top = bottom - entry.values[j] * pixelsPerUnit;
                canvas.drawRect(left, top, right, bottom, paint);
                bottom = top;
            }
        }

        paint.setColor(colorPrimary);

        final List<RectF> labels = new ArrayList<>();
        final List<BarEntry> sortedEntries = getSortedEntries();

        // The count above each bar
        for (final BarEntry entry: sortedEntries) {
            final int i = entry.index;

            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            final float y = originY - entry.barCount * pixelsPerUnit - density * 1 - paint.descent();
            if (entry.barCount == 0 || x < originX || x > originX + axisSizeX) {
                continue;
            }

            final String text = Integer.toString(entry.barCount);
            final float textWidth = paint.measureText(text);
            final RectF rectf = new RectF(x - textWidth/2 - density, y + paint.ascent() - density,
                    x + textWidth/2 + density, y + paint.descent() + density);

            while (!labels.isEmpty() && labels.get(0).bottom <= rectf.top) {
                labels.remove(0);
            }
            boolean ok = true;
            for (final RectF r: labels) {
                if (RectF.intersects(r, rectf)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }

            canvas.drawText(text, x, y, paint);
            labels.add(rectf);
        }

        paint.setTextSize(labelDensity * 9);
        paint.setColor(colorPrimaryTonedDown);

        // The count of level-up items, including the arrow icons
        for (int i=0; i<entries.size(); i++) {
            final BarEntry entry = entries.get(i);

            final int y = getPaddingTop();
            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            if (entry.numLevelUpItems == 0 || arrowIcon == null || x < originX || x > originX + axisSizeX) {
                continue;
            }

            arrowIcon.setBounds((int) (x - density * 6), y, (int) (x + density * 6), (int) (y + density * 9));
            arrowIcon.draw(canvas);
        }

        float lastX = -1;

        for (int i=0; i<entries.size(); i++) {
            final BarEntry entry = entries.get(i);

            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            if (entry.numLevelUpItems == 0 || arrowIcon == null || x < originX || x > originX + axisSizeX) {
                continue;
            }

            final String text = Integer.toString(entry.numLevelUpItems);
            final float textWidth = paint.measureText(text);
            if (x - textWidth/2 - density < lastX) {
                continue;
            }
            lastX = x + textWidth/2 + density;

            canvas.drawText(text, x, getPaddingTop() - paint.ascent(), paint);
        }

        paint.setColor(colorPrimary);
        paint.setTextSize(labelDensity * 10);

        // The time labels on the X axis
        int labelStep = modern ? Math.max(1, (int) Math.ceil(labelDensity * 46 / barAdvance)) : numShownBars / 6;
        for (int i=0; i<entries.size(); i+=labelStep) {
            final String label = getBarLabel(i);
            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            if (x < originX || x > originX + axisSizeX) {
                continue;
            }
            final float y = originY + density * 4 - paint.ascent();
            canvas.drawText(label, x, y, paint);
        }

        // The waterfall line
        if (GlobalSettings.Dashboard.getShowWaterfallLine()) {
            paint.setColor(colorWaterfall);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(density);

            float prevX = -1;
            float prevY = -1;

            for (int i=0; i<entries.size(); i++) {
                final BarEntry entry = entries.get(i);
                final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
                final float y = originY - entry.cumulativeCount * pixelsPerUnitCumulative;
                if (prevX >= originX && x <= originX + axisSizeX) {
                    canvas.drawLine(prevX, prevY, x, y, paint);
                }
                prevX = x;
                prevY = y;
            }
        }
    }

    /**
     * Draw the legend at the bottom of the chart.
     *
     * @param canvas the canvas to draw on
     */
    private void drawLegend(final Canvas canvas) {
        final float boxSize = density * 8;
        final float gapSize = density * 5;

        paint.setTextSize(labelDensity * 10);
        paint.setTextAlign(Paint.Align.LEFT);

        float x = getPaddingLeft() + density * 8;
        float boxY = originY + labelDensity * 20;
        float textY = boxY + density * 8;

        for (int i=0; i<segmentColors.length; i++) {
            float itemWidth = boxSize + gapSize * 2 + paint.measureText(legendLabels[i]);
            if (modern && x + itemWidth > getWidth() - getPaddingRight() && x > getPaddingLeft() + density * 8) {
                x = getPaddingLeft() + density * 8;
                boxY += labelDensity * 16;
                textY += labelDensity * 16;
            }
            paint.setColor(segmentColors[i]);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(0);
            canvas.drawRect(x, boxY, x + boxSize, boxY + boxSize, paint);
            x += boxSize + gapSize;

            final String label = legendLabels[i];
            paint.setColor(colorPrimary);
            canvas.drawText(label, x, textY, paint);
            paint.getTextBounds(label, 0, label.length(), rect);
            x += rect.width() + gapSize;
        }
    }

    private String modernSummary() {
        return getContext().getString(R.string.forecast_summary, totalCount, entries.size());
    }

    private String modernPeak() {
        if (totalCount == 0) return getContext().getString(R.string.forecast_empty);
        BarEntry peak = entries.get(0);
        for (BarEntry entry : entries) if (entry.barCount > peak.barCount) peak = entry;
        return getContext().getString(R.string.forecast_peak, peak.barCount,
                DateUtils.formatDateTime(getContext(), firstSlot + peak.index * HOUR,
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME));
    }

    private String modernLegend() {
        return getContext().getString(R.string.forecast_day_legend);
    }

    @SuppressWarnings("deprecation") // API 21-compatible; the builder is API 23+.
    private StaticLayout modernText(CharSequence text, int width, float size, int color) {
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setColor(color);
        textPaint.setTextSize(size * labelDensity);
        return new StaticLayout(text, textPaint, Math.max(1, width), Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
    }

    private String modernAction() {
        return getContext().getString(R.string.forecast_days_action);
    }

    private String dayLabel(DayEntry day) {
        if (DateUtils.isToday(day.start)) return getContext().getString(R.string.forecast_today);
        return DateUtils.formatDateTime(getContext(), day.start,
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL
                        | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
    }

    private String daySummary(DayEntry day) {
        return getContext().getString(R.string.forecast_hour_count, dayLabel(day), day.count);
    }

    private int modernHeight(int width) {
        int contentWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight());
        int height = getPaddingTop() + getPaddingBottom()
                + modernText(modernSummary(), contentWidth, 16, colorPrimary).getHeight()
                + modernText(modernPeak(), contentWidth, 12, colorPrimaryTonedDown).getHeight() + (int) (16 * density);
        for (DayEntry day : days.subList(0, Math.min(OVERVIEW_DAYS, days.size()))) {
            height += Math.max((int) (48 * density), modernText(daySummary(day), contentWidth, 14,
                    colorPrimary).getHeight() + (int) (26 * density));
        }
        return height + modernText(modernLegend(), contentWidth, 12, colorPrimaryTonedDown).getHeight()
                + modernText(modernAction(), contentWidth, 14, colorPrimary).getHeight() + (int) (20 * density);
    }

    private float drawModernText(Canvas canvas, CharSequence text, float y, float size, int color) {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        StaticLayout layout = modernText(text, width, size, color);
        canvas.save();
        canvas.translate(getPaddingLeft(), y);
        layout.draw(canvas);
        canvas.restore();
        for (int line = 0; line < layout.getLineCount(); line++) {
            modernTextBounds.add(new RectF(getPaddingLeft() + layout.getLineLeft(line), y + layout.getLineTop(line),
                    getPaddingLeft() + layout.getLineRight(line), y + layout.getLineBottom(line)));
        }
        return y + layout.getHeight();
    }

    /** One midnight-to-midnight strip per calendar day; no sideways scrolling or tiny axis labels. */
    private void drawModern(Canvas canvas) {
        modernTextBounds.clear();
        dayBounds.clear();
        paint.setTypeface(Typeface.DEFAULT);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);
        float y = drawModernText(canvas, modernSummary(), getPaddingTop(), 16, colorPrimary);
        y = drawModernText(canvas, modernPeak(), y + 4 * density, 12, colorPrimaryTonedDown) + 12 * density;
        int track = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHighest);
        int accent = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        for (DayEntry day : days.subList(0, Math.min(OVERVIEW_DAYS, days.size()))) {
            float top = y;
            y = drawModernText(canvas, daySummary(day), y, 14, colorPrimary);
            float barTop = y + 6 * density;
            float barBottom = barTop + 12 * density;
            RectF strip = new RectF(left, barTop, right, barBottom);
            paint.setColor(track);
            canvas.drawRoundRect(strip, 3 * density, 3 * density, paint);
            canvas.save();
            android.graphics.Path clip = new android.graphics.Path();
            clip.addRoundRect(strip, 3 * density, 3 * density, android.graphics.Path.Direction.CW);
            canvas.clipPath(clip);
            for (int i = day.first; i < day.end; i++) {
                BarEntry entry = entries.get(i);
                if (entry.barCount == 0) continue;
                float x = left + (right - left) * (firstSlot + i * HOUR - day.start) / (day.finish - day.start);
                float end = left + (right - left) * (firstSlot + (i + 1) * HOUR - day.start) / (day.finish - day.start);
                float intensity = .22f + .78f * entry.barCount / Math.max(1, maxBarCount);
                paint.setColor(androidx.core.graphics.ColorUtils.blendARGB(track, accent, intensity));
                canvas.drawRect(x, barTop, Math.max(x + density, end - density), barBottom, paint);
            }
            canvas.restore();
            // Noon is a spatial reference, without squeezing 24 labels into a phone-width row.
            Calendar noon = Calendar.getInstance();
            noon.setTimeInMillis(day.start);
            noon.set(Calendar.HOUR_OF_DAY, 12);
            float middle = left + (right - left) * (noon.getTimeInMillis() - day.start) / (day.finish - day.start);
            paint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface));
            paint.setStrokeWidth(density);
            canvas.drawLine(middle, barTop, middle, barBottom, paint);
            paint.setStrokeWidth(0);
            y = Math.max(top + 48 * density, barBottom + 8 * density);
            dayBounds.add(new RectF(left, top, right, y));
        }
        y = drawModernText(canvas, modernLegend(), y + 4 * density, 12, colorPrimaryTonedDown);
        drawModernText(canvas, modernAction(), y + 12 * density, 14, accent);
    }

    private void buildDays() {
        days.clear();
        DayEntry current = null;
        for (BarEntry entry : entries) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(firstSlot + entry.index * HOUR);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long start = calendar.getTimeInMillis();
            if (current == null || current.start != start) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                current = new DayEntry(start, calendar.getTimeInMillis(), entry.index, legendLabels.length);
                days.add(current);
            }
            current.end = entry.index + 1;
            current.count += entry.barCount;
            current.levelUp += entry.numLevelUpItems;
            for (int i = 0; i < entry.values.length; i++) current.values[i] += entry.values[i];
        }
    }

    private static final class DayEntry {
        final long start, finish;
        final int first;
        final int[] values;
        int end, count, levelUp;
        DayEntry(long start, long finish, int first, int buckets) {
            this.start = start; this.finish = finish; this.first = first;
            values = new int[buckets];
        }
    }

    private void onDrawImpl(final Canvas canvas) {
        final float baseWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        final float baseHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        paint.setTypeface(Typeface.DEFAULT);

        if (totalCount == 0) {
            paint.setColor(colorPrimary);
            paint.setTextSize(labelDensity * FONT_SIZE_NORMAL);
            final String s = "No upcoming reviews";
            paint.getTextBounds(s, 0, s.length(), rect);
            final float x = getPaddingLeft() + (baseWidth - rect.width()) / 2;
            final float y = getPaddingTop() + baseHeight / 2 - (paint.ascent() + paint.descent()) / 2;
            canvas.drawText(s, x, y, paint);
            return;
        }

        paint.setTextSize(labelDensity * 10);

        float bottomSpace = density * 30;
        if (modern) {
            int rows = 1;
            float width = density * 8;
            for (String label : legendLabels) {
                float itemWidth = density * 18 + paint.measureText(label);
                if (width + itemWidth > baseWidth && width > density * 8) { rows++; width = density * 8; }
                width += itemWidth;
            }
            bottomSpace = labelDensity * (20 + rows * 16);
        }
        originY = getPaddingTop() + baseHeight - bottomSpace;
        axisSizeY = baseHeight - bottomSpace - labelDensity * 15;
        if (axisSizeY <= 0) {
            return;
        }
        pixelsPerUnit = (axisSizeY / 1.1f) / maxBarCount;
        pixelsPerUnitCumulative = (axisSizeY / 1.1f) / totalCount;
        final float lineSpacing = Math.max(Math.abs(paint.getFontSpacing()), density * 10) * 1.8f;
        final String topLabel;
        switch (GlobalSettings.Dashboard.getTimeLineChartGridStyle()) {
            case FOR_WATERFALL:
                pixelsPerUnitGrid = pixelsPerUnitCumulative;
                axisIntervalY = roundUpInterval((int) Math.ceil(lineSpacing / pixelsPerUnitGrid));
                topLabel = Integer.toString(((totalCount + axisIntervalY - 1) / axisIntervalY) * axisIntervalY);
                break;
            case OFF:
                pixelsPerUnitGrid = pixelsPerUnit;
                axisIntervalY = 0;
                topLabel = "";
                break;
            case FOR_BARS:
            default:
                pixelsPerUnitGrid = pixelsPerUnit;
                axisIntervalY = roundUpInterval((int) Math.ceil(lineSpacing / pixelsPerUnitGrid));
                topLabel = Integer.toString(((maxBarCount + axisIntervalY - 1) / axisIntervalY) * axisIntervalY);
                break;
        }

        paint.getTextBounds(topLabel, 0, topLabel.length(), rect);
        originX = getPaddingLeft() + rect.width() + density * 12;
        axisSizeX = baseWidth - rect.width() - density * 20;
        if (axisSizeX <= 0) {
            return;
        }
        barAdvance = axisSizeX / numShownBars;

        drawGrid(canvas);
        drawBars(canvas);
        drawLegend(canvas);

        if (scroller != null && !scroller.isFinished() && scroller.computeScrollOffset()) {
            scrollOffset = Math.min(entries.size() * barAdvance - axisSizeX, Math.max(0, scroller.getCurrX()));
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        safe(() -> {
            super.onDraw(canvas);
            if (modern) drawModern(canvas);
            else onDrawImpl(canvas);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final boolean result = safe(false, () -> {
            if (modern || numShownBars < entries.size()) {
                if (gestureDetector == null) {
                    gestureDetector = new GestureDetectorCompat(getContext(), this);
                }
                gestureDetector.onTouchEvent(event);
                return true;
            }
            return false;
        });
        return result || super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        safe(() -> {
            if (scroller != null) {
                scroller.forceFinished(true);
            }
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
            prevVerticalScrollRawY = e.getRawY();
        });
        return true;
    }

    @Override
    public void onShowPress(final MotionEvent e) {
        //
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        if (!modern) return false;
        for (int i = 0; i < dayBounds.size(); i++) {
            if (dayBounds.get(i).contains(e.getX(), e.getY())) {
                playSoundEffect(android.view.SoundEffectConstants.CLICK);
                showDay(i);
                return true;
            }
        }
        performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (!modern || entries.isEmpty()) return false;
        final String[] labels = new String[days.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = daySummary(days.get(i));
        new MaterialAlertDialogBuilder(getContext()).setTitle(R.string.forecast_daily_title)
                .setItems(labels, (dialog, which) -> showDay(which))
                .setNegativeButton(android.R.string.cancel, null).show();
        return true;
    }

    private void showDay(int index) {
        DayEntry day = days.get(index);
        String[] hours = new String[day.end - day.first];
        for (int i = 0; i < hours.length; i++) {
            int slot = day.first + i;
            hours[i] = getContext().getString(R.string.forecast_hour_count, slotRange(slot), entries.get(slot).barCount);
        }
        new MaterialAlertDialogBuilder(getContext()).setTitle(daySummary(day))
                .setItems(hours, (dialog, which) -> showSlot(day.first + which))
                .setNeutralButton(R.string.forecast_day_totals, (dialog, which) -> {
                    StringBuilder detail = new StringBuilder(getContext().getString(R.string.forecast_reviews, day.count));
                    for (int i = 0; i < legendLabels.length; i++) detail.append("\n").append(legendLabels[i]).append(": ").append(day.values[i]);
                    detail.append("\n\n").append(getContext().getString(R.string.forecast_level_up, day.levelUp));
                    detail.append("\n").append(getContext().getString(R.string.forecast_cumulative, entries.get(day.end - 1).cumulativeCount));
                    if (day.first == 0) detail.append("\n\n").append(getContext().getString(R.string.forecast_partial_days));
                    new MaterialAlertDialogBuilder(getContext()).setTitle(dayLabel(day)).setMessage(detail)
                            .setPositiveButton(android.R.string.ok, null).show();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private String slotRange(int index) {
        final long start = firstSlot + index * HOUR;
        return DateUtils.formatDateRange(getContext(), start, start + HOUR,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_TIME);
    }

    private String slotDetails(int index) {
        final BarEntry entry = entries.get(index);
        StringBuilder detail = new StringBuilder(getContext().getString(R.string.forecast_reviews, entry.barCount));
        for (int j = 0; j < legendLabels.length; j++) {
            detail.append("\n").append(legendLabels[j]).append(": ").append(entry.values[j]);
        }
        detail.append("\n\n").append(getContext().getString(R.string.forecast_cumulative, entry.cumulativeCount));
        detail.append("\n").append(getContext().getString(R.string.forecast_level_up, entry.numLevelUpItems));
        if (index == 0) detail.append("\n\n").append(getContext().getString(R.string.forecast_overdue));
        return detail.toString();
    }

    private void showSlot(int index) {
        new MaterialAlertDialogBuilder(getContext()).setTitle(slotRange(index))
                .setMessage(slotDetails(index)).setPositiveButton(android.R.string.ok, null).show();
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
        safe(() -> {
            if (!modern) scrollOffset = Math.max(0, Math.min(entries.size() * barAdvance - axisSizeX, Math.max(0, scrollOffset + distanceX)));
            final float distance = prevVerticalScrollRawY - e2.getRawY();
            dispatchNestedScroll(0, 0, 0, (int) distance, null);
            prevVerticalScrollRawY = e2.getRawY();
            invalidate();
        });
        return true;
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        //
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
        if (modern) { dispatchNestedFling(0, -velocityY, false); return true; }
        safe(() -> {
            if (scroller == null) {
                scroller = new Scroller(getContext());
            }
            scroller.forceFinished(true);
            scroller.fling((int) scrollOffset, 0, (int) -velocityX, 0,
                    0, Math.max(0, (int) (entries.size() * barAdvance - axisSizeX)), 0, 0);
            dispatchNestedFling(0, -velocityY, false);
            invalidate();
        });
        return true;
    }

    /**
     * Build the dataset based on the SRS stage style.
     *
     * @param timeLine the timeline
     */
    private void buildDataSetSrsStage(final TimeLine timeLine) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < timeLine.getSize(); i++) {
            final List<Subject> reviews = timeLine.getTimeLine().get(i);
            final int[] values = {0, 0, 0, 0};
            int count = 0;
            for (final Subject review: reviews) {
                final SrsSystem.Stage stage = review.getSrsStage();
                int bucket = stage.getTimeLineBarChartBucket();
                if (bucket == 4) {
                    bucket = 3;
                }
                values[bucket]++;
                count++;
            }
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, timeLine.getNumRequiredForLevelUp().get(i), count, totalCount));
        }

        segmentColors = ActiveTheme.getShallowStageBucketColors4();

        legendLabels = new String[] {"Apprentice", "Guru", "Master", "Enlightened"};
    }

    /**
     * Build the dataset based on the Next SRS stage style.
     *
     * @param timeLine the timeline
     */
    private void buildDataSetNextSrsStage(final TimeLine timeLine) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < timeLine.getSize(); i++) {
            final List<Subject> reviews = timeLine.getTimeLine().get(i);
            final int[] values = {0, 0, 0, 0, 0};
            int count = 0;
            for (final Subject review: reviews) {
                final SrsSystem.Stage stage = review.getSrsStage().getNewStage(0);
                values[stage.getTimeLineBarChartBucket()]++;
                count++;
            }
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, timeLine.getNumRequiredForLevelUp().get(i), count, totalCount));
        }

        segmentColors = ActiveTheme.getShallowStageBucketColors5();

        legendLabels = new String[] {"Apprentice", "Guru", "Master", "Enlightened", "Burned"};
    }

    /**
     * Build the dataset based on the item type style.
     *
     * @param timeLine the timeline
     */
    private void buildDataSetItemType(final TimeLine timeLine) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < timeLine.getSize(); i++) {
            final List<Subject> reviews = timeLine.getTimeLine().get(i);
            final int[] values = {0, 0, 0};
            int count = 0;
            for (final Subject review: reviews) {
                int bucketIndex = review.getType().getTimeLineBarChartBucket();

                // Check if the item type is WANIKANI_KANA_VOCAB and update the bucket index accordingly
                if (review.getType() == SubjectType.WANIKANI_KANA_VOCAB) {
                    bucketIndex = SubjectType.WANIKANI_VOCAB.getTimeLineBarChartBucket();
                }

                values[bucketIndex]++;
                count++;
            }
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, timeLine.getNumRequiredForLevelUp().get(i), count, totalCount));
        }

        segmentColors = ActiveTheme.getSubjectTypeBucketColors();

        legendLabels = new String[] {"Radical", "Kanji", "Vocabulary"};
    }

    /**
     * Build a dummy data test set for testing of the chart code.
     *
     * @param size the size in hours
     */
    @SuppressWarnings("unused")
    private void buildDataSetTest(@SuppressWarnings("SameParameterValue") final int size) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < size; i++) {
            final int count = i + 10;
            final int[] values = {count};
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, 3, count, totalCount));
        }

        segmentColors = new int[] {0xFF2ECC71};

        legendLabels = new String[] {"Test"};
    }

    /**
     * Update the chart if the LiveData delivers a new timeline.
     *
     * @param timeLine the new timeline
     */
    private void update(final TimeLine timeLine) {
        if (LiveVacationMode.getInstance().get() || !GlobalSettings.Dashboard.getShowTimeLine()
                || GlobalSettings.getFirstTimeSetup() == 0) {
            setVisibility(View.GONE);
            return;
        }

        // buildDataSetTest(336);
        switch (GlobalSettings.Dashboard.getTimeLineChartStyle()) {
            case ITEM_TYPE:
                buildDataSetItemType(timeLine);
                break;
            case NEXT_SRS_STAGE:
                buildDataSetNextSrsStage(timeLine);
                break;
            case SRS_STAGE:
            default:
                buildDataSetSrsStage(timeLine);
                break;
        }

        firstSlot = timeLine.getFirstSlot();
        numShownBars = GlobalSettings.Dashboard.getTimeLineChartSizeShown();
        if (modern) {
            // ActiveTheme returns shared palette arrays: never mutate those persisted semantic colors.
            segmentColors = segmentColors.clone();
            for (int i = 0; i < segmentColors.length; i++) {
                segmentColors[i] = LearningColors.accent(getContext(), segmentColors[i]);
            }
            buildDays();
            scrollOffset = 0;
            StringBuilder accessible = new StringBuilder(modernSummary()).append(". ").append(modernPeak()).append(". ");
            for (DayEntry day : days) accessible.append(daySummary(day)).append(". ");
            accessible.append(getContext().getString(R.string.forecast_accessibility));
            setContentDescription(accessible);
            requestLayout();
        }

        setVisibility(View.VISIBLE);
        invalidate();
    }

    /**
     * A data set entry for one timeslot in the timeline.
     */
    private static final class BarEntry {
        /**
         * The index of the bar in the overall timeline.
         */
        private final int index;

        /**
         * Values for the segments that make up a bar.
         */
        private final int[] values;

        /**
         * The number of items on the level-up progression path in this timeslot.
         */
        private final int numLevelUpItems;

        /**
         * The total number of items in this bar (sum of values).
         */
        private final int barCount;

        /**
         * The cumulative total count of items in all timeslots up to and including this one.
         */
        private final int cumulativeCount;

        /**
         * The constructor.
         *
         * @param index The index of the bar in the overall timeline.
         * @param values Values for the segments that make up a bar.
         * @param numLevelUpItems The number of items on the level-up progression path in this timeslot.
         * @param barCount The total number of items in this bar (sum of values).
         * @param cumulativeCount The cumulative total count of items in all timeslots up to and including this one.
         */
        BarEntry(final int index, final int[] values, final int numLevelUpItems, final int barCount, final int cumulativeCount) {
            this.index = index;
            this.values = values;
            this.numLevelUpItems = numLevelUpItems;
            this.barCount = barCount;
            this.cumulativeCount = cumulativeCount;
        }
    }
}
