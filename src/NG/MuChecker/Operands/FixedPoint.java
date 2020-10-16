package NG.MuChecker.Operands;

import java.util.ArrayList;

/**
 * @author Geert van Ieperen created on 20-2-2020.
 */
public abstract class FixedPoint implements Formula {
    /** the character of the variable bound by this fixed point */
    public final char character;
    /** the index of this fixed point, starting from right. */
    public final int index;
    /** the next enclosing fixed point */
    public final FixedPoint parent;
    /** the variable representing this fixed point */
    public final FixedPointVariable variable;
    /** the formula contained by this fixed point */
    public Formula right;
    /** open or closed **/
    private boolean open;
    /** List of descendant fixpoints **/
    private ArrayList<FixedPoint> fixedPointsDesc = new ArrayList<>();
    /** List of descendant fixpoint variables **/
    private ArrayList<FixedPointVariable> varDesc = new ArrayList<>();

    public FixedPoint(char character, int index, FixedPoint parent, boolean open) {
        this.character = character;
        this.index = index;
        this.parent = parent;
        this.variable = new FixedPointVariable(this, character);
    }

    public FixedPoint setRight(Formula right) {
        this.right = right;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FixedPoint that = (FixedPoint) o;

        if (character != that.character) return false;
        return right.equals(that.right);
    }

    @Override
    public int hashCode() {
        int result = right.hashCode();
        result = 31 * result + (int) character;
        return result;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public ArrayList<FixedPoint> getFixedPointsDesc() {
        return fixedPointsDesc;
    }

    public void addDescendant(FixedPoint fp) {
        this.fixedPointsDesc.add(fp);
    }

    public void removeDescendent(FixedPoint fp) {
        this.fixedPointsDesc.remove(fp);
    }

    public void resetFixedPoint() {
        this.fixedPointsDesc.clear();
    }

    public ArrayList<FixedPointVariable> getVarDesc() {
        return varDesc;
    }

    public void addVar(FixedPointVariable var) {
        this.varDesc.add(var);
    }

    public void removeVar(FixedPointVariable var) {
        this.varDesc.remove(var);
    }

    public void resetVar() {
        this.varDesc.clear();
    }
}
