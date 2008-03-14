//
// $Id$

package com.threerings.opengl.gui;

import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.border.Border;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.ComponentListener;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.text.HTMLView;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * The basic entity in the UI user interface system. A hierarchy of components and component
 * derivations make up a user interface.
 */
public class Component
{
    /** The default component state. This is used to select the component's style pseudoclass among
     * other things. */
    public static final int DEFAULT = 0;

    /** A component state indicating that the mouse is hovering over the component. This is used to
     * select the component's style pseudoclass among other things. */
    public static final int HOVER = 1;

    /** A component state indicating that the component is disabled. This is used to select the
     * component's style pseudoclass among other things. */
    public static final int DISABLED = 2;

    /**
     * Configures this component with a custom stylesheet class. By default a component's class is
     * defined by its component type (label, button, checkbox, etc.) but one can provide custom
     * style information to a component by configuring it with a custom class and defining that
     * class in the applicable stylesheet.
     */
    public void setStyleClass (String styleClass)
    {
        if (isAdded()) {
            System.err.println("Warning: attempt to set style class after component was added to " +
                               "the interface heirarchy [comp=" + this + "].");
            Thread.dumpStack();
        }
        _styleClass = styleClass;
    }

    /**
     * Returns the Style class to be used for this component.
     */
    public String getStyleClass ()
    {
        return (_styleClass == null) ? getDefaultStyleClass() : _styleClass;
    }

    /**
     * Informs this component of its parent in the interface heirarchy.
     */
    public void setParent (Container parent)
    {
        if (_parent != null && parent != null) {
            Log.log.warning("Already added child readded to interface hierarchy! [comp=" + this +
                            ", oparent=" + _parent + ", nparent=" + parent + "].");
            Thread.dumpStack();
        } else if (_parent == null && parent == null) {
            Log.log.warning("Already removed child reremoved from interface hierarchy! " +
                            "[comp=" + this + "].");
            Thread.dumpStack();
        }
        _parent = parent;
    }

    /**
     * Returns the parent of this component in the interface hierarchy.
     */
    public Container getParent ()
    {
        return _parent;
    }

    /**
     * Returns the preferred size of this component, supplying a width and or height hint to the
     * component to inform it of restrictions in one of the two dimensions. Not all components will
     * make use of the hints, but layout managers should provide them if they know the component
     * will be forced to a particular width or height regardless of what it prefers.
     */
    public Dimension getPreferredSize (int whint, int hhint)
    {
        Dimension ps;
        // if we have a fully specified preferred size, just use it
        if (_preferredSize != null && _preferredSize.width != -1 && _preferredSize.height != -1) {
            ps = new Dimension(_preferredSize);

        } else {
            // override hints with preferred size
            if (_preferredSize != null) {
                if (_preferredSize.width > 0) {
                    whint = _preferredSize.width;
                }
                if (_preferredSize.height > 0) {
                    hhint = _preferredSize.height;
                }
            }

            // extract space from the hints for our insets
            Insets insets = getInsets();
            if (whint > 0) {
                whint -= insets.getHorizontal();
            }
            if (hhint > 0) {
                hhint -= insets.getVertical();
            }

            // compute our "natural" preferred size
            ps = computePreferredSize(whint, hhint);

            // now add our insets back in
            ps.width += insets.getHorizontal();
            ps.height += insets.getVertical();

            // then override it with user supplied values
            if (_preferredSize != null) {
                if (_preferredSize.width != -1) {
                    ps.width = _preferredSize.width;
                }
                if (_preferredSize.height != -1) {
                    ps.height = _preferredSize.height;
                }
            }
        }

        // now make sure we're not smaller in either dimension than our
        // background will allow
        Background background = getBackground();
        if (background != null) {
            ps.width = Math.max(ps.width, background.getMinimumWidth());
            ps.height = Math.max(ps.height, background.getMinimumHeight());
        }

        return ps;
    }

    /**
     * Configures the preferred size of this component. This will override any information provided
     * by derived classes that have opinions about their preferred size. Either the width or the
     * height can be configured as -1 in which case the computed preferred size will be used for
     * that dimension.
     */
    public void setPreferredSize (Dimension preferredSize)
    {
        _preferredSize = preferredSize;
    }

