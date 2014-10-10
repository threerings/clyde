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

import java.lang.ref.WeakReference;

import java.util.Iterator;
import java.util.LinkedHashSet;

import static com.threerings.ClydeLog.log;

/**
 * Much like WeakObserverList, but provides faster addition and removal in exchange for using
 * additional memory and generating additional garbage (the iterator) on notification.
 */
public class WeakObserverSet<T>
{
    /**
     * Instances of this interface are used to apply methods to all observers in a set.
     */
    public static interface ObserverOp<T>
    {
        /**
         * Called once for each observer in the set.
         *
         * @return true if the observer should remain in the set, false if it should be removed in
         * response to this application.
         */
        public boolean apply (T observer);
    }

    /**
     * A convenience method for creating an observer set that avoids duplicating the type
     * parameter on the right hand side.
     */
    public static <T> WeakObserverSet<T> newSet ()
    {
        return new WeakObserverSet<T>();
    }

    /**
     * Adds an observer to the set.
     */
    public boolean add (T observer)
    {
        if (_set.add(new ObserverRef<T>(observer))) {
            _dirty = true;
            return true;
        }
        log.warning("Observer attempted to observe set it's already observing!",
            "observer", observer, new Exception());
        return false;
    }

    /**
     * Removes an observer from the set.
     */
    public boolean remove (T observer)
    {
        boolean removed = _set.remove(new ObserverRef<T>(observer));
        if (removed) {
            _dirty = true;
        }
        return removed;
    }

    /**
     * Checks whether the set is empty.
     */
    public boolean isEmpty ()
    {
        return _set.isEmpty();
    }

    /**
     * Applies the specified operation to all observers in the set.
     */
    public void apply (ObserverOp<T> obop)
    {
        int ocount = _set.size();
        if (ocount == 0) {
            return;
        }
        if (_dirty) {
            if (_snap == null || _snap.length < ocount || _snap.length > (ocount << 3)) {
                @SuppressWarnings("unchecked") ObserverRef<T>[] snap =
                    (ObserverRef<T>[])new ObserverRef<?>[ocount];
                _snap = snap;
            }
            int idx = 0;
            for (Iterator<ObserverRef<T>> it = _set.iterator(); it.hasNext(); ) {
                ObserverRef<T> ref = it.next();
                if (ref.get() != null) {
                    _snap[idx++] = ref;
                } else {
                    it.remove();
                }
            }
            ocount = idx;
            _dirty = false;
        }
        for (int ii = 0; ii < ocount; ii++) {
            T observer = _snap[ii].get();
            if (observer != null) {
                if (!checkedApply(obop, observer)) {
                    remove(observer);
                }
            } else {
                _dirty = true; // remove on next application
            }
        }
    }

    @Override
    public String toString ()
    {
        return _set.toString();
    }

    /**
     * Applies the operation to the observer, catching and logging any exceptions thrown in the
     * process.
     */
    protected static <T> boolean checkedApply (ObserverOp<T> obop, T obs)
    {
        try {
            return obop.apply(obs);
        } catch (Throwable thrown) {
            log.warning("ObserverOp choked during notification", "op", obop, "obs", obs, thrown);
            // if they booched it, definitely don't remove them
            return true;
        }
    }

    /**
     * Represents a reference to an observer.
     */
    protected static class ObserverRef<T> extends WeakReference<T>
    {
        /**
         * Creates a new reference.
         */
        public ObserverRef (T referent)
        {
            super(referent);
        }

        @Override
        public int hashCode ()
        {
            return System.identityHashCode(get());
        }

        @Override
        public boolean equals (Object other)
        {
            return get() == ((ObserverRef<?>)other).get();
        }

        @Override
        public String toString ()
        {
            return String.valueOf(get());
        }
    }

    /** The contained set. */
    protected LinkedHashSet<ObserverRef<T>> _set = new LinkedHashSet<ObserverRef<T>>();

    /** A snapshot of the set. */
    protected ObserverRef<T>[] _snap;

    /** Set when we need to update the snapshot. */
    protected boolean _dirty = true;
}
