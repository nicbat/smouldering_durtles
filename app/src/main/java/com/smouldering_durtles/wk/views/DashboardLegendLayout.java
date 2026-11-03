package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** Compact equal-width legend cells; full stage names remain in accessibility descriptions. */
public final class DashboardLegendLayout extends ViewGroup {
    private int columns = 1;
    private int gap;
    public DashboardLegendLayout(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        float density = getResources().getDisplayMetrics().density;
        gap = Math.round(8 * density);
        int minCell = Math.round(48 * density * Math.max(1, getResources().getConfiguration().fontScale));
        columns = Math.max(1, Math.min(Math.max(1, getChildCount()), (available + gap) / (minCell + gap)));
        int cellWidth = Math.max(0, (available - gap * (columns - 1)) / columns);
        int height = getPaddingTop() + getPaddingBottom();
        for (int i = 0; i < getChildCount(); i += columns) {
            int rowHeight = 0;
            for (int j = i; j < Math.min(i + columns, getChildCount()); j++) {
                View child = getChildAt(j);
                child.measure(MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
            }
            height += rowHeight;
        }
        setMeasuredDimension(resolveSize(width, widthSpec), resolveSize(height, heightSpec));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int y = getPaddingTop();
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        for (int i = 0; i < getChildCount(); i += columns) {
            int rowHeight = 0;
            for (int j = i; j < Math.min(i + columns, getChildCount()); j++) {
                View child = getChildAt(j);
                int offset = (j - i) * (child.getMeasuredWidth() + gap);
                int x = rtl ? getWidth() - getPaddingRight() - offset - child.getMeasuredWidth() : getPaddingLeft() + offset;
                child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
                rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
            }
            y += rowHeight;
        }
    }

    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
