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

package com.smouldering_durtles.wk.livedata;

import android.os.Handler;
import android.os.Looper;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A watcher that keeps track of changes to subjects' assignments, statistics and study materials.
 *
 * <p>
 *     Listeners are asked ahead of time if they are interested in updates for a specific subject ID.
 * </p>
 */
public final class SubjectChangeWatcher {
    private static final SubjectChangeWatcher instance = new SubjectChangeWatcher();
    private final ThreadLocal<Set<Long>> deferredChanges = new ThreadLocal<>();

    private static final Object MARK = new Object();
    private final Map<SubjectChangeListener, Object> map = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static SubjectChangeWatcher getInstance() {
        return instance;
    }

    private SubjectChangeWatcher() {
        //
    }

    /**
     * Add a listener that will be notified of changes to subjects.
     *
     * @param listener the listener to add
     */
    public void addListener(final SubjectChangeListener listener) {
        map.put(listener, MARK);
    }

    /** Publish changes only after the supplied database transaction commits. */
    public void withDeferredChanges(final Runnable transaction) {
        if (deferredChanges.get() != null) {
            throw new IllegalStateException("Nested change batches are not supported");
        }
        final Set<Long> changed = new LinkedHashSet<>();
        deferredChanges.set(changed);
        try {
            transaction.run();
        } finally {
            deferredChanges.remove();
        }
        // Exceptions skip this loop: rolled-back data must never reach a subject screen.
        for (final long id : changed) {
            reportChange(id);
        }
    }

    /**
     * Report a change to a subject.
     *
     * @param subjectId the ID of the subject that has been changed
     */
    public void reportChange(final long subjectId) {
        final Set<Long> changed = deferredChanges.get();
        if (changed != null) {
            changed.add(subjectId);
            return;
        }
        safe(() -> {
            @Nullable Subject subject = null;
            final Iterable<SubjectChangeListener> listeners = new ArrayList<>(map.keySet());
            final AppDatabase db = WkApplication.getDatabase();
            for (final SubjectChangeListener listener: listeners) {
                if (listener.isInterestedInSubject(subjectId)) {
                    if (subject == null) {
                        subject = db.subjectDao().getById(subjectId);
                    }
                    if (subject != null) {
                        final Subject theSubject = subject;
                        new Handler(Looper.getMainLooper()).post(() -> safe(() -> listener.onSubjectChange(theSubject)));
                    }
                }
            }
        });
    }
}
