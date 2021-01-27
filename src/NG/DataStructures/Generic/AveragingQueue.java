package NG.DataStructures.Generic;

import java.io.Serializable;
import java.util.Arrays;

/**
 * a Collection that accepts floats and can only return the average of the last n entries (in constant time)
 * @author Geert van Ieperen created on 5-2-2018.
 */
public class AveragingQueue implements Serializable {
    private final float[] entries;
    private final int capacity;

    private float sum = 0;
    private int head = 0;

    public AveragingQueue(int capacity) {
        if (capacity < 1) capacity = 1;
        this.entries = new float[capacity];
        this.capacity = capacity;
    }

    /**
     * Add an item to this collection, deleting the last added entry. Runs in constant time
     * @param entry a new float
     */
    public void add(float entry) {
        sum -= entries[head];
        entries[head] = entry;
        sum += entry;
        head = (head + 1) % capacity;
    }

    /**
     * Fills the queue with the given entry
     * @param entry
     */
    public void fill(float entry) {
        Arrays.fill(entries, entry);
        sum = capacity * entry;
    }

    /**
     * Runs in constant time
     * @return the average of the last {@code capacity} items.
     */
    public float average() {
        return sum / capacity;
    }

    public void clear() {
        Arrays.fill(entries, 0);
        head = 0;
        sum = 0;
    }
}
