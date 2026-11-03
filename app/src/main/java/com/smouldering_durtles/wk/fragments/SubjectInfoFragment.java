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
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.activities.AbstractActivity;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.views.SubjectInfoView;
import com.smouldering_durtles.wk.views.SwipingScrollView;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;

import java.util.Objects;

/**
 * A fragment to show the full subject info dump.
 */
public final class SubjectInfoFragment extends AbstractFragment implements SwipingScrollView.OnSwipeListener {
    private final ViewProxy scrollView = new ViewProxy();
    private final ViewProxy subjectInfo = new ViewProxy();

    private @Nullable Subject currentSubject = null;
    private boolean paging = false;
    private long pageGeneration = 0;

    /**
     * The constructor.
     */
    public SubjectInfoFragment() {
        super(R.layout.fragment_subject_info);
    }

    /**
     * Create a new instance with arguments set.
     *
     * @param id the subject ID
     * @param ids the context list of subject IDs
     * @return the fragment
     */
    public static SubjectInfoFragment newInstance(final long id, final long[] ids) {
        final SubjectInfoFragment fragment = new SubjectInfoFragment();
        final Bundle args = new Bundle();
        args.putLong("id", id);
        args.putLongArray("ids", ids);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container,
                             @Nullable Bundle state) {
        if (com.smouldering_durtles.wk.GlobalSettings.Display.getModernSettings()) {
            final View view = inflater.cloneInContext(com.smouldering_durtles.wk.ui.ModernUi.context(requireContext()))
                    .inflate(R.layout.fragment_subject_info_modern, container, false);
            com.smouldering_durtles.wk.study.StudyStyle.page(view);
            return view;
        }
        return super.onCreateView(inflater, container, state);
    }

    @Override
    protected void onCreateLocal() {
        //
    }

    @Override
    protected void onResumeLocal() {
        subjectInfo.setToolbar(getToolbar());
    }

