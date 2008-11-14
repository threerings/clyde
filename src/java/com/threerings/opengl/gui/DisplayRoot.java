//
// $Id$

package com.threerings.opengl.gui;

import java.awt.Toolkit;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.InputEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;

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

        // poll the mouse and keyboard
        Mouse.poll();
        Keyboard.poll();

        // update the modifiers
        _modifiers = 0;
        int bcount = Mouse.getButtonCount();
        if (bcount >= 1 && Mouse.isButtonDown(0)) {
            _modifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if (bcount >= 2 && Mouse.isButtonDown(1)) {
            _modifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        if (bcount >= 3 && Mouse.isButtonDown(2)) {
            _modifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            _modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            _modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)) {
            _modifiers |= InputEvent.META_DOWN_MASK;
        }

        // process mouse events
        while (Mouse.next()) {
            int button = Mouse.getEventButton();
            if (button != -1) {
                MouseEvent event = new MouseEvent(
                    this, _tickStamp, _modifiers,
                    Mouse.getEventButtonState() ?
                        MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                    button, Mouse.getEventX(), Mouse.getEventY());
            }
        }

        // process keyboard events
        while (Keyboard.next()) {
            KeyEvent event = new KeyEvent(
                this, _tickStamp, _modifiers,
                Keyboard.getEventKeyState() ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                Keyboard.getEventCharacter(), Keyboard.getEventKey());
            dispatchKeyEvent(getFocus(), event);
        }
    }
}
