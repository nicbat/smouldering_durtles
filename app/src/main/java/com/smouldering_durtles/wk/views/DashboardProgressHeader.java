package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/** Fits label and count on one line, but never squeezes large text into fixed columns. */
public final class DashboardProgressHeader extends LinearLayout {
    public DashboardProgressHeader(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int available = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
        int gap = Math.round(12 * getResources().getDisplayMetrics().density);
        int wanted = gap;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            wanted += child.getMeasuredWidth();
        }
        boolean inline = wanted <= available;
        setOrientation(inline ? HORIZONTAL : VERTICAL);
        for (int i = 0; i < getChildCount(); i++) {
            LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
            params.width = inline && i == 0 ? 0 : LayoutParams.MATCH_PARENT;
            if (inline && i > 0) params.width = LayoutParams.WRAP_CONTENT;
            params.weight = inline && i == 0 ? 1 : 0;
            params.setMarginStart(inline && i > 0 ? gap : 0);
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
