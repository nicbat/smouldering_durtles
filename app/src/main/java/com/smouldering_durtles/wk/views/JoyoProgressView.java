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
import com.smouldering_durtles.wk.livedata.LiveJoyoProgress;
import com.smouldering_durtles.wk.model.JoyoProgress;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A custom view that shows a breakdown of Joyo progress.
 */
public final class JoyoProgressView extends TableLayout {
    private final List<CoverageProgressRowView> modernRows = new ArrayList<>();

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public JoyoProgressView(final Context context) {
        super(context);
        safe(this::init);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public JoyoProgressView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        safe(this::init);
    }

    /**
     * Initialize the view by observing the relevant LiveData instances.
     */
    private void init() {
        inflate(getContext(), R.layout.joyo_progress_modern, this);

        int[] rows = {R.id.joyoGrade1Row, R.id.joyoGrade2Row, R.id.joyoGrade3Row, R.id.joyoGrade4Row,
                R.id.joyoGrade5Row, R.id.joyoGrade6Row, R.id.joyoGrade7Row};
        for (int i = 0; i < rows.length; i++) {
            CoverageProgressRowView row = findViewById(rows[i]);
            row.setLabel(i == 6 ? getContext().getString(R.string.coverage_secondary_school)
                                : getContext().getString(R.string.coverage_grade, i + 1));
            modernRows.add(row);
        }
    }

    /**
     * Set the lifecycle owner for this view, to hook LiveData observers to.
     *
     * @param lifecycleOwner the lifecycle owner
     */
    public void setLifecycleOwner(final LifecycleOwner lifecycleOwner) {
        safe(() -> LiveJoyoProgress.getInstance().observe(lifecycleOwner, t -> safe(() -> update(t))));
    }

    /**
     * Update the table based on the latest progress data.
     *
     * @param joyoProgress the progress
     */
    private void update(final @Nullable JoyoProgress joyoProgress) {
        if (joyoProgress == null || LiveFirstTimeSetup.getInstance().get() == 0
                || !GlobalSettings.Dashboard.getShowJoyoProgress()) {
            setVisibility(GONE);
            return;
        }

        for (int i = 0; i < modernRows.size(); i++) {
            modernRows.get(i).setCounts(joyoProgress.getLocked(i + 1), joyoProgress.getPrePassed(i + 1),
                    joyoProgress.getPassed(i + 1), joyoProgress.getBurned(i + 1));
        }
        setVisibility(VISIBLE);
    }
}
