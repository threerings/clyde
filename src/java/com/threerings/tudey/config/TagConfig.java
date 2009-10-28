//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.tudey.config;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Tag configuration
 */
public class TagConfig extends DeepObject
    implements Exportable, Streamable
{
    /** The base tag array. */
    @Editable
    public String[] tags = new String[0];

    /** The derived tag config. */
    @Editable(editor="getPath", nullable=true)
    public TagConfig derived;

    /**
     * Returns the complete array.
     */
    public String[] getTags ()
    {
        if (derived == null) {
            return tags;
        }
        String[] concat = new String[getLength()];
        buildConcat(concat, 0);
        return concat;
    }

    /**
     * Calculates the total number of tags.
     */
    public int getLength ()
    {
        int length = tags.length;
        if (derived != null) {
            length += derived.getLength();
        }
        return length;
    }
    /**
     * Concatenates the tags onto the supplied array starting at the specified index.
     */
    protected void buildConcat (String[] concat, int idx)
    {
        for (int ii = 0; ii < tags.length; ii++) {
            concat[ii + idx] = tags[ii];
        }
        if (derived != null) {
            derived.buildConcat(concat, idx + tags.length);
        }
    }
}

