package com.smouldering_durtles.wk.ui;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import android.util.DisplayMetrics;
import android.graphics.Rect;
import android.view.View;
import com.smouldering_durtles.wk.adapter.search.SearchResultAdapter;
import com.smouldering_durtles.wk.enums.SubjectCardLayout;
import android.util.TypedValue;
import androidx.appcompat.view.ContextThemeWrapper;
import com.google.android.material.color.DynamicColors;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;

/** Scoped Material styling and responsive geometry for browsing results. */
public final class SearchStyle {
    private SearchStyle() {}

    public static Context context(Context base) {
        Context context = new ContextThemeWrapper(base, R.style.SmokySearchTheme);
        return GlobalSettings.Display.getSettingsDynamicColor()
                ? DynamicColors.wrapContextIfAvailable(context) : context;
    }

    /** Full-width group/form rows with readable subject cards that adapt to the actual viewport. */
    public static void configureResultGrid(RecyclerView list,
                                          SearchResultAdapter adapter) {
        final DisplayMetrics metrics = list.getResources().getDisplayMetrics();
        final boolean cards = isModern(list.getContext()) && GlobalSettings.Experimental
                .getSubjectCardLayoutSearch() == SubjectCardLayout.NORMAL;
        final int initialSpans = cards ? cardColumns(list, metrics.widthPixels)
                : Math.max(1, (int) ((metrics.widthPixels / metrics.density + 10) / 90));
        final GridLayoutManager manager =
                new GridLayoutManager(list.getContext(), initialSpans) {
                    @Override public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                        // Resolve columns before laying out cells, including live window resizes.
                        if (cards && !state.isPreLayout() && getWidth() > 0) {
                            int columns = cardColumns(list, getWidth());
                            if (columns != getSpanCount()) setSpanCount(columns);
                        }
                        super.onLayoutChildren(recycler, state);
                    }
                };
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                return adapter.getItemSpanSize(position, manager.getSpanCount());
            }
        });
        list.setLayoutManager(manager);
        if (!cards) return;
        list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override public void getItemOffsets(Rect out,
                    View view, RecyclerView parent,
                    RecyclerView.State state) {
                // Fixed half-gutters also align full-width headings/forms and remain stable on resize.
                int halfGap = Math.round(4 * metrics.density);
                out.set(halfGap, 0, halfGap, 0);
            }
        });
    }

    private static int cardColumns(RecyclerView list, int width) {
        float available = (width - list.getPaddingLeft() - list.getPaddingRight())
                / list.getResources().getDisplayMetrics().density;
        float minimum = 160 * Math.max(1, list.getResources().getConfiguration().fontScale);
        // At fractional densities the two 12dp insets can round a nominal 336dp interior down.
        // A 1dp tolerance keeps a 360dp phone at two columns instead of losing one to pixel rounding.
        return Math.max(1, Math.min(4, (int) ((available + 1) / (minimum + 8))));
    }

    public static boolean isModern(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.smokyModernSearch, value, true) && value.data != 0;
    }
}
