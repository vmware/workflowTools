package com.vmware.utils;

public class ArrayUtils {

    public static <T> boolean contains( final T[] array, final T v ) {
        if (array == null) {
            return false;
        }
        for ( final T e : array )
            if ( e == v || v != null && v.equals( e ) )
                return true;

        return false;
    }
}
