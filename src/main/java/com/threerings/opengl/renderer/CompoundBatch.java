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

import java.util.ArrayList;
import java.util.Collections;

/**
 * A batch that renders a list of sub-batches in order.
 */
public class CompoundBatch extends Batch
{
    /**
     * Creates an empty compound batch.
     */
    public CompoundBatch ()
    {
    }

    /**
     * Creates a compound batch to render the specified batches.
     */
    public CompoundBatch (Batch... batches)
    {
        Collections.addAll(_batches, batches);
    }

    /**
     * Returns a reference to the list of batches to render in order.
     */
    public ArrayList<Batch> getBatches ()
    {
        return _batches;
    }

    @Override
    public boolean draw (Renderer renderer)
    {
        renderer.render(_batches);
        return false;
    }

    @Override
    public int getPrimitiveCount ()
    {
        return 0; // the call to render(_batches) above counts the sub-batch primitives
    }

    /** The sub-batches of this batch. */
    protected ArrayList<Batch> _batches = new ArrayList<Batch>(1);
}
