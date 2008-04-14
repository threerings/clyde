//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

/**
 * Tests {@link SimpleSpace}.
 */
public class SimpleSpaceTest extends AbstractSpaceTest
{
    public SimpleSpaceTest (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    protected Space createSpace ()
    {
        return new SimpleSpace ();
    }
}
