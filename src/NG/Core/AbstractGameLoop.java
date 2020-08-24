package NG.Core;

import NG.DataStructures.Generic.AveragingQueue;
import NG.Tools.Logger;
import NG.Tools.Timer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;


/**
 * A general-purpose game loop that allows concurrent control on a process. This object starts in a paused state, and
 * can be started by a call to {@link Thread#start()} followed by a call to {@link #unPause()}.
 * @author Geert van Ieperen recreated on 29-10-2019
 */
public abstract class AbstractGameLoop extends Thread {
    private final Supplier<String> tickCounter;
    private final Supplier<String> possessionCounter;
    private float targetDeltaMillis;
    private CountDownLatch pauseBlock = new CountDownLatch(0);
    private boolean shouldStop = false;
    private boolean isPaused = true;
    private ConcurrentLinkedQueue<Runnable> postLoopActions = new ConcurrentLinkedQueue<>();
    private AveragingQueue avgTPS;
    private AveragingQueue avgPoss;

    /**
     * creates a new, paused gameloop
     * @param name      the name as displayed in {@link #toString()}
     * @param targetTps the target number of executions of {@link #update(float)} per second
     */
    public AbstractGameLoop(String name, int targetTps) {
        super(name);
        if (targetTps == 0) pauseBlock = new CountDownLatch(1);
        this.targetDeltaMillis = 1000f / targetTps;

        avgTPS = new AveragingQueue(targetTps / 2);
        avgPoss = new AveragingQueue(targetTps / 10);

        tickCounter = () -> String.format("%s TPS: %1.01f", name, avgTPS.average());
        possessionCounter = () -> String.format("%s POSS: %3d%%", name, (int) (100 * avgPoss.average()));
    }

    /**
     * invoked (targetTps) times per second
     * @param deltaTime real-time difference since last loop
     */
    protected abstract void update(float deltaTime) throws Exception;

    /**
     * commands the engine to finish the current loop, and then quit
     */
    public void stopLoop() {
        shouldStop = true;
        pauseBlock.countDown();
    }

    /**
     * is automatically called when this gameloop terminates
     */
    public abstract void cleanup();

    /**
     * start the loop, running until {@link #stopLoop()} is called.
     */
    public void run() {
        float deltaTime = 0;

        Logger.printOnline(tickCounter);
        Logger.printOnline(possessionCounter);

        try {
            pauseBlock.await();
            Timer loopTimer = new Timer();
            isPaused = false;

            while (!shouldStop || Thread.interrupted()) {
                // start measuring how long a gameloop takes
                loopTimer.updateLoopTime();

                // do stuff
                update(deltaTime);
                if (Thread.interrupted()) break;

                // do more stuff
                runPostLoopActions();
                if (Thread.interrupted()) break;

                // number of milliseconds remaining in this loop
                float remainingTime = targetDeltaMillis - loopTimer.getMillisSinceLastUpdate();

                // sleep at least 0 milliseconds
                long correctedTime = (long) Math.max(remainingTime, 0);
                Thread.sleep(correctedTime);

                // store the duration and set this as length of next update
                loopTimer.updateLoopTime();
                deltaTime = loopTimer.getElapsedSeconds();

                // update Ticks per Second
                float realTPS = 1000f / loopTimer.getElapsedMillis();
                avgTPS.add(realTPS);
                avgPoss.add((targetDeltaMillis - remainingTime) / targetDeltaMillis);

                // wait if the game is paused
                isPaused = true;
                pauseBlock.await();
                isPaused = false;
            }

        } catch (Exception ex) {
            Logger.ERROR.print(this + " has crashed! " + ex.getClass());
            exceptionHandler(ex);

        } catch (AssertionError ae) {
            Logger.ERROR.print(this + " has triggered an assertion: " + ae.getMessage());
            exceptionHandler(new Exception(ae));

        } finally {
            Logger.removeOnlinePrint(tickCounter);
            Logger.removeOnlinePrint(possessionCounter);
            runPostLoopActions();
            cleanup();
        }

        // terminate engine
        Logger.DEBUG.print(this + " is stopped");
    }

    private void runPostLoopActions() {
        while (!postLoopActions.isEmpty() && !Thread.interrupted()) {
            try {
                postLoopActions.remove().run();

            } catch (Exception ex) {
                Logger.ERROR.print(ex);
            }
        }
    }

    /**
     * is executed after printing the stacktrace
     * @param ex the exception that caused the crash
     */
    protected void exceptionHandler(Exception ex) {
        ex.printStackTrace();
    }

    public void defer(Runnable action) {
        postLoopActions.offer(action);
    }

    @Override
    public String toString() {
        return getName();
    }

    public void unPause() {
        pauseBlock.countDown();
        Logger.DEBUG.print("unpaused " + this);
    }

    public void pause() {
        pauseBlock = new CountDownLatch(1);
        Logger.DEBUG.print("paused " + this);
    }

    /**
     * @return true if this loop is not executing its loop. This method returns false if {@link #pause()} is called, but
     * the loop is still finishing its loop
     * @see #unPause()
     */
    public boolean isPaused() {
        return isPaused && (pauseBlock.getCount() > 0);
    }

    public int getTPS() {
        return (int) (1000f / targetDeltaMillis);
    }

    public void setTPS(int TPS) {
        this.targetDeltaMillis = 1000f / TPS;
    }
}
