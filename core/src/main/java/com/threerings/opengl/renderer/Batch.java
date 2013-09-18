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

package com.threerings.opengl.renderer;

/**
 * A geometry batch that can be queued for rendering.
 */
public abstract class Batch
    implements Cloneable
{
    /**
     * Compares two packed state keys.
     */
    public static int compareKeys (int[] k1, int[] k2)
    {
        int l1 = (k1 == null) ? 0 : k1.length;
        int l2 = (k2 == null) ? 0 : k2.length;
        int v1, v2, comp;
        for (int ii = 0, nn = Math.max(l1, l2); ii < nn; ii++) {
            v1 = (ii < l1) ? k1[ii] : 0;
            v2 = (ii < l2) ? k2[ii] : 0;
            if ((comp = v1 - v2) != 0) {
                return comp;
            }
        }
        return 0;
    }

    /** The batch's eye space z coordinate (negative if in front of the viewer). */
    public float depth;

    /** A comparable representation of the batch's state. */
    public int[] key;

    /**
     * Draws this batch using the specified renderer.
     *
     * @return true if the batch changed the color state, in which case it should be invalidated.
     */
    public abstract boolean draw (Renderer renderer);

    /**
     * Returns the number of primitives in this batch (optional; returns zero by default).
     */
    public int getPrimitiveCount ()
    {
        return 0;
    }

    @Override
    public Batch clone ()
    {
        try {
            Batch cbatch = (Batch)super.clone();
            cbatch.key = (key == null) ? null : key.clone();
            return cbatch;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
