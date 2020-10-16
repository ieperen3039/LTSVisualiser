package NG.MuChecker;

import NG.Graph.State;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A set of states, but then more efficient.
 * @author Geert van Ieperen created on 27-2-2020.
 */
public class StateSet extends AbstractSet<State> {
    public final State[] universe;
    public final BitSet mask;

    /**
     * Create a new set of states, containing all elements of the universe
     * @param universe all states
     */
    private StateSet(State[] universe) {
        this.universe = universe;
        this.mask = new BitSet(universe.length);
    }

    /**
     * Copies the given set. The new set has the same universe as this set.
     */
    public StateSet(StateSet other) {
        this.universe = other.universe;
        this.mask = (BitSet) other.mask.clone();
    }

    /** this = this U other */
    public void union(StateSet other) {
        assert Arrays.deepEquals(universe, other.universe);
        mask.or(other.mask);
    }

    /** this = this A other */
    public void intersect(StateSet other) {
        assert Arrays.deepEquals(universe, other.universe);
        mask.and(other.mask);
    }

    /** this = -this */
    public void negate() {
        mask.flip(0, universe.length);
    }

    /** this = this / other */
    public void diff(StateSet other) {
        assert Arrays.deepEquals(universe, other.universe);
        mask.andNot(other.mask);
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof State) {
            State s = (State) o;
            mask.set(s.index, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeIf(Predicate<? super State> filter) {
        int i = mask.nextSetBit(0);
        boolean removed = false;

        while (i >= 0) {
            State s = universe[i];
            if (filter.test(s)) {
                mask.clear(i);
                removed = true;
            }

            i = mask.nextSetBit(i + 1);
        }

        return removed;
    }

    @Override
    public void forEach(Consumer<? super State> action) {
        int i = mask.nextSetBit(0);

        while (i >= 0) {
            State s = universe[i];
            action.accept(s);

            i = mask.nextSetBit(i + 1);
        }
    }

    @Override
    public boolean add(State state) {
        mask.set(state.index);
        return true;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof State) {
            int index = ((State) o).index;
            return mask.get(index);
        }
        return false;
    }

    @Override
    public Iterator<State> iterator() {
        return new Iterator<State>() {
            int i = mask.nextSetBit(0);

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @Override
            public State next() {
                State s = universe[i];
                i = mask.nextSetBit(i + 1);
                return s;
            }
        };
    }

    @Override
    public void clear() {
        mask.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (getClass() != o.getClass()) {
            // set compare
            return super.equals(o);
        }

        StateSet states = (StateSet) o;

        if (!Arrays.equals(universe, states.universe)) return false;
        return mask.equals(states.mask);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(universe);
        result = 31 * result + mask.hashCode();
        return result;
    }

    @Override
    public Stream<State> stream() {
        return mask.stream().mapToObj(i -> universe[i]);
    }

    @Override
    public int size() {
        return mask.cardinality();
    }

    public boolean isSubsetOf(StateSet other) {
        BitSet notInOther = (BitSet) other.mask.clone();
        notInOther.flip(0, notInOther.size());
        return !notInOther.intersects(mask);
    }

    public boolean isSupersetOf(StateSet other) {
        return other.isSubsetOf(this);
    }

    public static StateSet allOf(State[] universe) {
        StateSet s = new StateSet(universe);
        s.negate();
        return s;
    }

    public static StateSet noneOf(State[] universe) {
        return new StateSet(universe);
    }
}
