package com.smouldering_durtles.wk.activities;

import android.content.res.Resources;
import com.smouldering_durtles.wk.ui.ModernUi;

/** Material presentation for utility screens, preserving their existing lifecycle and action bindings. */
public abstract class ModernActivity extends AbstractActivity {
    protected ModernActivity(int layout, int menu) { super(layout, menu); }

    @Override protected void customizeActivityTheme(Resources.Theme theme) {
        ModernUi.applyTheme(this, theme);
    }

    @Override protected void styleActivityContent() {
        ModernUi.styleActivity(this);
    }
}
