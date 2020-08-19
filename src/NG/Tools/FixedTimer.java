package NG.Tools;

import NG.Core.GameTimer;

/**
 * @author Geert van Ieperen created on 13-6-2020.
 */
public class FixedTimer extends GameTimer {
    public final long deltaTime;
    private long currentTime = 0;

    public FixedTimer(float renderDelay, int tps) {
        super(renderDelay);
        updateTimer();
        deltaTime = RESOLUTION / tps;
    }

    public void updateGameTime() {
        currentTime += deltaTime;
        gameTime.update(currentTime);
    }

    public void updateRenderTime() {
        renderTime.update(currentTime - renderDelay);
    }
}
