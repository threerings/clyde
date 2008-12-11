//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.layout.GroupLayout;

/**
 * A top-level window with a border, a background and a title bar. Note that a
 * decorated window uses a stretching {@link GroupLayout} and adds a label at
 * the top in the <code>window_title</code> style if a title was specified.
 */
public class DecoratedWindow extends Window
{
    /**
     * Creates a decorated window using the supplied look and feel.
     *
     * @param title the title of the window or null if no title bar is
     * desired.
     */
    public DecoratedWindow (GlContext ctx, StyleSheet style, String title)
    {
        super(ctx, GroupLayout.makeVStretch());
        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(
            GroupLayout.CONSTRAIN);

        if (title != null) {
            add(new Label(ctx, title, "window_title"), GroupLayout.FIXED);
        }
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "decoratedwindow";
    }
}
