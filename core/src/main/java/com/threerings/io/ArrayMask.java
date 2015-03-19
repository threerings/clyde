//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.io;

import java.io.IOException;

/**
 * Used to keep track of which entries in an array are null and which are
 * not. <em>Note:</em> only arrays up to 262,144 elements in length can be
 * handled by this class.
 */
public class ArrayMask
{
    /**
     * Creates an array mask suitable for unserializing.
     */
    public ArrayMask ()
    {
    }

    /**
     * Creates an array mask for an array of the specified length.
     */
    public ArrayMask (int length)
    {
        int mlength = length/8;
        if (length % 8 != 0) {
            mlength++;
        }
        _mask = new byte[mlength];
    }

    /**
     * Sets the bit indicating that the specified array index is non-null.
     */
    public void set (int index)
    {
        _mask[index/8] |= (1 << (index%8));
    }

    /**
     * Returns true if the specified array index should be non-null.
     */
    public boolean isSet (int index)
    {
        return (_mask[index/8] & (1 << (index%8))) != 0;
    }

    /**
     * Writes this mask to the specified output stream.
     */
    public void writeTo (ObjectOutputStream out)
        throws IOException
    {
        out.writeShort(_mask.length);
        out.write(_mask);
    }

    /**
     * Reads this mask from the specified input stream.
     */
    public void readFrom (ObjectInputStream in)
        throws IOException
    {
        int length = in.readShort();
        _mask = new byte[length];
        in.read(_mask);
    }

    /** A byte array with bits for every entry in the source array. */
    protected byte[] _mask;
}
