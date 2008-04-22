//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

/**
 * Tests {@link HashSpace}.
 */
public class HashSpaceTest extends AbstractSpaceTest
{
    public HashSpaceTest (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    protected Space createSpace ()
    {
        return new HashSpace(2f);
    }
}
