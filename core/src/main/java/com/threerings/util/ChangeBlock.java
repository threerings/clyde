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
 * A simple mechanism for preventing infinite recursion when one change listener responds to an
 * event by making another change, which in turns notifies another (or the same) listener, which
 * makes another change, and so on.  The following example demonstrates the pattern:
 *
 * <p><pre>
 * public void stateChanged (ChangeEvent event)
 * {
 *     if (!_block.enter()) {
 *         return;
 *     }
 *     try {
 *         // do something that may cause another state change
 *     } finally {
 *         _block.leave();
 *     }
 * }
 * </pre>
 */
public class ChangeBlock
{
    /**
     * Attempts to enter the change block.
     *
     * @return true if the change block was successfully entered, false if already in the change
     * block.
     */
    public boolean enter ()
    {
        return _changing ? false : (_changing = true);
    }

    /**
     * Leaves the change block.
     */
    public void leave ()
    {
        _changing = false;
    }

    /** If true, the current thread is in the change block. */
    protected boolean _changing;
}
