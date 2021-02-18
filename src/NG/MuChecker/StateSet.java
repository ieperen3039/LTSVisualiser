package NG.MuChecker;

import NG.Graph.State;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A set of states, represented as a bit-vector.
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
        assert Arrays.equals(universe, other.universe);
        mask.or(other.mask);
    }

    /** this = this A other */
    public void intersect(StateSet other) {
        assert Arrays.equals(universe, other.universe);
        mask.and(other.mask);
    }

    /** this = this / other */
    public void diff(StateSet other) {
        assert Arrays.equals(universe, other.universe);
        mask.andNot(other.mask);
    }

    public State any() {
        if (mask.isEmpty()) return null;
        return universe[mask.nextSetBit(0)];
    }

    @Override
    public boolean add(State state) {
        assert Arrays.asList(universe).contains(state);
        mask.set(state.index);
        return true;
    }

    /** this = -this */
    public void negate() {
        mask.flip(0, universe.length);
    }

    @Override
    public boolean addAll(Collection<? extends State> c) {
        for (State s : c) {
            mask.set(s.index);
        }
        return true;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof State) {
            assert Arrays.asList(universe).contains(o);

            int index = ((State) o).index;
            return mask.get(index);
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof State) {
            State s = (State) o;
            mask.clear(s.index);
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

    /**
     * Returns the states contained in this set. This iterator is NOT fail-fast, but is resilient against modification.
     * elements removed during iteration might get returned by the iterator, and added elements might be skipped.
     */
    @Override
    public Iterator<State> iterator() {
        assert mask.length() <= universe.length;
        return new Iterator<State>() {
            int i = mask.nextSetBit(0);
            int last = -1;

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @Override
            public State next() {
                State s = universe[i];
                last = i;
                i = mask.nextSetBit(i + 1);
                return s;
            }

            @Override
            public void remove() {
                mask.clear(last);
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return mask.isEmpty();
    }

    public boolean isSubsetOf(StateSet other) {
        assert Arrays.equals(universe, other.universe);
        BitSet notInOther = (BitSet) other.mask.clone();
        notInOther.flip(0, universe.length);
        return !notInOther.intersects(mask);
    }

    public boolean isSupersetOf(StateSet other) {
        assert Arrays.equals(universe, other.universe);
        return other.isSubsetOf(this);
    }

    /** return a U b */
    public static StateSet unionOf(StateSet a, StateSet b) {
        assert Arrays.equals(a.universe, b.universe);
        StateSet r = new StateSet(a);
        r.union(b);
        return r;
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
        return 31 * universe.length + mask.hashCode();
    }

    @Override
    public Stream<State> stream() {
        return mask.stream().mapToObj(i -> universe[i]);
    }

    @Override
    public int size() {
        return mask.cardinality();
    }

    /** return a A b */
    public static StateSet intersectionOf(StateSet a, StateSet b) {
        assert Arrays.equals(a.universe, b.universe);
        StateSet r = new StateSet(a);
        r.intersect(b);
        return r;
    }

    /** return (a A b) = 0 */
    public static boolean areDistinct(StateSet a, StateSet b) {
        assert Arrays.equals(a.universe, b.universe);
        return !a.mask.intersects(b.mask);
    }

    /** return -other */
    public static StateSet negationOf(StateSet other) {
        StateSet r = new StateSet(other);
        r.negate();
        return r;
    }

    public static StateSet allOf(State[] universe) {
        StateSet s = new StateSet(universe);
        s.negate();
        return s;
    }

    public static StateSet noneOf(State[] universe) {
        return new StateSet(universe);
    }

    public static StateSet fromPredicate(State[] universe, Predicate<State> predicate) {
        StateSet set = noneOf(universe);

        for (int i = 0; i < universe.length; i++) {
            State bState = universe[i];

            if (predicate.test(bState)) {
                set.mask.set(i);
            }
        }

        return set;
    }
}
