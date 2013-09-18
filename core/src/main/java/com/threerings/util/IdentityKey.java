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

/**
 * A hash key based on the identities of an array of elements.
 */
public class IdentityKey
{
    /**
     * Creates a new identity key with the identities of the supplied elements.
     */
    public IdentityKey (Object... elements)
    {
        _elements = elements;
    }

    /**
     * Creates a new identity key with the identities of the supplied elements.
     */
    public IdentityKey (Object firstElement, Object... otherElements)
    {
        _elements = new Object[otherElements.length + 1];
        _elements[0] = firstElement;
        System.arraycopy(otherElements, 0, _elements, 1, otherElements.length);
    }

    @Override
    public int hashCode ()
    {
        int hash = 1;
        for (Object element : _elements) {
            hash = 31*hash + System.identityHashCode(element);
        }
        return hash;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof IdentityKey)) {
            return false;
        }
        IdentityKey okey = (IdentityKey)other;
        if (_elements.length != okey._elements.length) {
            return false;
        }
        for (int ii = 0; ii < _elements.length; ii++) {
            if (_elements[ii] != okey._elements[ii]) {
                return false;
            }
        }
        return true;
    }

    /** The elements to compare. */
    protected Object[] _elements;
}