    /**
     * Configures the preferred size of this component. See {@link #setPreferredSize(Dimension)}.
     */
    public void setPreferredSize (int width, int height)
    {
        setPreferredSize(new Dimension(width, height));
    }

    /** Returns the x coordinate of this component. */
    public int getX ()
    {
        return _x;
    }

    /** Returns the y coordinate of this component. */
    public int getY ()
    {
        return _y;
    }

    /** Returns the width of this component. */
    public int getWidth ()
    {
        return _width;
    }

    /** Returns the height of this component. */
    public int getHeight ()
    {
        return _height;
    }

    /** Returns the x position of this component in absolute screen coordinates. */
    public int getAbsoluteX ()
    {
        return _x + ((_parent == null) ? 0 : _parent.getAbsoluteX());
    }

    /** Returns the y position of this component in absolute screen coordinates. */
    public int getAbsoluteY ()
    {
        return _y + ((_parent == null) ? 0 : _parent.getAbsoluteY());
    }

    /** Returns the bounds of this component in a new rectangle. */
    public Rectangle getBounds ()
    {
        return new Rectangle(_x, _y, _width, _height);
    }

    /**
     * Returns the insets configured on this component. <code>null</code> will never be returned,
     * an {@link Insets} instance with all fields set to zero will be returned instead.
     */
    public Insets getInsets ()
    {
        Insets insets = _insets[getState()];
        return (insets == null) ? Insets.ZERO_INSETS : insets;
    }

    /**
     * Returns the (foreground) color configured for this component.
     */
    public Color4f getColor ()
    {
        Color4f color = _colors[getState()];
        return (color != null) ? color : _colors[DEFAULT];
    }

    /**
     * Returns our bounds as a nicely formatted string.
     */
    public String boundsToString ()
    {
        return _width + "x" + _height + "+" + _x + "+" + _y;
    }

    /**
     * Returns the currently active border for this component.
     */
    public Border getBorder ()
    {
        Border border = _borders[getState()];
        return (border != null) ? border : _borders[DEFAULT];
    }

    /**
     * Returns a reference to the background used by this component.
     */
    public Background getBackground ()
    {
        Background background = _backgrounds[getState()];
        return (background != null) ? background : _backgrounds[DEFAULT];
    }

    /**
     * Configures the background for this component for the specified state.
     */
    public void setBackground (int state, Background background)
    {
        _backgrounds[state] = background;
    }

    /**
     * Returns a reference to the cursor used by this component.
     */
    public Cursor getCursor ()
    {
        return _cursor;
    }

    /**
     * Configures the cursor for this component.  This must only be called after the component has
     * been added to the interface hierarchy or the value will be overridden by the stylesheet
     * associated with this component.
     */
    public void setCursor (Cursor cursor)
    {
        _cursor = cursor;
    }

    /**
     * Sets the alpha level for this component.
     */
    public void setAlpha (float alpha)
    {
        _alpha = alpha;
    }

    /**
     * Returns the alpha transparency of this component.
     */
    public float getAlpha ()
    {
        return _alpha;
    }

    /**
     * Sets this components enabled state. A component that is not enabled should not respond to
     * user interaction and should render itself in such a way as not to afford user interaction.
     */
    public void setEnabled (boolean enabled)
    {
        if (enabled != _enabled) {
            _enabled = enabled;
            stateDidChange();
        }
    }

    /**
     * Returns true if this component is enabled and responding to user interaction, false if not.
     */
    public boolean isEnabled ()
    {
        return _enabled;
    }

    /**
     * Sets this component's visibility state.  A component that is invisible is not rendered and
     * does not contribute to the layout.
     */
    public void setVisible (boolean visible)
    {
        if (visible != _visible) {
            _visible = visible;
            invalidate();
        }
    }

    /**
     * Returns true if this component is visible, false if it is not.
     */
    public boolean isVisible ()
    {
        return _visible;
    }

