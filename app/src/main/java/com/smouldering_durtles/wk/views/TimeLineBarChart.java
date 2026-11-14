package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.livedata.LiveVacationMode;
import com.smouldering_durtles.wk.model.TimeLine;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import static com.smouldering_durtles.wk.Constants.HOUR;

/** One scrollable hourly waterfall. Bars show arrivals; the line shows cumulative workload. */
public final class TimeLineBarChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RectF> modernTextBounds = new ArrayList<>();
    private int[] counts = new int[0], cumulative = new int[0];
    private long firstSlot;
    private TimeLine source;
    private View controls;
    private int horizon = 24;
    private long[] times = new long[0];
    private String[] timeLabels = new String[0], dateLabels = new String[0];
    private final java.util.Map<String, StaticLayout> textCache = new java.util.HashMap<>();
    private final Path line = new Path();
    private StaticLayout[] captions = new StaticLayout[0];
    private int captionWidth = -1, maximumCaptionHeight;
    private StaticLayout lastDrawnCaption;
    private String[] countLabels = new String[0];
    private String maximumLabel = "1", totalLabel = "0";
    public void setHorizon(int hours) {
        horizon = hours; selected = -1; scrollOffset = 0;
        if (source != null) update(source);
    }
    @Override public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (controls != null) controls.setVisibility(visibility);
    }
    public View withControls() {
        android.widget.LinearLayout wrapper = new android.widget.LinearLayout(getContext());
        wrapper.setId(R.id.forecastPanel);
        controls = wrapper;
        wrapper.setVisibility(getVisibility());
        wrapper.setOrientation(android.widget.LinearLayout.VERTICAL);
        com.google.android.material.button.MaterialButtonToggleGroup choices = new com.google.android.material.button.MaterialButtonToggleGroup(getContext());
        choices.setSingleSelection(true);
        choices.setSelectionRequired(true);
        String[] names = {"24 hours", "7 days"}; int[] hours = {24, 168};
        for (int i = 0; i < names.length; i++) {
            final int span = hours[i];
            com.google.android.material.button.MaterialButton button = new com.google.android.material.button.MaterialButton(
                    getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setId(View.generateViewId()); button.setText(names[i]); button.setTextSize(14);
            button.setMinWidth(0); button.setMinimumWidth(0); button.setSingleLine(false);
            button.setMinimumHeight(com.smouldering_durtles.wk.ui.ModernUi.dp(this, 48));
            choices.addView(button, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
            if (horizon == span) choices.check(button.getId());
            choices.addOnButtonCheckedListener((group, id, checked) -> {
                if (checked && id == button.getId()) setHorizon(span);
            });
        }
        wrapper.addView(choices, new android.widget.LinearLayout.LayoutParams(-1, -2));
        wrapper.addView(this, new android.widget.LinearLayout.LayoutParams(-1, -2));
        return wrapper;
    }
    private int totalCount, availableNow, maxBarCount = 1, selected = -1;
    private float scrollOffset, downX, downY, lastX;
    private boolean dragging;
    private final float density, scaledDensity;
    private float plotLeft, plotRight, plotTop, plotBottom, barAdvance;
    private final int ink, muted, accent, fill, outline;

    public TimeLineBarChart(Context context) {
        this(context, null);
    }
    public TimeLineBarChart(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public TimeLineBarChart(Context context, @Nullable AttributeSet attrs, int style) {
        super(context, attrs, style);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        ink = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        muted = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        accent = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        fill = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondaryContainer);
        outline = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant);
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setLifecycleOwner(LifecycleOwner owner) {
        LiveTimeLine.getInstance().observe(owner, this::update);
    }

    private void update(TimeLine timeline) {
        source = timeline;
        textCache.clear();
        captionWidth = -1;
        if (LiveVacationMode.getInstance().get() || !GlobalSettings.Dashboard.getShowTimeLine()
                || GlobalSettings.getFirstTimeSetup() == 0) {
            setVisibility(GONE);
            if (controls != null) controls.setVisibility(GONE);
            return;
        }
        long previousFirstSlot = firstSlot;
        int previousSelected = selected;
        float previousScroll = scrollOffset;
        firstSlot = timeline.getFirstSlot();
        availableNow = timeline.getNumAvailableReviews();
        java.util.TreeMap<Long, Integer> buckets = new java.util.TreeMap<>();
        boolean daily = horizon != 24;
        java.util.Calendar day = java.util.Calendar.getInstance();
        day.setTimeInMillis(firstSlot); day.set(java.util.Calendar.HOUR_OF_DAY, 0); day.set(java.util.Calendar.MINUTE, 0);
        day.set(java.util.Calendar.SECOND, 0); day.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Calendar end = (java.util.Calendar) day.clone(); end.add(java.util.Calendar.DATE, 7);
        int slots = horizon == 24 ? Math.min(24, timeline.getSize()) : timeline.getSize();
        for (int i = 0; i < slots; i++) {
            long timestamp = firstSlot + i * HOUR;
            if (horizon == 168 && timestamp >= end.getTimeInMillis()) break;
            long key = timestamp;
            if (daily) {
                day.setTimeInMillis(timestamp); day.set(java.util.Calendar.HOUR_OF_DAY, 0); day.set(java.util.Calendar.MINUTE, 0);
                day.set(java.util.Calendar.SECOND, 0); day.set(java.util.Calendar.MILLISECOND, 0); key = day.getTimeInMillis();
            }
            int arrivals = Math.max(0, timeline.getTimeLine().get(i).size() - (i == 0 ? availableNow : 0));
            buckets.put(key, buckets.getOrDefault(key, 0) + arrivals);
        }
        counts = new int[buckets.size()]; cumulative = new int[counts.length]; times = new long[counts.length];
        timeLabels = new String[counts.length]; dateLabels = new String[counts.length];
        totalCount = availableNow; maxBarCount = 1;
        int position = 0;
        for (java.util.Map.Entry<Long, Integer> entry : buckets.entrySet()) {
            times[position] = entry.getKey(); counts[position] = entry.getValue(); totalCount += entry.getValue();
            cumulative[position] = totalCount; maxBarCount = Math.max(maxBarCount, entry.getValue());
            timeLabels[position] = DateUtils.formatDateTime(getContext(), entry.getKey(), daily ? DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY : DateUtils.FORMAT_SHOW_TIME);
            dateLabels[position] = DateUtils.formatDateTime(getContext(), entry.getKey(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR);
            position++;
        }
        countLabels = new String[counts.length];
        for (int i = 0; i < counts.length; i++) countLabels[i] = Integer.toString(counts[i]);
        maximumLabel = Integer.toString(maxBarCount); totalLabel = Integer.toString(totalCount);
        int elapsedSlots = previousFirstSlot == 0 || horizon != 24 ? 0 : (int) ((firstSlot - previousFirstSlot) / HOUR);
        selected =
                previousSelected < 0 || counts.length == 0 ? -1 : Math.max(0, Math.min(counts.length - 1, previousSelected - elapsedSlots));
        scrollOffset = Math.max(0, previousScroll - elapsedSlots * barAdvance);
        setVisibility(VISIBLE);
        if (controls != null) controls.setVisibility(VISIBLE);
        updateDescription();
        requestLayout();
        invalidate();
    }

    private String caption() {
        return captionFor(selected);
    }

    private String captionFor(int slot) {
        return "Arrivals (left) · Waiting (right)"
                + (slot >= 0 ? "\n" + slotDetails(slot) : "");
    }

    private String slotDetails(int slot) {
        return getContext().getString(R.string.forecast_timeline_slot,
                DateUtils.formatDateTime(getContext(), times[slot],
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL
                                | (horizon == 24 ? DateUtils.FORMAT_SHOW_TIME : 0)),
                counts[slot], cumulative[slot]);
    }

    private void updateDescription() {
        StringBuilder text = new StringBuilder(counts.length + (horizon == 24 ? " hourly" : " daily") + " buckets. " + totalCount + " waiting including " + availableNow + " due now. Assumes no reviews completed. Tap to choose a forecast date.");
        int first = Math.max(0, (int) (scrollOffset / Math.max(1, barAdvance)));
        int last =
                Math.min(counts.length, first + Math.max(1, (int) ((plotRight - plotLeft) / Math.max(1, barAdvance))));
        for (int i = first; i < last; i++) text.append(". ").append(slotDetails(i));
        if (!text.toString().contentEquals(getContentDescription() == null ? "" : getContentDescription()))
            setContentDescription(text);
    }

    @SuppressWarnings("deprecation")
    private StaticLayout textLayout(String text, int width) {
        String key = width + ":" + text;
        StaticLayout cached = textCache.get(key);
        if (cached != null) return cached;
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(muted);
        p.setTextSize(12 * scaledDensity);
        StaticLayout layout = new StaticLayout(text, p, Math.max(1, width), Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        textCache.put(key, layout);
        return layout;
    }

    private void prepareCaptions(int width) {
        if (captionWidth == width) return;
        captionWidth = width; maximumCaptionHeight = 0;
        captions = new StaticLayout[counts.length + 1];
        for (int i = -1; i < counts.length; i++) {
            captions[i + 1] = textLayout(captionFor(i), width);
            maximumCaptionHeight = Math.max(maximumCaptionHeight, captions[i + 1].getHeight());
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (width == 0)
            width = (int) (320 * density);
        prepareCaptions(width);
        int captionHeight = maximumCaptionHeight;
        setMeasuredDimension(
                width, resolveSize(captionHeight + (int) (224 * density + 48 * scaledDensity), heightSpec));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        plotLeft = 34 * scaledDensity;
        plotRight = width - 34 * scaledDensity;
        barAdvance = Math.max(horizon == 24 ? 24 * density : (plotRight - plotLeft) / Math.max(1, counts.length), 22 * scaledDensity);
        scrollOffset = Math.min(scrollOffset, maxScroll());
        updateDescription();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        prepareCaptions(getWidth());
        StaticLayout caption = captions[Math.max(0, Math.min(captions.length - 1, selected + 1))];
        caption.draw(canvas);
        if (caption != lastDrawnCaption) {
            lastDrawnCaption = caption; modernTextBounds.clear();
            for (int i = 0; i < caption.getLineCount(); i++) modernTextBounds.add(new RectF(
                    caption.getLineLeft(i), caption.getLineTop(i), caption.getLineRight(i), caption.getLineBottom(i)));
        }
        plotLeft = 34 * scaledDensity;
        plotRight = getWidth() - 34 * scaledDensity;
        plotTop = getHeight() - 204 * density - 48 * scaledDensity;
        plotBottom = plotTop + 180 * density;
        barAdvance = Math.max(horizon == 24 ? 24 * density : (plotRight - plotLeft) / Math.max(1, counts.length), 22 * scaledDensity);
        scrollOffset = Math.min(scrollOffset, maxScroll());
        paint.setColor(outline);
        paint.setStrokeWidth(density);
        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, paint);
        paint.setTextSize(10 * scaledDensity);
        paint.setColor(muted);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(maximumLabel, 0, plotTop, paint);
        canvas.drawText("0", 0, plotBottom, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(totalLabel, getWidth(), plotTop, paint);
        canvas.drawText("0", getWidth(), plotBottom, paint);
        canvas.save();
        canvas.clipRect(plotLeft, plotTop - 20 * scaledDensity, plotRight, getHeight());
        line.reset();
        float lastLabelRight = -1;
        int firstVisible = Math.max(0, (int) (scrollOffset / Math.max(1, barAdvance)) - 1);
        int lastVisible = Math.min(counts.length, firstVisible + (int) ((plotRight - plotLeft) / Math.max(1, barAdvance)) + 4);
        for (int i = firstVisible; i < lastVisible; i++) {
            float center = plotLeft + (i + .5f) * barAdvance - scrollOffset;
            float top = plotBottom - counts[i] * (plotBottom - plotTop) / maxBarCount;
            paint.setColor(i == selected ? accent : fill);
            paint.setStyle(Paint.Style.FILL);
            if (counts[i] > 0)
                canvas.drawRoundRect(center - barAdvance * .35f, top, center + barAdvance * .35f, plotBottom,
                        3 * density, 3 * density, paint);
            float cy = plotBottom - cumulative[i] * (plotBottom - plotTop) / Math.max(1, totalCount);
            if (i == firstVisible)
                line.moveTo(center, cy);
            else
                line.lineTo(center, cy);
            if (center >= plotLeft && center <= plotRight && counts[i] > 0) {
                paint.setTextSize(10 * scaledDensity);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(ink);
                canvas.drawText(countLabels[i], center, top - 4 * density, paint);
            }
            if (horizon != 24 || i % 3 == 0) {
                paint.setTextSize(11 * scaledDensity);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(muted);
                String time = timeLabels[i];
                float labelWidth = paint.measureText(time);
                float labelX = Math.max(plotLeft + labelWidth / 2, Math.min(plotRight - labelWidth / 2, center));
                if (center < plotLeft || center > plotRight || labelX - labelWidth / 2 < lastLabelRight + 4 * density)
                    continue;
                canvas.drawText(time, labelX, plotBottom + 8 * density - paint.ascent(), paint);
                lastLabelRight = labelX + labelWidth / 2;
                String day = dateLabels[i];
                canvas.drawText(
                        day, labelX, plotBottom + 12 * density - paint.ascent() + paint.getFontSpacing(), paint);
            }
        }
        paint.setColor(accent);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density);
        canvas.drawPath(line, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    private float maxScroll() {
        return Math.max(0, counts.length * barAdvance - (plotRight - plotLeft));
    }
    private void move(float delta) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset + delta));
        updateDescription();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = event.getX();
                downY = event.getY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX, dy = event.getY() - downY;
                if (!dragging && Math.abs(dx) > ViewConfiguration.get(getContext()).getScaledTouchSlop()
                        && Math.abs(dx) > Math.abs(dy))
                    dragging = true;
                if (dragging) {
                    if (getParent() != null)
                        getParent().requestDisallowInterceptTouchEvent(true);
                    move(lastX - event.getX());
                }
                lastX = event.getX();
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging && counts.length > 0) {
                    selected = Math.max(0,
                            Math.min(counts.length - 1, (int) ((event.getX() - plotLeft + scrollOffset) / barAdvance)));
                    performClick();
                    announceForAccessibility(slotDetails(selected));
                    invalidate();
                }
                if (getParent() != null)
                    getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setScrollable(maxScroll() > 0);
        if (counts.length > 0) info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Choose forecast date"));
        if (scrollOffset > 0)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        if (scrollOffset < maxScroll())
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
    }
    @Override
    public boolean performAccessibilityAction(int action, @Nullable Bundle args) {
        if (action == AccessibilityNodeInfo.ACTION_CLICK && counts.length > 0) {
            String[] labels = new String[counts.length];
            for (int i = 0; i < counts.length; i++) labels[i] = slotDetails(i);
            com.smouldering_durtles.wk.ui.ModernUi.dialog(getContext()).setTitle("Forecast dates").setItems(labels, (dialog, which) -> {
                selected = which; scrollOffset = Math.max(0, Math.min(maxScroll(), which * barAdvance));
                updateDescription(); invalidate(); announceForAccessibility(labels[which]);
            }).setNegativeButton("Close", null).show();
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            move(plotRight - plotLeft);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            move(plotLeft - plotRight);
            return true;
        }
        return super.performAccessibilityAction(action, args);
    }
}
