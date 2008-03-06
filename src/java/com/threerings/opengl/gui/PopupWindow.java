//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.gui.layout.LayoutManager;

/**
 * A window that is popped up to display something like a menu or a
 * tooltip or some other temporary, modal overlaid display.
 */
public class PopupWindow extends Window
{
    public PopupWindow (Window parent, LayoutManager layout)
    {
        super(parent.getStyleSheet(), layout);
        _parentWindow = parent;
        setLayer(parent.getLayer());

        // set up our background and border from the look and feel
//         setBackground(_lnf.createPopupBackground());
//         setBorder(_lnf.createPopupBorder());
    }

    @Override // documentation inherited
    public boolean shouldShadeBehind ()
    {
        return false;
    }

    /**
     * Sizes the window to its preferred size and then displays it at the
     * specified coordinates extending either above the location or below
     * as specified. The window position may be adjusted if it does not
     * fit on the screen at the specified coordinates.
     */
    public void popup (int x, int y, boolean above)
    {
        // add ourselves to the interface hierarchy if we're not already
        if (_root == null) {
            _parentWindow.getRoot().addWindow(this);
        }

        // size and position ourselves appropriately
        packAndFit(x, y, above);
    }

    /**
     * Called after we have been added to the display hierarchy to pack and
     * position this popup window.
     */
    protected void packAndFit (int x, int y, boolean above)
    {
        pack();

        // adjust x and y to ensure that we fit on the screen
        int width = _root.getDisplayWidth();
        int height = _root.getDisplayHeight();
        x = Math.min(width - getWidth(), x);
        y = above ?
            Math.min(height - getHeight(), y) : Math.max(0, y - getHeight());
        setLocation(x, y);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "popupwindow";
    }
}
