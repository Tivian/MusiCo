package eu.tivian.musico.utility;

import android.os.Handler;

/**
 * Task which should run periodically.
 */
public class PeriodicTask implements Runnable {
    /**
     * Interval in the milliseconds of the task.
     */
    private final long interval;

    /**
     * Task to be performed.
     */
    private final Runnable task;

    /**
     * Handler for the task.
     */
    private final Handler handler;

    /**
     * Creates the periodic task without starting it.
     *
     * @param task task to be performed.
     * @param interval interval in milliseconds.
     */
    public PeriodicTask(Runnable task, long interval) {
        this.handler = new Handler();
        this.interval = interval;
        this.task = task;
    }

    /**
     * Starts the periodic task.
     */
    public void start() {
        this.run();
    }

    /**
     * Stops the execution of periodic task.
     */
    public void stop() {
        handler.removeCallbacks(this);
    }

    /**
     * Handles the periodic task execution.
     */
    @Override
    public void run() {
        try {
            task.run();
        } finally {
            handler.postDelayed(this, interval);
        }
    }
}
