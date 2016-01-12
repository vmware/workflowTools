package com.vmware.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * HashSet which overwrites values on add.
 */
public class OverwritableSet<E> extends HashSet<E> {

    public OverwritableSet() {
    }

    public OverwritableSet(Collection<? extends E> c) {
        super(c);
    }

    @Override
    public boolean add(E value) {
        super.remove(value);
        return super.add(value);
    }

    @Override
    public boolean addAll(Collection<? extends E> values) {
        super.removeAll(values);
        return super.addAll(values);
    }

    /**
     * ArrayList which only adds value if it is not already in the list.
     */
    public static class UniqueArrayList<T> extends ArrayList<T> {

        @Override
        public boolean addAll(Collection<? extends T> collectionToAdd) {
            boolean listChanged = false;
            for (T element : collectionToAdd) {
                listChanged = this.add(element) || listChanged;
            }
            return listChanged;
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> collectionToAdd) {
            int indexLocation = index;
            for (T element : collectionToAdd) {
                if (!contains(element)) {
                    super.add(indexLocation++, element);
                }
            }
            return indexLocation > index;
        }

        @Override
        public void add(int index, T element) {
            if (!contains(element)) {
                super.add(index, element);
            }
        }

        @Override
        public boolean add(T element) {
            return !contains(element) && super.add(element);
        }
    }
}
