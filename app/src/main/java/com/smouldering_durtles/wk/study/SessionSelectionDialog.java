package com.smouldering_durtles.wk.study;

import android.widget.*;
import android.view.*;
import android.text.*;
import androidx.appcompat.app.AlertDialog;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.activities.*;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.ui.*;
import java.util.*;
import static com.smouldering_durtles.wk.util.ObjectSupport.*;

/** Due-only review and eligible lesson selection; reads the same DAO rules as the dashboard. */
public final class SessionSelectionDialog {
    private final AbstractActivity activity;
    private final boolean lessons;
    private final boolean[] types = {true, true, true};
    private final boolean[] stages = {true, true, true, true};
    private boolean current;
    private int size;
    private int generation;
    private final LinearLayout content, preview;
    private final TextView coverage;
    private final List<Button> typeButtons = new ArrayList<>(), stageButtons = new ArrayList<>();
    private AlertDialog dialog;
    private List<Subject> pool = Collections.emptyList(), selected = Collections.emptyList();
    private SessionSelectionDialog(AbstractActivity activity, boolean lessons, android.os.Bundle state) {
        this.activity = activity; this.lessons = lessons;
        size = lessons ? Math.max(1, GlobalSettings.Review.gexMaxLessonSessionSize()) : Integer.MAX_VALUE;
        if (state != null) {
            boolean[] savedTypes = state.getBooleanArray("types"), savedStages = state.getBooleanArray("stages");
            if (savedTypes != null && savedTypes.length == types.length) System.arraycopy(savedTypes, 0, types, 0, types.length);
            if (savedStages != null && savedStages.length == stages.length) System.arraycopy(savedStages, 0, stages, 0, stages.length);
            current = state.getBoolean("current"); size = state.getInt("size", size);
        }
        content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL);
        int gutter = ModernUi.dp(content, 16); content.setPadding(gutter, 0, gutter, gutter);
        if (!lessons) {
            ChoiceGrid presets = new ChoiceGrid(activity);
            presets.addChoice("All due", 0xff467bc1, () -> preset(false, false));
            presets.addChoice("All kanji", 0xffbd4e7d, () -> preset(true, false));
            presets.addChoice("Apprentice + Guru", 0xff8b5ac2, () -> preset(false, true)); content.addView(presets);
        }
        ChoiceGrid typeGrid = new ChoiceGrid(activity);
        String[] labels = {"Radicals", "Kanji", "Vocabulary"};
        for (int i = 0; i < 3; i++) {
            final int n = i;
            typeButtons.add(typeGrid.addChoice(labels[i], com.smouldering_durtles.wk.enums.ActiveTheme.getSubjectTypeBackgroundColors()[i], () -> {
                types[n] = !types[n]; refresh();
            }));
        }
        content.addView(typeGrid);
        ChoiceGrid scope = new ChoiceGrid(activity);
        final Button[] scopes = new Button[2];
        scopes[0] = scope.addChoice("All levels", 0xff467bc1, () -> { current = false; ChoiceGrid.selected(scopes[0], true); ChoiceGrid.selected(scopes[1], false); refresh(); });
        scopes[1] = scope.addChoice("Current level", 0xff467bc1, () -> { current = true; ChoiceGrid.selected(scopes[0], false); ChoiceGrid.selected(scopes[1], true); refresh(); });
        ChoiceGrid.selected(scopes[0], !current); ChoiceGrid.selected(scopes[1], current); content.addView(scope);
        if (lessons) {
            EditText amount = new EditText(activity); amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            amount.setHint("Maximum lessons"); amount.setContentDescription("Maximum lessons"); amount.setText(Integer.toString(size));
            ChoiceGrid sizes = new ChoiceGrid(activity);
            for (int n : new int[]{3,5,10}) sizes.addChoice(Integer.toString(n), 0xff467bc1, () -> amount.setText(Integer.toString(n)));
            content.addView(sizes); content.addView(amount);
            amount.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s,int st,int c,int a) { }
                public void onTextChanged(CharSequence s,int st,int before,int count) { size = safe(0, () -> Integer.parseInt(s.toString())); refresh(); }
                public void afterTextChanged(Editable e) { }
            });
        } else {
            ChoiceGrid stageGrid = new ChoiceGrid(activity);
            String[] names = {"Apprentice", "Guru", "Master", "Enlightened"};
            for (int i = 0; i < 4; i++) {
                final int n = i; stageButtons.add(stageGrid.addChoice(names[i], LearningColors.stage(activity, i), () -> { stages[n] = !stages[n]; refresh(); }));
            }
            content.addView(stageGrid);
        }
        coverage = new TextView(activity); content.addView(coverage);
        preview = new LinearLayout(activity); preview.setOrientation(LinearLayout.VERTICAL); content.addView(preview);
        ScrollView scroll = new ScrollView(activity); scroll.addView(content);
        dialog = ModernUi.dialog(activity).setTitle(lessons ? "Lesson batch" : "Custom reviews").setView(scroll)
                .setNegativeButton("Cancel", null).setPositiveButton(lessons ? "Start lessons" : "Start reviews", null).create();
        dialog.setOnShowListener(d -> { dialog.getButton(-1).setOnClickListener(v -> start()); refresh(); });
        dialog.setOnDismissListener(d -> generation++);
    }
    public static void show(AbstractActivity activity, boolean lessons) {
        if (!Session.getInstance().isInactive()) { activity.goToActivity(SessionActivity.class); return; }
        if (activity.getSupportFragmentManager().findFragmentByTag("sessionSelection") != null) return;
        Host host = new Host(); android.os.Bundle args = new android.os.Bundle(); args.putBoolean("lessons", lessons); host.setArguments(args);
        host.show(activity.getSupportFragmentManager(), "sessionSelection");
    }
    /** Fragment-owned dialog retains choices through recreation without retaining an Activity. */
    public static final class Host extends androidx.fragment.app.DialogFragment {
        private SessionSelectionDialog selection;
        @Override public android.app.Dialog onCreateDialog(android.os.Bundle state) {
            selection = new SessionSelectionDialog((AbstractActivity) requireActivity(), requireArguments().getBoolean("lessons"), state);
            return selection.dialog;
        }
        @Override public void onSaveInstanceState(android.os.Bundle state) {
            super.onSaveInstanceState(state);
            if (selection != null) {
                state.putBooleanArray("types", selection.types); state.putBooleanArray("stages", selection.stages);
                state.putBoolean("current", selection.current); state.putInt("size", selection.size);
            }
        }
        @Override public void onDismiss(android.content.DialogInterface dialog) {
            if (selection != null) selection.generation++;
            super.onDismiss(dialog);
        }
    }
    private void preset(boolean kanji, boolean early) {
        Arrays.fill(types, true); if (kanji) { types[0] = false; types[2] = false; }
        Arrays.fill(stages, true); if (early) { stages[2] = false; stages[3] = false; } refresh();
    }
    private List<Subject> eligible() {
        com.smouldering_durtles.wk.db.AppDatabase db = WkApplication.getDatabase();
        if (db.propertiesDao().getVacationMode()) return new ArrayList<>();
        int max = db.propertiesDao().getUserMaxLevelGranted();
        return lessons ? db.subjectCollectionsDao().getAvailableLessonItems(max)
                : db.subjectCollectionsDao().getUpcomingReviewItems(max, System.currentTimeMillis());
    }
    private List<Subject> filter(List<Subject> available, boolean[] types, boolean[] stages, boolean current, int size) {
        List<Subject> result = new ArrayList<>(); int level = WkApplication.getDatabase().propertiesDao().getUserLevel();
        for (Subject subject : available) {
            int type = subject.getType().isRadical() ? 0 : subject.getType().isKanji() ? 1 : 2;
            int stage = subject.getSrsStage().getSrsBreakdownBucket();
            if (types[type] && (!current || subject.getLevel() == level) && (lessons || stage >= 0 && stage < 4 && stages[stage])) result.add(subject);
        }
        // Keep normal lesson priority before applying this batch maximum.
        if (lessons) {
            com.smouldering_durtles.wk.db.AppDatabase db = WkApplication.getDatabase();
            java.util.Comparator<Subject> order = GlobalSettings.AdvancedLesson.getOrder().getComparator();
            if (GlobalSettings.AdvancedLesson.getOrderReversed()) order = order.reversed();
            order = GlobalSettings.AdvancedLesson.getOrderPriority().getComparator(order,
                    db.subjectCollectionsDao().getLevelUpIds(level, db.propertiesDao().getUserMaxLevelGranted()), level, db.propertiesDao().getUserMaxLevelGranted());
            result.sort(order);
            if (result.size() > Math.max(0, size)) result = new ArrayList<>(result.subList(0, Math.max(0, size)));
        }
        return result;
    }
    private void refresh() {
        if (coverage == null || dialog == null || !dialog.isShowing()) return;
        for (int i = 0; i < 3; i++) ChoiceGrid.selected(typeButtons.get(i), types[i]);
        for (int i = 0; i < stageButtons.size(); i++) ChoiceGrid.selected(stageButtons.get(i), stages[i]);
        int ticket = ++generation; coverage.setText("Loading…"); dialog.getButton(-1).setEnabled(false);
        boolean[] selectedTypes = types.clone(), selectedStages = stages.clone(); boolean currentOnly = current; int limit = size;
        runAsync(activity, () -> { List<Subject> available = eligible(); return Arrays.asList(available, filter(available, selectedTypes, selectedStages, currentOnly, limit)); }, result -> {
            if (ticket != generation || !dialog.isShowing() || result == null) return;
            pool = result.get(0); selected = result.get(1); showPreview();
        });
    }
    private void showPreview() {
        int[] counts = new int[3], buckets = new int[4];
        for (Subject subject : selected) {
            counts[subject.getType().isRadical() ? 0 : subject.getType().isKanji() ? 1 : 2]++;
            int stage = subject.getSrsStage().getSrsBreakdownBucket(); if (stage >= 0 && stage < 4) buckets[stage]++;
        }
        coverage.setText(selected.size() + " / " + pool.size() + (lessons ? " available lessons" : " due reviews")
                + " · " + (pool.isEmpty() ? 0 : Math.round(100f * selected.size() / pool.size())) + "%\n"
                + (pool.size() - selected.size()) + " excluded · " + counts[0] + " radicals · " + counts[1] + " kanji · " + counts[2] + " vocabulary");
        preview.removeAllViews();
        if (!lessons) {
            String[] names = {"Apprentice", "Guru", "Master", "Enlightened"};
            ChoiceGrid breakdown = new ChoiceGrid(activity);
            for (int i = 0; i < 4; i++) {
                Button stage = breakdown.addChoice(names[i] + "  " + buckets[i], LearningColors.stage(activity, i), () -> {});
                stage.setClickable(false); stage.setFocusable(false);
            }
            preview.addView(breakdown);
        }
        for (Subject subject : selected.subList(0, Math.min(selected.size(), 20))) {
            LinearLayout row = new LinearLayout(activity); row.setPadding(0, ModernUi.dp(row, 8), 0, ModernUi.dp(row, 8));
            TextView japanese = new TextView(activity); japanese.setText((subject.getCharacters() == null ? subject.getSlug() : subject.getCharacters()) + "\nLevel " + subject.getLevel());
            japanese.setTextColor(LearningColors.accent(activity, subject.getBackgroundColor()));
            TextView english = new TextView(activity); english.setText(subject.getOneMeaning()); english.setGravity(Gravity.END);
            row.addView(japanese, new LinearLayout.LayoutParams(0,-2,1)); row.addView(english,new LinearLayout.LayoutParams(0,-2,1)); preview.addView(row);
        }
        if (selected.size() > 20) { TextView more = new TextView(activity); more.setText("+ " + (selected.size() - 20) + " more items"); preview.addView(more); }
        dialog.getButton(-1).setEnabled(!selected.isEmpty() && Session.getInstance().isInactive());
    }
    private static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) setEnabled(((ViewGroup) view).getChildAt(i), enabled);
    }
    private void start() {
        dialog.getButton(-1).setEnabled(false);
        generation++;
        boolean[] selectedTypes = types.clone(), selectedStages = stages.clone(); boolean currentOnly = current; int limit = size;
        List<Subject> previewed = new ArrayList<>(selected);
        setEnabled(content, false);
        runAsync(activity, () -> {
            if (!Session.getInstance().isInactive() || WkApplication.getDatabase().propertiesDao().getSessionType() != com.smouldering_durtles.wk.enums.SessionType.NONE) return false;
            List<Subject> fresh = filter(eligible(), selectedTypes, selectedStages, currentOnly, limit);
            java.util.Set<Long> before = new java.util.HashSet<>(), after = new java.util.HashSet<>();
            for (Subject s : previewed) before.add(s.getId()); for (Subject s : fresh) after.add(s.getId());
            if (fresh.isEmpty() || !before.equals(after)) return false;
            if (lessons) Session.getInstance().startNewLessonSession(fresh, fresh.size());
            else Session.getInstance().startNewReviewSession(fresh, fresh.size());
            return true;
        }, started -> {
            if (Boolean.TRUE.equals(started)) { dialog.dismiss(); activity.goToActivity(SessionActivity.class); }
            else { setEnabled(content, true); Toast.makeText(activity, "Available items changed. Review the updated selection.", Toast.LENGTH_SHORT).show(); refresh(); }
        });
    }
}
