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

package com.threerings.tudey.config;

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

import com.samskivert.util.ArrayUtil;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Tag configuration
 */
public class TagConfig extends DeepObject
    implements Exportable, Streamable, Iterable<String>
{
    /** The base tag array. */
    @Editable
    public String[] tags = ArrayUtil.EMPTY_STRING;

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
        int idx = 0;
        for (TagConfig tc = this; tc != null; tc = tc.derived) {
            System.arraycopy(tc.tags, 0, concat, idx, tc.tags.length);
            idx += tc.tags.length;
        }
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

    // from Iterable
    public Iterator<String> iterator ()
    {
        return new AbstractIterator<String>() {
            protected String computeNext () {
                do {
                    if (idx < tc.tags.length) {
                        return tc.tags[idx++];
                    }
                    tc = tc.derived;
                    idx = 0;
                } while (tc != null);
                return endOfData();
            }
            protected TagConfig tc = TagConfig.this;
            protected int idx = 0;
        };
    }
}
