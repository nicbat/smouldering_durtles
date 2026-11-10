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

import android.animation.Animator;
import android.util.DisplayMetrics;
import android.view.View;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.livedata.SubjectChangeListener;
import com.smouldering_durtles.wk.model.FloatingUiState;
import com.smouldering_durtles.wk.model.Question;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.model.TypefaceConfiguration;
import com.smouldering_durtles.wk.proxy.ViewProxy;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.orElse;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Abstract superclass for the various quiz fragments.
 */
public abstract class AbstractSessionFragment extends AbstractFragment implements SubjectChangeListener {
    private final ViewProxy questionView = new ViewProxy();
    private final ViewProxy progress = new ViewProxy();
    private final ViewProxy srsIndicator = new ViewProxy();
    private final ViewProxy specialButton1 = new ViewProxy();
    private final ViewProxy specialButton2 = new ViewProxy();
    private final ViewProxy specialButton3 = new ViewProxy();
    private final ViewProxy questionType = new ViewProxy();
    private final ViewProxy questionText = new ViewProxy();
    private final ViewProxy starRating = new ViewProxy();

    /**
     * The singleton session for convenience.
     */
    protected static final Session session = Session.getInstance();

    /**
     * The constructor.
     *
     * @param layoutId the layout resource ID for this fragment
     */
    protected AbstractSessionFragment(final int layoutId) {
        super(layoutId);
    }

    @Override
    public View onCreateView(android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container,
                             @Nullable android.os.Bundle state) {
        final View view = super.onCreateView(inflater, container, state);
        if (view != null) com.smouldering_durtles.wk.study.StudyStyle.page(view);
        return view;
    }

    /**
     * Show the soft keyboard.
     *
     * @param view the view to attach the IME to.
     */
    protected final void showSoftInput(final ViewProxy view) {
        safe(() -> {
            final @Nullable View delegate = view.getDelegate();
            if (delegate != null) {
                showSoftInput(delegate);
            }
        });
    }

