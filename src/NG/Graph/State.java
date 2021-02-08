package NG.Graph;

import NG.DataStructures.Generic.Color4f;
import NG.Graph.Rendering.GraphElement;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Geert van Ieperen created on 16-10-2020.
 */
public class State extends GraphElement {
    public static final Color4f BASE_COLOR = Color4f.WHITE;

    public final Vector3f position;
    public String label; // the node label, if any
    public final int index;
    public int classIndex;

    public boolean isFixed = false;
    public boolean stayFixed = false;

    private final List<Transition> outgoing = new ArrayList<>();
    private final List<Transition> incoming = new ArrayList<>();

    public State(Vector3fc position, String label, int index, int classIndex) {
        this.position = new Vector3f(position);
        this.label = label;
        this.index = index;
        this.classIndex = classIndex;
        colors.add(Priority.BASE, BASE_COLOR);
    }

    public State(State other, String newLabel) {
        this.position = other.position;
        this.label = newLabel;
        this.index = other.index;
        this.classIndex = other.classIndex;
        colors.add(Priority.BASE, BASE_COLOR);
    }

    public void add(Transition t) {
        if (t.from == this) {
            outgoing.add(t);
        } else {
            incoming.add(t);
        }
    }

    public List<Transition> getOutgoing() {
        return Collections.unmodifiableList(outgoing);
    }

    public List<Transition> getIncoming() {
        return Collections.unmodifiableList(incoming);
    }

    @Override
    public String toString() {
        return "Node " + label + " @" + Vectors.toString(position);
    }

    @Override
    public String identifier() {
        return "Node " + label;
    }
}
