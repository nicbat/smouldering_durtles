package com.smouldering_durtles.wk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.ui.*;
import java.util.*;
import javax.annotation.Nullable;
import static com.smouldering_durtles.wk.util.ObjectSupport.*;

/** Due-only custom reviews and eligible lesson batches; reads the same DAO rules as the dashboard. */
public final class SessionSelectionActivity extends ModernActivity {
    private static final String EXTRA_LESSONS = "lessons";
    private boolean lessons;
    private final boolean[] types = {true, true, true};
    private final boolean[] stages = {true, true, true, true};
    private boolean current;
    private int size;
    private int generation;
    private boolean visible;
    private boolean poolLoaded;
    private int poolLevel;
    private static final String[] STAGE_NAMES = {"Apprentice", "Guru", "Master", "Enlightened"};
    private LinearLayout content, preview;
    private TextView coverage, coverageDetails;
    private Button startButton;
    private final List<Button> typeButtons = new ArrayList<>(), stageButtons = new ArrayList<>();
    private List<Subject> pool = Collections.emptyList(), selected = Collections.emptyList();

    public SessionSelectionActivity() {
        super(R.layout.activity_session_selection, R.menu.generic_options_menu);
    }

    public static void show(AbstractActivity activity, boolean lessons) {
        if (!Session.getInstance().isInactive()) { activity.goToActivity(SessionActivity.class); return; }
        safe(() -> activity.startActivity(new Intent(activity, SessionSelectionActivity.class).putExtra(EXTRA_LESSONS, lessons)));
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle state) {
        lessons = getIntent().getBooleanExtra(EXTRA_LESSONS, false);
        setTitle(lessons ? "Lesson batch" : "Custom reviews");
        size = lessons ? Math.max(1, GlobalSettings.Review.gexMaxLessonSessionSize()) : Integer.MAX_VALUE;
        if (state != null) {
            boolean[] savedTypes = state.getBooleanArray("types"), savedStages = state.getBooleanArray("stages");
            if (savedTypes != null && savedTypes.length == types.length) System.arraycopy(savedTypes, 0, types, 0, types.length);
            if (savedStages != null && savedStages.length == stages.length) System.arraycopy(savedStages, 0, stages, 0, stages.length);
            current = state.getBoolean("current"); size = state.getInt("size", size);
        }
        content = findViewById(R.id.selectionContent);
        startButton = findViewById(R.id.selectionStart);
        startButton.setText(lessons ? "Start lessons" : "Start reviews");
        ModernUi.button(startButton, ModernUi.ButtonRole.PRIMARY);
        startButton.setMinHeight(ModernUi.dp(startButton, 56));
        startButton.setMinimumHeight(ModernUi.dp(startButton, 56));
        startButton.setTextSize(16);
        startButton.setPadding(ModernUi.dp(startButton, 20), ModernUi.dp(startButton, 14),
                ModernUi.dp(startButton, 20), ModernUi.dp(startButton, 14));
        startButton.setOnClickListener(v -> start());
        if (!lessons) {
            ChoiceGrid presets = new ChoiceGrid(this);
            presets.addChoice("All due", 0xff467bc1, () -> preset(false, false));
            presets.addChoice("All kanji", 0xffbd4e7d, () -> preset(true, false));
            presets.addChoice("Apprentice + Guru", 0xff8b5ac2, () -> preset(false, true)); content.addView(presets);
        }
        ChoiceGrid typeGrid = new ChoiceGrid(this);
        String[] labels = {"Radicals", "Kanji", "Vocabulary"};
        for (int i = 0; i < 3; i++) {
            final int n = i;
            typeButtons.add(typeGrid.addChoice(labels[i], com.smouldering_durtles.wk.ui.LearningColors.subject(this, i), () -> {
                types[n] = !types[n]; refresh();
            }));
        }
        content.addView(typeGrid);
        ChoiceGrid scope = new ChoiceGrid(this);
        final Button[] scopes = new Button[2];
        scopes[0] = scope.addChoice("All levels", 0xff467bc1, () -> { current = false; ChoiceGrid.selected(scopes[0], true); ChoiceGrid.selected(scopes[1], false); refresh(); });
        scopes[1] = scope.addChoice("Current level", 0xff467bc1, () -> { current = true; ChoiceGrid.selected(scopes[0], false); ChoiceGrid.selected(scopes[1], true); refresh(); });
        ChoiceGrid.selected(scopes[0], !current); ChoiceGrid.selected(scopes[1], current); content.addView(scope);
        if (lessons) {
            EditText amount = new EditText(this); amount.setInputType(InputType.TYPE_CLASS_NUMBER);
            amount.setHint("Maximum lessons"); amount.setContentDescription("Maximum lessons"); amount.setText(Integer.toString(size));
            amount.setId(View.generateViewId());
            TextView amountLabel = selectionText(14, true);
            amountLabel.setText("Maximum lessons"); amountLabel.setLabelFor(amount.getId());
            amountLabel.setPadding(0, 0, 0, ModernUi.dp(amountLabel, 8));
            content.addView(amountLabel);
            ChoiceGrid sizes = new ChoiceGrid(this);
            for (int n : new int[]{3,5,10}) sizes.addChoice(Integer.toString(n), 0xff467bc1, () -> amount.setText(Integer.toString(n)));
            ModernUi.style(amount);
            content.addView(sizes); content.addView(amount);
            amount.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s,int st,int c,int a) { }
                public void onTextChanged(CharSequence s,int st,int before,int count) { size = safe(0, () -> Integer.parseInt(s.toString())); refresh(); }
                public void afterTextChanged(Editable e) { }
            });
        } else {
            ChoiceGrid stageGrid = new ChoiceGrid(this);
            for (int i = 0; i < 4; i++) {
                final int n = i; stageButtons.add(stageGrid.addChoice(STAGE_NAMES[i], LearningColors.stage(this, i), () -> { stages[n] = !stages[n]; refresh(); }));
            }
            content.addView(stageGrid);
        }
        coverage = selectionText(18, false);
        coverage.setTypeface(coverage.getTypeface(), android.graphics.Typeface.BOLD);
        coverage.setPadding(0, ModernUi.dp(coverage, 8), 0, ModernUi.dp(coverage, 4));
        content.addView(coverage);
        coverageDetails = selectionText(14, true);
        coverageDetails.setLineSpacing(ModernUi.dp(coverageDetails, 3), 1);
        coverageDetails.setPadding(0, 0, 0, ModernUi.dp(coverageDetails, 20));
        content.addView(coverageDetails);
        preview = new LinearLayout(this); preview.setOrientation(LinearLayout.VERTICAL); content.addView(preview);
    }

    private TextView selectionText(int sizeSp, boolean secondary) {
        TextView text = new TextView(this);
        text.setTextSize(sizeSp);
        text.setTextColor(com.google.android.material.color.MaterialColors.getColor(text,
                secondary ? com.google.android.material.R.attr.colorOnSurfaceVariant
                        : com.google.android.material.R.attr.colorOnSurface));
        return text;
    }

    @Override
    protected void styleActivityContent() {
        // Choices and previews already carry semantic colors; the utility pass would overwrite them.
        ModernUi.chrome(this);
        if (content != null) ensureReadableText(content);
    }

    private void ensureReadableText(View view) {
        if (view instanceof TextView && !(view instanceof EditText)) {
            TextView text = (TextView) view;
            if (view instanceof Button) return; // ChoiceGrid owns its fill/ink state pairing.
            int background = com.google.android.material.color.MaterialColors.getColor(view,
                    com.google.android.material.R.attr.colorSurface);
            text.setTextColor(ChoiceGrid.readableInk(text.getCurrentTextColor(), background));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) ensureReadableText(group.getChildAt(i));
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle state) {
        super.onSaveInstanceState(state);
        state.putBooleanArray("types", types); state.putBooleanArray("stages", stages);
        state.putBoolean("current", current); state.putInt("size", size);
    }

    @Override
    protected void onResumeLocal() {
        visible = true;
        // A session may have started elsewhere while this screen was in the background.
        if (!Session.getInstance().isInactive()) { finish(); return; }
        poolLoaded = false;
        refresh();
    }

    @Override
    protected void onPauseLocal() {
        visible = false; generation++;
    }

    @Override
    protected void enableInteractionLocal() {
        //
    }

    @Override
    protected void disableInteractionLocal() {
        //
    }

    @Override
    protected boolean showWithoutApiKey() {
        return false;
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
        return filter(available, types, stages, current, size, WkApplication.getDatabase().propertiesDao().getUserLevel());
    }
    private List<Subject> filter(List<Subject> available, boolean[] types, boolean[] stages, boolean current, int size, int level) {
        List<Subject> result = new ArrayList<>();
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
        if (coverage == null || !visible) return;
        for (int i = 0; i < 3; i++) ChoiceGrid.selected(typeButtons.get(i), types[i]);
        for (int i = 0; i < stageButtons.size(); i++) ChoiceGrid.selected(stageButtons.get(i), stages[i]);
        if (!lessons && poolLoaded) {
            // Toggles only change local selection; Start still revalidates against fresh due items.
            selected = filter(pool, types, stages, current, size, poolLevel);
            showPreview();
            return;
        }
        int ticket = ++generation; coverage.setText("Loading…"); coverageDetails.setText(""); startButton.setEnabled(false);
        boolean[] selectedTypes = types.clone(), selectedStages = stages.clone(); boolean currentOnly = current; int limit = size;
        runAsync(this, () -> {
            List<Subject> available = eligible();
            int level = WkApplication.getDatabase().propertiesDao().getUserLevel();
            return new SelectionResult(available, filter(available, selectedTypes, selectedStages, currentOnly, limit, level), level);
        }, result -> {
            if (ticket != generation || !visible || result == null) return;
            pool = result.available; selected = result.selected; poolLevel = result.level; poolLoaded = true;
            showPreview();
        });
    }
    private static final class SelectionResult {
        final List<Subject> available, selected;
        final int level;
        SelectionResult(List<Subject> available, List<Subject> selected, int level) {
            this.available = available; this.selected = selected; this.level = level;
        }
    }
    private void showPreview() {
        int[] counts = new int[3];
        for (Subject subject : selected) {
            counts[subject.getType().isRadical() ? 0 : subject.getType().isKanji() ? 1 : 2]++;
        }
        coverage.setText(selected.size() + (lessons ? " lesson" : " review")
                + (selected.size() == 1 ? " selected" : "s selected"));
        int excluded = pool.size() - selected.size();
        coverageDetails.setText("Of " + pool.size() + (lessons ? " available" : " due")
                + " · " + (pool.isEmpty() ? 0 : Math.round(100f * selected.size() / pool.size())) + "%"
                + (excluded == 0 ? "" : " · " + excluded + " excluded")
                + "\n" + counts[0] + " radicals · " + counts[1] + " kanji · " + counts[2] + " vocabulary");
        preview.removeAllViews();
        if (!lessons) {
            int[] due = new int[4];
            for (Subject subject : pool) {
                int type = subject.getType().isRadical() ? 0 : subject.getType().isKanji() ? 1 : 2;
                int stage = subject.getSrsStage().getSrsBreakdownBucket();
                if (types[type] && (!current || subject.getLevel() == poolLevel) && stage >= 0 && stage < 4) due[stage]++;
            }
            for (int i = 0; i < stageButtons.size(); i++) {
                String label = (stages[i] ? "✓ " : "") + STAGE_NAMES[i] + "\n" + due[i] + " due";
                if (!label.contentEquals(stageButtons.get(i).getText())) stageButtons.get(i).setText(label);
            }
        }
        // Review selection must not reveal upcoming subjects or their answers.
        if (lessons) {
            for (Subject subject : selected.subList(0, Math.min(selected.size(), 20))) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, ModernUi.dp(row, 12), 0, ModernUi.dp(row, 12));
                LinearLayout identity = new LinearLayout(this); identity.setOrientation(LinearLayout.VERTICAL);
                TextView japanese = selectionText(22, false);
                japanese.setText(subject.getCharacters() == null ? subject.getSlug() : subject.getCharacters());
                int type = subject.getType().isRadical() ? 0 : subject.getType().isKanji() ? 1 : 2;
                japanese.setTextColor(LearningColors.subject(this, type));
                TextView level = selectionText(12, true); level.setText("Level " + subject.getLevel());
                identity.addView(japanese); identity.addView(level);
                TextView english = selectionText(16, false);
                english.setText(subject.getOneMeaning()); english.setGravity(Gravity.END);
                english.setPadding(ModernUi.dp(english, 16), 0, 0, 0);
                row.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
                row.addView(english, new LinearLayout.LayoutParams(0, -2, 1)); preview.addView(row);
            }
            if (selected.size() > 20) {
                TextView more = selectionText(14, true); more.setText("+ " + (selected.size() - 20) + " more lessons"); preview.addView(more);
            }
        }
        ensureReadableText(preview);
        startButton.setEnabled(!selected.isEmpty() && Session.getInstance().isInactive());
    }
    private static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) setEnabled(((ViewGroup) view).getChildAt(i), enabled);
    }
    private void start() {
        startButton.setEnabled(false);
        generation++;
        boolean[] selectedTypes = types.clone(), selectedStages = stages.clone(); boolean currentOnly = current; int limit = size;
        List<Subject> previewed = new ArrayList<>(selected);
        setEnabled(content, false);
        runAsync(this, () -> {
            if (!Session.getInstance().isInactive() || WkApplication.getDatabase().propertiesDao().getSessionType() != com.smouldering_durtles.wk.enums.SessionType.NONE) return false;
            List<Subject> fresh = filter(eligible(), selectedTypes, selectedStages, currentOnly, limit);
            java.util.Set<Long> before = new java.util.HashSet<>(), after = new java.util.HashSet<>();
            for (Subject s : previewed) before.add(s.getId()); for (Subject s : fresh) after.add(s.getId());
            if (fresh.isEmpty() || !before.equals(after)) return false;
            if (lessons) Session.getInstance().startNewLessonSession(fresh, fresh.size());
            else Session.getInstance().startNewReviewSession(fresh, fresh.size());
            return true;
        }, started -> {
            if (Boolean.TRUE.equals(started)) { goToActivity(SessionActivity.class); finish(); }
            else { setEnabled(content, true); Toast.makeText(this, "Available items changed. Review the updated selection.", Toast.LENGTH_SHORT).show(); poolLoaded = false; refresh(); }
        });
    }
}
