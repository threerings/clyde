//
// $Id$

package com.threerings.delta;

import java.io.IOException;

import com.threerings.io.ArrayMask;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Extends {@link ArrayMask} to avoid writing the (redundant) mask length when streaming.
 */
public class BareArrayMask extends ArrayMask
{
    /**
     * Creates a new array mask with the supplied length.
     */
    public BareArrayMask (int length)
    {
        super(length);
    }

    @Override // documentation inherited
    public void writeTo (ObjectOutputStream out)
        throws IOException
    {
        out.write(_mask);
    }

    @Override // documentation inherited
    public void readFrom (ObjectInputStream in)
        throws IOException
    {
        in.read(_mask);
    }
}
