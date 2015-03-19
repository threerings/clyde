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

package com.threerings.util;

import java.util.ArrayList;

import java.io.IOException;

import com.samskivert.annotation.ReplacedBy;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * An {@link ArrayList} extension that can be streamed. The contents of the list must also be of
 * streamable types.
 *
 * @see Streamable
 * @param <E> the type of elements stored in this list.
 */
@ReplacedBy("java.util.List")
public class StreamableArrayList<E> extends ArrayList<E>
    implements Streamable
{
    /**
     * Creates an empty StreamableArrayList.
     */
    public static <E> StreamableArrayList<E> newList ()
    {
        return new StreamableArrayList<E>();
    }

    /**
     * Writes our custom streamable fields.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        int ecount = size();
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeObject(get(ii));
        }
    }

    /**
     * Reads our custom streamable fields.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        int ecount = in.readInt();
        ensureCapacity(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            @SuppressWarnings("unchecked") E elem = (E)in.readObject();
            add(elem);
        }
    }
}
