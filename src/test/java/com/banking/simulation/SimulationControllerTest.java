package com.banking.simulation;

import com.banking.simulation.SimulationController.SimulationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimulationControllerTest {

    private SimulationController controller;

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.stop();
        }
    }

    @Test
    void testInitialStoppedState() {
        controller = new SimulationController();

        assertEquals(SimulationState.STOPPED, controller.getState());
        assertTrue(controller.isStopped());
        assertFalse(controller.isRunning());
        assertFalse(controller.isPaused());
    }

    @Test
    void testStartChangesStateToRunning() {
        controller = new SimulationController(2);
        controller.start();

        assertEquals(SimulationState.RUNNING, controller.getState());
        assertTrue(controller.isRunning());
        assertFalse(controller.isPaused());
        assertFalse(controller.isStopped());
        assertEquals(2, controller.getWorkerThreads().size());
    }

    @Test
    void testPauseChangesStateToPaused() {
        controller = new SimulationController(2);
        controller.start();
        controller.pause();

        assertEquals(SimulationState.PAUSED, controller.getState());
        assertTrue(controller.isPaused());
        assertFalse(controller.isRunning());
        assertFalse(controller.isStopped());
    }

    @Test
    void testResumeReturnsToRunning() {
        controller = new SimulationController(2);
        controller.start();
        controller.pause();
        controller.resume();

        assertEquals(SimulationState.RUNNING, controller.getState());
        assertTrue(controller.isRunning());
        assertFalse(controller.isPaused());
        assertFalse(controller.isStopped());
    }

    @Test
    void testStopReturnsToStopped() {
        controller = new SimulationController(2);
        controller.start();
        controller.stop();

        assertEquals(SimulationState.STOPPED, controller.getState());
        assertTrue(controller.isStopped());
        assertFalse(controller.isRunning());
        assertFalse(controller.isPaused());
    }

    @Test
    void testWorkersActuallyRunWhileSimulationIsRunning() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        controller = new SimulationController(2, () -> {
            counter.incrementAndGet();
            latch.countDown();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controller.start();
        boolean reached = latch.await(3, TimeUnit.SECONDS);

        assertTrue(reached, "Workers should execute tasks and decrement latch");
        assertTrue(counter.get() >= 10, "Counter should be incremented at least 10 times");
    }

    @Test
    void testWorkersStopAfterStop() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        controller = new SimulationController(2, () -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controller.start();
        Thread.sleep(50);
        List<Thread> activeThreads = controller.getWorkerThreads();
        assertFalse(activeThreads.isEmpty());

        controller.stop();

        // Verify that all previous worker threads are terminated
        for (Thread thread : activeThreads) {
            assertFalse(thread.isAlive(), "Worker thread should be terminated after stop()");
        }

        int countAtStop = counter.get();
        Thread.sleep(50);
        assertEquals(countAtStop, counter.get(), "Counter should not increase after stop()");
        assertTrue(controller.getWorkerThreads().isEmpty());
    }

    @Test
    void testStartingAgainAfterStopWorks() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch firstRunLatch = new CountDownLatch(5);

        controller = new SimulationController(2, () -> {
            counter.incrementAndGet();
            firstRunLatch.countDown();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controller.start();
        assertTrue(firstRunLatch.await(3, TimeUnit.SECONDS));
        controller.stop();
        assertEquals(SimulationState.STOPPED, controller.getState());

        int countAfterFirstStop = counter.get();
        CountDownLatch secondRunLatch = new CountDownLatch(5);
        // Change task or reuse controller to run again
        SimulationController restartableController = new SimulationController(2, () -> {
            counter.incrementAndGet();
            secondRunLatch.countDown();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        restartableController.start();
        assertTrue(secondRunLatch.await(3, TimeUnit.SECONDS));
        restartableController.stop();

        assertTrue(counter.get() >= countAfterFirstStop + 5);

        // Also test controller restarting itself directly
        controller.start();
        assertTrue(controller.isRunning());
        assertEquals(2, controller.getWorkerThreads().size());
        controller.stop();
        assertTrue(controller.isStopped());
    }

    @Test
    void testRepeatedStartPauseResumeStopCallsDoNotCrashOrDuplicateWorkers() {
        controller = new SimulationController(3);

        // Calling start multiple times
        controller.start();
        controller.start();
        assertEquals(SimulationState.RUNNING, controller.getState());
        assertEquals(3, controller.getWorkerThreads().size());

        // Calling pause multiple times
        controller.pause();
        controller.pause();
        assertEquals(SimulationState.PAUSED, controller.getState());

        // Calling resume multiple times
        controller.resume();
        controller.resume();
        assertEquals(SimulationState.RUNNING, controller.getState());

        // Calling stop multiple times
        controller.stop();
        controller.stop();
        assertEquals(SimulationState.STOPPED, controller.getState());

        // Calling pause/resume while stopped does not crash
        controller.pause();
        assertEquals(SimulationState.STOPPED, controller.getState());
        controller.resume();
        assertEquals(SimulationState.STOPPED, controller.getState());
    }

    @Test
    void testPausePreventsWorkersFromExecutingUntilResumed() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean allowProgress = new AtomicBoolean(true);

        controller = new SimulationController(2, () -> {
            if (allowProgress.get()) {
                counter.incrementAndGet();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controller.start();
        Thread.sleep(50);

        controller.pause();
        // Give time for any currently running iteration to finish
        Thread.sleep(50);
        int countDuringPause = counter.get();

        // Wait a bit more during pause
        Thread.sleep(100);
        assertEquals(countDuringPause, counter.get(), "Worker executions must freeze while paused");

        // Now resume
        controller.resume();
        Thread.sleep(100);
        assertTrue(counter.get() > countDuringPause, "Worker executions should resume after resume()");
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SimulationController(0));
        assertThrows(IllegalArgumentException.class, () -> new SimulationController(-1));
        assertThrows(IllegalArgumentException.class, () -> new SimulationController(2, null, -5));
        assertThrows(IllegalArgumentException.class, () -> new SimulationController((List<Runnable>) null));
        assertThrows(IllegalArgumentException.class, () -> new SimulationController(List.of()));
    }
}
