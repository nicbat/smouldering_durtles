package com.smouldering_durtles.wk.dashboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.InsetDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.smouldering_durtles.wk.R;

/** Presentation for existing dashboard views. No data aggregation or study actions live here. */
public final class DashboardStyle {
    private DashboardStyle() {}

    public static void apply(android.app.Activity activity) {
        androidx.appcompat.widget.Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setElevation(0);
        int surface = MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorSurface);
        androidx.core.view.WindowInsetsControllerCompat bars = androidx.core.view.WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(MaterialColors.isColorLight(surface));
        bars.setAppearanceLightNavigationBars(MaterialColors.isColorLight(surface));
        View timeline = activity.findViewById(R.id.timeLineBarChart);
        timeline.getLayoutParams().height = dp(timeline, Math.round(220 * Math.max(1,
                timeline.getResources().getConfiguration().fontScale)));
        titledPanel(timeline, R.string.dashboard_forecast_title);
        titledPanel(activity.findViewById(R.id.srsBreakDownView), R.string.dashboard_srs_title);
        for (int id : new int[] {R.id.levelProgressView, R.id.post60ProgressView, R.id.joyoProgressView,
                R.id.jlptProgressView, R.id.lessonReviewBreakdownView}) {
            View view = activity.findViewById(id);
            if (view != null) card(view);
        }
    }

    public static boolean isModern(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.smokyModernDashboard, value, true) && value.data != 0;
    }

    public static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    public static MaterialShapeDrawable surface(View view) {
        MaterialShapeDrawable shape = new MaterialShapeDrawable(ShapeAppearanceModel.builder()
                .setAllCornerSizes(dp(view, 24)).build());
        shape.setFillColor(ColorStateList.valueOf(MaterialColors.getColor(view,
                com.google.android.material.R.attr.colorSurfaceContainerLow)));
        return shape;
    }

    /** Retint only legacy primary-colored labels, leaving subject/SRS labels independent. */
    public static void textColors(View view) {
        int primary = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary);
        if (view instanceof TextView && ((TextView) view).getCurrentTextColor() == primary) {
            ((TextView) view).setTextColor(MaterialColors.getColor(view,
                    com.google.android.material.R.attr.colorOnSurface));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) textColors(group.getChildAt(i));
        }
    }

    public static void card(View view) {
        view.setBackground(surface(view));
        int padding = dp(view, 16);
        view.setPadding(padding, padding, padding, padding);
        textColors(view);
    }

    /** Adds a heading to an existing chart. A hidden chart takes no space, including its heading. */
    public static void titledPanel(View chart, int title) {
        ViewGroup parent = (ViewGroup) chart.getParent();
        int index = parent.indexOfChild(chart);
        ViewGroup.LayoutParams old = chart.getLayoutParams();
        int chartHeight = old.height;
        parent.removeView(chart);
        Panel panel = new Panel(chart, title);
        LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panel.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, chartHeight));
        chart.setBackgroundColor(Color.TRANSPARENT);
        parent.addView(panel, index, outer);
    }

    private static final class Panel extends LinearLayout {
        private final View chart;
        Panel(View chart, int title) {
            super(chart.getContext());
            this.chart = chart;
            setOrientation(VERTICAL);
            setBackground(new InsetDrawable(surface(this), 0, dp(this, 12), 0, 0));
            setPadding(dp(this, 16), dp(this, 28), dp(this, 16), dp(this, 16));
            TextView heading = new TextView(getContext());
            heading.setText(title);
            heading.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
            heading.setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
            heading.setPadding(0, 0, 0, dp(this, 12));
            ViewCompat.setAccessibilityHeading(heading, true);
            addView(heading);
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            if (chart.getVisibility() == GONE) setMeasuredDimension(MeasureSpec.getSize(widthSpec), 0);
            else super.onMeasure(widthSpec, heightSpec);
        }
    }
}
