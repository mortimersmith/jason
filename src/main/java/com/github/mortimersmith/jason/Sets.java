package com.github.mortimersmith.jason;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Sets
{
    @SuppressWarnings("unchecked")
    public static <T> Set<T> of(T... ts)
    {
        HashSet<T> s = new HashSet<>();
        s.addAll(Arrays.asList(ts));
        return Collections.unmodifiableSet(s);
    }
}
