package com.vmware.util;

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

    public static String[] join(String[] cAliases, String[] jAliases) {
        int length = cAliases.length + jAliases.length;
        String[] result = new String[length];
        System.arraycopy(cAliases, 0, result, 0, cAliases.length);
        System.arraycopy(jAliases, 0, result, cAliases.length, jAliases.length);
        return result;
    }
}
