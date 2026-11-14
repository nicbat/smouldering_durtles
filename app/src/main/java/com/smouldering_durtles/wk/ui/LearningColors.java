package com.smouldering_durtles.wk.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.smouldering_durtles.wk.GlobalSettings;

/** Presentation policy only: never rewrites the user's semantic palettes or subject data. */
public final class LearningColors {
    private LearningColors() { }

    public static int subject(Context context, int type) {
        if ("original".equals(GlobalSettings.Display.getLearningColors()))
            return com.smouldering_durtles.wk.enums.ActiveTheme.getSubjectTypeButtonBackgroundColors()[type];
        // The legacy dark palettes use neutral panel/button backgrounds. Soft identity
        // controls keep the planned blue/pink/purple hues across system theme changes.
        return new int[]{0xff467bc1, 0xffbd4e7d, 0xff8b5ac2}[Math.max(0, Math.min(2, type))];
    }

    public static int stage(Context context, int bucket) {
        int[] defaults = {0xffbd4e7d, 0xff8b5ac2, 0xff467bc1, 0xff248b80, 0xff70777d};
        if ("original".equals(GlobalSettings.Display.getLearningColors()))
            return com.smouldering_durtles.wk.enums.ActiveTheme.getShallowStageBucketColors5()[Math.max(0, Math.min(4, bucket))];
        return accent(context, defaults[Math.max(0, Math.min(4, bucket))]);
    }
    public static int cue(Context context, boolean reading) {
        return accent(context, reading ? 0xffaa730d : 0xff26845c);
    }

    public static boolean isMuted(Context context) {
        return (ModernUi.isModern(context) || com.smouldering_durtles.wk.dashboard.DashboardStyle.isModern(context)) && !"original".equals(GlobalSettings.Display.getLearningColors());
    }

    public static int fill(Context context, int original) {
        if (!isMuted(context)) return original;
        int surface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerLow, original);
        return "neutral".equals(GlobalSettings.Display.getLearningColors()) ? surface
                : ColorUtils.blendARGB(surface, original, 0.08f);
    }

    public static int ink(Context context, int original) {
        return isMuted(context) ? MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOnSurface, original) : original;
    }

    public static int accent(Context context, int original) {
        if (!isMuted(context)) return original;
        if ("neutral".equals(GlobalSettings.Display.getLearningColors())) {
            return MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, original);
        }
        int onSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, original);
        return ColorUtils.blendARGB(original, onSurface, 0.24f);
    }

    public static MaterialShapeDrawable surface(View view, int original, int radius) {
        MaterialShapeDrawable shape = ModernUi.surface(view, fill(view.getContext(), original), radius);
        if (isMuted(view.getContext())) shape.setStroke(ModernUi.dp(view, 1),
                ColorStateList.valueOf(accent(view.getContext(), original)));
        return shape;
    }
}
