package NG.GUIMenu.LayoutManagers;

import NG.GUIMenu.Components.SComponent;
import org.joml.Vector2ic;

import java.util.*;

/**
 * Uses {@link Vector2ic} as object placement. The vector determines the grid (x, y) position and must be positive
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class GridLayoutManager implements SLayoutManager {
    private final SComponent[][] grid;
    private final int xElts;
    private final int yElts;
    private int nrOfElements;
    /** maximum of the minimum, used as minimum */
    private int[] minRowHeight;
    private int[] minColWidth;
    private int nOfRowGrows = 0;
    private int nOfColGrows = 0;
    private boolean[] rowWantGrow;
    private boolean[] colWantGrow;

    public GridLayoutManager(int xElts, int yElts) {
        assert xElts > 0 && yElts > 0 : "A grid without room for items is not allowed";
        this.grid = new SComponent[xElts][yElts];
        this.xElts = xElts;
        this.yElts = yElts;
        minColWidth = new int[xElts];
        minRowHeight = new int[yElts];
        colWantGrow = new boolean[xElts];
        rowWantGrow = new boolean[yElts];
        nrOfElements = 0;
    }

    /**
     * adds a component to the grid at the specified position
     * @param comp the component to be added
     * @param x    the x grid position
     * @param y    the y grip position
     * @throws IndexOutOfBoundsException if the given x and y fall outside the grid
     */
    public void add(SComponent comp, int x, int y) throws IndexOutOfBoundsException {
        try {
            grid[x][y] = comp;
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(String.format(
                    "Tried adding component on position (%d, %d) while grid is of size [ %d x %d ]",
                    x, y, xElts, yElts
            ));
        }

        nrOfElements++;
    }

    @Override
    public void add(SComponent comp, Object prop) {
        assert comp != null;

        if (prop instanceof Vector2ic) {
            Vector2ic pos = (Vector2ic) prop;
            add(comp, pos.x(), pos.y());

        } else {
            throw new IllegalArgumentException("prop must be an Vector2i instance, but was " + (prop == null ? "null" : prop
                    .getClass()));
        }
    }

    @Override
    public void remove(SComponent comp) {
        for (int x = 0; x < xElts; x++) {
            SComponent[] col = grid[x];
            for (int y = 0; y < yElts; y++) {
                if (comp.equals(col[y])) {
                    grid[x][y] = null;
                    nrOfElements--;
                    return;
                }
            }
        }
    }

    @Override
    public void recalculateProperties() {
        int startChangeNr = nrOfElements;
        nOfRowGrows = 0;
        nOfColGrows = 0;
        rowWantGrow = new boolean[yElts];
        colWantGrow = new boolean[xElts];
        minRowHeight = new int[yElts];
        minColWidth = new int[xElts];


        for (int x = 0; x < xElts; x++) {
            SComponent[] col = grid[x];

            for (int y = 0; y < yElts; y++) {
                SComponent elt = col[y];
                if (elt == null || !elt.isVisible()) continue;

                minRowHeight[y] = Math.max(elt.minHeight(), minRowHeight[y]);
                minColWidth[x] = Math.max(elt.minWidth(), minColWidth[x]);

                if (elt.wantVerticalGrow() && !rowWantGrow[y]) {
                    rowWantGrow[y] = true;
                    nOfRowGrows++;
                }

                if (elt.wantHorizontalGrow() && !colWantGrow[x]) {
                    colWantGrow[x] = true;
                    nOfColGrows++;
                }
            }
        }

        // if something changed while restructuring, try again
        if (startChangeNr != nrOfElements) recalculateProperties();
    }

    @Override
    public Collection<SComponent> getComponents() {
        if (nrOfElements == 0) return Collections.emptySet();
        return new AbstractCollection<SComponent>() {
            @Override
            public Iterator<SComponent> iterator() {
                return new GridIterator();
            }

            @Override
            public int size() {
                return nrOfElements;
            }
        };
    }

    @Override
    public int getMinimumWidth() {
        int min = 0;
        for (int w : minColWidth) {
            min += w;
        }
        return min;
    }

    @Override
    public int getMinimumHeight() {
        int min = 0;
        for (int w : minRowHeight) {
            min += w;
        }
        return min;
    }

    @Override
    public void placeComponents(Vector2ic position, Vector2ic dimensions) {
        int[] colSizes = calculateDimensionSizes(minColWidth, nOfColGrows, colWantGrow, dimensions.x());
        int[] rowSizes = calculateDimensionSizes(minRowHeight, nOfRowGrows, rowWantGrow, dimensions.y());

        int xPos = position.x();
        for (int x = 0; x < xElts; x++) {
            int yPos = position.y();
            for (int y = 0; y < yElts; y++) {
                SComponent elt = grid[x][y];

                if (elt != null) {
                    elt.setPosition(xPos, yPos);
                    elt.setSize(colSizes[x], rowSizes[y]);
                }

                yPos += rowSizes[y];
            }
            xPos += colSizes[x];
        }
    }

    @Override
    public Class<?> getPropertyClass() {
        return Vector2ic.class;
    }

    @Override
    public void clear() {
        for (int x = 0; x < xElts; x++) {
            for (int y = 0; y < yElts; y++) {
                grid[x][y] = null;
            }
        }
    }

    @Override
    public boolean wantHorizontalGrow() {
        for (boolean doWantGrow : colWantGrow) {
            if (doWantGrow) return true;
        }
        return false;
    }

    @Override
    public boolean wantVerticalGrow() {
        for (boolean doWantGrow : rowWantGrow) {
            if (doWantGrow) return true;
        }
        return false;
    }

    /**
     * calculates the positions of the elements in one dimension, relative to (0, 0)
     * @param minSizes  an array with all minimum sizes
     * @param nOfGrows  the number of trues in the {@code wantGrows} table
     * @param wantGrows for each position, whether at least one element wants to grow
     * @param size      the size of the area where the elements can be placed in
     * @return a list of pixel positions that places the components according to the layout
     */
    static int[] calculateDimensionSizes(int[] minSizes, int nOfGrows, boolean[] wantGrows, int size) {
        int nOfElements = minSizes.length;

        int spareSize = size;
        for (int s : minSizes) {
            spareSize -= s;
        }

        int growValue = 0;
        if (nOfGrows > 0 && spareSize > 0) {
            growValue = spareSize / nOfGrows;
        }
        if (nOfGrows == 0 && spareSize > 0) {
            growValue = spareSize / nOfElements;
        }

        int[] widths = new int[nOfElements];
        for (int i = 0; i < nOfElements; i++) {
            int eltWidth = minSizes[i];
            if (wantGrows[i] || nOfGrows == 0) {
                eltWidth += growValue;
            }
            widths[i] = eltWidth;
        }

        return widths;
    }

    private class GridIterator implements Iterator<SComponent> {
        final int yMax;
        final int startNrOfElts = nrOfElements;
        int xCur = -1;
        int yCur = 0;

        GridIterator() {
            yMax = grid[0].length;
            progress();
        }

        @Override
        public boolean hasNext() {
            return yCur < yMax;
        }

        @Override
        public SComponent next() {
            if (startNrOfElts != nrOfElements) {
                throw new ConcurrentModificationException("Grid changed while iterating");
            }

            SComponent retVal = grid[xCur][yCur];

            if (retVal == null) {
                throw new IllegalStateException(String.format("null on (%d, %d) with %d elements", xCur, yCur, nrOfElements));
            }

            progress();
            return retVal;
        }

        private void progress() {
            do {
                xCur++;
                if (xCur == xElts) {
                    xCur = 0;
                    yCur++;
                }
            } while (hasNext() && grid[xCur][yCur] == null);
        }
    }
}
