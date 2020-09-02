package NG.Graph;

import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;

import java.util.Comparator;

/**
 * @author Geert van Ieperen created on 1-9-2020.
 */
public abstract class GraphElement {
    protected final PairList<Priority, Color4f> colors = new PairList<>();

    public enum Priority {
        MAXIMUM, HOVER, ATTRIBUTE, FIXATE_POSITION, BASE
    }

    public Color4f getColor() {
        return colors.right(0);
    }

    public void addColor(Color4f color, Priority priority) {
        int index = colors.indexOfLeft(priority);

        if (index < 0) {
            colors.add(priority, color);
            colors.sort(Comparator.comparingInt(pair -> pair.left.ordinal()));

        } else {
            colors.set(index, priority, color);
        }
    }

    public void resetColor(Priority priority) {
        int index = colors.indexOfLeft(priority);
        assert index != -1 : priority;
        colors.remove(index);
    }
}
