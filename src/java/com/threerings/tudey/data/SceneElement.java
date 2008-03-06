//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.export.Exportable;
import com.threerings.util.DeepUtil;

/**
 * Superclass for objects stored in the scene.
 */
public abstract class SceneElement extends SimpleStreamableObject
    implements Exportable, Cloneable
{
    @Override // documentation inherited
    public Object clone ()
    {
        return DeepUtil.copy(this);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return DeepUtil.equals(this, other);
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return DeepUtil.hashCode(this);
    }
}
