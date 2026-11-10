package com.smouldering_durtles.wk.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;
import com.google.android.material.slider.Slider;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;

/** Material presentation over AndroidX's integer preference, persistence, bounds and saved state. */
public final class MaterialSliderPreference extends SeekBarPreference {
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener fontListener =
            (preferences, key) -> { if ("selected_fonts".equals(key) || "quiz_question_view_height".equals(key)) notifyChanged(); };

    @Override public void onAttached() {
        super.onAttached();
        if (getSharedPreferences() != null) getSharedPreferences().registerOnSharedPreferenceChangeListener(fontListener);
    }

    @Override public void onDetached() {
        if (getSharedPreferences() != null) getSharedPreferences().unregisterOnSharedPreferenceChangeListener(fontListener);
        super.onDetached();
    }

    public MaterialSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_slider);
    }

    @Override public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        Slider slider = (Slider) holder.findViewById(R.id.materialPreferenceSlider);
        if (slider == null) return;
        holder.itemView.setOnKeyListener(null); // Keyboard events belong to the visible Slider.
        slider.clearOnChangeListeners();
        slider.clearOnSliderTouchListeners();
        slider.setValueFrom(getMin());
        slider.setValueTo(Math.max(getMax(), getMin() + 1));
        int increment = Math.max(1, getSeekBarIncrement());
        slider.setStepSize((getMax() - getMin()) % increment == 0 ? increment : 1);
        slider.setValue(getValue());
        slider.setEnabled(isEnabled() && getMax() > getMin());
        slider.setContentDescription(getTitle());
        com.smouldering_durtles.wk.views.FontSizePreviewView preview =
                (com.smouldering_durtles.wk.views.FontSizePreviewView) holder.findViewById(R.id.fontSizePreview);
        boolean fontSize = com.smouldering_durtles.wk.views.FontSizePreviewView.supports(getKey());
        if (preview != null) {
            preview.setVisibility(fontSize ? android.view.View.VISIBLE : android.view.View.GONE);
            if (fontSize) preview.update(getKey(), getValue());
        }
        final boolean[] tracking = {false};
        TextView valueLabel = (TextView) holder.findViewById(androidx.preference.R.id.seekbar_value);
        slider.addOnChangeListener((control, value, fromUser) -> {
            if (!fromUser) return;
            if (fontSize && preview != null) preview.update(getKey(), Math.round(value));
            if (valueLabel != null) valueLabel.setText(Integer.toString(Math.round(value)));
            if (!tracking[0] || getUpdatesContinuously()) {
                acceptValue(control);
                if (fontSize && preview != null) preview.update(getKey(), getValue());
                if (valueLabel != null) valueLabel.setText(Integer.toString(getValue()));
            }
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(Slider control) { tracking[0] = true; }
            @Override public void onStopTrackingTouch(Slider control) {
                tracking[0] = false;
                acceptValue(control);
                if (fontSize && preview != null) preview.update(getKey(), getValue());
                if (valueLabel != null) valueLabel.setText(Integer.toString(getValue()));
            }
        });
    }

    private void acceptValue(Slider control) {
        int next = Math.round(control.getValue());
        if (next == getValue()) return;
        if (callChangeListener(next)) setValue(next);
        else control.setValue(getValue());
    }
}
