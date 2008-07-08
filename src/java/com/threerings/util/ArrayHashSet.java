//
// $Id$

package com.threerings.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.samskivert.util.Collections;

/**
 * A {@link HashSet} that contains an {@link ArrayList} in order to allow iterating over
 * the elements of the set (in insertion order) without creating an {@link Iterator} object.
 */
public class ArrayHashSet<E> extends HashSet<E>
{
    /**
     * Returns the element at the specified index.
     */
    public E get (int index)
    {
        return _list.get(index);
    }

    @Override // documentation inherited
    public boolean add (E element)
    {
        return super.add(element) ? _list.add(element) : false;
    }

    @Override // documentation inherited
    public boolean remove (Object element)
    {
        return super.remove(element) ? _list.remove(element) : false;
    }

    @Override // documentation inherited
    public void clear ()
    {
        super.clear();
        _list.clear();
    }

    @Override // documentation inherited
    public Iterator<E> iterator ()
    {
        return Collections.getUnmodifiableIterator(super.iterator());
    }

    @Override // documentation inherited
    public Object clone ()
    {
        @SuppressWarnings("unchecked") ArrayHashSet<E> cset = (ArrayHashSet<E>)super.clone();
        @SuppressWarnings("unchecked") ArrayList<E> clist = (ArrayList<E>)_list.clone();
        cset._list = clist;
        return cset;
    }

    /** The list that mirrors the set contents. */
    protected ArrayList<E> _list = new ArrayList<E>();
}
