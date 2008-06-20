//
// $Id$

package com.threerings.config;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.threerings.util.DeepUtil;

/**
 * Stores arguments, extending {@link TreeMap} to implement {@link #hashCode} and {@link #equals}
 * using {@link Arrays#deepHashCode} and {@link Arrays#deepEquals} to provide expected behavior
 * when using arrays as values.  Also implements {@link #clone} to deep-copy values.
 */
public class ArgumentMap extends TreeMap<String, Object>
{
    @Override // documentation inherited
    public Object clone ()
    {
        ArgumentMap cmap = (ArgumentMap)super.clone();
        for (Map.Entry<String, Object> entry : cmap.entrySet()) {
            entry.setValue(DeepUtil.copy(entry.getValue(), null));
        }
        cmap._a1 = new Object[1];
        cmap._a2 = new Object[1];
        return cmap;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ArgumentMap)) {
            return false;
        }
        ArgumentMap omap = (ArgumentMap)other;
        if (size() != omap.size()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : entrySet()) {
            String key = entry.getKey();
            if (!omap.containsKey(key)) {
                return false;
            }
            _a1[0] = entry.getValue();
            _a2[0] = omap.get(key);
            if (!Arrays.deepEquals(_a1, _a2)) {
                return false;
            }
        }
        return true;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        int hash = 0;
        for (Map.Entry<String, Object> entry : entrySet()) {
            _a1[0] = entry.getValue();
            hash += entry.getKey().hashCode() ^ Arrays.deepHashCode(_a1);
        }
        return hash;
    }

    /** Used for {@link Arrays#deepHashCode} and {@link Arrays#deepEquals}. */
    protected Object[] _a1 = new Object[1], _a2 = new Object[1];
}
