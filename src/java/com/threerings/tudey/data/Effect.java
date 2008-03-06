//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.tudey.client.EffectHandler;

/**
 * A general scene effect.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /**
     * Creates a handler for this effect.
     */
    public abstract EffectHandler createHandler ();
}
