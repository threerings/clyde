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

package com.threerings.opengl.gui.layout;

import com.threerings.opengl.gui.util.Dimension;

/**
 * This record is used by the group layout managers to return a set of
 * statistics computed for their target widgets.
 */
public class DimenInfo
{
    public int count;

    public int totwid;
    public int tothei;

    public int maxwid;
    public int maxhei;

    public int numfix;
    public int fixwid;
    public int fixhei;

    public int maxfreewid;
    public int maxfreehei;

    public int totweight;

    public Dimension[] dimens;

    public String toString ()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[count=").append(count);
        buf.append(", totwid=").append(totwid);
        buf.append(", tothei=").append(tothei);
        buf.append(", maxwid=").append(maxwid);
        buf.append(", maxhei=").append(maxhei);
        buf.append(", numfix=").append(numfix);
        buf.append(", fixwid=").append(fixwid);
        buf.append(", fixhei=").append(fixhei);
        buf.append(", maxfreewid=").append(maxfreewid);
        buf.append(", maxfreehei=").append(maxfreehei);
        buf.append(", totweight=").append(totweight);
        return buf.append("]").toString();
    }
}
