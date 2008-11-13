//
// $Id$

package com.threerings.opengl.gui;

import java.awt.Toolkit;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.gui.Log.*;

/**
 * A root for {@link Display}-based apps.
 */
public class DisplayRoot extends Root
{
    public DisplayRoot (GlContext ctx)
    {
        super(ctx);
        _clipboard = Toolkit.getDefaultToolkit().getSystemSelection();
    }

    @Override // documentation inherited
    public int getDisplayWidth ()
    {
        return Display.getDisplayMode().getWidth();
    }

    @Override // documentation inherited
    public int getDisplayHeight ()
    {
        return Display.getDisplayMode().getHeight();
    }

    @Override // documentation inherited
    public void setCursor (Cursor cursor)
    {
        try {
            Mouse.setNativeCursor(cursor == null ? null : cursor.getLWJGLCursor());
        } catch (LWJGLException e) {
            log.warning("Failed to set cursor.", "cursor", cursor, e);
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        super.tick(elapsed);

        // process mouse events
        while (Mouse.next()) {
        }

        // process keyboard events
        while (Keyboard.next()) {

        }
    }
}
