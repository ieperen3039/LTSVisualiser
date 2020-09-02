package NG.InputHandling.MouseTools;

import NG.Core.Main;
import org.joml.Vector3fc;

/**
 * A mouse tool that implements the standard behaviour of the pointer user input.
 *
 * <dl>
 * <dt>Entities:</dt>
 * <dd>The entity gets selected</dd>
 * <dt>Map:</dt>
 * <dd>If an entity is selected, open an action menu</dd>
 * </dl>
 * @author Geert van Ieperen. Created on 26-11-2018.
 */
public class DefaultMouseTool extends AbstractMouseTool {

    public DefaultMouseTool(Main root) {
        super(root);
    }

    @Override
    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
    }
}
