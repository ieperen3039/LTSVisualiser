package NG.Graph;

import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 16-10-2020.
 */
public class Transition extends GraphElement {
    public final State from;
    public final State to;
    public final Vector3fc fromPosition;
    public final Vector3fc toPosition;
    public final Vector3f handlePos;
    public String label;

    public Transition(State from, State to, String label) {
        this.from = from;
        this.to = to;
        this.fromPosition = from.position;
        this.toPosition = to.position;
        this.handlePos = new Vector3f(fromPosition).lerp(toPosition, 0.5f);
        this.label = label;
        colors.add(Priority.BASE, EdgeMesh.BASE_COLOR);
    }

    @Override
    public String toString() {
        return String.format("(%s -%s-> %s)", from.identifier(), label, to.identifier());
    }

    @Override
    public String identifier() {
        return "Action " + label;
    }
}
