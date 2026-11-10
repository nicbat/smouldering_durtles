package com.smouldering_durtles.wk.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import android.text.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.activities.BrowseActivity;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.SubjectCardLayout;
import com.smouldering_durtles.wk.model.*;
import com.smouldering_durtles.wk.ui.*;
import com.smouldering_durtles.wk.util.SearchUtil;
import com.smouldering_durtles.wk.views.AdvancedSearchFormView;
import java.util.*;
import static com.smouldering_durtles.wk.util.ObjectSupport.*;

/** One local library for browsing and querying, with cancellable delivery and recycled results. */
public final class LibraryFragment extends AbstractFragment {
    private AdvancedSearchParameters parameters = new AdvancedSearchParameters();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int generation;
    private android.os.Parcelable scrollState;
    private EditText query;
    private TextView count;
    private RecyclerView results;
    private final Cards cards = new Cards();
    public LibraryFragment() { super(0); }
    public static LibraryFragment newInstance(String text) {
        LibraryFragment fragment = new LibraryFragment();
        Bundle args = new Bundle(); args.putString("query", text); fragment.setArguments(args); return fragment;
    }
    @Override protected void onCreateLocal() {
        if (getArguments() != null) parameters.query = getArguments().getString("query", "");
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        LinearLayout page = new LinearLayout(requireContext()); page.setOrientation(LinearLayout.VERTICAL);
        int gap = ModernUi.dp(page, 16); page.setPadding(gap, 0, gap, 0);
        query = new EditText(requireContext()); query.setId(R.id.query); query.setSingleLine(true); query.setHint("Japanese, reading, or meaning");
        page.addView(query, new LinearLayout.LayoutParams(-1, -2));
        Button filters = new Button(requireContext()); filters.setText("Filters"); ModernUi.button(filters, ModernUi.ButtonRole.SECONDARY);
        filters.setOnClickListener(v -> filters()); page.addView(filters, new LinearLayout.LayoutParams(-1, -2));
        Button saved = new Button(requireContext()); saved.setText("Saved searches & actions"); ModernUi.button(saved, ModernUi.ButtonRole.TEXT);
        saved.setOnClickListener(v -> savedSearches()); page.addView(saved);
        count = new TextView(requireContext()); count.setId(R.id.numHits); count.setPadding(0, gap / 2, 0, gap / 2); page.addView(count);
        results = new RecyclerView(requireContext()); results.setId(R.id.resultView); results.setLayoutManager(new LinearLayoutManager(requireContext()));
        results.setAdapter(cards); page.addView(results, new LinearLayout.LayoutParams(-1, 0, 1)); return page;
    }
    @Override public void onViewCreatedLocal(View view, Bundle state) {
        if (state != null) scrollState = state.getParcelable("libraryScroll");
        if (state != null) safe(() -> parameters = Converters.getObjectMapper().readValue(state.getString("filters", "{}"), AdvancedSearchParameters.class));
        query.setText(parameters.query);
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { parameters.query = s.toString(); schedule(); }
            public void afterTextChanged(Editable e) { }
        });
        schedule();
    }
    private void schedule() {
        int ticket = ++generation; handler.removeCallbacksAndMessages(null); count.setText("Loading…");
        handler.postDelayed(() -> safe(() -> {
            String snapshot = Converters.getObjectMapper().writeValueAsString(parameters);
            runAsync(this, () -> SearchUtil.searchSubjects(2, snapshot), found -> {
                if (ticket != generation || getView() == null || found == null) return;
                found.sort(parameters.sortOrder.getComparator(System.currentTimeMillis()));
                cards.items = found; cards.notifyDataSetChanged();
                if (scrollState != null) { results.getLayoutManager().onRestoreInstanceState(scrollState); scrollState = null; }
                count.setText(found.size() + (found.size() == 1 ? " item" : " items") + filterSummary());
            });
        }), 200);
    }
    private String filterSummary() {
        return (parameters.minLevel == null ? "" : " · Levels " + parameters.minLevel + "–" + parameters.maxLevel)
                + (parameters.partOfSpeech.isEmpty() ? "" : " · " + parameters.partOfSpeech)
                + (parameters.pitchPattern.isEmpty() ? "" : " · Pitch filter");
    }
    private void savedSearches() {
        com.smouldering_durtles.wk.livedata.LiveSearchPresets presets = com.smouldering_durtles.wk.livedata.LiveSearchPresets.getInstance();
        List<String> labels = new ArrayList<>(); labels.add("Save current search"); labels.add("Result actions"); labels.addAll(presets.getNames());
        ModernUi.dialog(requireContext()).setTitle("Saved searches & actions").setItems(labels.toArray(new String[0]), (dialog, which) -> safe(() -> {
            if (which == 0) {
                EditText name = new EditText(requireContext()); name.setHint("Search name");
                androidx.appcompat.app.AlertDialog prompt = ModernUi.dialog(requireContext()).setTitle("Save search").setView(name)
                        .setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
                prompt.setOnShowListener(d -> prompt.getButton(-1).setOnClickListener(v -> safe(() -> {
                    String title = name.getText().toString().trim();
                    if (title.isEmpty()) { name.setError("Enter a name"); return; }
                    if (presets.getByName(title) != null) { name.setError("Choose a new name"); return; }
                    String snapshot = Converters.getObjectMapper().writeValueAsString(parameters);
                    runAsync(this, () -> { WkApplication.getDatabase().searchPresetDao().setPreset(title, 2, snapshot); return true; }, result -> {
                        if (isAdded()) Toast.makeText(requireContext(), "Search saved", Toast.LENGTH_SHORT).show();
                    }); prompt.dismiss();
                }))); prompt.show();
            } else if (which == 1) {
                ((BrowseActivity) requireActivity()).loadSearchResultFragment(null, 2, Converters.getObjectMapper().writeValueAsString(parameters));
            } else {
                com.smouldering_durtles.wk.db.model.SearchPreset preset = presets.getByName(labels.get(which));
                if (preset == null) return;
                if (preset.type == 2) parameters = Converters.getObjectMapper().readValue(preset.data, AdvancedSearchParameters.class);
                else if (preset.type == 1) { parameters = new AdvancedSearchParameters(); parameters.query = preset.data; }
                else { ((BrowseActivity) requireActivity()).loadSearchResultFragment(preset.name, preset.type, preset.data); return; }
                query.setText(parameters.query); schedule();
            }
        })).show();
    }
    private void filters() {
        AdvancedSearchFormView form = new AdvancedSearchFormView(requireContext()); form.injectParameters(parameters);
        form.findViewById(R.id.searchButton1).setVisibility(View.GONE); form.findViewById(R.id.searchButton2).setVisibility(View.GONE);
        ScrollView scroll = new ScrollView(requireContext()); scroll.addView(form);
        androidx.appcompat.app.AlertDialog dialog = ModernUi.dialog(requireContext()).setTitle("Library filters").setView(scroll)
                .setNegativeButton("Cancel", null).setNeutralButton("Reset", (d,w) -> {
                    String text = parameters.query; parameters = new AdvancedSearchParameters(); parameters.query = text; schedule();
                }).setPositiveButton("Apply", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            if (!form.validateLevels()) return;
            AdvancedSearchParameters next = form.extractParameters(); next.query = query.getText().toString(); parameters = next; schedule(); dialog.dismiss();
        }));
        dialog.show();
    }
    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); if (results != null) state.putParcelable("libraryScroll", results.getLayoutManager().onSaveInstanceState()); safe(() -> state.putString("filters", Converters.getObjectMapper().writeValueAsString(parameters)));
    }
    @Override public void onDestroyView() { scrollState = results.getLayoutManager().onSaveInstanceState(); generation++; handler.removeCallbacksAndMessages(null); super.onDestroyView(); }
    @Override protected void onResumeLocal() { ((BrowseActivity) requireActivity()).hideSearchResultMenu(); }
    @Override public String getToolbarTitle() { return "Library"; }
    @Override protected int getToolbarBackgroundColor() { return 0; }
    @Override public void enableInteraction() { interactionEnabled = true; }
    @Override public void disableInteraction() { interactionEnabled = false; }
    @Override public Subject getCurrentSubject() { return null; }
    @Override public void showOrHideSoftInput() { }
    @Override public void updateViews() { }
    @Override public String getPrefillSearchQuery() { return parameters.query; }
    private final class Cards extends RecyclerView.Adapter<Holder> {
        List<Subject> items = Collections.emptyList();
        final SubjectCardBinder binder = new SubjectCardBinder(SubjectCardLayout.NORMAL);
        public Holder onCreateViewHolder(ViewGroup parent, int type) {
            return new Holder(binder.createView(com.smouldering_durtles.wk.enums.SubjectType.WANIKANI_VOCAB, parent));
        }
        public void onBindViewHolder(Holder holder, int position) {
            Subject subject = items.get(position);
            binder.bind(holder.itemView, subject, v -> {
                long[] ids = new long[items.size()]; for (int i = 0; i < items.size(); i++) ids[i] = items.get(i).getId();
                ((BrowseActivity) requireActivity()).loadSubjectInfoFragment(subject.getId(), ids,
                        com.smouldering_durtles.wk.enums.FragmentTransitionAnimation.NONE);
            }, true, true);
        }
        public int getItemCount() { return items.size(); }
    }
    private static final class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
}
