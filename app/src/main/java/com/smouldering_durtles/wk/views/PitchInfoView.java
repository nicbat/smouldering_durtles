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
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.model.PitchInfo;
import com.smouldering_durtles.wk.util.ViewUtil;

import javax.annotation.Nullable;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
import static com.smouldering_durtles.wk.Constants.FONT_SIZE_NORMAL;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Custom view that shows the pitch info for one reading.
 */
public final class PitchInfoView extends ConstraintLayout {
    private int textSize = FONT_SIZE_NORMAL;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public PitchInfoView(final Context context) {
        super(context);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public PitchInfoView(final Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     * @param defStyleAttr the default style
     */
    public PitchInfoView(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * The normal text size for the text in the headline.
     * @param textSize the value
     */
    public void setTextSize(final int textSize) {
        this.textSize = textSize;
    }

    private void setPitchInfoImpl(final String kana, final Iterable<PitchInfo> pitchInfos) {
        removeAllViews();

        android.widget.LinearLayout patterns = new android.widget.LinearLayout(getContext());
        patterns.setOrientation(android.widget.LinearLayout.VERTICAL);
        for (final PitchInfo info : pitchInfos) {
            PitchInfoDiagramView diagram = new PitchInfoDiagramView(getContext());
            diagram.setPitchNumber(info.getPitchNumber());
            diagram.setText(kana);
            diagram.setTextSize(textSize + 4);
            android.widget.LinearLayout annotated = new android.widget.LinearLayout(getContext());
            annotated.setOrientation(android.widget.LinearLayout.VERTICAL);
            annotated.addView(diagram);
            TextView reading = new TextView(getContext());
            reading.setText(kana);
            reading.setTextSize(textSize + 4);
            reading.setIncludeFontPadding(false);
            ViewUtil.setJapaneseLocale(reading);
            annotated.addView(reading);
            // Long readings may scroll as one annotated unit; never separate the path from its morae.
            android.widget.HorizontalScrollView strip = new android.widget.HorizontalScrollView(getContext());
            strip.setFillViewport(false);
            strip.addView(annotated);
            patterns.addView(strip, new android.widget.LinearLayout.LayoutParams(-1, -2));
            String label = patternLabel(info.getPitchNumber(), diagram.getNumMora());
            TextView caption = new TextView(getContext());
            caption.setText(label + " · " + info.getPitchNumber()
                    + (isEmpty(info.getPartOfSpeech()) ? "" : " (" + info.getPartOfSpeech() + ")"));
            caption.setTextSize(textSize);
            caption.setPadding(0, dp2px(4), 0, dp2px(8));
            patterns.addView(caption, new android.widget.LinearLayout.LayoutParams(-1, -2));
            diagram.setContentDescription(kana + ", " + label + ", " + info.getPitchNumber());
        }
        LayoutParams params = new LayoutParams(0, WRAP_CONTENT);
        params.startToStart = params.endToEnd = params.topToTop = PARENT_ID;
        addView(patterns, params);
    }

    public static String patternLabel(int number, int morae) {
        if (number == 0) return "平板 · Flat";
        if (number == 1) return "頭高 · Falling";
        if (number == morae) return "尾高 · Final drop";
        return "中高 · Rise–fall";
    }

    /**
     * Set the kana reading and pitch info records for this view.
     *
     * @param kana the reading as a string of kana
     * @param pitchInfos the list of applicable pitch info records
     */
    public void setPitchInfo(final String kana, final Iterable<PitchInfo> pitchInfos) {
        safe(() -> setPitchInfoImpl(kana, pitchInfos));
    }

    private int dp2px(@SuppressWarnings("SameParameterValue") final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
