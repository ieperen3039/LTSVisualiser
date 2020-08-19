package NG.GUIMenu.Components;

import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * a row/column of toggle buttons, of which always exactly one is active
 * @author Geert van Ieperen created on 21-2-2020.
 */
public class SExclusiveButtonRow extends SDecorator {
    private List<Consumer<Integer>> selectionListeners;

    private SToggleButton selected;
    private int selectedIndex;
    private List<Consumer<Integer>> deselectionListeners;

    /**
     * creates a row of buttons that display the given texts. There will be {@code elements.length} buttons, regardless
     * of the contents of elements. If {@code elements} contains null elements, the buttons will have no text.
     * @param horizontal if true, buttons are positioned in a row. if false, buttons are stacked in a column
     * @param elements   the names of the buttons. There will be as much buttons as names.
     */
    public SExclusiveButtonRow(boolean horizontal, String[] elements) {
        super(
                (horizontal ? new SPanel(elements.length, 1) : new SPanel(1, elements.length))
        );
        selectionListeners = new ArrayList<>();
        deselectionListeners = new ArrayList<>();

        Vector2i pos = new Vector2i(0, 0);
        Vector2ic delta = horizontal ? new Vector2i(1, 0) : new Vector2i(0, 1);

        for (int i = 0; i < elements.length; i++) {
            String label = elements[i] == null ? "" : elements[i];
            SToggleButton button = new SToggleButton(label) {
                @Override // override to ignore deselection by click
                public void onClick(int button, int xSc, int ySc) {
                    if (!isActive()) setActive(true);
                }
            };
            int index = i;

            button.addStateChangeListener((s) -> select(s, button, index));

            add(button, pos);
            pos.add(delta);

            if (i == 0) selected = button;
        }
    }

    /**
     * Adds a listener that is activated when the selection is changed. The listener receives the index of the newly
     * selected button, as given in the constructor. The index of the previous selection is passed immediately, as if
     * this listener receives the previous input (or button 0 if no input was provided).
     * @param listener receives the index of the selected button.
     * @return this
     */
    public SExclusiveButtonRow addSelectionListener(Consumer<Integer> listener) {
        selectionListeners.add(listener);
        listener.accept(selectedIndex);
        return this;
    }

    /**
     * Adds a listener that is activated when the selection is changed. The listener receives the index of the button
     * that is deactivated, as given in the constructor.
     * @param listener receives the index of the deselected button.
     * @return this
     */
    public SExclusiveButtonRow addDeselectionListener(Consumer<Integer> listener) {
        deselectionListeners.add(listener);
        return this;
    }

    private void select(boolean toActive, SToggleButton elt, int index) {
        if (toActive) {
            SToggleButton previousButton = selected;
            selected = elt;
            selectedIndex = index;

            previousButton.setActive(false); // activates the state change listener of that button
            selectionListeners.forEach(c -> c.accept(index));

        } else {
            deselectionListeners.forEach(c -> c.accept(index));
        }

    }
}
