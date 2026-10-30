package com.smouldering_durtles.wk.ui;

import android.content.Context;
import android.util.TypedValue;
import androidx.appcompat.view.ContextThemeWrapper;
import com.google.android.material.color.DynamicColors;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;

/** A local Material context: shared study/subject Views retain their original theme. */
public final class SearchStyle {
    private SearchStyle() {}

    public static Context context(Context base) {
        Context context = new ContextThemeWrapper(base, R.style.SmokySearchTheme);
        return GlobalSettings.Display.getSettingsDynamicColor()
                ? DynamicColors.wrapContextIfAvailable(context) : context;
    }

    public static boolean isModern(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.smokyModernSearch, value, true) && value.data != 0;
    }
}
