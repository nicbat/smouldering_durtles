package com.smouldering_durtles.wk.dashboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
        ViewGroup scroll = activity.findViewById(R.id.mainScrollView);
        View content = scroll.getChildAt(0);
        content.setPadding(dp(content, 16), dp(content, 8), dp(content, 16), dp(content, 16));
        androidx.core.view.WindowInsetsControllerCompat bars = androidx.core.view.WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(MaterialColors.isColorLight(surface));
        bars.setAppearanceLightNavigationBars(MaterialColors.isColorLight(surface));
        View timeline = activity.findViewById(R.id.timeLineBarChart);
        timeline.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        titledPanel(timeline, R.string.dashboard_forecast_title);
        titledPanel(activity.findViewById(R.id.srsBreakDownView), R.string.dashboard_srs_title);
        for (int id : new int[] {R.id.levelProgressView, R.id.post60ProgressView, R.id.joyoProgressView,
                R.id.jlptProgressView, R.id.lessonReviewBreakdownView}) {
            View view = activity.findViewById(id);
            if (view != null) card(view);
        }
        for (int id : new int[] {R.id.joyoProgressView, R.id.jlptProgressView, R.id.lessonReviewBreakdownView}) {
            statisticsTable((android.widget.TableLayout) activity.findViewById(id));
        }
        ViewGroup post60 = activity.findViewById(R.id.post60ProgressView);
        if (post60 != null && post60.getChildAt(0) instanceof TextView) heading((TextView) post60.getChildAt(0));
        for (int id : new int[] {R.id.keyboardHelpView, R.id.firstTimeSetupView}) {
            View view = activity.findViewById(id);
            card(view);
            com.smouldering_durtles.wk.ui.ModernUi.style(view);
        }
        for (int id : new int[] {R.id.apiErrorView, R.id.apiKeyRejectedView}) errorPanel((LinearLayout) activity.findViewById(id));
        int[][] panels = {{R.id.modernDashboard, R.string.dashboard_title},
                {R.id.levelProgressView, R.string.dashboard_level_progress_title},
                {R.id.post60ProgressView, R.string.dashboard_post60_title},
                {R.id.joyoProgressView, R.string.coverage_joyo_title},
                {R.id.jlptProgressView, R.string.coverage_jlpt_title},
                {R.id.lessonReviewBreakdownView, R.string.dashboard_breakdown_title},
                {R.id.recentUnlocksView, R.string.recent_unlocks_compact},
                {R.id.criticalConditionView, R.string.dashboard_critical_title},
                {R.id.burnedItemsView, R.string.recent_burns_compact},
                {R.id.keyboardHelpView, R.string.dashboard_keyboard_title},
                {R.id.firstTimeSetupView, R.string.dashboard_setup_title},
                {R.id.apiKeyRejectedView, R.string.dashboard_api_token_title}};
        for (int[] entry : panels) {
            View view = activity.findViewById(entry[0]);
            if (view instanceof ViewGroup && entry[0] != R.id.modernDashboard
                    && entry[0] != R.id.lessonReviewBreakdownView && entry[0] != R.id.keyboardHelpView
                    && entry[0] != R.id.firstTimeSetupView && entry[0] != R.id.apiKeyRejectedView) {
                ((ViewGroup) view).getChildAt(0).setVisibility(View.GONE);
            }
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setPadding(0, 0, 0, 0);
            CollapsibleDashboardPanel.wrap(view, activity.getText(entry[1]));
        }
    }

    private static void heading(TextView title) {
        title.setTextAppearance(title.getContext(), com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        title.setTextColor(MaterialColors.getColor(title, com.google.android.material.R.attr.colorOnSurface));
        title.setGravity(android.view.Gravity.START);
        title.setPadding(0, 0, 0, dp(title, 12));
        ViewCompat.setAccessibilityHeading(title, true);
    }

    /** Tables remain tables: preserve IDs/data and give dense statistics a readable type/spacing scale. */
    private static void statisticsTable(android.widget.TableLayout table) {
        if (table == null) return;
        for (int i = 0; i < table.getChildCount(); i++) {
            View child = table.getChildAt(i);
            if (!(child instanceof android.widget.TableRow)) continue;
            android.widget.TableRow row = (android.widget.TableRow) child;
            row.setMinimumHeight(dp(row, 48));
            row.setPadding(0, dp(row, 4), 0, dp(row, 4));
            for (int j = 0; j < row.getChildCount(); j++) {
                View cell = row.getChildAt(j);
                if (!(cell instanceof TextView)) continue;
                TextView text = (TextView) cell;
                if (i == 0 && row.getChildCount() == 1) heading(text);
                else {
                    text.setTextSize(14);
                    text.setPadding(dp(text, 4), dp(text, 8), dp(text, 4), dp(text, 8));
                    text.setGravity(j == 0 ? android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL
                            : android.view.Gravity.CENTER);
                }
            }
        }
    }

    private static void errorPanel(LinearLayout panel) {
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(panel, 16), dp(panel, 12), dp(panel, 16), dp(panel, 12));
        panel.setBackground(com.smouldering_durtles.wk.ui.ModernUi.surface(panel,
                MaterialColors.getColor(panel, com.google.android.material.R.attr.colorErrorContainer), 16));
        for (int i = 0; i < panel.getChildCount(); i++) {
            View child = panel.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.weight = 0;
            if (child instanceof android.widget.Button) com.smouldering_durtles.wk.ui.ModernUi.button((android.widget.Button) child);
            else if (child instanceof TextView) ((TextView) child).setTextColor(
                    MaterialColors.getColor(panel, com.google.android.material.R.attr.colorOnErrorContainer));
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
                .setAllCornerSizes(dp(view, 16)).build());
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
        int padding = dp(view, 12);
        view.setPadding(padding, padding, padding, padding);
        textColors(view);
    }

    /** Adds a heading to an existing chart. A hidden chart takes no space, including its heading. */
    public static void titledPanel(View chart, int title) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        CollapsibleDashboardPanel.wrap(chart, chart.getResources().getText(title));
    }
}
