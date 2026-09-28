package com.banking.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimulationController {

    public enum SimulationState {
        STOPPED,
        RUNNING,
        PAUSED
    }

    private final Object stateLock = new Object();
    private volatile SimulationState state = SimulationState.STOPPED;

    private final int workerCount;
    private final Runnable defaultTask;
    private final List<Runnable> customTasks;
    private final long delayMillis;

    private final List<Thread> workerThreads = new ArrayList<>();

    public SimulationController() {
        this(5, null, 0);
    }

    public SimulationController(int workerCount) {
        this(workerCount, null, 0);
    }

    public SimulationController(int workerCount, Runnable defaultTask) {
        this(workerCount, defaultTask, 0);
    }

    public SimulationController(int workerCount, Runnable defaultTask, long delayMillis) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be greater than zero");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }
        this.workerCount = workerCount;
        this.defaultTask = defaultTask;
        this.customTasks = null;
        this.delayMillis = delayMillis;
    }

    public SimulationController(List<Runnable> tasks) {
        this(tasks, 0);
    }

    public SimulationController(List<Runnable> tasks, long delayMillis) {
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Tasks list cannot be null or empty");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }
        this.workerCount = tasks.size();
        this.defaultTask = null;
        this.customTasks = new ArrayList<>(tasks);
        this.delayMillis = delayMillis;
    }

    public void start() {
        synchronized (stateLock) {
            if (state == SimulationState.RUNNING || state == SimulationState.PAUSED) {
                return;
            }

            state = SimulationState.RUNNING;
            workerThreads.clear();

            for (int i = 0; i < workerCount; i++) {
                final Runnable taskToRun;
                if (customTasks != null && i < customTasks.size()) {
                    taskToRun = customTasks.get(i);
                } else {
                    taskToRun = defaultTask;
                }

                Thread workerThread = new Thread(() -> runWorkerLoop(taskToRun), "TransferWorker-" + (i + 1));
                workerThreads.add(workerThread);
            }

            for (Thread workerThread : workerThreads) {
                workerThread.start();
            }
        }
    }

    public void pause() {
        synchronized (stateLock) {
            if (state != SimulationState.RUNNING) {
                return;
            }
            state = SimulationState.PAUSED;
        }
    }

    public void resume() {
        synchronized (stateLock) {
            if (state != SimulationState.PAUSED) {
                return;
            }
            state = SimulationState.RUNNING;
            stateLock.notifyAll();
        }
    }

    public void stop() {
        List<Thread> threadsToJoin;
        synchronized (stateLock) {
            if (state == SimulationState.STOPPED) {
                return;
            }
            state = SimulationState.STOPPED;
            stateLock.notifyAll();
            threadsToJoin = new ArrayList<>(workerThreads);
            for (Thread thread : threadsToJoin) {
                thread.interrupt();
            }
        }

        for (Thread thread : threadsToJoin) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized (stateLock) {
            workerThreads.clear();
        }
    }

    public boolean isRunning() {
        return state == SimulationState.RUNNING;
    }

    public boolean isPaused() {
        return state == SimulationState.PAUSED;
    }

    public boolean isStopped() {
        return state == SimulationState.STOPPED;
    }

    public SimulationState getState() {
        return state;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public List<Thread> getWorkerThreads() {
        synchronized (stateLock) {
            return Collections.unmodifiableList(new ArrayList<>(workerThreads));
        }
    }

    private void runWorkerLoop(Runnable task) {
        while (true) {
            synchronized (stateLock) {
                while (state == SimulationState.PAUSED) {
                    try {
                        stateLock.wait();
                    } catch (InterruptedException e) {
                        if (state == SimulationState.STOPPED) {
                            return;
                        }
                    }
                }
                if (state == SimulationState.STOPPED) {
                    return;
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            try {
                if (task != null) {
                    task.run();
                }
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable t) {
                if (Thread.currentThread().isInterrupted() || state == SimulationState.STOPPED) {
                    return;
                }
            }
        }
    }
}
