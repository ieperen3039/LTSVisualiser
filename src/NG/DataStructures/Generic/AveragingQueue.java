package NG.DataStructures.Generic;

import java.io.Serializable;

/**
 * a Collection that accepts floats and can only return the average of the last n entries (in constant time)
 * @author Geert van Ieperen created on 5-2-2018.
 */
public class AveragingQueue implements Serializable {
    private float[] entries;
    private float sum = 0;
    private int head = 0;
    private int capacity;

    public AveragingQueue(int capacity) {
        if (capacity < 1) capacity = 1;
        this.entries = new float[capacity];
        this.capacity = capacity;
    }

    /**
     * add an item to this collection, deleting the last added entry. Runs in constant time
     * @param entry a new float
     */
    public void add(float entry) {
        sum -= entries[head];
        entries[head] = entry;
        sum += entry;
        head = (head + 1) % capacity;
    }

    /**
     * Runs in constant time
     * @return the average of the last {@code capacity} items.
     */
    public float average() {
        return sum / capacity;
    }
}
