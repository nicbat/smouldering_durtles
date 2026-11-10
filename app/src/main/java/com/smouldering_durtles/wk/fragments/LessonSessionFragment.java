/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.fragments;

import android.os.Bundle;
import android.view.View;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.model.FloatingUiState;
import com.smouldering_durtles.wk.model.Question;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.AudioUtil;
import com.smouldering_durtles.wk.views.SubjectInfoView;
import com.smouldering_durtles.wk.views.SwipingScrollView;

import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Fragment for the lesson presentation.
 */
public final class LessonSessionFragment extends AbstractSessionFragment implements SwipingScrollView.OnSwipeListener {
    private @Nullable SessionItem item = null;
    private @Nullable Subject subject = null;

    private final ViewProxy previousButton = new ViewProxy();
    private final ViewProxy nextButton = new ViewProxy();
    private final ViewProxy progress = new ViewProxy();
    private final ViewProxy scrollView = new ViewProxy();
    private final ViewProxy subjectInfo = new ViewProxy();

    /**
     * The constructor.
     */
    public LessonSessionFragment() {
        super(R.layout.fragment_lesson);
    }

    /**
     * Create a new instance with arguments set.
     *
     * @param subjectId the subject ID to show
     * @return the fragment
     */
    public static LessonSessionFragment newInstance(final long subjectId) {
        final LessonSessionFragment fragment = new LessonSessionFragment();

        final Bundle args = new Bundle();
        args.putLong("subjectId", subjectId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onCreateLocal() {
        final @Nullable Bundle args = getArguments();
        if (args != null) {
            final long subjectId = args.getLong("subjectId", -1);
            item = session.findItemBySubjectId(subjectId);
            if (item != null) {
                subject = item.getSubject();
            }
        }
    }

    @Override
    protected void onResumeLocal() {
        nextButton.requestFocus();
        playAudio();
    }

    @Override
    public void onViewCreatedLocal(final View view, final @Nullable Bundle savedInstanceState) {
        if (item == null || subject == null) {
            return;
        }

        previousButton.setDelegate(view, R.id.previousButton);
        nextButton.setDelegate(view, R.id.nextButton);
        progress.setDelegate(view, R.id.progress);
        scrollView.setDelegate(view, R.id.scrollView);
        subjectInfo.setDelegate(view, R.id.subjectInfo);

        scrollView.setSwipeListener(this);

        final List<SessionItem> items = session.getItems();

        subjectInfo.setMaxFontSize(GlobalSettings.Font.getMaxFontSizeLesson());
        subjectInfo.setContainerType(SubjectInfoView.ContainerType.LESSON_PRESENTATION);
        subjectInfo.setSubject(this, subject);

        previousButton.setVisibility(!session.isOnFirstLessonItem());
        nextButton.setText(session.isOnLastLessonItem() ? "Start quiz" : "Next");
        progress.setTextFormat("%d/%d", items.indexOf(item)+1, items.size());
        com.smouldering_durtles.wk.study.LessonStyle.navigation(view, items.indexOf(item) + 1, items.size());
        com.smouldering_durtles.wk.study.LessonStyle.restoreDetails(view, savedInstanceState != null
                && savedInstanceState.getBoolean("lessonDetailsExpanded"));

        previousButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            if (!session.isOnFirstLessonItem()) {
                disableInteraction();
                session.moveToPreviousLessonItem();
            }
        }));

        nextButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            if (session.isOnLastLessonItem()) {
                showReadyBatch();
            }
            else {
                session.moveToNextLessonItem();
            }
        }));
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getView() != null) outState.putBoolean("lessonDetailsExpanded",
                com.smouldering_durtles.wk.study.LessonStyle.isDetailsExpanded(getView()));
    }

    @Override
    public CharSequence getToolbarTitle() {
        return subject == null ? "" : subject.getInfoTitle("", "");
    }

    @Override
    public int getToolbarBackgroundColor() {
        return subject == null ? 0 : subject.getBackgroundColor();
    }

    @Override
    public void enableInteraction() {
        safe(() -> {
            previousButton.enableInteraction();
            nextButton.enableInteraction();
            interactionEnabled = true;
        });
    }

    @Override
    public void disableInteraction() {
        safe(() -> {
            interactionEnabled = false;
            previousButton.disableInteraction();
            nextButton.disableInteraction();
        });
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public Subject getCurrentSubject() {
        return subject;
    }

    @Override
    public @Nullable SessionItem getItem() {
        return item;
    }

    @Override
    public @Nullable Question getQuestion() {
        return null;
    }

    @Override
    public @Nullable Subject getSubject() {
        return subject;
    }

    @Override
    public void showOrHideSoftInput() {
        hideSoftInput();
    }

    @Override
    public void updateViews() {
        //
    }

    @Override
    public FragmentTransitionAnimation getAnimation(final AbstractSessionFragment newFragment) {
        if (newFragment instanceof LessonSessionFragment && item != newFragment.getItem()) {
            return session.getQuestionChoiceReason().getAnimation();
        }
        return FragmentTransitionAnimation.RTL;
    }

    @Override
    public void onSwipeLeft(final SwipingScrollView view) {
        safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            if (!session.isOnFirstLessonItem()) {
                disableInteraction();
                session.moveToPreviousLessonItem();
            }
        });
    }

    @Override
    public void onSwipeRight(final SwipingScrollView view) {
        safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            if (session.isOnLastLessonItem()) {
                showReadyBatch();
            }
            else {
                session.moveToNextLessonItem();
            }
        });
    }

    private void showReadyBatch() {
        enableInteraction();
        android.widget.LinearLayout rows = new android.widget.LinearLayout(requireContext()); rows.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = com.smouldering_durtles.wk.ui.ModernUi.dp(rows, 16); rows.setPadding(pad, 0, pad, pad);
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext()); scroll.addView(rows);
        androidx.appcompat.app.AlertDialog dialog = com.smouldering_durtles.wk.ui.ModernUi.dialog(requireContext())
                .setTitle("Ready for the quiz · " + session.getItems().size() + " items").setView(scroll)
                .setNegativeButton("Keep studying", null).setPositiveButton("Start quiz", (d, w) -> { disableInteraction(); session.startQuiz(); }).create();
        for (int i = 0; i < session.getItems().size(); i++) {
            final int index = i; com.smouldering_durtles.wk.db.model.Subject entry = session.getItems().get(i).getSubject();
            if (entry == null) continue;
            android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext()); row.setPadding(0, pad / 2, 0, pad / 2); row.setMinimumHeight(pad * 3);
            android.widget.TextView japanese = new android.widget.TextView(requireContext()); japanese.setText(entry.getCharacters() == null ? entry.getSlug() : entry.getCharacters());
            japanese.setTextColor(com.smouldering_durtles.wk.ui.LearningColors.accent(requireContext(), entry.getBackgroundColor()));
            android.widget.TextView english = new android.widget.TextView(requireContext()); english.setText(entry.getOneMeaning()); english.setGravity(android.view.Gravity.END);
            row.addView(japanese, new android.widget.LinearLayout.LayoutParams(0, -2, 1)); row.addView(english, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
            row.setFocusable(true); row.setContentDescription("Revisit " + japanese.getText() + ", " + english.getText());
            row.setOnClickListener(v -> { dialog.dismiss(); session.revisitLessonItem(index); }); rows.addView(row);
        }
        dialog.show();
    }

    private void playAudio() {
        if (item == null || subject == null) {
            return;
        }

        if (FloatingUiState.audioPlayed) {
            return;
        }

        if (GlobalSettings.Audio.getAutoplayLessonPresentation()) {
            FloatingUiState.audioPlayed = true;
            AudioUtil.playAudio(subject, null);
        }
    }
}