    @Override
    public void onViewCreatedLocal(final View view, final @Nullable Bundle savedInstanceState) {
        scrollView.setDelegate(view, R.id.scrollView);
        subjectInfo.setDelegate(view, R.id.subjectInfo);
        if (savedInstanceState != null) com.smouldering_durtles.wk.study.LessonStyle.restoreDetails(view,
                savedInstanceState.getBoolean("detailsExpanded"));

        scrollView.setSwipeListener(this);
        final View previous = view.findViewById(R.id.previousSubject);
        final View next = view.findViewById(R.id.nextSubject);
        if (previous != null) previous.setOnClickListener(v -> onSwipeLeft(view.findViewById(R.id.scrollView)));
        if (next != null) next.setOnClickListener(v -> onSwipeRight(view.findViewById(R.id.scrollView)));

        final @Nullable Bundle args = getArguments();
        if (args != null) {
            final long subjectId = args.getLong("id", -1);
            if (subjectId != -1) {
                runAsync(
                        this,
                        () -> WkApplication.getDatabase().subjectDao().getById(subjectId),
                        result -> {
                            currentSubject = result;
                            updateViews();
                        });
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (getView() != null) state.putBoolean("detailsExpanded",
                com.smouldering_durtles.wk.study.LessonStyle.isDetailsExpanded(getView()));
    }

    /** Keep the current page and back-stack entry until the adjacent subject is ready. */
    public void pageToSubject(long id, long[] ids, FragmentTransitionAnimation direction) {
        if (paging || !interactionEnabled) return;
        paging = true;
        final long generation = pageGeneration;
        updateNavigation();
        runAsync(this, () -> WkApplication.getDatabase().subjectDao().getById(id), result -> {
            if (generation != pageGeneration) return;
            if (getView() == null || !isAdded()) { paging = false; return; }
            if (result == null) { paging = false; updateNavigation(); return; }
            showAdjacentSubject(result, ids, direction);
        });
    }

    /** Present an already loaded adjacent item without replacing this fragment or its back-stack entry. */
    public void showAdjacentSubject(Subject result, long[] ids, FragmentTransitionAnimation direction) {
        if (getView() == null) { paging = false; return; }
        paging = true;
        updateNavigation();
        View content = getView().findViewById(R.id.scrollView);
        Runnable bind = () -> {
            if (getView() == null) { paging = false; return; }
            currentSubject = result;
            requireArguments().putLong("id", result.getId());
            requireArguments().putLongArray("ids", ids);
            updateViews();
            ((android.widget.ScrollView) content).scrollTo(0, 0);
        };
        boolean animations = direction != FragmentTransitionAnimation.NONE && GlobalSettings.Display.getSlideAnimations()
                && (android.os.Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled());
        if (!animations) { bind.run(); paging = false; updateNavigation(); return; }
        float offset = com.smouldering_durtles.wk.ui.ModernUi.dp(content, 12)
                * (direction == FragmentTransitionAnimation.LTR ? -1 : 1);
        content.animate().cancel();
        content.animate().alpha(0).translationX(-offset).setDuration(70).withEndAction(() -> {
            bind.run();
            content.setTranslationX(offset);
            content.animate().alpha(1).translationX(0).setDuration(140)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> { paging = false; updateNavigation(); }).start();
        }).start();
    }

    @Override
    public void onDestroyView() {
        pageGeneration++;
        paging = false;
        if (getView() != null) {
            View content = getView().findViewById(R.id.scrollView);
            content.animate().cancel();
            content.setAlpha(1);
            content.setTranslationX(0);
        }
        super.onDestroyView();
    }

    @Override
    public @Nullable String getToolbarTitle() {
        return null;
    }

    @Override
    public int getToolbarBackgroundColor() {
        return 0;
    }

    @Override
    public void enableInteraction() {
        interactionEnabled = true;
        updateNavigation();
    }

    @Override
    public void disableInteraction() {
        interactionEnabled = false;
        updateNavigation();
    }

    @Override
    public @Nullable Subject getCurrentSubject() {
        return currentSubject;
    }

    @Override
    public void showOrHideSoftInput() {
        hideSoftInput();
    }

    @Override
    public void updateViews() {
        if (currentSubject != null) {
            subjectInfo.setMaxFontSize(GlobalSettings.Font.getMaxFontSizeBrowse());
            subjectInfo.setContainerType(SubjectInfoView.ContainerType.BROWSE);
            subjectInfo.setSubject(this, currentSubject);
        }
        updateNavigation();
        updateCurrentSubject();
    }

    private void updateNavigation() {
        final View view = getView();
        if (view == null || view.findViewById(R.id.subjectNavigation) == null) return;
        final Bundle args = getArguments();
        final long[] ids = args == null ? null : args.getLongArray("ids");
        int index = -1;
        if (ids != null && currentSubject != null) {
            for (int i = 0; i < ids.length; i++) if (ids[i] == currentSubject.getId()) { index = i; break; }
        }
        view.findViewById(R.id.subjectNavigation).setVisibility(index >= 0 && ids.length > 1 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.previousSubject).setEnabled(index > 0 && interactionEnabled && !paging);
        view.findViewById(R.id.nextSubject).setEnabled(index >= 0 && index < ids.length - 1 && interactionEnabled && !paging);
    }

    private void goToAdjacentSubject(AbstractActivity activity, long id, long[] ids,
                                     FragmentTransitionAnimation animation) {
        if (activity instanceof com.smouldering_durtles.wk.activities.BrowseActivity) {
            ((com.smouldering_durtles.wk.activities.BrowseActivity) activity).pageSubjectInfoFragment(id, ids, animation);
        } else activity.goToSubjectInfo(id, ids, animation);
    }

    @Override
    public void onSwipeLeft(final SwipingScrollView view) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        final @Nullable Bundle args = getArguments();
        if (!interactionEnabled || paging || currentSubject == null || activity == null || args == null) {
            return;
        }
        final long[] ids = args.getLongArray("ids");
        if (ids == null) {
            return;
        }
        for (int i=1; i<ids.length; i++) {
            if (ids[i] == currentSubject.getId()) {
                goToAdjacentSubject(activity, ids[i-1], ids, FragmentTransitionAnimation.LTR);
                break;
            }
        }
    }

    @Override
    public void onSwipeRight(final SwipingScrollView view) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        final @Nullable Bundle args = getArguments();
        if (!interactionEnabled || paging || currentSubject == null || activity == null || args == null) {
            return;
        }
        final long[] ids = Objects.requireNonNull(args.getLongArray("ids"));
        for (int i=0; i<ids.length-1; i++) {
            if (ids[i] == currentSubject.getId()) {
                goToAdjacentSubject(activity, ids[i+1], ids, FragmentTransitionAnimation.RTL);
                break;
            }
        }
    }
}
