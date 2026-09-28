package com.banking.ui;

import com.banking.simulation.SimulationController;
import com.banking.simulation.SimulationController.SimulationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class SimulationFrameTest {

    private SimulationFrame frame;
    private SimulationController controller;

    @BeforeEach
    void setUp() throws Exception {
        runOnEdt(() -> {
            controller = new SimulationController(2);
            frame = new SimulationFrame(controller);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        runOnEdt(() -> {
            if (controller != null && !controller.isStopped()) {
                controller.stop();
            }
            if (frame != null) {
                frame.dispose();
            }
        });
    }

    private void runOnEdt(Runnable runnable) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeAndWait(runnable);
        }
    }

    @Test
    void testFrameInitializationAndTitle() throws Exception {
        runOnEdt(() -> {
            assertEquals("GhostSim - Concurrent Banking Simulation", frame.getTitle());
            assertNotNull(frame.getStartButton());
            assertEquals("START", frame.getStartButton().getText());
            assertNotNull(frame.getPauseButton());
            assertEquals("PAUSE", frame.getPauseButton().getText());
            assertNotNull(frame.getResumeButton());
            assertEquals("RESUME", frame.getResumeButton().getText());
            assertNotNull(frame.getStopButton());
            assertEquals("STOP", frame.getStopButton().getText());
            assertNotNull(frame.getStatusLabel());
            assertTrue(frame.getStatusLabel().getText().contains("STOPPED"));
            assertNotNull(frame.getLogArea());
        });
    }

    @Test
    void testInitialButtonStates() throws Exception {
        runOnEdt(() -> {
            assertTrue(frame.getStartButton().isEnabled());
            assertFalse(frame.getPauseButton().isEnabled());
            assertFalse(frame.getResumeButton().isEnabled());
            assertFalse(frame.getStopButton().isEnabled());
            assertTrue(frame.getStatusLabel().getText().contains("STOPPED"));
        });
    }

    @Test
    void testStartButtonAction() throws Exception {
        runOnEdt(() -> {
            frame.getStartButton().doClick();
            assertEquals(SimulationState.RUNNING, controller.getState());
            assertFalse(frame.getStartButton().isEnabled());
            assertTrue(frame.getPauseButton().isEnabled());
            assertFalse(frame.getResumeButton().isEnabled());
            assertTrue(frame.getStopButton().isEnabled());
            assertTrue(frame.getStatusLabel().getText().contains("RUNNING"));
        });
    }

    @Test
    void testPauseAndResumeButtonActions() throws Exception {
        runOnEdt(() -> {
            frame.getStartButton().doClick();
            frame.getPauseButton().doClick();

            assertEquals(SimulationState.PAUSED, controller.getState());
            assertFalse(frame.getStartButton().isEnabled());
            assertFalse(frame.getPauseButton().isEnabled());
            assertTrue(frame.getResumeButton().isEnabled());
            assertTrue(frame.getStopButton().isEnabled());
            assertTrue(frame.getStatusLabel().getText().contains("PAUSED"));

            frame.getResumeButton().doClick();

            assertEquals(SimulationState.RUNNING, controller.getState());
            assertFalse(frame.getStartButton().isEnabled());
            assertTrue(frame.getPauseButton().isEnabled());
            assertFalse(frame.getResumeButton().isEnabled());
            assertTrue(frame.getStopButton().isEnabled());
            assertTrue(frame.getStatusLabel().getText().contains("RUNNING"));
        });
    }

    @Test
    void testStopButtonAction() throws Exception {
        runOnEdt(() -> {
            frame.getStartButton().doClick();
            frame.getStopButton().doClick();

            assertFalse(frame.getPauseButton().isEnabled());
            assertFalse(frame.getResumeButton().isEnabled());
            assertFalse(frame.getStopButton().isEnabled());
            assertTrue(frame.getStartButton().isEnabled());
            assertTrue(frame.getStatusLabel().getText().contains("STOPPED"));
        });

        // Wait a short moment for background thread stop to complete
        Thread.sleep(100);
        assertTrue(controller.isStopped());
    }

    @Test
    void testDefaultConstructorAndLogArea() throws Exception {
        runOnEdt(() -> {
            SimulationFrame defaultFrame = new SimulationFrame();
            assertNotNull(defaultFrame.getController());
            defaultFrame.appendLog("Test Log Entry");
            assertTrue(defaultFrame.getLogArea().getText().contains("Test Log Entry"));
            defaultFrame.dispose();
        });
    }
}
