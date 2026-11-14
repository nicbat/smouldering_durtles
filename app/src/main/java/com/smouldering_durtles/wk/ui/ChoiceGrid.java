package com.smouldering_durtles.wk.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;

/** Balanced, measured choice rows. Text scales; four options become 2 by 2 on phones. */
public final class ChoiceGrid extends ViewGroup {
    private int columns = 1;
    private int[] heights = new int[0];
    public ChoiceGrid(Context context) {
        super(context);
        // A grid owns its trailing group gap, including when stacked directly in legacy forms.
        setPadding(0, 0, 0, ModernUi.dp(this, 20));
    }
    public Button addChoice(String label, int color, Runnable onClick) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setSingleLine(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        ModernUi.button(button, ModernUi.ButtonRole.SECONDARY);
        button.setTag(R.id.choiceColor, color);
        styleChoice(button);
        button.setOnClickListener(v -> onClick.run());
        addView(button, new LayoutParams(-1, -2));
        return button;
    }
    public static void selected(Button button, boolean selected) {
        if (Boolean.TRUE.equals(button.getTag(R.id.choiceToggle)) && button.isSelected() == selected) return;
        String label = button.getText().toString().replaceFirst("^✓ ", "");
        button.setText((selected ? "✓ " : "") + label);
        button.setSelected(selected);
        button.setTag(R.id.choiceToggle, true);
        styleChoice(button);
        androidx.core.view.ViewCompat.setStateDescription(button, selected ? "Selected" : "Not selected");
    }
    /** Reapplied by the shared utility styling pass without losing semantic identity. */
    public static void styleChoice(Button button) {
        int color = (Integer) button.getTag(R.id.choiceColor);
        if ("neutral".equals(GlobalSettings.Display.getLearningColors()))
            color = MaterialColors.getColor(button, androidx.appcompat.R.attr.colorPrimary);
        int surface = MaterialColors.getColor(button, com.google.android.material.R.attr.colorSurface);
        int onSurface = MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnSurface);
        boolean active = button.getTag(R.id.choiceToggle) == null || button.isSelected();
        int fill = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(surface, color, active ? 0.22f : 0.04f), 255);
        int ink = readableInk(color, fill);
        int border = readableInk(color, surface);
        int[][] states = {new int[]{-android.R.attr.state_enabled}, new int[]{}};
        MaterialShapeDrawable shape = ModernUi.surface(button, fill, 12);
        shape.setFillColor(new ColorStateList(states, new int[]{ColorUtils.blendARGB(surface, onSurface, 0.08f), fill}));
        shape.setStroke(ModernUi.dp(button, active ? 2 : 1), new ColorStateList(states,
                new int[]{ColorUtils.blendARGB(surface, onSurface, 0.12f),
                        ColorUtils.blendARGB(fill, border, active ? 0.65f : 0.3f)}));
        ViewCompat.setBackgroundTintList(button, null);
        RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(ink, 38)), shape, null);
        button.setBackground(ripple);
        // The wrapper starts with an empty state. Propagate AFTER wrapping, including when
        // replacing a selected button's background without a view-state transition.
        ripple.setState(new int[0]);
        ripple.setState(button.getDrawableState());
        shape.setState(new int[0]);
        shape.setState(button.getDrawableState());
        button.setTextColor(new ColorStateList(states,
                new int[]{ColorUtils.blendARGB(surface, onSurface, 0.38f), ink}));
    }

    /** Keep the hue while moving toward black/white only as far as legibility needs. */
    public static int readableInk(int color, int background) {
        background = ColorUtils.setAlphaComponent(background, 255);
        int target = ColorUtils.calculateContrast(Color.WHITE, background) > ColorUtils.calculateContrast(Color.BLACK, background)
                ? Color.WHITE : Color.BLACK;
        color = ColorUtils.setAlphaComponent(color, 255);
        for (int step = 0; step <= 20; step++) {
            int candidate = ColorUtils.blendARGB(color, target, step / 20f);
            if (ColorUtils.calculateContrast(candidate, background) >= 4.5) return candidate;
        }
        return target;
    }

    @Override protected void onMeasure(int w, int h) {
        int width = Math.max(0, MeasureSpec.getSize(w) - getPaddingLeft() - getPaddingRight()), gap = ModernUi.dp(this, 8);
        int minimum = Math.round(120 * getResources().getDisplayMetrics().density * getResources().getConfiguration().fontScale);
        columns = width >= getChildCount() * minimum + Math.max(0, getChildCount() - 1) * gap ? Math.max(1, getChildCount()) : width >= minimum * 2 + gap ? 2 : 1;
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
        setMeasuredDimension(width + getPaddingLeft() + getPaddingRight(), resolveSize(height + getPaddingTop() + getPaddingBottom(), h));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int gap = ModernUi.dp(this, 8), y = getPaddingTop();
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int x = getPaddingLeft() + (i % columns) * ((width - gap * (columns - 1)) / columns + gap);
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getWidth() - x - child.getMeasuredWidth();
            child.layout(x, y, x + child.getMeasuredWidth(), y + heights[i / columns]);
            if (i % columns == columns - 1) y += heights[i / columns] + gap;
        }
    }
}
