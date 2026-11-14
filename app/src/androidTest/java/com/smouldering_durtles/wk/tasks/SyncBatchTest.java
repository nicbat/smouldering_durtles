package com.smouldering_durtles.wk.tasks;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smouldering_durtles.wk.api.model.ApiAssignment;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.livedata.LiveApiProgress;
import com.smouldering_durtles.wk.livedata.SubjectChangeListener;
import com.smouldering_durtles.wk.livedata.SubjectChangeWatcher;
import com.smouldering_durtles.wk.model.TaskCounts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SyncBatchTest {
    private AppDatabase db;
    @Before public void setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase.class).build();
        LiveApiProgress.reset(true, "test");
    }
    @After public void cleanup() { db.close(); }

    private void insertTask(long id) {
        TaskDefinition task = new TaskDefinition();
        task.setId((int) id);
        task.setTaskClass(GetAssignmentsTask.class);
        db.taskDefinitionDao().insertTaskDefinition(task);
    }

    private JsonNode page() throws Exception {
        return new ObjectMapper().readTree("["
                + "{\"id\":1,\"object\":\"assignment\",\"data_updated_at\":null,\"data\":{}},"
                + "{\"id\":2,\"object\":\"assignment\",\"data_updated_at\":null,\"data\":{}}]");
    }

    @Test public void failedPageRollsBackWithoutProgressOrNotificationsThenRetries() throws Exception {
        AtomicInteger notifications = new AtomicInteger();
        SubjectChangeListener listener = new SubjectChangeListener() {
            @Override public boolean isInterestedInSubject(long id) {
                assertFalse("Notification after commit only", db.inTransaction());
                notifications.incrementAndGet();
                return false;
            }
            @Override public void onSubjectChange(Subject subject) { fail(); }
        };
        SubjectChangeWatcher.getInstance().addListener(listener);
        try {
            ApiTask.applyCollectionPage(db, page(), ApiAssignment.class, entity -> {
                insertTask(entity.getId());
                SubjectChangeWatcher.getInstance().reportChange(entity.getId());
                if (entity.getId() == 2) throw new IllegalStateException("simulated write failure");
            });
            fail("Expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("simulated write failure", expected.getMessage());
        }
        assertEquals(0, db.taskDefinitionDao().getCount());
        assertEquals(0, notifications.get());
        assertEquals(0, LiveApiProgress.getNumProcessedEntities());
        ApiTask.applyCollectionPage(db, page(), ApiAssignment.class, entity -> {
            assertTrue(db.inTransaction());
            insertTask(entity.getId());
            SubjectChangeWatcher.getInstance().reportChange(entity.getId());
        });
        assertEquals(2, db.taskDefinitionDao().getCount());
        assertEquals(2, LiveApiProgress.getNumProcessedEntities());
        assertEquals(2, notifications.get());
    }

    @Test public void malformedPageCannotPartiallyApplyValidFirstRecord() throws Exception {
        JsonNode invalid = new ObjectMapper().readTree("["
                + "{\"id\":1,\"object\":\"assignment\",\"data_updated_at\":null,\"data\":{}},"
                + "{\"id\":2,\"object\":\"assignment\",\"data_updated_at\":null,\"data\":{\"srs_stage\":{}}}]");
        try {
            ApiTask.applyCollectionPage(db, invalid, ApiAssignment.class,
                    entity -> insertTask(entity.getId()));
            fail("Expected malformed record to fail");
        } catch (java.io.IOException expected) { /* expected */ }
        assertEquals(0, db.taskDefinitionDao().getCount());
        assertEquals(0, LiveApiProgress.getNumProcessedEntities());
    }

    @Test public void queueSeparatesUploadsSyncAndLocalOrMediaWork() throws Exception {
        for (Class<? extends ApiTask> type : java.util.Arrays.asList(
                ReportSessionItemTask.class, SubmitStudyMaterialTask.class, GetAssignmentsTask.class,
                DownloadAudioTask.class, DownloadPitchInfoTask.class,
                ScanAudioDownloadStatusTask.class, LoadReferenceDataTask.class)) {
            TaskDefinition task = new TaskDefinition();
            task.setTaskClass(type);
            db.taskDefinitionDao().insertTaskDefinition(task);
        }
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<TaskCounts> result = new AtomicReference<>();
        androidx.lifecycle.LiveData<TaskCounts> live = db.taskDefinitionDao().getLiveCounts();
        androidx.lifecycle.Observer<TaskCounts> observer = counts -> {
            result.set(counts); received.countDown();
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.observeForever(observer));
        try {
            assertTrue(received.await(5, TimeUnit.SECONDS));
            TaskCounts counts = result.get();
            assertEquals(5, counts.getApiCount()); // Existing setup completion semantics preserved.
            assertEquals(3, counts.getSyncCount());
            assertEquals(2, counts.getUploadCount());
            assertEquals(1, counts.getAudioCount());
            assertEquals(1, counts.getPitchInfoCount());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.removeObserver(observer));
        }
    }
}
