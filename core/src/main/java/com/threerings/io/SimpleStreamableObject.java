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

import com.samskivert.util.StringUtil;

import com.threerings.util.ActionScript;

/**
 * A simple serializable object implements the {@link Streamable}
 * interface and provides a default {@link Object#toString} implementation which
 * outputs all public members.
 */
public class SimpleStreamableObject implements Streamable
{
    @Override
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[");
        toString(buf);
        return buf.append("]").toString();
    }

    /**
     * Handles the toString-ification of all public members. Derived
     * classes can override and include non-public members if desired.
     */
    @ActionScript(name="toStringBuilder")
    protected void toString (StringBuilder buf)
    {
        StringUtil.fieldsToString(buf, this);
    }
}
