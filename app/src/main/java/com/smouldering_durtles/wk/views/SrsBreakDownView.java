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

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.smouldering_durtles.wk.Actment;
import com.smouldering_durtles.wk.dashboard.DashboardStyle;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.livedata.LiveSrsBreakDown;
import com.smouldering_durtles.wk.model.AdvancedSearchParameters;
import com.smouldering_durtles.wk.model.SrsBreakDown;
import com.smouldering_durtles.wk.model.SrsSystemRepository;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.ThemeUtil;
import com.smouldering_durtles.wk.ui.LearningColors;
import android.widget.TextView;
import java.text.NumberFormat;
import com.smouldering_durtles.wk.util.WeakLcoRef;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A custom view that shows the SRS breakdown on the dashboard.
 */
public final class SrsBreakDownView extends ConstraintLayout {
    private SrsBreakDown latest;
    private final List<ViewProxy> counts = new ArrayList<>();
    private final List<ViewProxy> views = new ArrayList<>();

    private @Nullable WeakLcoRef<Actment> actmentRef = null;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public SrsBreakDownView(final Context context) {
        super(context);
        safe(this::init);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public SrsBreakDownView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        safe(this::init);
    }

    /**
     * Initialize the view by observing the relevant LiveData instances.
     */
    private void init() {
        final boolean modern = DashboardStyle.isModern(getContext());
        inflate(getContext(), modern ? R.layout.srs_breakdown_modern : R.layout.srs_breakdown, this);
        setBackgroundResource(R.drawable.main_activity_view_background);

        counts.add(new ViewProxy(this, R.id.breakdownBucket0Count));
        counts.add(new ViewProxy(this, R.id.breakdownBucket1Count));
        counts.add(new ViewProxy(this, R.id.breakdownBucket2Count));
        counts.add(new ViewProxy(this, R.id.breakdownBucket3Count));
        counts.add(new ViewProxy(this, R.id.breakdownBucket4Count));

        views.add(new ViewProxy(this, R.id.breakdownBucket0View));
        views.add(new ViewProxy(this, R.id.breakdownBucket1View));
        views.add(new ViewProxy(this, R.id.breakdownBucket2View));
        views.add(new ViewProxy(this, R.id.breakdownBucket3View));
        views.add(new ViewProxy(this, R.id.breakdownBucket4View));

        for (int i=0; i<5; i++) {
            if (modern) {
                int[] swatches = {R.id.stageSwatch0, R.id.stageSwatch1, R.id.stageSwatch2, R.id.stageSwatch3, R.id.stageSwatch4};
                findViewById(swatches[i]).setBackgroundColor(LearningColors.accent(getContext(), ActiveTheme.getShallowStageBucketColors5()[i]));
                continue;
            }
            Drawable background = views.get(i).getBackground();
            if (background != null) {
                background.setColorFilter(new PorterDuffColorFilter(
                        ActiveTheme.getShallowStageBucketColors5()[i],
                        PorterDuff.Mode.SRC_ATOP));
            }
            if (ActiveTheme.getCurrentTheme() == ActiveTheme.LIGHT) {
                counts.get(i).setShadowLayer(3, 1, 1, Color.BLACK);
            }
            else {
                counts.get(i).setShadowLayer(0, 0, 0, 0);
            }
        }

        views.get(0).setOnClickListener(v -> safe(() -> {
            if (actmentRef == null) {
                return;
            }
            final @Nullable Actment actment = actmentRef.get();
            final AdvancedSearchParameters parameters = new AdvancedSearchParameters();
            for (int i=0; i<SrsSystemRepository.getMaxNumApprenticeStages(); i++) {
                parameters.srsStages.add("prepass:" + i);
            }
            final String searchParameters = Converters.getObjectMapper().writeValueAsString(parameters);
            actment.goToSearchResult(2, searchParameters, null);
        }));

        views.get(1).setOnClickListener(v -> safe(() -> {
            if (actmentRef == null) {
                return;
            }
            final @Nullable Actment actment = actmentRef.get();
            final AdvancedSearchParameters parameters = new AdvancedSearchParameters();
            for (int i=0; i<SrsSystemRepository.getMaxNumGuruStages(); i++) {
                parameters.srsStages.add("pass:" + i);
            }
            final String searchParameters = Converters.getObjectMapper().writeValueAsString(parameters);
            actment.goToSearchResult(2, searchParameters, null);
        }));

        views.get(2).setOnClickListener(v -> safe(() -> {
            if (actmentRef == null) {
                return;
            }
            final @Nullable Actment actment = actmentRef.get();
            final AdvancedSearchParameters parameters = new AdvancedSearchParameters();
            parameters.srsStages.add("master");
            final String searchParameters = Converters.getObjectMapper().writeValueAsString(parameters);
            actment.goToSearchResult(2, searchParameters, null);
        }));

        views.get(3).setOnClickListener(v -> safe(() -> {
            if (actmentRef == null) {
                return;
            }
            final @Nullable Actment actment = actmentRef.get();
            final AdvancedSearchParameters parameters = new AdvancedSearchParameters();
            parameters.srsStages.add("enlightened");
            final String searchParameters = Converters.getObjectMapper().writeValueAsString(parameters);
            actment.goToSearchResult(2, searchParameters, null);
        }));

        views.get(4).setOnClickListener(v -> safe(() -> {
            if (actmentRef == null) {
                return;
            }
            final @Nullable Actment actment = actmentRef.get();
            final AdvancedSearchParameters parameters = new AdvancedSearchParameters();
            parameters.srsStages.add("burned");
            final String searchParameters = Converters.getObjectMapper().writeValueAsString(parameters);
            actment.goToSearchResult(2, searchParameters, null);
        }));
    }

