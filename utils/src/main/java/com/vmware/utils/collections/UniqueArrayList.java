package com.vmware.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * ArrayList which only adds value if it is not already in the list.
 */
public class UniqueArrayList<T> extends ArrayList<T> {

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
