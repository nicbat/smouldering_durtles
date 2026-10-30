package com.smouldering_durtles.wk.ui;

import android.content.res.ColorStateList;
import android.view.Menu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.smouldering_durtles.wk.R;

/** Screen-local toolbar presentation; AppCompat keeps ownership of menu actions/submenus. */
public final class MaterialToolbar {
    private MaterialToolbar() {}

    public static void styleOverflow(Toolbar toolbar, boolean dynamicColor) {
        toolbar.setPopupTheme(dynamicColor && DynamicColors.isDynamicColorAvailable()
                ? R.style.SmokyOverflowDynamicTheme : R.style.SmokyOverflowTheme);
    }

    public static void tintIcons(Toolbar toolbar, Menu menu) {
        int color = MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorOnSurface);
        for (int i = 0; i < menu.size(); i++) {
            MenuItemCompat.setIconTintList(menu.getItem(i), ColorStateList.valueOf(color));
        }
    }
}
