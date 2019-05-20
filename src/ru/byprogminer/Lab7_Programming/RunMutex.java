package ru.byprogminer.Lab7_Programming;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *  currentThreads | stopping | Case
 * ----------------+----------+------
 *       empty     |   false  | Not running
 *     not empty   |   false  | Running
 *     not empty   |   true   | Stopping
 *       empty     |   true   | WUT?
 */
public class RunMutex {

    private static class StateContainer {

        private final Set<Thread> currentThreads = Collections.synchronizedSet(new HashSet<>());
        private volatile boolean stopping = false;

        public synchronized boolean isStopping() {
            return stopping;
        }

        public synchronized void setStopping(boolean stopping) {
            this.stopping = stopping;
        }
    }

    private final StateContainer state = new StateContainer();

    public boolean tryRun() {
        while (!state.currentThreads.isEmpty()) {
            if (!state.isStopping()) {
                return false;
            }
        }

        synchronized (state) {
            state.currentThreads.add(Thread.currentThread());
            state.setStopping(false);
        }

        return true;
    }

    public void shareRun(Thread thread) {
        if (!state.currentThreads.contains(Thread.currentThread())) {
            throw new RuntimeException("share from another thread");
        }

        state.currentThreads.add(thread);
    }

    public boolean isRunning() {
        return state.currentThreads.contains(Thread.currentThread()) && !state.isStopping();
    }

    public void finish() {
        if (state.currentThreads.isEmpty()) {
            state.setStopping(false);
            return;
        }

        if (!state.currentThreads.contains(Thread.currentThread())) {
            throw new RuntimeException("finish from another thread");
        }

        synchronized (state) {
            state.currentThreads.remove(Thread.currentThread());

            if (state.currentThreads.isEmpty()) {
                state.setStopping(false);
            }
        }
    }

    public synchronized void stop() {
        if (state.currentThreads.isEmpty()) {
            state.setStopping(false);
            return;
        }

        state.setStopping(true);
    }

    public Set<Thread> getCurrentThreads() {
        return Collections.unmodifiableSet(state.currentThreads);
    }
}
