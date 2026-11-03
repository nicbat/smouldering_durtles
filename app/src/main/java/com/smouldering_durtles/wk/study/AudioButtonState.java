package com.smouldering_durtles.wk.study;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.core.view.ViewCompat;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;

/** Presentation of the same persisted mute flag used by AudioUtil. */
public final class AudioButtonState {
    private AudioButtonState() { }
    public static void bind(Button button) {
        button.setTag(R.id.audioAction, true);
        refresh(button);
    }
    public static void refresh(View root) {
        if (root == null) return;
        refresh(root, WkApplication.getDatabase().propertiesDao().getIsMuted());
    }
    public static void refresh(View root, boolean muted) {
        if (Boolean.TRUE.equals(root.getTag(R.id.audioAction))) {
            root.setEnabled(!muted);
            ViewCompat.setStateDescription(root, muted ? root.getContext().getString(R.string.audio_muted) : null);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) refresh(group.getChildAt(i), muted);
        }
    }
}
