package com.smouldering_durtles.wk.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/** Balanced, measured choice rows. Text scales; four options become 2 by 2 on phones. */
public final class ChoiceGrid extends ViewGroup {
    private int columns = 1;
    private int[] heights = new int[0];
    public ChoiceGrid(Context context) { super(context); }
    public Button addChoice(String label, int color, Runnable onClick) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setSingleLine(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        ModernUi.button(button, ModernUi.ButtonRole.SECONDARY);
        button.setTextColor(LearningColors.accent(getContext(), color));
        button.setOnClickListener(v -> onClick.run());
        addView(button, new LayoutParams(-1, -2));
        return button;
    }
    public static void selected(Button button, boolean selected) {
        String label = button.getText().toString().replaceFirst("^✓ ", "");
        button.setText((selected ? "✓ " : "") + label);
        button.setSelected(selected);
        androidx.core.view.ViewCompat.setStateDescription(button, selected ? "Selected" : "Not selected");
    }
    @Override protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w), gap = ModernUi.dp(this, 8);
        int minimum = Math.round(120 * getResources().getDisplayMetrics().density * getResources().getConfiguration().fontScale);
        columns = width >= getChildCount() * minimum ? Math.max(1, getChildCount()) : width >= minimum * 2 ? 2 : 1;
        int rows = (getChildCount() + columns - 1) / columns;
        heights = new int[rows];
        for (int i = 0; i < getChildCount(); i++) {
            boolean full = columns == 2 && i == getChildCount() - 1 && getChildCount() % 2 == 1;
            int cell = full ? width : (width - gap * (columns - 1)) / columns;
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cell, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            heights[i / columns] = Math.max(heights[i / columns], getChildAt(i).getMeasuredHeight());
        }
        int height = Math.max(0, rows - 1) * gap;
        for (int value : heights) height += value;
        setMeasuredDimension(width, resolveSize(height, h));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int gap = ModernUi.dp(this, 8), y = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int x = (i % columns) * ((getWidth() - gap * (columns - 1)) / columns + gap);
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getWidth() - x - child.getMeasuredWidth();
            child.layout(x, y, x + child.getMeasuredWidth(), y + heights[i / columns]);
            if (i % columns == columns - 1) y += heights[i / columns] + gap;
        }
    }
}
