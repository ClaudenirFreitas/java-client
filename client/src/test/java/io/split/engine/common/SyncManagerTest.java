package io.split.engine.common;

import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManagerImpl;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;

public class SyncManagerTest {
    private Synchronizer _synchronizer;
    private PushManager _pushManager;
    private SDKReadinessGates _gates;
    private ImpressionsManagerImpl _impressionsManager;
    private TelemetrySynchronizer _telemetrySynchronizer;
    private TelemetrySyncTask _telemetrySyncTask;
    private EventsTask _eventsTask;
    private SplitClientConfig _config;
    private SegmentSynchronizationTaskImp _segmentSynchronizationTaskImp;
    private SplitSynchronizationTask _splitSynchronizationTask;

    @Before
    public void setUp() {
        _synchronizer = Mockito.mock(Synchronizer.class);
        _pushManager = Mockito.mock(PushManager.class);
        _gates = Mockito.mock(SDKReadinessGates.class);
        _impressionsManager = Mockito.mock(ImpressionsManagerImpl.class);
        _telemetrySynchronizer = Mockito.mock(TelemetrySynchronizer.class);
        _telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        _eventsTask = Mockito.mock(EventsTask.class);
        _config = Mockito.mock(SplitClientConfig.class);
        _segmentSynchronizationTaskImp = Mockito.mock(SegmentSynchronizationTaskImp.class);
        _splitSynchronizationTask = Mockito.mock(SplitSynchronizationTask.class);
    }

    @Test
    public void startWithStreamingFalseShouldStartPolling() throws InterruptedException {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        _gates.sdkInternalReady();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);

        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);
        SyncManagerImp syncManager = new SyncManagerImp( splitTasks, false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(),
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null, splitAPI);
        syncManager.start();
        Thread.sleep(1000);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
    }

    @Test
    public void startWithStreamingTrueShouldStartSyncAll() throws InterruptedException {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);

        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);
        SyncManager sm = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, new LinkedBlockingQueue<>(),
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null, splitAPI);
        sm.start();
        Thread.sleep(1000);
        Mockito.verify(_synchronizer, Mockito.times(0)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).start();
        Mockito.verify(telemetryStorage, Mockito.times(1)).recordStreamingEvents(Mockito.any());
    }

    @Test
    public void onStreamingAvailable() throws InterruptedException {
        TelemetryStorage telemetryStorage = Mockito.mock(TelemetryStorage.class);
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messages,
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null, splitAPI);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messages.offer(PushManager.Status.STREAMING_READY);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).stopPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(1)).startWorkers();
        Mockito.verify(telemetryStorage, Mockito.times(1)).recordStreamingEvents(Mockito.any());
        t.interrupt();
    }

    @Test
    public void onStreamingDisabled() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null, splitAPI);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_DOWN);
        Thread.sleep(500);

        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_pushManager, Mockito.times(1)).stopWorkers();
        t.interrupt();
    }

    @Test
    public void onStreamingShutdown() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);
        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null,splitAPI);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_OFF);
        Thread.sleep(500);
        Mockito.verify(_pushManager, Mockito.times(1)).stop();
        t.interrupt();
    }

    @Test
    public void onConnected() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null,splitAPI);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_READY);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).stopPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        t.interrupt();
    }

    @Test
    public void onDisconnect() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);

        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null, splitAPI);
        Thread t = new Thread(syncManager::incomingPushStatusHandler);
        t.start();
        messsages.offer(PushManager.Status.STREAMING_OFF);
        Thread.sleep(500);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        t.interrupt();
    }

    @Test
    public void onDisconnectAndReconnect() throws InterruptedException { // Check with mauro. reconnect should call pushManager.start again, right?
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messsages = new LinkedBlockingQueue<>();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);

        Mockito.when(_synchronizer.syncAll()).thenReturn(true);
        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, true, _synchronizer, _pushManager, messsages,
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null, splitAPI);
        syncManager.start();
        messsages.offer(PushManager.Status.STREAMING_BACKOFF);
        Thread.sleep(1200);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(1)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(2)).start();
    }

    @Test
    public void syncAllRetryThenShouldStartPolling() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        SplitTasks splitTasks = SplitTasks.build(_splitSynchronizationTask, _segmentSynchronizationTaskImp,
                _impressionsManager, _eventsTask, _telemetrySyncTask);
        SplitAPI splitAPI = Mockito.mock(SplitAPI.class);

        Mockito.when(_synchronizer.syncAll()).thenReturn(false).thenReturn(true);
        SyncManagerImp syncManager = new SyncManagerImp(splitTasks, false, _synchronizer, _pushManager, new LinkedBlockingQueue<>(),
                _gates, telemetryStorage, _telemetrySynchronizer, _config, null, splitAPI);
        syncManager.start();
        Thread.sleep(2000);
        Mockito.verify(_synchronizer, Mockito.times(1)).startPeriodicFetching();
        Mockito.verify(_synchronizer, Mockito.times(2)).syncAll();
        Mockito.verify(_pushManager, Mockito.times(0)).start();
        Mockito.verify(_gates, Mockito.times(1)).sdkInternalReady();
        Mockito.verify(_telemetrySynchronizer, Mockito.times(1)).synchronizeConfig(Mockito.anyObject(), Mockito.anyLong(), Mockito.anyObject(), Mockito.anyObject());
    }
}
