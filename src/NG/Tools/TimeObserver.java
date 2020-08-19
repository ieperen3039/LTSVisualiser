package NG.Tools;

import NG.DataStructures.Generic.AveragingQueue;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Generic.PairList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 10-5-2019.
 */
public class TimeObserver {
    private static final String NONE = "Other";
    private static final int QUEUE_SIZE = 5;
    private final int queueSize;

    private AveragingQueue loopTimes;
    private Map<String, AveragingQueue> allMeasures;
    private Map<String, Integer> thisLoopMeasures;
    private long thisLoopStart;
    private boolean includeOther;

    private String currentMeasure;
    private long currentStart;

    public TimeObserver() {
        this(QUEUE_SIZE, true);
    }

    public TimeObserver(int queueSize, boolean doIncludeOther) {
        this.allMeasures = new HashMap<>();
        this.thisLoopMeasures = new HashMap<>();
        this.loopTimes = new AveragingQueue(queueSize);
        this.queueSize = queueSize;

        currentMeasure = NONE;
        currentStart = System.nanoTime();
        includeOther = doIncludeOther;
    }

    public void startNewLoop() {
        endTiming();
        float loopDurationNanos = (currentStart - thisLoopStart);
        if (!includeOther) loopDurationNanos -= thisLoopMeasures.remove(NONE);

        loopTimes.add(loopDurationNanos / 1e9f);
        thisLoopStart = currentStart;

        for (String elt : thisLoopMeasures.keySet()) {
            allMeasures.computeIfAbsent(elt, e -> new AveragingQueue(queueSize));
        }

        Iterator<String> iterator = allMeasures.keySet().iterator();
        while (iterator.hasNext()) {
            String elt = iterator.next();
            int eltNanos = thisLoopMeasures.getOrDefault(elt, 0);
            float entry = eltNanos / loopDurationNanos;
            AveragingQueue queue = allMeasures.get(elt);
            queue.add(entry);

            if (queue.average() == 0) iterator.remove();
        }

        thisLoopMeasures.clear();
    }

    public void startTiming(String identifier) {
        endTiming();
        currentMeasure = identifier;
    }

    public void endTiming(String identifier) {
        if (!currentMeasure.equals(identifier)) throw new IllegalStateException("Was timing " + currentMeasure);
        endTiming();
    }

    private void endTiming() {
        long currentTime = System.nanoTime();

        long value = currentTime - currentStart;
        int current = thisLoopMeasures.getOrDefault(currentMeasure, 0);
        if (value + current < Integer.MAX_VALUE) {
            thisLoopMeasures.put(currentMeasure, (int) value + current);

        } else {
            thisLoopMeasures.put(currentMeasure, Integer.MAX_VALUE);
        }

        currentMeasure = NONE;
        currentStart = currentTime;
    }

    public PairList<String, Float> results() {
        PairList<String, Float> pairs = new PairList<>(allMeasures.size());

        for (String elt : allMeasures.keySet()) {
            float average = allMeasures.get(elt).average();
            pairs.add(elt, average);
        }

        pairs.sort((a, b) -> -Float.compare(a.right, b.right));

        return pairs;
    }

    public String resultsTable() {
        StringBuilder builder = new StringBuilder(String.format("Time division averages of %d loops of %1.04f sec:", queueSize, loopTimes
                .average()));
        for (Pair<String, Float> result : results()) {
            builder.append("\n");
            builder.append(String.format("| %-30s | %4.01f%% |", result.left, result.right * 100));
        }
        return builder.toString();
    }
}