    /**
     * The part of onCreateView that is common among quiz fragments.
     *
     * @param view the root view of the fragment
     * @param question the current question
     * @param subject the subject for the current question
     * @param ankiMode true if this is an Anki mode fragment
     */
    protected final void onViewCreatedCommon(final View view, final Question question, final Subject subject,
                                            final boolean ankiMode) {
        if (!ankiMode) {
            View input = view.findViewById(R.id.questionEdit);
            android.view.ViewGroup parent = (android.view.ViewGroup) input.getParent();
            int index = parent.indexOfChild(input);
            android.view.ViewGroup.LayoutParams original = input.getLayoutParams();
            parent.removeView(input);
            android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            parent.addView(row, index, original);
            row.addView(input, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
            android.widget.ImageButton undo = new android.widget.ImageButton(requireContext());
            undo.setImageResource(R.drawable.ic_undo_24);
            undo.setContentDescription("Undo incorrect answer");
            undo.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            undo.setImageTintList(android.content.res.ColorStateList.valueOf(
                    com.google.android.material.color.MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary)));
            int size = com.smouldering_durtles.wk.ui.ModernUi.dp(view, 48);
            row.addView(undo, new android.widget.LinearLayout.LayoutParams(size, size));
            undo.setVisibility(session.canUndoIncorrect() ? View.VISIBLE : View.GONE);
            undo.setOnClickListener(v -> safe(() -> {
                if (!interactionEnabled || !session.canUndoIncorrect()) return;
                disableInteraction();
                session.undoAndRetry();
            }));
        }
        questionView.setDelegate(view, R.id.questionView);
        progress.setDelegate(view, R.id.progress);
        srsIndicator.setDelegate(view, R.id.srsIndicator);
        specialButton1.setDelegate(view, R.id.specialButton1);
        specialButton2.setDelegate(view, R.id.specialButton2);
        specialButton3.setDelegate(view, R.id.specialButton3);
        questionType.setDelegate(view, R.id.questionType);
        questionText.setDelegate(view, R.id.questionText);
        starRating.setDelegate(view, R.id.starRating);

        // Set the background color to match the type of subject (radical, kanji, vocab, kana_vocab)
        questionView.setBackgroundColor(subject.getBackgroundColor());

        // Show a "15/50" display in the corner to show how many items in the session
        // have been finished already
        progress.setText(session.getProgressText());

        // Show the star rating
        starRating.setVisibility(GlobalSettings.Other.getEnableStarRatings());
        starRating.setSubject(subject);

        // Show the current item's SRS stage in the top center
        srsIndicator.setText(subject.getSrsStage().getName());
        srsIndicator.setVisibility(GlobalSettings.Review.getEnableSrsIndicator() && !subject.getSrsStage().isInitial());

        // Show the special button 1 if allowed and relevant
        specialButton1.setVisibility(GlobalSettings.AdvancedOther.getSpecialButton1Behavior().canShow()
                && (ankiMode || GlobalSettings.AdvancedOther.getSpecialButton1Behavior() != com.smouldering_durtles.wk.enums.SpecialButtonBehavior.UNDO_AND_RETRY_NOW));
        com.smouldering_durtles.wk.study.StudyStyle.specialAction(specialButton1.getDelegate(), GlobalSettings.AdvancedOther.getSpecialButton1Behavior());

        // Show the special button 2 if allowed and relevant
        specialButton2.setVisibility(GlobalSettings.AdvancedOther.getSpecialButton2Behavior().canShow()
                && (ankiMode || GlobalSettings.AdvancedOther.getSpecialButton2Behavior() != com.smouldering_durtles.wk.enums.SpecialButtonBehavior.UNDO_AND_RETRY_NOW));
        com.smouldering_durtles.wk.study.StudyStyle.specialAction(specialButton2.getDelegate(), GlobalSettings.AdvancedOther.getSpecialButton2Behavior());

        // Show the special button 3 if allowed and relevant
        specialButton3.setVisibility(GlobalSettings.AdvancedOther.getSpecialButton3Behavior().canShow()
                && (ankiMode || GlobalSettings.AdvancedOther.getSpecialButton3Behavior() != com.smouldering_durtles.wk.enums.SpecialButtonBehavior.UNDO_AND_RETRY_NOW));
        com.smouldering_durtles.wk.study.StudyStyle.specialAction(specialButton3.getDelegate(), GlobalSettings.AdvancedOther.getSpecialButton3Behavior());

        // The main display of the text of the subject being quizzed
        final TypefaceConfiguration typefaceConfiguration =
                session.getCurrentTypefaceConfiguration(orElse(subject.getCharacters(), ""));
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        final int maxWidth = displayMetrics.widthPixels - dp2px(32);
        final int maxHeight = Math.round(GlobalSettings.Font.getMaxFontSizeQuizText() * displayMetrics.scaledDensity);
        questionText.setTransparent(true);
        questionText.setTypefaceConfiguration(typefaceConfiguration);
        questionText.setSubject(subject, question, session.getType());
        questionText.setMaxSize(maxWidth, maxHeight);
        questionText.setSizeForQuiz(true);

        // Toggle between typefaceConfiguration and DEFAULT on click
        questionText.setOnClickListener(v -> safe(() ->
                questionText.setTypefaceConfiguration(
                        questionText.getTypefaceConfiguration() == TypefaceConfiguration.DEFAULT
                                ? typefaceConfiguration
                                : TypefaceConfiguration.DEFAULT
                )
        ));

        // Color the question type view white or black for meaning and reading respectively
        questionType.setText(question.getTitle());
        questionType.setBackgroundColor(question.getType().getHintColor());
        questionType.setTextColor(question.getType().getHintContrastColor());
        questionType.setVisibility(true);
        com.smouldering_durtles.wk.study.StudyStyle.question(view, subject.getBackgroundColor());
        int bucket = subject.getSrsStage().getSrsBreakdownBucket();
        if (bucket >= 0) srsIndicator.setTextColor(com.smouldering_durtles.wk.ui.LearningColors.stage(requireContext(), bucket));

        specialButton1.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            GlobalSettings.AdvancedOther.getSpecialButton1Behavior().perform();
            com.smouldering_durtles.wk.study.StudyStyle.specialAction(specialButton1.getDelegate(), GlobalSettings.AdvancedOther.getSpecialButton1Behavior());
        }));

        specialButton2.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            GlobalSettings.AdvancedOther.getSpecialButton2Behavior().perform();
            com.smouldering_durtles.wk.study.StudyStyle.specialAction(specialButton2.getDelegate(), GlobalSettings.AdvancedOther.getSpecialButton2Behavior());
        }));

        specialButton3.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            GlobalSettings.AdvancedOther.getSpecialButton3Behavior().perform();
            com.smouldering_durtles.wk.study.StudyStyle.specialAction(specialButton3.getDelegate(), GlobalSettings.AdvancedOther.getSpecialButton3Behavior());
        }));
    }

    @Override
    public final void onSubjectChange(final Subject subject) {
        final @Nullable Subject currentSubject = getSubject();
        if (currentSubject == null) {
            return;
        }

        if (currentSubject.getId() == subject.getId()) {
            if (GlobalSettings.Review.getEnableSrsIndicator() && !subject.getSrsStage().isInitial()) {
                srsIndicator.setText(subject.getSrsStage().getName());
            }
            else {
                srsIndicator.setVisibility(false);
            }
            currentSubject.setSrsStage(subject.getSrsStage());
        }
    }

    @Override
    public final boolean isInterestedInSubject(final long subjectId) {
        final @Nullable Subject subject = getSubject();
        return subject != null && subject.getId() == subjectId;
    }

    /**
     * Show a brief toast/animation to indicate a correct/incorrect answer was given. This is mostly
     * useful when the quiz has lightning-mode advanced to the next question already.
     *
     * @param view the animation view to use for the animation.
     * @param animationId the Lottie animation ID
     */
    protected final void showPreviousAnswerToast(final ViewProxy view, final int animationId) {
        if (FloatingUiState.toastPlayed) {
            return;
        }

        view.setAnimation(animationId);
        view.setVisibility(View.VISIBLE);
        view.setRepeatCount(0);

        view.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(final Animator animation) {
                //
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                safe(() -> {
                    view.setVisibility(View.GONE);
                    view.removeAnimatorListener(this);
                });
            }

            @Override
            public void onAnimationCancel(final Animator animation) {
                safe(() -> {
                    view.setVisibility(View.GONE);
                    view.removeAnimatorListener(this);
                });
            }

            @Override
            public void onAnimationRepeat(final Animator animation) {
                safe(() -> {
                    view.setVisibility(View.GONE);
                    view.removeAnimatorListener(this);
                });
            }
        });

        view.playAnimation();

        FloatingUiState.toastPlayed = true;
    }

    /**
     * Get the session item that this fragment is currently dealing with.
     *
     * @return the item or null if there is no specific item
     */
    public abstract @Nullable SessionItem getItem();

    /**
     * Get the question that this fragment is currently dealing with.
     *
     * @return the question or null if there is no specific question
     */
    public abstract @Nullable Question getQuestion();

    /**
     * Get the subject that this fragment is currently dealing with.
     *
     * @return the subject or null if there is no specific question
     */
    protected abstract @Nullable Subject getSubject();

    /**
     * What transition animation to use, given the current and new fragment, and the current session state.
     *
     * @param newFragment the new fragment to transition to
     * @return the animation type
     */
    public abstract FragmentTransitionAnimation getAnimation(AbstractSessionFragment newFragment);
}
