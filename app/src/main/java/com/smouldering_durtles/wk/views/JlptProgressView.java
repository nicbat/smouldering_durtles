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
import android.util.AttributeSet;
import android.widget.TableLayout;

import androidx.lifecycle.LifecycleOwner;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup;
import com.smouldering_durtles.wk.livedata.LiveJlptProgress;
import com.smouldering_durtles.wk.model.JlptProgress;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A custom view that shows a breakdown of JLPT progress.
 */
public final class JlptProgressView extends TableLayout {
    private final List<CoverageProgressRowView> modernRows = new ArrayList<>();

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public JlptProgressView(final Context context) {
        super(context);
        safe(this::init);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public JlptProgressView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        safe(this::init);
    }

    /**
     * Initialize the view by observing the relevant LiveData instances.
     */
    private void init() {
        inflate(getContext(), R.layout.jlpt_progress_modern, this);

        int[] rows = {
                R.id.jlptLevel1Row, R.id.jlptLevel2Row, R.id.jlptLevel3Row, R.id.jlptLevel4Row, R.id.jlptLevel5Row};
        for (int i = 0; i < rows.length; i++) {
            CoverageProgressRowView row = findViewById(rows[i]);
            row.setLabel(getContext().getString(R.string.coverage_jlpt_level, i + 1));
            modernRows.add(row);
        }
    }

    /**
     * Set the lifecycle owner for this view, to hook LiveData observers to.
     *
     * @param lifecycleOwner the lifecycle owner
     */
    public void setLifecycleOwner(final LifecycleOwner lifecycleOwner) {
        safe(() -> LiveJlptProgress.getInstance().observe(lifecycleOwner, t -> safe(() -> update(t))));
    }

    /**
     * Update the table based on the latest progress data.
     *
     * @param jlptProgress the progress
     */
    private void update(final @Nullable JlptProgress jlptProgress) {
        if (jlptProgress == null || LiveFirstTimeSetup.getInstance().get() == 0
                || !GlobalSettings.Dashboard.getShowJlptProgress()) {
            setVisibility(GONE);
            return;
        }

        for (int i = 0; i < modernRows.size(); i++) {
            modernRows.get(i).setCounts(jlptProgress.getLocked(i + 1), jlptProgress.getPrePassed(i + 1),
                    jlptProgress.getPassed(i + 1), jlptProgress.getBurned(i + 1));
        }
        setVisibility(VISIBLE);
    }
}
