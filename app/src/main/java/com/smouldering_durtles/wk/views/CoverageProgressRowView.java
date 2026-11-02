package com.smouldering_durtles.wk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smouldering_durtles.wk.R;
import java.text.NumberFormat;
import javax.annotation.Nullable;

/** Compact curriculum overview; source buckets remain exact and independently inspectable. */
public final class CoverageProgressRowView extends LinearLayout {
    private final TextView label;
    private final TextView summary;
    private final CoverageBar bar;
    private final int[] counts = new int[4];
    private String title = "";
    private String completionDescription = "";

    public CoverageProgressRowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        setPadding(0, dp(6), 0, dp(6));
        setMinimumHeight(dp(48));
        TypedValue ripple = new TypedValue();
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.selectableItemBackground, ripple, true);
        if (ripple.resourceId != 0) setBackgroundResource(ripple.resourceId);
        setFocusable(true);
        setClickable(true);
        final DashboardProgressHeader header = new DashboardProgressHeader(context, null);
        label = new TextView(context);
        label.setId(R.id.coverageLabel);
        label.setTextAppearance(context, com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        label.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
        summary = new TextView(context);
        summary.setId(R.id.coverageSummary);
        summary.setTextAppearance(context, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        summary.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        header.addView(label, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        header.addView(summary, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        bar = new CoverageBar(context);
        bar.setId(R.id.coverageBar);
        LayoutParams barParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(6));
        barParams.topMargin = dp(5);
        addView(bar, barParams);
        header.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        bar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        ViewCompat.setAccessibilityDelegate(this, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, androidx.core.view.accessibility.AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
        setOnClickListener(view -> new MaterialAlertDialogBuilder(getContext()).setTitle(title)
                .setMessage(details()).setPositiveButton(android.R.string.ok, null).show());
        setCounts(0, 0, 0, 0);
    }

    public void setLabel(String text) { title = text; label.setText(text); updateDescription(); }

    public void setCounts(int locked, int inProgress, int passed, int burned) {
        counts[0] = locked;
        counts[1] = inProgress;
        counts[2] = passed;
        counts[3] = burned;
        final long total = (long) locked + inProgress + passed + burned;
        final long completed = (long) passed + burned;
        NumberFormat numbers = NumberFormat.getIntegerInstance();
        summary.setText(total == 0 ? getContext().getString(R.string.coverage_no_kanji)
                : getContext().getString(R.string.coverage_fraction, numbers.format(completed), numbers.format(total)));
        completionDescription = total == 0 ? getContext().getString(R.string.coverage_no_kanji)
                : getContext().getString(R.string.coverage_completed_description, numbers.format(completed), numbers.format(total));
        bar.fraction = total <= 0 ? 0 : Math.max(0, Math.min(1, (float) ((double) completed / total)));
        bar.invalidate();
        updateDescription();
    }

    private String details() {
        NumberFormat numbers = NumberFormat.getIntegerInstance();
        return getContext().getString(R.string.coverage_bucket_details,
                numbers.format(counts[0]), numbers.format(counts[1]), numbers.format(counts[2]), numbers.format(counts[3]));
    }

    private void updateDescription() {
        setContentDescription(title + ". " + completionDescription + ". " + details()
                + ". " + getContext().getString(R.string.coverage_action));
    }

    private int color(int attribute) { return MaterialColors.getColor(this, attribute); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class CoverageBar extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float fraction;
        CoverageBar(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float radius = getHeight() / 2f;
            paint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHighest));
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, paint);
            paint.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
            float extent = getWidth() * fraction;
            if (extent > 0) {
                boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                canvas.drawRoundRect(rtl ? getWidth() - extent : 0, 0,
                        rtl ? getWidth() : extent, getHeight(), radius, radius, paint);
            }
        }
    }
}
