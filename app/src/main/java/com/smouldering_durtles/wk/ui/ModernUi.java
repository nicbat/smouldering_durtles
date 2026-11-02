package com.smouldering_durtles.wk.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.AbstractActivity;
import com.smouldering_durtles.wk.util.ThemeUtil;

/** Shared presentation primitives. Existing listeners, model bindings and saved-state types stay intact. */
public final class ModernUi {
    private ModernUi() {}

    /** Explicit action hierarchy; a stored view tag keeps repeated styling from erasing the role. */
    public enum ButtonRole { PRIMARY, SECONDARY, TEXT }

    public static Context context(Context base) {
        if (isModern(base)) return base;
        Context themed = new ContextThemeWrapper(base, R.style.SmokyUiTheme);
        return GlobalSettings.Display.getSettingsDynamicColor() ? DynamicColors.wrapContextIfAvailable(themed) : themed;
    }

    public static boolean isModern(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.smokyModernUi, value, true) && value.data != 0;
    }

    public static void applyTheme(Activity activity, Resources.Theme theme) {
        theme.applyStyle(R.style.SmokyClassicSettingsOverlay, true);
        if (GlobalSettings.Display.getModernSettings()) {
            theme.applyStyle(R.style.SmokyUiTheme, true);
            if (GlobalSettings.Display.getSettingsDynamicColor()) DynamicColors.applyToActivityIfAvailable(activity);
        }
    }

    public static AlertDialog.Builder dialog(Context context) {
        return GlobalSettings.Display.getModernSettings()
                ? new MaterialAlertDialogBuilder(context(context)) : new AlertDialog.Builder(context);
    }

    public static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    public static MaterialShapeDrawable surface(View view, int color, int radius) {
        MaterialShapeDrawable drawable = new MaterialShapeDrawable(ShapeAppearanceModel.builder()
                .setAllCornerSizes(dp(view, radius)).build());
        drawable.setFillColor(ColorStateList.valueOf(color));
        return drawable;
    }

    /** Apply before binding semantic subject/question colors. Never called automatically on study content. */
    public static void style(View view) {
        if (!isModern(view.getContext()) || view instanceof Toolbar) return;
        final int foreground = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface);
        if (view instanceof android.widget.Spinner) {
            view.setMinimumHeight(dp(view, 48));
        }
        if (view instanceof CompoundButton) {
            ((CompoundButton) view).setTextColor(foreground);
            view.setMinimumHeight(dp(view, 48));
        } else if (view instanceof Button) {
            button((Button) view);
        } else if (view instanceof EditText) {
            EditText input = (EditText) view;
            input.setTextColor(foreground);
            input.setHintTextColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant));
            // TextInputLayout owns its child's outline; do not draw a second one.
            if (!(view.getParent() instanceof android.widget.FrameLayout
                    && ((View) view.getParent()).getParent() instanceof com.google.android.material.textfield.TextInputLayout)) {
                MaterialShapeDrawable background = surface(view,
                        MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerLow), 8);
                background.setStroke(dp(view, 1), MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutline));
                ViewCompat.setBackgroundTintList(input, null);
                input.setBackground(background);
                input.setPadding(dp(view, 16), dp(view, 12), dp(view, 16), dp(view, 12));
            }
            input.setMinimumHeight(dp(view, 56));
        } else if (view instanceof TextView) {
            TextView text = (TextView) view;
            // Only replace neutral legacy ink; colored subject/SRS spans and explicit semantic text survive.
            int old = text.getCurrentTextColor();
            if (old == ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary)
                    || old == MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary)) {
                text.setTextColor(foreground);
            }
        } else if (view instanceof ProgressBar) {
            ColorStateList tint = ColorStateList.valueOf(MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary));
            ((ProgressBar) view).setProgressTintList(tint);
            ((ProgressBar) view).setIndeterminateTintList(tint);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) style(group.getChildAt(i));
        }
    }

    public static void button(Button button) {
        final Object role = button.getTag(R.id.modernActionRole);
        if (button instanceof com.google.android.material.button.MaterialButton && !(role instanceof ButtonRole)) return;
        button(button, role instanceof ButtonRole ? (ButtonRole) role : ButtonRole.SECONDARY);
    }

    public static void button(Button button, ButtonRole role) {
        if (!isModern(button.getContext())) return;
        button.setTag(R.id.modernActionRole, role);
        int foreground = MaterialColors.getColor(button, role == ButtonRole.PRIMARY
                ? com.google.android.material.R.attr.colorOnPrimary
                : role == ButtonRole.TEXT ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSecondaryContainer);
        int fill = role == ButtonRole.TEXT ? Color.TRANSPARENT : MaterialColors.getColor(button,
                role == ButtonRole.PRIMARY ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorSecondaryContainer);
        int onSurface = MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnSurface);
        int[][] states = {new int[] {-android.R.attr.state_enabled}, new int[] {}};
        button.setTextColor(new ColorStateList(states, new int[] {ColorUtils.setAlphaComponent(onSurface, 97), foreground}));
        ColorStateList fills = new ColorStateList(states, new int[] {
                role == ButtonRole.TEXT ? Color.TRANSPARENT : ColorUtils.setAlphaComponent(onSurface, 31), fill});
        ColorStateList ripple = ColorStateList.valueOf(ColorUtils.setAlphaComponent(foreground, 38));
        if (button instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton material = (com.google.android.material.button.MaterialButton) button;
            material.setBackgroundTintList(fills);
            material.setCornerRadius(dp(button, 12));
            material.setStrokeWidth(0);
            material.setRippleColor(ripple);
            material.setIconTint(button.getTextColors());
        } else {
            MaterialShapeDrawable shape = surface(button, fill, 12);
            shape.setFillColor(fills);
            ViewCompat.setBackgroundTintList(button, null);
            button.setBackground(new RippleDrawable(ripple, shape, null));
        }
        button.setShadowLayer(0, 0, 0, 0);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(button, 48));
        button.setMinimumHeight(dp(button, 48));
        button.setPadding(dp(button, 16), dp(button, 8), dp(button, 16), dp(button, 8));
        button.setElevation(0);
        // The platform Button animator otherwise restores elevation/translationZ on press.
        button.setStateListAnimator(null);
        button.setTranslationZ(0);
    }

    public static void chrome(AbstractActivity activity) {
        Toolbar toolbar = activity.getToolbar();
        if (toolbar == null || !(isModern(activity) || isModern(toolbar.getContext()))) return;
        Context colorContext = isModern(activity) ? activity : toolbar.getContext();
        int surface = MaterialColors.getColor(colorContext, com.google.android.material.R.attr.colorSurface, Color.BLACK);
        int ink = MaterialColors.getColor(colorContext, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
        toolbar.setBackgroundColor(surface);
        toolbar.setTitleTextColor(ink);
        toolbar.setSubtitleTextColor(MaterialColors.getColor(colorContext, com.google.android.material.R.attr.colorOnSurfaceVariant, ink));
        toolbar.setElevation(0);
        if (toolbar.getNavigationIcon() != null) toolbar.getNavigationIcon().mutate().setTint(ink);
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().mutate().setTint(ink);
        MaterialToolbar.tintIcons(toolbar, toolbar.getMenu());
        boolean light = MaterialColors.isColorLight(surface);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView()).setAppearanceLightStatusBars(light);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView()).setAppearanceLightNavigationBars(light);
        activity.getWindow().setStatusBarColor(android.os.Build.VERSION.SDK_INT >= 23 ? surface : Color.rgb(29, 27, 32));
        activity.getWindow().setNavigationBarColor(android.os.Build.VERSION.SDK_INT >= 26 ? surface : Color.rgb(29, 27, 32));
    }

    public static void styleActivity(AbstractActivity activity) {
        if (!isModern(activity)) return;
        style(activity.findViewById(android.R.id.content));
        chrome(activity);
    }
}
