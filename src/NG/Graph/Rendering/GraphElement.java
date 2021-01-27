package NG.Graph.Rendering;

import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;

import java.util.Comparator;

import static NG.Graph.Rendering.GraphElement.Priority.BASE;

/**
 * @author Geert van Ieperen created on 1-9-2020.
 */
public abstract class GraphElement {
    protected final PairList<Priority, Color4f> colors = new PairList<>();
    public Color4f border = Color4f.BLACK;

    public enum Priority {
        MAXIMUM, HOVER, USER_COLOR, PATH, IGNORE, EXTERNAL, ACTION_MARKING, MU_FORMULA, FIXATE_POSITION, COMPARE, BASE
    }

    public Color4f getColor() {
        assert !colors.isEmpty();
        return colors.right(0);
    }

    public void addColor(Color4f color, Priority priority) {
        assert color != null;
        int index = colors.indexOfLeft(priority);

        if (index < 0) {
            colors.add(priority, color);
            colors.sort(Comparator.comparingInt(pair -> pair.left.ordinal()));

        } else {
            colors.set(index, priority, color);
        }
    }

    public abstract String identifier();

    public void resetColor(Priority priority) {
        if (priority == BASE) throw new IllegalArgumentException("Cannot remove priority " + BASE);
        int index = colors.indexOfLeft(priority);
        if (index == -1) return;
        colors.remove(index);
    }
}