    /**
     * Returns true if this component is both added to the interface hierarchy and visible, false
     * if not.
     */
    public boolean isShowing ()
    {
        return isAdded() && isVisible();
    }

    /**
     * Returns the state of this component, either {@link #DEFAULT} or {@link #DISABLED}.
     */
    public int getState ()
    {
        return _enabled ? (_hover ? HOVER : DEFAULT) : DISABLED;
    }

    /**
     * Sets a user defined property on this component. User defined properties allow the
     * association of arbitrary additional data with a component for application specific purposes.
     */
    public void setProperty (String key, Object value)
    {
        if (_properties == null) {
            _properties = new HashMap<String, Object>();
        }
        _properties.put(key, value);
    }

    /**
     * Returns the user defined property mapped to the specified key, or null.
     */
    public Object getProperty (String key)
    {
        return (_properties == null) ? null : _properties.get(key);
    }

    /**
     * Returns whether or not this component accepts the keyboard focus.
     */
    public boolean acceptsFocus ()
    {
        return false;
    }

    /**
     * Returns true if this component has the focus.
     */
    public boolean hasFocus ()
    {
        return isAdded() ? getWindow().getRoot().getFocus() == this : false;
    }

    /**
     * Returns the component that should receive focus if this component is clicked. If this
     * component does not accept focus, its parent will be checked and so on.
     */
    public Component getFocusTarget ()
    {
        if (acceptsFocus()) {
            return this;
        } else if (_parent != null) {
            return _parent.getFocusTarget();
        } else {
            return null;
        }
    }

    /**
     * Requests that this component be given the input focus.
     */
    public void requestFocus ()
    {
        // sanity check
        if (!acceptsFocus()) {
            Log.log.warning("Unfocusable component requested focus: " + this);
            Thread.dumpStack();
            return;
        }

        Window window = getWindow();
        if (window == null) {
            Log.log.warning("Focus requested for un-added component: " + this);
            Thread.dumpStack();
        } else {
            window.requestFocus(this);
        }
    }

    /**
     * Sets the upper left position of this component in absolute screen coordinates.
     */
    public void setLocation (int x, int y)
    {
        setBounds(x, y, _width, _height);
    }

    /**
     * Sets the width and height of this component in screen coordinates.
     */
    public void setSize (int width, int height)
    {
        setBounds(_x, _y, width, height);
    }

    /**
     * Sets the bounds of this component in screen coordinates.
     *
     * @see #setLocation
     * @see #setSize
     */
    public void setBounds (int x, int y, int width, int height)
    {
        if (_x != x || _y != y) {
            _x = x;
            _y = y;
        }
        if (_width != width || _height != height) {
            _width = width;
            _height = height;
            invalidate();
        }
    }

    /**
     * Adds a listener to this component. The listener will be notified when events of the
     * appropriate type are dispatched on this component.
     */
    public void addListener (ComponentListener listener)
    {
        if (_listeners == null) {
            _listeners = new ArrayList<ComponentListener>();
        }
        _listeners.add(listener);
    }

    /**
     * Removes a listener from this component. Returns true if the listener was in fact in the
     * listener list for this component, false if not.
     */
    public boolean removeListener (ComponentListener listener)
    {
        if (_listeners != null) {
            return _listeners.remove(listener);
        } else {
            return false;
        }
    }

    /**
     * Removes all listeners registered on this component.
     */
    public void removeAllListeners ()
    {
    	_listeners.clear();
    }

    /**
     * Configures the tooltip text for this component. If the text starts with &lt;html&gt; then
     * the tooltip will be displayed with an @{link HTMLView} otherwise it will be displayed with a
     * {@link Label}.
     */
    public void setTooltipText (String text)
    {
        _tiptext = text;
    }

    /**
     * Returns the tooltip text configured for this component.
     */
    public String getTooltipText ()
    {
        return _tiptext;
    }

    /**
     * Sets where to position the tooltip window.
     *
     * @param mouse if true, the window will appear relative to the mouse position, if false, the
     * window will appear relative to the component bounds.
     */
    public void setTooltipRelativeToMouse (boolean mouse)
    {
        _tipmouse = mouse;
    }

