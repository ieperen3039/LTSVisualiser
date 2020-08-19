package NG.Core;

/**
 * A class of which a game usually only needs one of. The constructor of these methods should not accept any other
 * ToolElements. Initialisation of objects should be executed in the {@link #init(Root)} method.
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public interface ToolElement {
    /**
     * Initialize the state of this object. You should not assume anything about the order of how other {@code
     * GameAspect}'s {@code init} methods have been called.
     */
    void init(Root root) throws Exception;

    /**
     * destroy any resources used by this object. The effects of this method should be invertible with the {@link
     * #init(Root)} method
     */
    void cleanup();
}
