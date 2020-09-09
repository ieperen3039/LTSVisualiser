package NG.DataStructures.Generic;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Optimized version of {@code List<Pair<L, R>>} instances. This list does not permit null values
 * @author Geert van Ieperen created on 29-6-2018.
 */
public class PairList<L, R> extends AbstractList<Pair<L, R>> {
    public static final PairList EMPTY_LIST = new PairList<>(
            Collections.emptyList(), Collections.emptyList()
    );

    private final List<L> leftList;
    private final List<R> rightList;

    public PairList(int expectedSize) {
        leftList = new ArrayList<>(expectedSize);
        rightList = new ArrayList<>(expectedSize);
    }

    public PairList() {
        leftList = new ArrayList<>();
        rightList = new ArrayList<>();
    }

    /**
     * creates a pairlist by copying the pairs of the given list.
     * @param pairs a list of pairs.
     */
    public PairList(List<Pair<L, R>> pairs) {
        this(pairs.size());
        addAll(pairs);
    }

    public PairList(Map<L, R> pairs) {
        leftList = new ArrayList<>(pairs.size());
        rightList = new ArrayList<>(pairs.size());
        for (L left : pairs.keySet()) {
            add(left, pairs.get(left));
        }
    }

    /**
     * creates a pairlist whose elements are represented by the given lists. Changes in this object write through the
     * given lists, and changes in the given lists write through this pairlist. Unbalance between the size of the two
     * lists due to external interference is not checked.
     * @param leftElements  elements that are used as left elements
     * @param rightElements elements that are used as right elements
     */
    public PairList(List<L> leftElements, List<R> rightElements) {
        if (leftElements.size() != rightElements.size()) {
            throw new IllegalArgumentException(
                    "Given arrays are of unequal size: " + leftElements.size() + ", " + rightElements.size()
            );
        }

        this.leftList = leftElements;
        this.rightList = rightElements;
    }

    /** adds the given pair to the end of the list */
    public boolean add(Pair<L, R> p) {
        return add(p.left, p.right);
    }

    /** adds a pair to the end of the list */
    public boolean add(L left, R right) {
        leftList.add(left);
        rightList.add(right);
        return true;
    }

    @Override
    public void add(int index, Pair<L, R> element) {
        add(index, element.left, element.right);
    }

    public void add(int index, L left, R right) {
        // ensure index >= size()
        for (int i = size(); i <= index; i++) {
            leftList.add(null);
            rightList.add(null);
        }

        leftList.add(index, left);
        rightList.add(index, right);
    }

    public void addAll(PairList<L, R> source) {
        leftList.addAll(source.leftList);
        rightList.addAll(source.rightList);
    }

    @Override
    public boolean addAll(Collection<? extends Pair<L, R>> c) {
        c.forEach(this::add);
        return true;
    }

    /** sets the element of the given position to the given pair */
    public Pair<L, R> set(int index, Pair<L, R> p) {
        Pair<L, R> old = get(index);
        set(index, p.left, p.right);
        return old;
    }

    public void removeVoid(int index) {
        leftList.remove(index);
        rightList.remove(index);
    }


    @Override
    public Pair<L, R> remove(int index) {
        return new Pair<>(
                leftList.remove(index),
                rightList.remove(index)
        );
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            return -1;
        } else {
            if (o instanceof Pair) {
                Pair pair = (Pair) o;
                try {
                    Iterator<L> lit = leftList.iterator();
                    Iterator<R> rit = rightList.iterator();
                    for (int i = 0; i < size(); i++) {
                        if ((lit.next() == pair.left) && (rit.next() == pair.right)) {
                            return i;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException | NoSuchElementException ex) {
                    throw new ConcurrentModificationException();
                }
            }

            return -1;
        }
    }

    /**
     * Returns the index of the first occurrence of the specified element in the left side of this list, or -1 if the
     * left side of this list does not contain the element.
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in this list, or -1 if this list does not
     * contain the element
     */
    public int indexOfLeft(L o) {
        return leftList.indexOf(o);
    }


    /**
     * Returns the index of the first occurrence of the specified element in the right side of this list, or -1 if the
     * right side of this list does not contain the element.
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in this list, or -1 if this list does not
     * contain the element
     */
    public int indexOfRight(R o) {
        return rightList.indexOf(o);
    }

    /** sets the element of the given position to the given pair */
    public void set(int index, L left, R right) {
        leftList.set(index, left);
        rightList.set(index, right);
    }

    /** @return the left value of the pair at the given nextIndex */
    public L left(int index) {
        return leftList.get(index);
    }

    /** @return the right value of the pair at the given nextIndex */
    public R right(int index) {
        return rightList.get(index);
    }

    public Pair<L, R> get(int index) {
        return new Pair<>(leftList.get(index), rightList.get(index));
    }

    @Override
    public int size() {
        return leftList.size();
    }

    @Override
    public boolean isEmpty() {
        return leftList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        return indexOf(o) != -1;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index == -1) return false;
        leftList.remove(index);
        rightList.remove(index);
        return true;
    }

