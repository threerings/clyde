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

package com.threerings.opengl.gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.annotations.Beta;
import com.google.common.collect.Iterables;

import org.lwjgl.input.IME;
import org.lwjgl.opengl.GL11;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ObserverList;

import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.SimpleOverlay;
import com.threerings.opengl.util.Tickable;

import com.threerings.opengl.gui.config.CursorConfig;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.EventListener;
import com.threerings.opengl.gui.event.FocusEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.gui.text.IMEComponent;

import static com.threerings.opengl.gui.Log.log;

/**
 * Connects the BUI system into the JME scene graph.
 */
public abstract class Root extends SimpleOverlay
    implements Tickable
{
    /** The name of the default cursor config. */
    public static final String DEFAULT_CURSOR = "Default";

    public Root (GlContext ctx)
    {
        super(ctx);
        _soundGroup = ctx.getSoundManager().createGroup(ctx.getClipProvider(), SOUND_SOURCES);
    }

    /**
     * Releases the resources held by this root.
     */
    public void dispose ()
    {
        _soundGroup.dispose();
    }

    /**
     * Returns the sound group to use for feedback effects.
     */
    public SoundGroup getSoundGroup ()
    {
        return _soundGroup;
    }

    /**
     * Plays a sound by path.
     */
    public void playSound (String path)
    {
        getSound(path).play(true);
    }

    /**
     * Retrieves an instance of the sound at the specified path from the sound group.
     */
    public Sound getSound (String path)
    {
        // make the sound source-relative to avoid distance attenuation for mono sounds
        Sound sound = _soundGroup.getSound(path);
        sound.setSourceRelative(true);
        return sound;
    }

    /**
     * Returns the current timestamp used to stamp event times.
     */
    public long getTickStamp ()
    {
        return _tickStamp;
    }

    /**
     * Returns the current set of event modifiers.
     */
    public int getModifiers ()
    {
        return _modifiers;
    }

    /**
     * Returns a reference to the clipboard.
     */
    public Clipboard getClipboard ()
    {
        return _clipboard;
    }

    /**
     * Returns the text in the clipboard, or <code>null</code> for none.
     */
    public String getClipboardText ()
    {
        if (_clipboard == null) {
            return null;
        }
        try {
            Transferable contents = _clipboard.getContents(this);
            return (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) ?
                (String)contents.getTransferData(DataFlavor.stringFlavor) : null;

        } catch (Exception e) {
            log.warning("Failed to access clipboard.", e);
            return null;
        }
    }

    /**
     * Sets the text in the clipboard.
     */
    public void setClipboardText (String text)
    {
        if (_clipboard == null) {
            return;
        }
        StringSelection select = new StringSelection(text);
        _clipboard.setContents(select, select);
    }

    /**
     * Registers a top-level window with the input system.
     */
    public void addWindow (Window window)
    {
        addWindow(window, false);
    }

    /**
     * Registers a top-level window with the input system.
     *
     * @param topLayer if true, will set the window layer to the top most layer if it's current
     * layer is less than that.
     */
    public void addWindow (Window window, boolean topLayer)
    {
        // make a note of the current top window
        Window curtop = getTopWindow();

        if (topLayer && curtop != null) {
            window.setLayer(Math.max(window.getLayer(), curtop.getLayer()));
        }

        // add this window into the stack and resort
        _windows.add(window);
        resortWindows();

        // if this window is now the top window, we need to transfer the focus to it (and save the
        // previous top window's focus)
        Component pendfocus = null;
        if (_windows.get(_windows.size()-1) == window && !window.isOverlay()) {
            // store the previous top window's focus and clear it
            if (_focus != null && curtop != null) {
                curtop._savedFocus = _focus;
                setFocus(null);
            }
            // make a note of the window's previous saved focus
            pendfocus = window._savedFocus;
            window._savedFocus = null;
        }

        // add this window to the hierarchy (which may set a new focus)
        window.setRoot(this);

        // if no new focus was set when we added the window, give the focus to the previously
        // pending focus component
        if (_focus == null && pendfocus != null) {
            setFocus(pendfocus);
        }

        // recompute the hover component; the window may be under the mouse
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Returns true if the specified window is on top.
     */
    public boolean isOnTop (Window window)
    {
        return window == getTopWindow();
    }

    /**
     * Ensures that the specified window is on top.
     */
    public void moveToTop (Window window)
    {
        Window curtop = getTopWindow();
        if (curtop != null) {
            window.setLayer(Math.max(window.getLayer(), curtop.getLayer()));
            if (_windows.remove(window)) {
                _windows.add(window);
            }
        }
    }

    /**
     * Returns a reference to the topmost window, or <code>null</code> if there are no windows.
     */
    public Window getTopWindow ()
    {
        int size = _windows.size();
        return (size == 0) ? null : _windows.get(size - 1);
    }

    /**
     * Called when an added window's layer is changed. Adjusts the ordering of the windows in the
     * stack.
     */
    public void resortWindows ()
    {
        Collections.sort(_windows);
    }

    /**
     * Removes all windows from the root node.
     */
    public void removeAllWindows ()
    {
        setFocus(null);
        for (int ii = _windows.size() - 1; ii >= 0; ii--) {
            Window window = _windows.remove(ii);
            // remove the window from the interface heirarchy
            window.setRoot(null);
        }

        // then remove the hover component (which may result in a mouse exited even being
        // dispatched to the window or one of its children)
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Removes a window from participation in the input system.
     */
    public void removeWindow (Window window)
    {
        // if our focus is in this window, clear it
        if (_focus != null && _focus.getWindow() == window) {
            setFocus(null);
        }

        // clear any saved focus reference
        window._savedFocus = null;

        // first remove the window from our list
        if (!_windows.remove(window)) {
            log.warning("Requested to remove unmanaged window.", "window", window, new Exception());
            return;
        }

        // then remove the hover component (which may result in a mouse exited even being
        // dispatched to the window or one of its children)
        updateHoverComponent(_mouseX, _mouseY);

        // remove the window from the interface heirarchy
        window.setRoot(null);

        // remove any associated popup windows
        for (Window owindow : _windows.toArray(new Window[_windows.size()])) {
            if (owindow.getParentWindow() == window) {
                removeWindow(owindow);
            }
        }

        // finally restore the focus to the new top-most window if it has a saved focus
        if (_windows.size() > 0) {
            Window top = _windows.get(_windows.size()-1);
            top.gotFocus();
        }
    }

    /**
     * Adds a tick participant to the list.
     */
    public void addTickParticipant (Tickable participant)
    {
        _tickParticipants.add(participant);
    }

    /**
     * Removes a tick participant from the list.
     */
    public void removeTickParticipant (Tickable participant)
    {
        _tickParticipants.remove(participant);
    }

    /**
     * Configures the number of seconds that the mouse must rest over a component to trigger a
     * tooltip. If the value is set to zero, tips will appear immediately.
     */
    public void setTooltipTimeout (float seconds)
    {
        _tipTime = seconds;
    }

    /**
     * Returns the tool tip timeout. See {@link #setTooltipTimeout} for details.
     */
    public float getTooltipTimeout ()
    {
        float htimeout = (_hcomponent == null) ? -1f : _hcomponent.getTooltipTimeout();
        return (htimeout < 0f) ? _tipTime : htimeout;
    }

    /**
     * Sets the preferred width of tooltip windows. The default is to prefer a width slightly less
     * wide that the entire window.
     */
    public void setTooltipPreferredWidth (int width)
    {
        _tipWidth = width;
    }

    /**
     * Registers a listener that will be notified of all events prior to their being dispatched
     * normally.
     */
    public void addGlobalEventListener (EventListener listener)
    {
        _globals.add(listener);
    }

    /**
     * Removes a global event listener registration.
     */
    public void removeGlobalEventListener (EventListener listener)
    {
        _globals.remove(listener);
    }

    /**
     * This is called by a window or a scroll pane when it has become invalid.  The root node
     * should schedule a revalidation of this component on the next tick or the next time an event
     * is processed.
     */
    public void rootInvalidated (Validateable root)
    {
        // add the component to the list of invalid roots
        if (!_invalidRoots.contains(root)) {
            _invalidRoots.addLast(root);
        }
    }

    /**
     * Configures a component to receive all events that are not sent to some other component. When
     * an event is not consumed during normal processing, it is sent to the default event targets,
     * most recently registered to least recently registered.
     */
    public void pushDefaultEventTarget (Component component)
    {
        _defaults.add(component);
    }

    /**
     * Pops the default event target off the stack.
     */
    public void popDefaultEventTarget (Component component)
    {
        _defaults.remove(component);
    }

    /**
     * Requests that the specified component be given the input focus.  Pass null to clear the
     * focus.
     */
    public void requestFocus (Component component)
    {
        setFocus(component);
    }

    /**
     * Returns the component that currently has the focus, or null.
     */
    public Component getFocus ()
    {
        return ((_focus != null) && _focus.isEnabled()) ? _focus : null;
    }

    /**
     * Returns the total number of windows added to this node.
     */
    public int getWindowCount ()
    {
        return _windows.size();
    }

    /**
     * Returns the window at the specified index.
     */
    public Window getWindow (int index)
    {
        return _windows.get(index);
    }

    /**
     * Returns a live List "view" of the windows added to this node.
     */
    public List<Window> getWindows ()
    {
        return Collections.unmodifiableList(_windows);
    }

    /**
     * Returns an Iterable over our Windows, their children, their children's children, and so on.
     */
    public Iterable<Component> getDescendants ()
    {
        return Container.getDescendants(_windows);
    }

    /**
     * Returns the width of the display area.
     */
    public abstract int getDisplayWidth ();

    /**
     * Returns the height of the display area.
     */
    public abstract int getDisplayHeight ();

    /**
     * Sets the cursor to display (or <code>null</code> to use the default cursor).
     */
    public void setCursor (Cursor cursor)
    {
        if (_cursor == cursor) {
            return;
        }
        _cursor = cursor;
        if (!isDragging()) {
            updateCursor(cursor);
        }
    }

    /**
     * Sets the mouse position.
     */
    public void setMousePosition (int x, int y)
    {
        mouseMoved(_tickStamp, x, y, false);
    }

    /**
     * Sets the mouse as being pressed at the current position.
     */
    @Beta
    public void setMouseDown (int button)
    {
        mousePressed(_tickStamp, button, _mouseX, _mouseY, false);
    }

    /**
     * Sets the mouse as being released at the current position.
     */
    @Beta
    public void setMouseUp (int button)
    {
        mouseReleased(_tickStamp, button, _mouseX, _mouseY, false);
    }

    /**
     * Returns the x coordinate of the mouse cursor.
     */
    public int getMouseX ()
    {
        return _mouseX;
    }

    /**
     * Returns the y coordinate of the mouse cursor.
     */
    public int getMouseY ()
    {
        return _mouseY;
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return "Root@" + hashCode();
    }

    /**
     * A large component that changes its tooltip while it is the hover component in the normal
     * course of events can call this method to force an update to the tooltip window.
     */
    public void tipTextChanged (Component component)
    {
        if (component == _hcomponent) {
            clearTipWindow();
        }
    }

    /**
     * Called when a component loses its hoverability.
     */
    public void hoverabilityDeactivated (Component comp)
    {
        if (_hcomponent != null && Iterables.contains(comp.getDownwards(), _hcomponent)) {
            updateHoverComponent(_mouseX, _mouseY);
        }
    }

    /**
     * Sets the color of the shade behind the first active modal window.
     */
    public void setModalShade (Color4f color)
    {
        _modalShade = color;
    }

    /**
     * Initiates a drag operation.
     */
    public void startDrag (TransferHandler handler, Component source, int action)
    {
        _dhandler = handler;
        _dsource = source;
        _ddata = handler.createTransferable(source);
        _daction = action;
        _dicon = handler.getVisualRepresentation(_ddata);

        // We're just about to clear the click component, but it may be armed.
        // Once we clear it, it won't get any more mouse events and will continue to think
        // it is armed unless we fake up some events...
        long now = System.currentTimeMillis();
        dispatchMouseEvent(_ccomponent,
            new MouseEvent(this, now, 0, MouseEvent.MOUSE_DRAGGED,
                Integer.MIN_VALUE, Integer.MIN_VALUE));
        dispatchMouseEvent(_ccomponent,
            new MouseEvent(this, now, 0, MouseEvent.MOUSE_RELEASED,
                MouseEvent.BUTTON1, Integer.MIN_VALUE, Integer.MIN_VALUE, 1));

        _ccomponent = null; // now clear the click component
        updateCursor(null);
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Clears out any drag operation in progress.
     */
    public void clearDrag ()
    {
        _dhandler = null;
        _dsource = null;
        _ddata = null;
        _dicon = null;
        updateCursor(_cursor);
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Checks whether a drag operation is in progress.
     */
    public boolean isDragging ()
    {
        return _dhandler != null;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // update the tick stamp
        _tickStamp = System.currentTimeMillis();

        // notify our tick participants
        _tickParticipants.apply(_tickOp.init(elapsed));

        // repeat keys as necessary
        if (!_pressedKeys.isEmpty()) {
            for (KeyRecord key : _pressedKeys.values()) {
                key.maybeRepeat();
            }
        }

        // validate all invalid roots
        boolean updateHover = false;
        while (!_invalidRoots.isEmpty()) {
            Validateable root = _invalidRoots.removeFirst();
            // make sure the root is still added to the view hierarchy
            if (root.isAdded()) {
                root.validate();
                updateHover = true;
            }
        }

        // update the hover component if necessary
        if (updateHover) {
            updateHoverComponent(_mouseX, _mouseY);
        }

        // check to see if we need to pop up a tooltip
        _lastMoveTime += elapsed;
        _lastTipTime += elapsed;
        String tiptext;
        if (_hcomponent == null || _tipwin != null ||
            (_lastMoveTime < getTooltipTimeout() &&
             _lastTipTime > TIP_MODE_RESET) ||
            (tiptext = _hcomponent.getTooltipText()) == null) {
            if (_tipwin != null) {
                _lastTipTime = 0;
            }
            return;
        }

        // make sure the hover component is in a window and wants a tooltip
        Window hwin = _hcomponent.getWindow();
        Component tcomp = _hcomponent.createTooltipComponent(tiptext);
        if (hwin == null || tcomp == null) {
            return;
        }

        // create, set up and show the tooltip window
        _tipwin = new Window(_ctx, new BorderLayout()) {
            public boolean isOverlay () {
                return true; // don't steal input focus
            }
        };
        _tipwin.setLayer(Integer.MAX_VALUE/2);
        _tipwin.setStyleConfig(_hcomponent.getTooltipWindowStyle());
        _tipwin.add(tcomp, BorderLayout.CENTER);
        addWindow(_tipwin);

        // it's possible that adding the tip window will cause it to immediately be removed, so
        // make sure we don't NPE
        if (_tipwin == null) {
            return;
        }

        // if it's still here, lay it out
        int width = getDisplayWidth();
        int height = getDisplayHeight();
        _tipwin.pack(_tipWidth == -1 ? width-10 : _tipWidth, height-10);
        int tx = 0, ty = 0;
        if (_hcomponent.isTooltipRelativeToMouse()) {
            tx = _mouseX - _tipwin.getWidth()/2;
            ty = _mouseY + 10;
        } else {
            tx = _hcomponent.getAbsoluteX() +
                (_hcomponent.getWidth() - _tipwin.getWidth()) / 2;
            ty = _hcomponent.getAbsoluteY() + _hcomponent.getHeight() + 10;
        }
        tx = Math.max(5, Math.min(tx, width-_tipwin.getWidth()-5));
        ty = Math.min(ty, height- _tipwin.getHeight() - 5);
        _tipwin.setLocation(tx, ty);
        // we need to validate here because we're adding a window in the middle of our normal frame
        // processing
        _tipwin.validate();
    }

    @Override
    protected void draw ()
    {
        Window modalWin = null;
        if (_modalShade != null) {
            for (int ii = _windows.size() - 1; ii >= 0; ii--) {
                Window win = _windows.get(ii);
                if (win.shouldShadeBehind()) {
                    modalWin = win;
                    break;
                }
            }
        }

        // make sure we're in modelview matrix mode
        Renderer renderer = _ctx.getRenderer();
        renderer.setMatrixMode(GL11.GL_MODELVIEW);

        // render all of our windows
        for (int ii = 0, ll = _windows.size(); ii < ll; ii++) {
            Window win = _windows.get(ii);
            try {
                if (win == modalWin) {
                    renderModalShade(renderer);
                }
                win.render(renderer);
            } catch (Throwable t) {
                log.warning(win + " failed in render()", t);
            }
        }

        // render the drag icon, if any, at the mouse location
        if (_dicon != null) {
            _dicon.render(
                renderer, _mouseX - _dicon.getWidth()/2, _mouseY - _dicon.getHeight()/2, 0.5f);
        }
    }

    /**
     * Updates the cursor.
     */
    protected abstract void updateCursor (Cursor cursor);

    /**
     * Returns a reference to the default cursor, or <code>null</code> to use the operating system
     * default.
     */
    protected Cursor getDefaultCursor ()
    {
        CursorConfig config = _ctx.getConfigManager().getConfig(
            CursorConfig.class, DEFAULT_CURSOR);
        return (config == null) ? null : config.getCursor(_ctx);
    }

    /**
     * Handles a mouse pressed event.
     */
    protected void mousePressed (long when, int button, int x, int y, boolean consume)
    {
        checkMouseMoved(x, y);

        setFocus(_ccomponent = getTargetComponent());
        MouseEvent event = new MouseEvent(
            this, when, _modifiers, MouseEvent.MOUSE_PRESSED, button, x, y);
        if (consume) {
            event.consume();
        }
        if (button == MouseEvent.BUTTON1) {
            _px = x;
            _py = y;
        }
        dispatchMouseEvent(_ccomponent, event);
    }

    /**
     * Handles a mouse released event.
     */
    protected void mouseReleased (long when, int button, int x, int y, boolean consume)
    {
        checkMouseMoved(x, y);

        if (button == MouseEvent.BUTTON1 && _dhandler != null) {
            TransferHandler chandler = (_hcomponent == null) ?
                null : _hcomponent.getTransferHandler();
            if (chandler != null && chandler.importData(_hcomponent, _ddata)) {
                _dhandler.exportDone(_dsource, _ddata, _daction);
            }
            clearDrag();
        }

        MouseEvent event = new MouseEvent(
            this, when, _modifiers, MouseEvent.MOUSE_RELEASED, button, x, y);
        if (consume) {
            event.consume();
        }
        dispatchMouseEvent(getTargetComponent(), event);
        _ccomponent = null;
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Handles a mouse moved/dragged event.
     */
    protected void mouseMoved (long when, int x, int y, boolean consume)
    {
        // if the mouse has moved, generate a moved or dragged event
        if (!checkMouseMoved(x, y)) {
            return;
        }
        Component tcomponent = getTargetComponent();
        int type = (tcomponent != null && tcomponent == _ccomponent) ?
            MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED;
        MouseEvent event = new MouseEvent(
            this, when, _modifiers, type, _mouseX, _mouseY);
        if (consume) {
            event.consume();
        }
        if (type == MouseEvent.MOUSE_DRAGGED &&
                (_modifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            TransferHandler handler = tcomponent.getTransferHandler();
            int actions = (handler == null) ?
                TransferHandler.NONE : handler.getSourceActions(tcomponent);
            int dist = Math.abs(x - _px) + Math.abs(y - _py);
            if (actions != TransferHandler.NONE && dist >= DRAG_DISTANCE) {
                handler.exportAsDrag(tcomponent, event,
                    actions == TransferHandler.COPY ? actions : TransferHandler.MOVE);
            }
        }
        dispatchMouseEvent(tcomponent, event);
    }

    /**
     * Handles a mouse wheel event.
     */
    protected void mouseWheeled (long when, int x, int y, int delta, boolean consume)
    {
        checkMouseMoved(x, y);

        MouseEvent event = new MouseEvent(
            this, when, _modifiers, MouseEvent.MOUSE_WHEELED,
            -1, x, y, 0, delta);
        if (consume) {
            event.consume();
        }
        dispatchMouseEvent(getTargetComponent(), event);

        // calculate our new hover component
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Checks for a change to the mouse location, calling {@link #mouseDidMove} and returning
     * <code>true</code> if the mouse moved.
     */
    protected boolean checkMouseMoved (int x, int y)
    {
        // determine whether the mouse moved
        if (_mouseX != x || _mouseY != y) {
            mouseDidMove(x, y);
            return true;
        }
        return false;
    }

    /**
     * Handles a key press event.
     */
    protected void keyPressed (long when, char keyChar, int keyCode, boolean consume)
    {
        KeyEvent event = new KeyEvent(
            this, when, _modifiers, KeyEvent.KEY_PRESSED, keyChar, keyCode, false);
        if (consume) {
            event.consume();
        }
        dispatchKeyEvent(getFocus(), event);
    }

    /**
     * Handles a key release event.
     */
    protected void keyReleased (long when, char keyChar, int keyCode, boolean consume)
    {
        KeyEvent event = new KeyEvent(
            this, when, _modifiers, KeyEvent.KEY_RELEASED, keyChar, keyCode, false);
        if (consume) {
            event.consume();
        }
        dispatchKeyEvent(getFocus(), event);
    }

    /**
     * Dispatches a mouse event, performing extra processing for clicks.
     */
    protected boolean dispatchMouseEvent (Component target, MouseEvent event)
    {
        // dispatch event before click processing
        boolean dispatched = dispatchEvent(target, event);

        // note press/release events
        int type = event.getType();
        if (type == MouseEvent.MOUSE_PRESSED) {
            _buttons[event.getButton()].wasPressed(target, event);
        } else if (type == MouseEvent.MOUSE_RELEASED) {
            _buttons[event.getButton()].wasReleased(target, event);
        }
        return dispatched;
    }

    /**
     * Dispatches a key event, performing extra processing for key repeats.
     */
    protected boolean dispatchKeyEvent (Component target, KeyEvent event)
    {
        // keep track of keys pressed

        int keyCode = event.getKeyCode();
        if (keyCode != 0) {
            if (event.getType() == KeyEvent.KEY_PRESSED) {
                if (_pressedKeys.containsKey(keyCode)) {
                    return false; // we're already repeating the key
                }
                _pressedKeys.put(keyCode, new KeyRecord(event));

            } else { // event.getType() == KeyEvent.KEY_RELEASED
                _pressedKeys.remove(event.getKeyCode());
            }
        }
        return dispatchEvent(target, event);
    }

    /**
     * Dispatches an event to the specified target (which may be null). If the target is null, or
     * did not consume the event, it will be passed on to the most recently opened modal window if
     * one exists (and the supplied target component was not a child of that window) and then to
     * the default event targets if the event is still unconsumed.
     *
     * @return true if the event was dispatched, false if not.
     */
    protected boolean dispatchEvent (Component target, Event event)
    {
        // notify our global listeners if we have any
        for (EventListener listener : _globals) {
            try {
                listener.eventDispatched(event);
            } catch (Exception e) {
                log.warning("Global event listener choked.", "listener", listener, e);
            }
        }

        // first try the "natural" target of the event if there is one
        Window sentwin = null;
        if (target != null) {
            if (target.dispatchEvent(event)) {
                return true;
            }
            sentwin = target.getWindow();
        }

        // next try the most recently opened modal window, if we have one
        for (int ii = _windows.size()-1; ii >= 0; ii--) {
            Window window = _windows.get(ii);
            if (window.isModal()) {
                if (window != sentwin) {
                    if (window.dispatchEvent(event)) {
                        return true;
                    }
                }
                break;
            }
        }

        // finally try the default event targets
        for (int ii = _defaults.size()-1; ii >= 0; ii--) {
            Component deftarg = _defaults.get(ii);
            if (deftarg.dispatchEvent(event)) {
                return true;
            }
        }

        // let our caller know that the event was not dispatched by anyone
        return false;
    }

    /**
     * Configures the component that has keyboard focus.
     */
    protected void setFocus (Component focus)
    {
        // allow the component we clicked on to adjust the focus target
        if (focus != null) {
            focus = focus.getFocusTarget();
        }

        // if the focus is changing, dispatch an event to report it
        if (_focus != focus) {
            Component oldFocus = _focus;
            _focus = focus;
            if (oldFocus != null) {
                oldFocus.dispatchEvent(new FocusEvent(this, getTickStamp(), FocusEvent.FOCUS_LOST));
                if (oldFocus instanceof IMEComponent) {
                    setIMEFocus(false);
                }
            }
            if (_focus != null) {
                _focus.dispatchEvent(new FocusEvent(this, getTickStamp(), FocusEvent.FOCUS_GAINED));
                if (_focus instanceof IMEComponent) {
                    setIMEFocus(true);
                }
            }
        }
    }

    /**
     * Called when the focus of an IME enabled component changes.
     */
    protected void setIMEFocus (boolean focused)
    {
        IME.setEnabled(focused);
    }

    /**
     * Called by a window when its position changes. This triggers a recomputation of the hover
     * component as the window may have moved out from under or under the mouse.
     */
    protected void windowDidMove (Window window)
    {
        updateHoverComponent(_mouseX, _mouseY);
    }

    protected void mouseDidMove (int mouseX, int mouseY)
    {
        // update some tracking bits
        _mouseX = mouseX;
        _mouseY = mouseY;

        // calculate our new hover component
        updateHoverComponent(_mouseX, _mouseY);

        if (_tipwin == null) {
            _lastMoveTime = 0;
        }
    }

    /**
     * Recomputes the component over which the mouse is hovering, generating mouse exit and entry
     * events as necessary.
     */
    protected void updateHoverComponent (int mx, int my)
    {
        // delay updating the hover component if we have a clicked component
        if (_ccomponent != null) {
            return;
        }

        // check for a new hover component starting with each of our root components
        Component nhcomponent = null;
        for (int ii = _windows.size()-1; ii >= 0; ii--) {
            Window comp = _windows.get(ii);
            nhcomponent = comp.getHitComponent(mx, my);
            if (nhcomponent != null && nhcomponent.getWindow() != _tipwin) {
                if (_dhandler == null) {
                    break;
                }
                TransferHandler chandler = nhcomponent.getTransferHandler();
                if (chandler != null && chandler.canImport(
                        nhcomponent, _ddata.getTransferDataFlavors())) {
                    break;
                }
            }
            // if this window is modal, stop here
            if (comp.isModal()) {
                break;
            }
        }

        // generate any necessary mouse entry or exit events
        if (_hcomponent != nhcomponent) {
            // inform the previous component that the mouse has exited
            if (_hcomponent != null) {
                _hcomponent.dispatchEvent(new MouseEvent(this, getTickStamp(), _modifiers,
                                                         MouseEvent.MOUSE_EXITED, mx, my));
            }
            // inform the new component that the mouse has entered
            if (nhcomponent != null) {
                nhcomponent.dispatchEvent(new MouseEvent(this, getTickStamp(), _modifiers,
                                                         MouseEvent.MOUSE_ENTERED, mx, my));
            }
            _hcomponent = nhcomponent;

            // clear out any tooltip business in case the hover component changed as a result of a
            // window popping up
            if (_hcomponent == null || _hcomponent.getWindow() != _tipwin) {
                clearTipWindow();
            }
        }
    }

    protected Component getTargetComponent ()
    {
        // mouse press and mouse motion events do not necessarily go to
        // the component under the mouse. when the mouse is clicked down
        // on a component (any button), it becomes the "clicked"
        // component, the target for all subsequent click and motion
        // events (which become drag events) until all buttons are
        // released
        if (_ccomponent != null) {
            return _ccomponent;
        }
        // if there's no clicked component, use the hover component
        if (_hcomponent != null) {
            return _hcomponent;
        }
        // if there's no hover component, use the default event target
        return null;
    }

    protected void clearTipWindow ()
    {
        _lastMoveTime = 0;
        if (_tipwin != null) {
            Window tipwin = _tipwin;
            _tipwin = null;
            // this might trigger a recursive call to clearTipWindow
            removeWindow(tipwin);
        }
    }

    protected void renderModalShade (Renderer renderer)
    {
        int width = getDisplayWidth();
        int height = getDisplayHeight();

        renderer.setColorState(_modalShade);
        renderer.setTextureState(null);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(width, 0);
        GL11.glVertex2f(width, height);
        GL11.glVertex2f(0, height);
        GL11.glEnd();
    }

    /**
     * Contains the state of a mouse button.
     */
    protected class ButtonRecord
    {
        /**
         * Called when the button is pressed.
         */
        public void wasPressed (Component target, MouseEvent press)
        {
            long when = press.getWhen();
            _clickTime = when + CLICK_INTERVAL;
            _count = (when < _chainTime) ? (_count + 1) : 1;
        }

        /**
         * Called when the button is released.
         */
        public void wasReleased (Component target, MouseEvent release)
        {
            long when = release.getWhen();
            if (when < _clickTime) {
                MouseEvent event = new MouseEvent(
                    Root.this, when, _modifiers, MouseEvent.MOUSE_CLICKED,
                    release.getButton(), release.getX(), release.getY(), _count);
                dispatchEvent(target, event);
                _chainTime = when + CLICK_CHAIN_INTERVAL;
            }
        }

        protected long _clickTime, _chainTime;
        protected int _count;
    }

    /**
     * Describes a key being held down.
     */
    protected class KeyRecord
    {
        public KeyRecord (KeyEvent press)
        {
            _press = press;
            _nextRepeat = press.getWhen() + KEY_REPEAT_DELAY;
        }

        /**
         * Returns a reference to the press event.
         */
        public KeyEvent getPress ()
        {
            return _press;
        }

        /**
         * Dispatches a key repeat event if appropriate.
         */
        public void maybeRepeat ()
        {
            if (_tickStamp < _nextRepeat) {
                return;
            }
            KeyEvent event = new KeyEvent(
                Root.this, _tickStamp, _modifiers, KeyEvent.KEY_PRESSED,
                _press.getKeyChar(), _press.getKeyCode(), true);
            dispatchEvent(getFocus(), event);
            _nextRepeat += 1000L / KEY_REPEAT_RATE;
        }

        protected KeyEvent _press;
        protected long _nextRepeat;
    }

    /**
     * Used to notify the tick participants.
     */
    protected static class TickOp
        implements ObserverList.ObserverOp<Tickable>
    {
        /**
         * Initializes this op with the elapsed time.
         *
         * @return a reference to the op, for chaining.
         */
        public TickOp init (float elapsed)
        {
            _elapsed = elapsed;
            return this;
        }

        // documentation inherited from interface ObserverList.ObserverOp
        public boolean apply (Tickable tickable)
        {
            tickable.tick(_elapsed);
            return true;
        }

        /** The elapsed time since the last tick. */
        protected float _elapsed;
    }

    protected long _tickStamp;
    protected int _modifiers;
    protected int _mouseX, _mouseY;
    protected Clipboard _clipboard;

    protected Window _tipwin;
    protected float _lastMoveTime, _tipTime = 1f, _lastTipTime;
    protected int _tipWidth = -1;
    protected Color4f _modalShade;

    protected ArrayList<Window> _windows = new ArrayList<Window>();
    protected Component _hcomponent, _ccomponent;
    protected Component _focus;
    protected ArrayList<Component> _defaults = new ArrayList<Component>();
    protected ObserverList<Tickable> _tickParticipants = ObserverList.newSafeInOrder();
    protected TickOp _tickOp = new TickOp();
    protected CopyOnWriteArrayList<EventListener> _globals =
        new CopyOnWriteArrayList<EventListener>();

    protected ArrayDeque<Validateable> _invalidRoots = new ArrayDeque<Validateable>();

    /** A sound group for feedback effects. */
    protected SoundGroup _soundGroup;

    /** The cursor being displayed. */
    protected Cursor _cursor;

    /** Mouse button information. */
    protected ButtonRecord[] _buttons = new ButtonRecord[MouseEvent.MAX_BUTTONS];
    { // initializer
        for (int ii = 0; ii < _buttons.length; ii++) {
            _buttons[ii] = new ButtonRecord();
        }
    }

    /** Keys currently pressed, mapped by key code. */
    protected HashIntMap<KeyRecord> _pressedKeys = new HashIntMap<KeyRecord>();

    /** The location at which the last press occurred, for drag tracking. */
    protected int _px, _py;

    /** When dragging, the transfer handler. */
    protected TransferHandler _dhandler;

    /** When dragging, the drag source component. */
    protected Component _dsource;

    /** When dragging, the drag data. */
    protected Transferable _ddata;

    /** When dragging, the drag action. */
    protected int _daction;

    /** When dragging, the visual representation of the dragged data. */
    protected Icon _dicon;

    protected static final float TIP_MODE_RESET = 0.6f;

    /** Mouse buttons released within this interval after being pressed are counted as clicks. */
    protected static final long CLICK_INTERVAL = 250L;

    /** Clicks this close to one another are chained together for counting purposes. */
    protected static final long CLICK_CHAIN_INTERVAL = 250L;

    /** The key press repeat rate. */
    protected static final int KEY_REPEAT_RATE = 25;

    /** The delay in milliseconds before auto-repeated key presses will begin. */
    protected static final long KEY_REPEAT_DELAY = 500L;

    /** The distance from the press location at which we can start a drag operation. */
    protected static final int DRAG_DISTANCE = 16;

    /** The number of sound sources to allocate. */
    protected static final int SOUND_SOURCES = 2;
}