    /**
     * Returns true if the tooltip window should be position relative to the mouse.
     */
    public boolean isTooltipRelativeToMouse ()
    {
        return _tipmouse;
    }

    /**
     * Returns true if this component is added to a hierarchy of components that culminates in a
     * top-level window.
     */
    public boolean isAdded ()
    {
        Window win = getWindow();
        return (win != null && win.isAdded());
    }

    /**
     * Returns true if this component has been validated and laid out.
     */
    public boolean isValid ()
    {
        return _valid;
    }

    /**
     * Instructs this component to lay itself out and then mark itself as valid.
     */
    public void validate ()
    {
        if (!_valid) {
            if (isVisible()) {
                layout();
            }
            _valid = true;
        }
    }

    /**
     * Marks this component as invalid and needing a relayout. If the component is valid, its
     * parent will also be marked as invalid.
     */
    public void invalidate ()
    {
        if (_valid) {
            _valid = false;
            if (_parent != null) {
                _parent.invalidate();
            }
        }
    }

    /**
     * Translates into the component's coordinate space, renders the background and border and then
     * calls {@link #renderComponent} to allow the component to render itself.
     */
    public void render (Renderer renderer)
    {
        if (!_visible) {
            return;
        }
        GL11.glTranslatef(_x, _y, 0);

        try {
            // render our background
            renderBackground(renderer);

            // render any custom component bits
            renderComponent(renderer);

            // render our border
            renderBorder(renderer);

        } finally {
            GL11.glTranslatef(-_x, -_y, 0);
        }
    }

    /**
     * Returns the component "hit" by the specified mouse coordinates which might be this component
     * or any of its children. This method should return null if the supplied mouse coordinates are
     * outside the bounds of this component.
     */
    public Component getHitComponent (int mx, int my)
    {
        if (isVisible() && (mx >= _x) && (my >= _y) &&
            (mx < _x + _width) && (my < _y + _height)) {
            return this;
        }
        return null;
    }

    /**
     * Instructs this component to process the supplied event. If the event is not processed, it
     * will be passed up to its parent component for processing. Derived classes should thus only
     * call <code>super.dispatchEvent</code> for events that they did not "consume".
     *
     * @return true if this event was consumed, false if not.
     */
    public boolean dispatchEvent (Event event)
    {
        // events that should not be propagated up the hierarchy are marked as processed
        // immediately to avoid sending them to our parent or to other windows
        boolean processed = !event.propagateUpHierarchy();

        // handle focus traversal
        if (event instanceof KeyEvent) {
            KeyEvent kev = (KeyEvent)event;
            if (kev.getType() == KeyEvent.KEY_PRESSED) {
                int modifiers = kev.getModifiers(), keyCode = kev.getKeyCode();
                if (keyCode == Keyboard.KEY_TAB) {
                    if (modifiers == 0) {
                        getWindow().requestFocus(getNextFocus());
                        processed = true;
                    } else if (modifiers == KeyEvent.SHIFT_DOWN_MASK) {
                        getWindow().requestFocus(getPreviousFocus());
                        processed = true;
                    }
                }
            }
        }

        // handle mouse hover detection
        if (_enabled && event instanceof MouseEvent) {
            int ostate = getState();
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_ENTERED:
                _hover = true;
                processed = true;
                break;
            case MouseEvent.MOUSE_EXITED:
                _hover = false;
                processed = true;
                break;
            }

            // update our component state if necessary
            if (getState() != ostate) {
                stateDidChange();
            }
            if (processed && changeCursor()) {
                updateCursor(_cursor);
            }
        }

        // dispatch this event to our listeners
        if (_listeners != null) {
            for (int ii = 0, ll = _listeners.size(); ii < ll; ii++) {
                event.dispatch(_listeners.get(ii));
            }
        }

        // if we didn't process the event, pass it up to our parent
        if (!processed && _parent != null) {
            return getParent().dispatchEvent(event);
        }