    @Override
    public void clear() {
        leftList.clear();
        rightList.clear();
    }

    @Override
    public Spliterator<Pair<L, R>> spliterator() {
        return new PairSpliterator(0, size() - 1);
    }

    @Override
    public ListIterator<Pair<L, R>> listIterator(int index) {
        return new PairListIterator(index);
    }

    public void forEach(BiConsumer<L, R> action) {
        for (int i = 0; i < size(); i++) {
            action.accept(leftList.get(i), rightList.get(i));
        }
    }

    public void addAll(List<L> leftNew, List<R> rightNew) {
        if (leftNew.size() != rightNew.size()) {
            throw new IllegalArgumentException(
                    "Given lists are of unequal size: " + leftNew.size() + ", " + rightNew.size()
            );
        }

        leftList.addAll(leftNew);
        rightList.addAll(rightNew);
    }

    public L[] leftToArray(L[] a) {
        return leftList.toArray(a);
    }

    public R[] rightToArray(R[] a) {
        return rightList.toArray(a);
    }

    public static <L, R> PairList<L, R> empty() {
        return (PairList<L, R>) EMPTY_LIST;
    }

    /**
     * allows iteration over this pairList
     */
    private class PairListIterator implements ListIterator<Pair<L, R>> {
        private int nextIndex;
        private int initialSize;

        public PairListIterator(int start) {
            nextIndex = start;
            initialSize = size();
        }

        @Override
        public boolean hasNext() {
            return nextIndex < initialSize;
        }

        @Override
        public Pair<L, R> next() {
            return new Pair<>(leftList.get(nextIndex), rightList.get(nextIndex++));
        }

        @Override
        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        @Override
        public Pair<L, R> previous() {
            nextIndex -= 2;
            return new Pair<>(leftList.get(nextIndex), rightList.get(nextIndex));
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 2;
        }

        @Override
        public void remove() {
            leftList.remove(nextIndex - 1);
            rightList.remove(nextIndex - 1);
            initialSize--;
        }

        @Override
        public void set(Pair<L, R> pair) {
            leftList.set(nextIndex - 1, pair.left);
            rightList.set(nextIndex - 1, pair.right);
        }

        @Override
        public void add(Pair<L, R> pair) {
            PairList.this.add(nextIndex, pair);
        }
    }

    private class PairSpliterator implements Spliterator<Pair<L, R>> {
        int nextInd;
        int maxInd;

        public PairSpliterator(int startInd, int maxInd) {
            nextInd = startInd;
            this.maxInd = maxInd;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Pair<L, R>> action) {
            if (nextInd > maxInd) {
                return false;
            }

            Pair<L, R> p = get(nextInd++);
            action.accept(p);
            return true;
        }

        @Override
        public Spliterator<Pair<L, R>> trySplit() {
            int newMax = (maxInd - nextInd) / 2;
            int newIndex = nextInd;
            nextInd = newMax + 1;
            return new PairSpliterator(newIndex, newMax);
        }

        @Override
        public long estimateSize() {
            return (maxInd - nextInd) + 1;
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | NONNULL | IMMUTABLE;
        }
    }

}
