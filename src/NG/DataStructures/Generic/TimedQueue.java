package NG.DataStructures.Generic;

import java.util.List;

/**
 * A queue that allows a producer to queue timed objects (e.g. positions) while a consumer takes the next item from a
 * specified timestamp onwards.
 * @author Geert van Ieperen created on 13-12-2017.
 */
public interface TimedQueue<T> {

    /**
     * add an element to be accessible in the interval [the timeStamp of the previous item, the given timestamp] Items
     * added to the queue with a timestamp less than the previous addition will cause the previous value to be removed
     * @param element   the element that will be returned upon calling {@link #getNext(double)}
     * @param timeStamp the timestamp in seconds from where this element becomes active
     */
    void add(T element, double timeStamp);

    /**
     * @param timeStamp the timestamp to consider
     * @return the earliest element with a timestamp strictly after the given timestamp.
     */
    T getNext(double timeStamp);

    /**
     * @param timeStamp the timestamp to consider
     * @return the latest element with a timestamp strictly before the given timestamp.
     */
    T getPrevious(double timeStamp);

    /**
     * @param timeStamp the timestamp to consider
     * @return the lowest timestamp equal to or later than the given timestamp with an element, or {@link
     * Double#POSITIVE_INFINITY} if no such element exists.
     */
    double timeOfNext(double timeStamp);

    /**
     * @param timeStamp the timestamp to consider
     * @return the highest timestamp strictly less than the given timestamp, or {@link Double#NEGATIVE_INFINITY} if no
     * such element exists.
     */
    double timeOfPrevious(double timeStamp);

    default double timeUntilNext(double timeStamp) {
        return timeOfNext(timeStamp) - timeStamp;
    }

    default double timeSincePrevious(double timeStamp) {
        return timeStamp - timeOfPrevious(timeStamp);
    }

    /**
     * upon returning, nextTimeStamp > timeStamp or there exist no item with such timestamp.
     * @param timeStamp the time until where the state of the queue should be updated.
     */
    void removeUntil(double timeStamp);

    /**
     * @param start a start time
     * @param end   an end time
     * @return all elements that end later than {@code start} and start earlier or at {@code end}, in order
     */
    List<T> getRange(double start, double end);
}
