package NG.Core;

import NG.DataStructures.Tracked.TrackedObject;

/**
 * A combination of a gameloop timer and a render timer. The timers are updated on calls to {@link #updateGameTime()}
 * and {@link #updateRenderTime()}
 * <p>
 * In contrary of what you may think, a float is only accurate up to 2^24 = 16 777 216 integer values. This means that
 * using a float will only have sub-millisecond precision for 4.6 hours. using a double for timestamp gives us 2^53
 * seconds = 9.00719925 * 10^15 values, which gives sub-millisecond precision for 285 616 years.
 */
@SuppressWarnings("WeakerAccess")
public class GameTimer {
    /** multiplication factor to multiply system time units to game-seconds */
    protected static final double SYSTEM_TO_SECONDS = 1e-9;
    protected static final int RESOLUTION = 10_000; // 1/10th millisecond accuracy
    protected static final double SYSTEM_TO_RESOLUTION = RESOLUTION * SYSTEM_TO_SECONDS;
    protected static final double RESOLUTION_TO_SECONDS = 1.0 / RESOLUTION;
    protected final TrackedObject<Long> gameTime;
    protected final TrackedObject<Long> renderTime;
    protected final long renderDelay;
    /** timer ticks since creating this gametimer */
    protected long currentInternalTime = 0;
    protected boolean isPaused = false;
    /** last record of system time */
    private long lastSystemNanos;

    public GameTimer(float renderDelay) {
        this(0f, renderDelay);
    }

    public GameTimer(double startTime, float renderDelay) {
        this.currentInternalTime = (long) (RESOLUTION * startTime);
        this.gameTime = new TrackedObject<>(((long) (startTime * RESOLUTION)));
        this.renderDelay = (long) (RESOLUTION * renderDelay);
        this.renderTime = new TrackedObject<>(((long) ((startTime - renderDelay) * RESOLUTION)));
        this.lastSystemNanos = System.nanoTime();
    }

    public void updateGameTime() {
        updateTimer();
        gameTime.update(currentInternalTime);
    }

    public void updateRenderTime() {
        updateTimer();
        renderTime.update(currentInternalTime - renderDelay);
    }

    public double getGameTime() {
        return gameTime.current() * RESOLUTION_TO_SECONDS;
    }

    public double getGameTimeDifference() {
        return (gameTime.current() - gameTime.previous()) * RESOLUTION_TO_SECONDS;
    }

    public double getRenderTime() {
        return renderTime.current() * RESOLUTION_TO_SECONDS;
    }

    public double getRenderTimeDifference() {
        return (renderTime.current() - renderTime.previous()) * RESOLUTION_TO_SECONDS;
    }

    /** may be called anytime */
    protected void updateTimer() {
        long currentNanos = System.nanoTime();
        long deltaTime = (long) ((currentNanos - lastSystemNanos) * SYSTEM_TO_RESOLUTION);
        lastSystemNanos = currentNanos;

        if (!isPaused) currentInternalTime += deltaTime;
    }

    /** stops the in-game time */
    public void pause() {
        updateTimer();
        isPaused = true;
    }

    /** lets the in-game time proceed, without jumping */
    public void unPause() {
        updateTimer();
        isPaused = false;
    }

    /**
     * @param offset the ingame time is offset by the given time
     */
    public void addOffset(double offset) {
        currentInternalTime += offset * RESOLUTION;
    }

    /** sets the ingame time to the given time */
    public void set(double time) {
        updateTimer();
        currentInternalTime = (long) (time * RESOLUTION);

        updateGameTime();
        updateRenderTime();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " @" + (double) currentInternalTime / RESOLUTION + (isPaused ? "(paused)" : "");
    }

    public boolean isPaused() {
        return isPaused;
    }

    public static GameTimer create(float fps, float tps) {
        float tickDuration = (1f / tps);
        float frameDuration = (1f / fps);
        return new GameTimer(tickDuration + frameDuration);
    }
}
