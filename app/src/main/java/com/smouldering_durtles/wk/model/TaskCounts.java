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

package com.smouldering_durtles.wk.model;

/**
 * Model for the number of active network background tasks.
 */
public final class TaskCounts {
    private final int apiCount;
    private final int audioCount;
    private final int pitchInfoCount;
    private final int syncCount;
    private final int uploadCount;

    /**
     * The constructor.
     *
     * @param apiCount the total number of API tasks
     * @param audioCount the total number of audio download tasks
     * @param pitchInfoCount the total number of pitch info download tasks
     */
    public TaskCounts(final int apiCount, final int audioCount, final int pitchInfoCount,
                      final int syncCount, final int uploadCount) {
        this.apiCount = apiCount;
        this.audioCount = audioCount;
        this.pitchInfoCount = pitchInfoCount;
        this.syncCount = syncCount;
        this.uploadCount = uploadCount;
    }

    /**
     * The total number of API tasks.
     * @return the value
     */
    public int getApiCount() {
        return apiCount;
    }

    /**
     * The total number of audio download tasks.
     * @return the value
     */
    public int getAudioCount() {
        return audioCount;
    }

    /**
     * The total number of pitch info download tasks.
     * @return the value
     */
    public int getPitchInfoCount() {
        return pitchInfoCount;
    }

    /** Network sync tasks only, excluding local maintenance and media downloads. */
    public int getSyncCount() { return syncCount; }

    /** Persisted session results and study-material edits awaiting acknowledgement. */
    public int getUploadCount() { return uploadCount; }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TaskCounts other = (TaskCounts) obj;
        return apiCount == other.apiCount && audioCount == other.audioCount && pitchInfoCount == other.pitchInfoCount
                && syncCount == other.syncCount && uploadCount == other.uploadCount;
    }

    @Override
    public int hashCode() {
        return apiCount + audioCount + pitchInfoCount + syncCount + uploadCount;
    }

    /**
     * Is the collection of tasks empty?.
     *
     * @return true if it is
     */
    public boolean isEmpty() {
        return apiCount == 0 && audioCount == 0 && pitchInfoCount == 0;
    }
}
