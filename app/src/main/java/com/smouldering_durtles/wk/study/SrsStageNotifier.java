package com.smouldering_durtles.wk.study;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.model.SrsSystem;
import com.smouldering_durtles.wk.ui.ModernUi;

/** A quiet, explicit stage transition outside the question glyph. */
public final class SrsStageNotifier {
    private SrsStageNotifier() { }

    /** A separate toast window never participates in measuring the question or answer editor. */
    @SuppressWarnings("deprecation")
    public static android.widget.Toast show(android.content.Context context, SrsSystem.Stage oldStage, SrsSystem.Stage newStage) {
        TextView notice = new TextView(context);
        bind(notice, oldStage, newStage);
        notice.setMaxWidth(context.getResources().getDisplayMetrics().widthPixels - ModernUi.dp(notice, 32));
        android.widget.Toast toast = new android.widget.Toast(context);
        toast.setView(notice);
        toast.setDuration(android.widget.Toast.LENGTH_SHORT);
        if (context instanceof android.app.Activity) {
            androidx.core.view.WindowInsetsCompat insets = androidx.core.view.ViewCompat.getRootWindowInsets(
                    ((android.app.Activity) context).getWindow().getDecorView());
            if (insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())) {
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom + ModernUi.dp(notice, 16));
            }
        }
        toast.show();
        return toast;
    }

    public static void bind(TextView view, SrsSystem.Stage oldStage, SrsSystem.Stage newStage) {
        int ink = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface);
        view.setText(view.getContext().getString(R.string.srs_stage_change, oldStage.getName(), newStage.getName()));
        view.setContentDescription(view.getContext().getString(R.string.srs_stage_change_accessibility,
                oldStage.getName(), newStage.getName()));
        view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        view.setTextColor(ink);
        view.setTextSize(14);
        view.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        view.setShadowLayer(0, 0, 0, 0);
        view.setElevation(0);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        view.setPadding(ModernUi.dp(view, 12), ModernUi.dp(view, 10), ModernUi.dp(view, 12), ModernUi.dp(view, 10));
        view.setBackground(ModernUi.surface(view, MaterialColors.getColor(view,
                com.google.android.material.R.attr.colorSurfaceContainerHighest), 12));
        Drawable icon = ContextCompat.getDrawable(view.getContext(),
                newStage.compareTo(oldStage) > 0 ? R.drawable.ic_stage_promoted : R.drawable.ic_stage_demoted);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(ink);
            icon.setBounds(0, 0, ModernUi.dp(view, 20), ModernUi.dp(view, 20));
        }
        view.setCompoundDrawablesRelative(icon, null, null, null);
        view.setCompoundDrawablePadding(ModernUi.dp(view, 8));
    }
}
