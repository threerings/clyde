//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

/**
 * Tests {@link Space}s.
 */
public abstract class AbstractSpaceTest extends TestCase
{
    public AbstractSpaceTest (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    public void setUp ()
    {
        _space = createSpace();
        assertNotNull(_space);
    }

    public void testAddRemove ()
    {
        // TODO implement
    }

    public void testContains ()
    {
        // TODO implement
    }

    public void testSize ()
    {
        // TODO implement
    }

    public void testIntersects ()
    {
        // TODO implement
    }

    public void testGetIntersectingShape ()
    {
        // TODO implement
    }

    public void testGetIntersectingSelf ()
    {
        // TODO implement
    }

    /**
     * Creates the space used in this test.
     */
    protected abstract Space createSpace ();

    /** The space to test. */
    protected Space _space;
}
