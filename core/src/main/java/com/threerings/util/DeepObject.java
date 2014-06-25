//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.util;

import com.samskivert.util.StringUtil;

/**
 * A base class for objects that uses the methods of {@link DeepUtil} to implement {@link #clone},
 * {@link #equals}, and {@link #hashCode} reflectively.
 */
public abstract class DeepObject
    implements Cloneable, Copyable
{
    // documentation inherited from interface Copyable
    public Object copy (Object dest)
    {
        return copy(dest, null);
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest, Object outer)
    {
        return DeepUtil.copy(this, dest, outer);
    }

    @Override
    public DeepObject clone ()
    {
        return (DeepObject) copy(null);
    }

    @Override
    public boolean equals (Object other)
    {
        return DeepUtil.equals(this, other);
    }

    @Override
    public int hashCode ()
    {
        return DeepUtil.hashCode(this);
    }

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
