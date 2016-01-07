package com.vmware.utils.collections;

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
}