        return processed;
    }

    /**
     * Instructs this component to lay itself out. This is called as a result of the component
     * changing size.
     */
    protected void layout ()
    {
        // we have nothing to do by default
    }

    /**
     * Computes and returns a preferred size for this component. This method is called if no
     * overriding preferred size has been supplied.
     *
     * @return the computed preferred size of this component <em>in a newly created Dimension</em>
     * instance which will be adopted (and modified) by the caller.
     */
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(0, 0);
    }

    /**
     * This method is called when we are added to a hierarchy that is connected to a top-level
     * window (at which point we can rely on having a look and feel and can set ourselves up).
     */
    protected void wasAdded ()
    {
        configureStyle(getWindow().getStyleSheet());
    }

    /**
     * Instructs this component to fetch its style configuration from the supplied style
     * sheet. This method is called when a component is added to the interface hierarchy.
     */
    protected void configureStyle (StyleSheet style)
    {
        if (_preferredSize == null) {
            _preferredSize = style.getSize(this, null);
        }

        _cursor = style.getCursor(this, null);
        _tipStyle = style.getTooltipStyle(this, null);
        for (int ii = 0; ii < getStateCount(); ii++) {
            _colors[ii] = style.getColor(this, getStatePseudoClass(ii));
            _insets[ii] = style.getInsets(this, getStatePseudoClass(ii));
            _borders[ii] = style.getBorder(this, getStatePseudoClass(ii));
            if (_borders[ii] != null) {
                _insets[ii] = _borders[ii].adjustInsets(_insets[ii]);
            }
            if (_backgrounds[ii] == null) {
                _backgrounds[ii] = style.getBackground(this, getStatePseudoClass(ii));
            }
        }
    }

    /**
     * This method is called when we are removed from a hierarchy that is connected to a top-level
     * window. If we wish to clean up after things done in {@link #wasAdded}, this is a fine place
     * to do so.
     */
    protected void wasRemoved ()
    {
        // mark ourselves as invalid so that if this component is again added to an interface
        // heirarchy it will revalidate at that time
        _valid = false;
    }

    /**
     * Creates the component that will be used to display our tooltip. This method will only be
     * called if {@link #getTooltipText} returns non-null text.
     */
    protected Component createTooltipComponent (String tiptext)
    {
        if (tiptext.startsWith("<html>")) {
            return new HTMLView("", tiptext);
        } else {
            return new Label(tiptext, _tipStyle);
        }
    }

    /**
     * Renders the background for this component.
     */
    protected void renderBackground (Renderer renderer)
    {
        Background background = getBackground();
        if (background != null) {
            background.render(renderer, 0, 0, _width, _height, _alpha);
        }
    }

    /**
     * Renders the border for this component.
     */
    protected void renderBorder (Renderer renderer)
    {
        Border border = getBorder();
        if (border != null) {
            border.render(renderer, 0, 0, _width, _height, _alpha);
        }
    }

    /**
     * Renders any custom bits for this component. This is called with the graphics context
     * translated to (0, 0) relative to this component.
     */
    protected void renderComponent (Renderer renderer)
    {
    }

    /**
     * Returns the default stylesheet class to be used for all instances of this component. Derived
     * classes will likely want to override this method and set up a default class for their type
     * of component.
     */
    protected String getDefaultStyleClass ()
    {
        return "component";
    }

    /**
     * Returns the number of different states that this component can take.  These states
     * correspond to stylesheet pseudoclasses that allow components to customize their
     * configuration based on whether they are enabled or disabled, or pressed if they are a
     * button, etc.
     */
    protected int getStateCount ()
    {
        return STATE_COUNT;
    }

    /**
     * Returns the pseudoclass identifier for the specified component state.  This string will be
     * the way that the state is identified in the associated stylesheet. For example, the {@link
     * #DISABLED} state maps to <code>disabled</code> and is configured like so:
     *
     * <pre>
     * component:disabled {
     *    color: #CCCCCC; // etc.
     * }
     * </pre>
     */
    protected String getStatePseudoClass (int state)
    {
        return STATE_PCLASSES[state];
    }

    /**
     * Called when the component's state has changed.
     */
    protected void stateDidChange ()
    {
        invalidate();
    }

    /**
     * Returns true if the component should update the mouse cursor.
     */
    protected boolean changeCursor ()
    {
        return _enabled && _visible && _hover;
    }

    /**
     * Updates the mouse cursor with the supplied cursor.
     */
    protected void updateCursor (Cursor cursor)
    {
        if (cursor != null) {
            cursor.show();
        }
    }

    /**
     * Returns the window that defines the root of our component hierarchy.
     */
    protected Window getWindow ()
    {
        if (this instanceof Window) {
            return (Window)this;
        } else if (_parent != null) {
            return _parent.getWindow();
        } else {
            return null;
        }
    }

    /**
     * Searches for the next component that should receive the keyboard focus. If such a component
     * can be found, it will be returned. If no other focusable component can be found and this
     * component is focusable, this component will be returned. Otherwise, null will be returned.
     */
    protected Component getNextFocus ()
    {
        if (_parent instanceof Container) {
            return _parent.getNextFocus(this);
        } else if (acceptsFocus()) {
            return this;
        } else {
            return null;
        }
    }

    /**
     * Searches for the previous component that should receive the keyboard focus. If such a
     * component can be found, it will be returned. If no other focusable component can be found
     * and this component is focusable, this component will be returned. Otherwise, null will be
     * returned.
     */
    protected Component getPreviousFocus ()
    {
        if (_parent instanceof Container) {
            return _parent.getPreviousFocus(this);
        } else if (acceptsFocus()) {
            return this;
        } else {
            return null;
        }
    }

    /**
     * Returns the first descendant of this component that accepts the keyboard focus
     * (including the component itself), or <code>null</code> if no descendants accept it.
     */
    protected Component getFirstDescendantFocus ()
    {
        return acceptsFocus() ? this : null;
    }

    /**
     * Returns the last descendant of this component that accepts the keyboard focus
     * (including the component itself), or <code>null</code> if no descendants accept it.
     */
    protected Component getLastDescendantFocus ()
    {
        return acceptsFocus() ? this : null;
    }

    /**
     * Dispatches an event emitted by this component. The event is given to the root node for
     * processing though in general it will result in an immediate call to {@link #dispatchEvent}
     * with the event.
     *
     * @return true if the event was emitted, false if it was dropped because we are not currently
     * added to the interface hierarchy.
     */
    protected boolean emitEvent (Event event)
    {
        Window window;
        Root root;
        if ((window = getWindow()) == null ||
            (root = window.getRoot()) == null) {
            return false;
        }
        root.dispatchEvent(this, event);
        return true;
    }

    /**
     * Activates scissoring and sets the scissor region to the intersection of the current region
     * (if any) and the specified rectangle.
     *
     * @param store a rectangle to hold the previous scissor region for later restoration
     * @return <code>null</code> if scissoring was disabled, otherwise a reference to the
     * <code>store</code> parameter.
     */
    protected static Rectangle intersectScissor (
        Renderer renderer, Rectangle store, int x, int y, int width, int height)
    {
        Rectangle scissor = renderer.getScissor();
        _rect.set(x, y, width, height);
        if (scissor != null) {
            _rect.intersectLocal(store.set(scissor));
        } else {
            store = null;
        }
        renderer.setScissor(_rect);
        return store;
    }

    protected Container _parent;
    protected String _styleClass;
    protected Dimension _preferredSize;
    protected int _x, _y, _width, _height;
    protected ArrayList<ComponentListener> _listeners;
    protected HashMap<String, Object> _properties;
    protected String _tiptext;
    protected String _tipStyle;
    protected boolean _tipmouse;

    protected boolean _valid, _enabled = true, _visible = true, _hover;
    protected float _alpha = 1f;

    protected Color4f[] _colors = new Color4f[getStateCount()];
    protected Insets[] _insets = new Insets[getStateCount()];
    protected Border[] _borders = new Border[getStateCount()];
    protected Background[] _backgrounds = new Background[getStateCount()];
    protected Cursor _cursor;

    /** Temporary storage for scissor box. */
    protected static Rectangle _rect = new Rectangle();

    protected static final int STATE_COUNT = 3;
    protected static final String[] STATE_PCLASSES = { null, "hover", "disabled" };
}
