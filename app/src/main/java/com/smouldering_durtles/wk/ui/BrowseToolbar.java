package com.smouldering_durtles.wk.ui;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;

/** Material toolbar/search input without changing the theme of legacy browse fragments. */
public final class BrowseToolbar extends Toolbar {
    public BrowseToolbar(Context context, AttributeSet attrs) {
        super(GlobalSettings.Display.getModernSettings() ? SearchStyle.context(context)
                : new ContextThemeWrapper(context, R.style.WKToolbarTheme), attrs);
    }

    @Override
    public void setBackgroundColor(int color) {
        // Subject headlines may request a semantic toolbar color. Keep browsing chrome neutral;
        // the subject headline inside the page retains its radical/kanji/vocabulary color.
        super.setBackgroundColor(SearchStyle.isModern(getContext())
                ? com.google.android.material.color.MaterialColors.getColor(this,
                    com.google.android.material.R.attr.colorSurface) : color);
    }
}
