package com.smouldering_durtles.wk.dashboard;

import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.R;

/** The body owns data visibility; its separate host owns the persisted disclosure state. */
public final class CollapsibleDashboardPanel extends LinearLayout {
    private final View body;
    private final FrameLayout host;
    private final TextView heading;
    private final SharedPreferences preferences;
    private final String key;
    private boolean collapsed;

    private CollapsibleDashboardPanel(View body, CharSequence title) {
        super(body.getContext());
        this.body = body;
        key = "dashboard_collapsed_" + getResources().getResourceEntryName(body.getId());
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        collapsed = preferences.getBoolean(key, false);
        setOrientation(VERTICAL);
        setBackground(DashboardStyle.surface(this));
        setPadding(dp(12), 0, dp(12), 0);
        heading = new TextView(getContext());
        heading.setText(title);
        heading.setTextAppearance(getContext(), com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        heading.setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setMinimumHeight(dp(48));
        heading.setPadding(0, dp(8), 0, dp(8));
        ViewCompat.setAccessibilityHeading(heading, true);
        ViewCompat.setAccessibilityDelegate(heading, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override public void onInitializeAccessibilityNodeInfo(View view,
                    androidx.core.view.accessibility.AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
        android.util.TypedValue ripple = new android.util.TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        heading.setBackgroundResource(ripple.resourceId);
        addView(heading, new LayoutParams(-1, -2));
        host = new FrameLayout(getContext());
        host.setPadding(0, 0, 0, dp(12));
        host.addView(body, new FrameLayout.LayoutParams(-1, -2));
        addView(host, new LayoutParams(-1, -2));
        heading.setOnClickListener(v -> {
            collapsed = !collapsed;
            preferences.edit().putBoolean(key, collapsed).apply();
            updateDisclosure();
        });
        updateDisclosure();
    }

    private int dp(int value) { return DashboardStyle.dp(this, value); }

    private void updateDisclosure() {
        // Keep the host visible at zero height: GONE would suppress child visibility/layout
        // notifications, leaving a stale heading if a data observer hides a collapsed body.
        host.getLayoutParams().height = collapsed ? 0 : LayoutParams.WRAP_CONTENT;
        host.setLayoutParams(host.getLayoutParams());
        host.setImportantForAccessibility(collapsed ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        host.setDescendantFocusability(collapsed ? FOCUS_BLOCK_DESCENDANTS : FOCUS_AFTER_DESCENDANTS);
        android.graphics.drawable.Drawable arrow = androidx.core.content.ContextCompat.getDrawable(getContext(),
                collapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
        if (arrow != null) {
            arrow = arrow.mutate();
            arrow.setTint(heading.getCurrentTextColor());
            arrow.setBounds(0, 0, dp(24), dp(24));
        }
        heading.setCompoundDrawablesRelative(null, null, arrow, null);
        heading.setCompoundDrawablePadding(dp(8));
        ViewCompat.setStateDescription(heading, getContext().getString(
                collapsed ? R.string.dashboard_collapsed : R.string.dashboard_expanded));
    }

    public static void wrap(View body, CharSequence title) {
        ViewGroup parent = (ViewGroup) body.getParent();
        int index = parent.indexOfChild(body);
        ViewGroup.LayoutParams params = body.getLayoutParams();
        parent.removeView(body);
        CollapsibleDashboardPanel panel = new CollapsibleDashboardPanel(body, title);
        android.graphics.drawable.Drawable background = body.getId() == R.id.apiKeyRejectedView
                ? com.smouldering_durtles.wk.ui.ModernUi.surface(panel, MaterialColors.getColor(panel,
                    com.google.android.material.R.attr.colorErrorContainer), 16)
                : DashboardStyle.surface(panel);
        int topGap = 0;
        int bottomGap = 0;
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
            topGap = margins.topMargin;
            bottomGap = margins.bottomMargin;
            background = new android.graphics.drawable.InsetDrawable(background, 0, margins.topMargin, 0, margins.bottomMargin);
            margins.topMargin = 0;
            margins.bottomMargin = 0;
        }
        panel.setBackground(background);
        panel.setPadding(panel.dp(12), topGap, panel.dp(12), bottomGap);
        parent.addView(panel, index, params);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (body.getVisibility() == GONE) setMeasuredDimension(MeasureSpec.getSize(widthSpec), 0);
        else super.onMeasure(widthSpec, heightSpec);
    }
}
