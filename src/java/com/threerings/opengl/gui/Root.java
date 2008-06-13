//
// $Id$

package com.threerings.opengl.gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.HashIntMap;

import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.CullState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.LineState;
import com.threerings.opengl.renderer.state.PolygonState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.state.StencilState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.EventListener;
import com.threerings.opengl.gui.event.FocusEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.layout.BorderLayout;

import static java.util.logging.Level.*;
import static com.threerings.opengl.gui.Log.*;

/**
 * Connects the BUI system into the JME scene graph.
 */
public abstract class Root
    implements Tickable, Renderable
{
    /** The baseline render states. */
    public static final RenderState[] STATES = new RenderState[] {
        AlphaState.PREMULTIPLIED, ArrayState.DISABLED, null, ColorMaskState.ALL,
        CullState.DISABLED, DepthState.DISABLED, FogState.DISABLED, LightState.DISABLED,
        LineState.DEFAULT, null, null, PolygonState.DEFAULT, ShaderState.DISABLED,
        StencilState.DISABLED, null, TransformState.IDENTITY };

    public Root (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Returns the current timestamp used to stamp event times.
     */
    public long getTickStamp ()
    {
        return _tickStamp;
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
            log.log(WARNING, "Failed to access clipboard.", e);
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
        Window curtop = null;
        if (_windows.size() > 0) {
            curtop = _windows.get(_windows.size()-1);
        }

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
        return (_windows.size() > 0 &&
                _windows.get(_windows.size()-1) == window);
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
            Log.log.warning("Requested to remove unmanaged window [window=" + window + "].");
            Thread.dumpStack();
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
        return _tipTime;
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
    public void rootInvalidated (Component root)
    {
        // add the component to the list of invalid roots
        if (!_invalidRoots.contains(root)) {
            _invalidRoots.add(root);
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
        return _focus;
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
    public abstract void setCursor (Cursor cursor);

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
     * Sets the color of the shade behind the first active modal window.
     */
    public void setModalShade (Color4f color)
    {
        _modalShade = color;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // update the tick stamp
        _tickStamp = System.currentTimeMillis();

        // repeat keys as necessary
        if (!_pressedKeys.isEmpty()) {
            for (KeyRecord key : _pressedKeys.values()) {
                key.maybeRepeat();
            }
        }

        // validate all invalid roots
        while (_invalidRoots.size() > 0) {
            Component root = _invalidRoots.remove(0);
            // make sure the root is still added to the view hierarchy
            if (root.isAdded()) {
                root.validate();
            }
        }

        // notify our tick participants (in reverse order, so that they can remove themselves)
        for (int ii = _tickParticipants.size() - 1; ii >= 0; ii--) {
            _tickParticipants.get(ii).tick(elapsed);
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
        _tipwin = new Window(hwin.getStyleSheet(), new BorderLayout()) {
            public boolean isOverlay () {
                return true; // don't steal input focus
            }
        };
        _tipwin.setLayer(Integer.MAX_VALUE/2);
        _tipwin.setStyleClass("tooltip_window");
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

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        _ctx.getRenderer().enqueueOrtho(_batch);
    }

    /**
     * Renders the contents of the UI.
     */
    protected void render (Renderer renderer)
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

        // apply our baseline render states
        renderer.setStates(STATES);

        // make sure we're in modelview matrix mode
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
                Log.log.log(Level.WARNING, win + " failed in render()", t);
            }
        }
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
        if (event.getType() == KeyEvent.KEY_PRESSED) {
            int keyCode = event.getKeyCode();
            if (_pressedKeys.containsKey(keyCode)) {
                return false; // we're already repeating the key
            }
            _pressedKeys.put(keyCode, new KeyRecord(event));

        } else { // event.getType() == KeyEvent.KEY_RELEASED
            _pressedKeys.remove(event.getKeyCode());
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
        for (int ii = 0, ll = _globals.size(); ii < ll; ii++) {
            try {
                _globals.get(ii).eventDispatched(event);
            } catch (Exception e) {
                Log.log.log(Level.WARNING, "Global event listener choked " +
                            "[listener=" + _globals.get(ii) + "].", e);
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
            if (_focus != null) {
                _focus.dispatchEvent(new FocusEvent(this, getTickStamp(), FocusEvent.FOCUS_LOST));
            }
            _focus = focus;
            if (_focus != null) {
                _focus.dispatchEvent(new FocusEvent(this, getTickStamp(), FocusEvent.FOCUS_GAINED));
            }
        }
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
        // check for a new hover component starting with each of our root components
        Component nhcomponent = null;
        for (int ii = _windows.size()-1; ii >= 0; ii--) {
            Window comp = _windows.get(ii);
            nhcomponent = comp.getHitComponent(mx, my);
            if (nhcomponent != null && nhcomponent.getWindow() != _tipwin) {
                break;
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
            _target = target;
        }

        /**
         * Called when the button is released.
         */
        public void wasReleased (Component target, MouseEvent release)
        {
            long when = release.getWhen();
            if (target == _target && when < _clickTime) {
                MouseEvent event = new MouseEvent(
                    Root.this, when, _modifiers, MouseEvent.MOUSE_CLICKED,
                    release.getButton(), release.getX(), release.getY(), _count);
                dispatchEvent(_target, event);
                _chainTime = when + CLICK_CHAIN_INTERVAL;
            }
        }

        protected long _clickTime, _chainTime;
        protected int _count;
        protected Component _target;
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
         * Dispatches a key repeat event if appropriate.
         */
        public void maybeRepeat ()
        {
            if (_tickStamp < _nextRepeat) {
                return;
            }
            KeyEvent event = new KeyEvent(
                Root.this, _tickStamp, _modifiers, KeyEvent.KEY_PRESSED,
                _press.getKeyChar(), _press.getKeyCode());
            dispatchEvent(getFocus(), event);
            _nextRepeat += 1000L / KEY_REPEAT_RATE;
        }

        protected KeyEvent _press;
        protected long _nextRepeat;
    }

    protected GlContext _ctx;

    protected Batch _batch = new Batch() {
        public boolean draw (Renderer renderer) {
            render(renderer);
            return false;
        }
    };

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
    protected ArrayList<Tickable> _tickParticipants = new ArrayList<Tickable>();
    protected ArrayList<EventListener> _globals = new ArrayList<EventListener>();

    protected ArrayList<Component> _invalidRoots = new ArrayList<Component>();

    /** Mouse button information. */
    protected ButtonRecord[] _buttons = new ButtonRecord[] {
        new ButtonRecord(), new ButtonRecord(), new ButtonRecord() };

    /** Keys currently pressed, mapped by key code. */
    protected HashIntMap<KeyRecord> _pressedKeys = new HashIntMap<KeyRecord>();

    protected static final float TIP_MODE_RESET = 0.6f;

    /** Mouse buttons released within this interval after being pressed are counted as clicks. */
    protected static final long CLICK_INTERVAL = 250L;

    /** Clicks this close to one another are chained together for counting purposes. */
    protected static final long CLICK_CHAIN_INTERVAL = 250L;

    /** The key press repeat rate. */
    protected static final int KEY_REPEAT_RATE = 25;

    /** The delay in milliseconds before auto-repeated key presses will begin. */
    protected static final long KEY_REPEAT_DELAY = 500L;
}
