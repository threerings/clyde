//
// $Id$

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
	StringBuffer buf = new StringBuffer();
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
