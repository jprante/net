package org.xbib.net.http.util;

import java.util.SortedSet;
import java.util.TreeMap;

public class LimitedStringMap extends TreeMap<String, SortedSet<String>> {

    private final int limit;

    public LimitedStringMap() {
        this(1024);
    }

    public LimitedStringMap(int limit) {
        this.limit = limit;
    }

    @Override
    public SortedSet<String> put(String key, SortedSet<String> value) {
        if (size() < limit) {
            return super.put(key, value);
        }
        return null;
    }
}
