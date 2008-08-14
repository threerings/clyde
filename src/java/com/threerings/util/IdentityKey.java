//
// $Id$

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

    @Override // documentation inherited
    public int hashCode ()
    {
        int hash = 1;
        for (Object element : _elements) {
            hash = 31*hash + System.identityHashCode(element);
        }
        return hash;
    }

    @Override // documentation inherited
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
