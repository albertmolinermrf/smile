/*******************************************************************************
 * Copyright (c) 2010-2019 Haifeng Li
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/

package smile.util;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import smile.sort.QuickSort;

/**
 * Sparse array of double values.
 * @author Haifeng Li
 *
 */
public class SparseArray implements Iterable<SparseArray.Entry>, Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * The entry in a sparse array of double values.
     */
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Format for toString. */
        private static DecimalFormat format = new DecimalFormat("#.######");

        /**
         * The index of entry.
         */
        public int i;
        /**
         * The value of entry.
         */
        public double x;

        /**
         * Constructor.
         * @param i the index of entry.
         * @param x the value of entry.
         */
        public Entry(int i, double x) {
            this.i = i;
            this.x = x;
        }

        @Override
        public String toString() {
            return String.format("%d:%s", i, format.format(x));
        }
    }

    /**
     * Entry as an object has too much overhead and not CPU cache friendly.
     * Use two continuous array lists for index and value correspondingly.
     */
    private IntArrayList index;
    private DoubleArrayList value;
    
    /**
     * Constructor.
     */
    public SparseArray() {
        this(10);
    }

    /**
     * Constructor.
     */
    public SparseArray(List<Entry> entries) {
        index = new IntArrayList(entries.size());
        value = new DoubleArrayList(entries.size());

        for (Entry e : entries) {
            index.add(e.i);
            value.add(e.x);
        }
    }

    /**
     * Constructor.
     */
    public SparseArray(Stream<Entry> stream) {
        this(stream.collect(Collectors.toList()));
    }

    /**
     * Constructor.
     * @param initialCapacity the number of rows in the matrix.
     */
    private SparseArray(int initialCapacity) {
        index = new IntArrayList(initialCapacity);
        value = new DoubleArrayList(initialCapacity);
    }

    @Override
    public String toString() {
        return stream().map(Entry::toString).collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Returns the number of nonzero entries.
     * @return the number of nonzero entries
     */
    public int size() {
        return index.size();
    }

    /**
     * Returns true if the array is empty.
     * @return true if the array is empty.
     */
    public boolean isEmpty() {
        return index.isEmpty();
    }
    
    /**
     * Returns an iterator of nonzero entries.
     * @return an iterator of nonzero entries
     */
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < size();
            }

            @Override
            public Entry next() {
                Entry e = new Entry(index.get(i), value.get(i));
                i += 1;
                return e;
            }
        };
    }

    /** Returns the stream of nonzero entries. */
    public Stream<Entry> stream() {
        return IntStream.range(0, size()).mapToObj(i -> new Entry(index.get(i), value.get(i)));
    }

    /** Sorts the array elements such that the indices are in ascending order. */
    public void sort() {
        QuickSort.sort(index.data, value.data, size());
    }

    /**
     * Returns the value of i-th entry.
     * @param i the index of entry.
     * @return the value of entry, 0.0 if the index doesn't exist in the array.
     */
    public double get(final int i) {
        int length = size();
        for (int k = 0; k < length; k++) {
            if (index.get(k) == i) return value.get(k);
        }
        return 0.0;
    }

    /**
     * Sets or add an entry.
     * @param i the index of entry.
     * @param x the value of entry.
     * @return true if a new entry added, false if an existing entry updated.
     */
    public boolean set(int i, double x) {
        if (x == 0.0) {
            remove(i);
            return false;
        }

        int length = size();
        for (int k = 0; k < length; k++) {
            if (index.get(k) == i) {
                value.set(k, x);
                return false;
            }
        }

        index.add(i);
        value.add(x);
        return true;
    }

    /**
     * Append an entry to the array, optimizing for the case where the
     * index is greater than all existing indices in the array.
     * @param i the index of entry.
     * @param x the value of entry.
     */
    public void append(int i, double x) {
        if (x != 0.0) {
            index.add(i);
            value.add(x);
        }
    }
    
    /**
     * Removes an entry.
     * @param i the index of entry.
     */
    public void remove(int i) {
        int length = size();
        for (int k = 0; k < length; k++) {
            if (index.get(k) == i) {
                index.remove(k);
                value.remove(k);
                return;
            }
        }
    }
}