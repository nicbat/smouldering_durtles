package com.smouldering_durtles.wk.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.smouldering_durtles.wk.R;

/** Layout corrections for legacy utility pages. No listeners, values or visibility are changed. */
public final class UtilityStyle {
    private UtilityStyle() { }

    public static void apply(View root) {
        if (!ModernUi.isModern(root.getContext())) return;
        ModernUi.style(root);
        pageGutters(root);
        action(root, R.id.saveButton, ModernUi.ButtonRole.PRIMARY);
        action(root, R.id.startButton, ModernUi.ButtonRole.PRIMARY);
        action(root, R.id.stopButton, ModernUi.ButtonRole.TEXT);
        for (int id : new int[]{R.id.exportSearchPresets, R.id.importSearchPresets,
                R.id.exportStarRatings, R.id.importStarRatings}) {
            View control = root.findViewById(id);
            if (control == null) continue;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) control.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.topMargin = ModernUi.dp(root, 8);
            control.setLayoutParams(params);
        }
        View start = root.findViewById(R.id.startButton);
        if (start != null && start.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) start.getLayoutParams();
            params.setMarginEnd(ModernUi.dp(root, 8));
            start.setLayoutParams(params);
        }
        // A token field needs its full width; a baseline-aligned Save button squeezed it at 2x text.
        View token = root.findViewById(R.id.apiKey);
        if (token != null && token.getParent() instanceof ConstraintLayout) {
            ConstraintLayout form = (ConstraintLayout) token.getParent();
            ConstraintSet constraints = new ConstraintSet();
            constraints.clone(form);
            constraints.connect(R.id.apiKey, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            constraints.clear(R.id.saveButton, ConstraintSet.BASELINE);
            constraints.clear(R.id.saveButton, ConstraintSet.START);
            constraints.connect(R.id.saveButton, ConstraintSet.TOP, R.id.apiKey, ConstraintSet.BOTTOM, ModernUi.dp(root, 8));
            constraints.connect(R.id.saveButton, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            constraints.connect(R.id.document, ConstraintSet.TOP, R.id.saveButton, ConstraintSet.BOTTOM, ModernUi.dp(root, 16));
            constraints.applyTo(form);
        }
    }

    private static void action(View root, int id, ModernUi.ButtonRole role) {
        View view = root.findViewById(id);
        if (view instanceof Button) ModernUi.button((Button) view, role);
    }

    private static void pageGutters(View view) {
        if (view instanceof ScrollView) {
            View content = ((ScrollView) view).getChildAt(0);
            if (content != null) {
                int inset = ModernUi.dp(view, 16);
                content.setPadding(inset, inset, inset, inset);
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) pageGutters(group.getChildAt(i));
        }
    }
}