    private void splitPresentation(SrsBreakDown model) {
        android.widget.LinearLayout content = (android.widget.LinearLayout) findViewById(R.id.srsDistributionBar).getParent();
        DashboardLegendLayout split = findViewById(R.id.srsSplitLegend);
        if (split == null) {
            split = new DashboardLegendLayout(getContext(), null);
            split.setId(R.id.srsSplitLegend);
            content.addView(split, new android.widget.LinearLayout.LayoutParams(-1, -2));
            TextView toggle = new TextView(getContext());
            toggle.setId(R.id.srsSplitToggle);
            toggle.setTextSize(14);
            toggle.setTextColor(com.google.android.material.color.MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
            toggle.setMinHeight(com.smouldering_durtles.wk.ui.ModernUi.dp(this, 48));
            toggle.setGravity(android.view.Gravity.CENTER_VERTICAL);
            toggle.setFocusable(true);
            toggle.setOnClickListener(v -> {
                android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putBoolean("srs_split_stages", !prefs.getBoolean("srs_split_stages", false)).apply();
                update(latest);
            });
            content.addView(toggle, new android.widget.LinearLayout.LayoutParams(-1, -2));
        }
        boolean expanded = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("srs_split_stages", false);
        findViewById(R.id.srsLegend).setVisibility(expanded ? GONE : VISIBLE);
        split.setVisibility(expanded ? VISIBLE : GONE);
        ((TextView) findViewById(R.id.srsSplitToggle)).setText(expanded ? R.string.srs_group_stages : R.string.srs_split_stages);
        androidx.core.view.ViewCompat.setStateDescription(findViewById(R.id.srsSplitToggle), getContext().getString(
                expanded ? R.string.lesson_expanded : R.string.lesson_collapsed));
        SrsDistributionBarView bar = findViewById(R.id.srsDistributionBar);
        if (!expanded) { bar.setColors(null); return; }
        split.removeAllViews();
        int[] values = new int[9];
        int[] colors = java.util.Arrays.copyOfRange(ActiveTheme.getStageDeepBucketColors(), 2, 11);
        String[] labels = {"A1", "A2", "A3", "A4", "G1", "G2", "Master", "Enl.", "Burned"};
        String[] full = {"Apprentice I", "Apprentice II", "Apprentice III", "Apprentice IV", "Guru I", "Guru II", "Master", "Enlightened", "Burned"};
        for (int i = 0; i < 9; i++) {
            final int bucket = i;
            values[i] = model.getPost60DeepCount(i + 2);
            TextView cell = new TextView(getContext());
            cell.setText(labels[i] + "\n" + NumberFormat.getIntegerInstance().format(values[i]));
            cell.setTextSize(12);
            cell.setGravity(android.view.Gravity.CENTER);
            cell.setMinHeight(com.smouldering_durtles.wk.ui.ModernUi.dp(this, 48));
            cell.setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
            cell.setContentDescription(full[i] + ", " + values[i]);
            cell.setFocusable(true);
            android.graphics.drawable.ColorDrawable swatch = new android.graphics.drawable.ColorDrawable(LearningColors.accent(getContext(), colors[i]));
            swatch.setBounds(0, 0, com.smouldering_durtles.wk.ui.ModernUi.dp(this, 16), com.smouldering_durtles.wk.ui.ModernUi.dp(this, 3));
            cell.setCompoundDrawables(null, null, null, swatch);
            cell.setCompoundDrawablePadding(com.smouldering_durtles.wk.ui.ModernUi.dp(this, 4));
            cell.setOnClickListener(v -> safe(() -> {
                if (actmentRef == null) return;
                AdvancedSearchParameters parameters = new AdvancedSearchParameters();
                if (bucket < 4) {
                    int end = bucket == 3 ? SrsSystemRepository.getMaxNumApprenticeStages() : bucket + 1;
                    for (int n = bucket; n < end; n++) parameters.srsStages.add("prepass:" + n);
                } else if (bucket < 6) {
                    int start = bucket - 4;
                    int end = bucket == 5 ? SrsSystemRepository.getMaxNumGuruStages() : start + 1;
                    for (int n = start; n < end; n++) parameters.srsStages.add("pass:" + n);
                } else parameters.srsStages.add(new String[]{"master", "enlightened", "burned"}[bucket - 6]);
                actmentRef.get().goToSearchResult(2, Converters.getObjectMapper().writeValueAsString(parameters), null);
            }));
            split.addView(cell);
        }
        bar.setColors(colors);
        bar.setValues(values);
    }

    /**
     * Set the lifecycle owner for this view, to hook LiveData observers to.
     *
     * @param actment the lifecycle owner
     */
    public void setLifecycleOwner(final Actment actment) {
        actmentRef = new WeakLcoRef<>(actment);
        safe(() -> LiveSrsBreakDown.getInstance().observe(actment, t -> safe(() -> {
            if (t != null) {
                update(t);
            }
        })));
    }

    /**
     * Update the text based on the latest SrsBreakDown data available.
     *
     * @param srsBreakDown the SRS breakdown summary
     */
    private void update(final SrsBreakDown srsBreakDown) {
        latest = srsBreakDown;
        if (GlobalSettings.getFirstTimeSetup() == 0 || !GlobalSettings.Dashboard.getShowSrsBreakdown()) {
            setVisibility(GONE);
            return;
        }

        setVisibility(VISIBLE);

        int[] distribution = new int[5];
        long total = 0;
        boolean modern = DashboardStyle.isModern(getContext());
        String[] stages = {"Apprentice", "Guru", "Master", "Enlightened", "Burned"};
        int[] stageIds = {R.id.breakdownBucket0View, R.id.breakdownBucket1View,
                R.id.breakdownBucket2View, R.id.breakdownBucket3View, R.id.breakdownBucket4View};
        for (int i=0; i<5; i++) {
            distribution[i] = srsBreakDown.getSrsBreakdownCount(i);
            total += distribution[i];
            counts.get(i).setText(distribution[i]);
            if (modern) findViewById(stageIds[i])
                    .setContentDescription(stages[i] + ", " + distribution[i]);
        }
        if (modern) {
            ((SrsDistributionBarView) findViewById(R.id.srsDistributionBar)).setValues(distribution);
            splitPresentation(srsBreakDown);
            ((TextView) findViewById(R.id.srsDistributionTotal)).setText(total == 0
                    ? getContext().getString(R.string.dashboard_srs_empty)
                    : getContext().getString(R.string.dashboard_srs_total, NumberFormat.getIntegerInstance().format(total)));
        }
    }
}
