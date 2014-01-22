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

import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.Objects;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.config.ManagedConfig;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.border.Border;
import com.threerings.opengl.gui.config.CursorConfig;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.ComponentListener;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.text.HTMLView;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

import static com.threerings.opengl.gui.Log.log;

/**
 * The basic entity in the UI user interface system. A hierarchy of components and component
 * derivations make up a user interface.
 */
public class Component
    implements ConfigUpdateListener<ManagedConfig>, Validateable
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
     * Creates a tooltip component of the default form with the default style.
     */
    public static Component createDefaultTooltipComponent (GlContext ctx, String tiptext)
    {
        return createDefaultTooltipComponent(ctx, tiptext, null);
    }

    /**
     * Creates a tooltip component of the default form.
     */
    public static Component createDefaultTooltipComponent (
        GlContext ctx, String tiptext, ConfigReference<StyleConfig> tipstyle)
    {
        if (tiptext.startsWith("<html>")) {
            return new HTMLView(ctx, "", tiptext);
        } else {
            Label label = new Label(ctx, tiptext);
            label.setStyleConfig(tipstyle);
            return label;
        }
    }

    /**
     * Creates a new component.
     */
    public Component (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Returns a reference to the component context.
     */
    public GlContext getContext ()
    {
        return _ctx;
    }

    /**
     * Sets the style configuration.
     */
    public void setStyleConfig (String name)
    {
        setStyleConfig(new ConfigReference<StyleConfig>(name));
    }

    /**
     * Sets the style configuration.
     */
    public void setStyleConfig (
        String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        setStyleConfig(new ConfigReference<StyleConfig>(name, firstKey, firstValue, otherArgs));
    }

    /**
     * Sets the style configuration.
     */
    public void setStyleConfig (ConfigReference<StyleConfig> ref)
    {
        if (ref == null) {
            ref = new ConfigReference<StyleConfig>(getDefaultStyleConfig());
        }
        StyleConfig[] styleConfigs = new StyleConfig[_styleConfigs.length];
        styleConfigs[0] = _ctx.getConfigManager().getConfig(StyleConfig.class, ref);
        String name = ref.getName();
        ArgumentMap args = ref.getArguments();
        for (int ii = 1; ii < styleConfigs.length; ii++) {
            styleConfigs[ii] = _ctx.getConfigManager().getConfig(
                StyleConfig.class, name + ":" + getStatePseudoClass(ii), args);
        }
        setStyleConfigs(styleConfigs);
    }

    /**
     * Sets the style configurations.
     */
    public void setStyleConfigs (StyleConfig... styleConfigs)
    {
        for (int ii = 0; ii < _styleConfigs.length; ii++) {
            StyleConfig oconfig = _styleConfigs[ii];
            StyleConfig nconfig = (ii < styleConfigs.length) ? styleConfigs[ii] : null;
            if (oconfig == nconfig && oconfig != null) {
                continue;
            }
            if (oconfig != null) {
                oconfig.removeListener(this);
            }
            if (nconfig == null) {
                if (ii == DEFAULT) {
                    nconfig = _ctx.getConfigManager().getConfig(
                        StyleConfig.class, getDefaultStyleConfig());
                } else {
                    nconfig = _styleConfigs[getFallbackState(ii)];
                }
            }
            if ((_styleConfigs[ii] = nconfig) != null) {
                // make sure we're not already listening
                nconfig.removeListener(this);
                nconfig.addListener(this);
            }
            updateFromStyleConfig(ii);
        }
    }

    /**
     * Returns a reference to the component's array of style configs.
     */
    public StyleConfig[] getStyleConfigs ()
    {
        return _styleConfigs;
    }

    /**
     * Returns a reference to the component's tooltip window style config.
     */
    public String getTooltipWindowStyle ()
    {
        return _tooltip.getWindowStyle();
    }

    /**
     * Informs this component of its parent in the interface heirarchy.
     */
    public void setParent (Container parent)
    {
        if (_parent != null && parent != null) {
            Log.log.warning("Already added child readded to interface hierarchy! [comp=" + this +
                            ", oparent=" + _parent + ", nparent=" + parent + "].", new Exception());
        } else if (_parent == null && parent == null) {
            Log.log.warning("Already removed child reremoved from interface hierarchy! " +
                            "[comp=" + this + "].", new Exception());
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
     * Return an Iterable over our parent, its parent, and so on.
     * Nulls will not be returned.
     */
    public Iterable<Container> getParents ()
    {
        return new Iterable<Container>() {
            public Iterator<Container> iterator () {
                return new AbstractIterator<Container>() {
                    Component c = Component.this;
                    protected Container computeNext () {
                        Container p = c.getParent();
                        if (p == null) {
                            return endOfData();
                        }
                        c = p;
                        return p;
                    }
                };
            }
        };
    }

    /**
     * Returns a live List "view" of the *direct* children of this Component.
     */
    public List<Component> getChildren ()
    {
        return ImmutableList.of();
    }

    /**
     * Returns an Iterable over our children, and their children, and so on.
     */
    public Iterable<Component> getDescendants ()
    {
        // Note: Container's implementation of this method would work here too
        // (as long as it replaced _children with getChildren()).
        // This is an optimized implementation for Component.
        return ImmutableList.of();
    }

    /**
     * Returns an Iterable starting with this Component and continuing as with getParents().
     */
    public Iterable<Component> getUpwards ()
    {
        return Iterables.concat(ImmutableList.of(this), getParents());
    }

    /**
     * Returns an Iterable starting with this Component and continuing as with getDescendants().
     */
    public Iterable<Component> getDownwards ()
    {
        // Note: Container's implementation of this method would work here too.
        // This is an optimized implementation for Component.
        return ImmutableList.of(this);
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
        if (_cursor == cursor) {
            return;
        }
        _cursor = cursor;
        if (isAdded() && changeCursor() && _hover) {
            updateCursor(_cursor);
        }
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

            // make sure we no longer have the input focus
            if (!visible && hasFocus()) {
                getWindow().getRoot().requestFocus(null);
            }
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
     * Sets this component's hoverability state.
     */
    public void setHoverable (boolean hoverable)
    {
        if (_hoverable != hoverable) {
            _hoverable = hoverable;
            if (!hoverable && isAdded()) {
                getWindow().getRoot().hoverabilityDeactivated(this);
            }
        }
    }

    /**
     * Returns this component's hoverability state.
     */
    public boolean isHoverable ()
    {
        return _hoverable;
    }

    /**
     * Returns true if this component is added to the interface hierarchy, visible, and all of its
     * parents up the hierarchy are also visible.
     */
    public boolean isShowing ()
    {
        if (!isAdded()) {
            return false;
        }
        for (Component c = this; c != null; c = c.getParent()) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
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
        return isAdded() && (getWindow().getRoot().getFocus() == this);
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
            Log.log.warning("Unfocusable component requested focus: " + this, new Exception());
            return;
        }

        Window window = getWindow();
        if (window == null) {
            Log.log.warning("Focus requested for un-added component: " + this, new Exception());
        } else {
            window.requestFocus(this);
        }
    }

    /**
     * Sets the upper left position of this component in absolute screen coordinates.
     */
    public final void setLocation (int x, int y)
    {
        setBounds(x, y, _width, _height);
    }

    /**
     * Sets the width and height of this component in screen coordinates.
     */
    public final void setSize (int width, int height)
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
     * Sets the transformation offset reference.  This is only used for rendering; it does not
     * affect the component's behavior.
     */
    public void setOffset (Transform2D offset)
    {
        _offset = offset;
    }

    /**
     * Returns the transformation offset reference.
     */
    public Transform2D getOffset ()
    {
        return _offset;
    }

    /**
     * Adds a listener to this component. The listener will be notified when events of the
     * appropriate type are dispatched on this component.
     */
    public void addListener (ComponentListener listener)
    {
        if (_listeners == null) {
            _listeners = new CopyOnWriteArrayList<ComponentListener>();
        }
        _listeners.add(listener);
    }

    /**
     * Removes a listener from this component. Returns true if the listener was in fact in the
     * listener list for this component, false if not.
     */
    public boolean removeListener (ComponentListener listener)
    {
        return (_listeners != null) && _listeners.remove(listener);
    }

    /**
     * Removes all listeners registered on this component.
     */
    public void removeAllListeners ()
    {
        _listeners = null;
    }

    /**
     * Removes all listeners of the specified class.
     */
    public void removeAllListeners (Class<? extends ComponentListener> clazz)
    {
        if (_listeners == null) {
            return;
        }
        for (int ii = _listeners.size() - 1; ii >= 0; ii--) {
            if (clazz.isInstance(_listeners.get(ii))) {
                _listeners.remove(ii);
            }
        }
    }

    /**
     * Configures the tooltip text for this component. If the text starts with &lt;html&gt; then
     * the tooltip will be displayed with an @{link HTMLView} otherwise it will be displayed with a
     * {@link Label}.
     */
    public void setTooltipText (String text)
    {
        _tooltip.setText(text);
    }

    /**
     * Returns the tooltip text configured for this component.
     */
    public String getTooltipText ()
    {
        return _tooltip.getText();
    }

    /**
     * Sets where to position the tooltip window.
     *
     * @param mouse if true, the window will appear relative to the mouse position, if false, the
     * window will appear relative to the component bounds.
     */
    public void setTooltipRelativeToMouse (boolean mouse)
    {
        _tooltip.setRelativeToMouse(mouse);
    }

    /**
     * Returns true if the tooltip window should be position relative to the mouse.
     */
    public boolean isTooltipRelativeToMouse ()
    {
        return _tooltip.isRelativeToMouse();
    }

    /**
     * Returns the component's tooltip timeout, or -1 to use the default.
     */
    public float getTooltipTimeout ()
    {
        return _tooltip.getTimeout();
    }

    /**
     * Returns the tooltip.
     */
    public Tooltip getTooltip ()
    {
        return _tooltip;
    }

    /**
     * Sets the tooltip.
     */
    public void setTooltip (Tooltip tooltip)
    {
        _tooltip = tooltip;
    }

    /**
     * Sets the transfer handler for this component.
     */
    public void setTransferHandler (TransferHandler handler)
    {
        _transferHandler = handler;
    }

    /**
     * Returns the transfer handler for this component.  If this component has no handler of its
     * own, then the request will be forwarded to the parent, and so on.
     */
    public TransferHandler getTransferHandler ()
    {
        return (_transferHandler == null && _parent != null) ?
            _parent.getTransferHandler() : _transferHandler;
    }

    @Override // from Validateable
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

    @Override // from Validateable
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
        if (_offset != null) {
            GL11.glPushMatrix();
            applyTransform();
        } else {
            GL11.glTranslatef(_x, _y, 0);
        }

        try {
            // render our background
            renderBackground(renderer);

            // render any custom component bits
            renderComponent(renderer);

            // render our border
            renderBorder(renderer);

        } finally {
            if (_offset != null) {
                GL11.glPopMatrix();
            } else {
                GL11.glTranslatef(-_x, -_y, 0);
            }
        }
    }

    /**
     * Returns the component "hit" by the specified mouse coordinates which might be this component
     * or any of its children. This method should return null if the supplied mouse coordinates are
     * outside the bounds of this component.
     */
    public Component getHitComponent (int mx, int my)
    {
        return (isVisible() && _hoverable && contains(mx, my)) ? this : null;
    }

    /**
     * Determines whether this component contains the specified coordinates.
     */
    public boolean contains (int mx, int my)
    {
        return (mx >= _x) && (my >= _y) && (mx < _x + _width) && (my < _y + _height);
    }

    /**
     * Request to have the specified rectangle, in this component's coordinate space,
     * scrolled into view. This request will be repeatedly forwarded to parent containers
     * until one can handle the request. If no ScrollPane's viewport contains this component
     * then this request is unlikely to do anything.
     */
    public final void scrollRectToVisible (Rectangle rect)
    {
        // this method is final, we always defer to the 4-arg version
        scrollRectToVisible(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Request to have the specified rectangle, in this component's coordinate space,
     * scrolled into view. This request will be repeatedly forwarded to parent containers
     * until one can handle the request. If no ScrollPane's viewport contains this component
     * then this request is unlikely to do anything.
     */
    public void scrollRectToVisible (int x, int y, int w, int h)
    {
        if (_parent != null) {
            _parent.scrollRectToVisible(x + _x, y + _y, w, h);
        }
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
                if (keyCode == Keyboard.KEY_TAB && getWindow() != null) {
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
        if (event instanceof MouseEvent) {
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
                updateCursor(_hover ? _cursor : null);
            }
        }

        // dispatch this event to our listeners
        if (_listeners != null) {
            for (ComponentListener listener : _listeners) {
                event.dispatch(listener);
            }
        }

        // if we didn't process the event, pass it up to our parent
        if (!processed && _parent != null) {
            return getParent().dispatchEvent(event);
        }

        return processed;
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        StyleConfig config = (StyleConfig)event.getConfig();
        for (int ii = 0; ii < _styleConfigs.length; ii++) {
            if (_styleConfigs[ii] == config) {
                updateFromStyleConfig(ii);
            }
        }
    }

    @Override
    public final String toString ()
    {
        // DO NOT remove the 'final'. If you want to add information, override toStringHelper
        // and add fields. You're welcome.
        return toStringHelper().toString();
    }

    /**
     * Helper method for toStringing.
     */
    protected Objects.ToStringHelper toStringHelper ()
    {
        return Objects.toStringHelper(this);
    }

    /**
     * Updates the component's style from the style configs.
     */
    protected void updateFromStyleConfig (int state)
    {
        // resolve the underlying implementation
        StyleConfig config = _styleConfigs[state];
        StyleConfig.Original original = (config == null) ? null : config.getOriginal(_ctx);
        updateFromStyleConfig(state, original == null ? StyleConfig.NULL_ORIGINAL : original);
        invalidate();
    }

    /**
     * Updates from the resolved style config.
     */
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        if (state == DEFAULT) {
            _preferredSize = (config.size == null) ?
                _preferredSize : config.size.createDimension();
            _tooltip.setStyle(config.tooltipStyle);
            CursorConfig cconfig = _ctx.getConfigManager().getConfig(
                CursorConfig.class, config.cursor);
            _cursor = (cconfig == null) ? null : cconfig.getCursor(_ctx);
        }
        _colors[state] = config.color;
        _insets[state] = config.padding.createInsets();
        _borders[state] = (config.border == null) ? null : config.border.getBorder();
        if (_borders[state] != null) {
            _insets[state] = _borders[state].adjustInsets(_insets[state]);
        }
        _backgrounds[state] = (config.background == null) ?
            null : config.background.getBackground(_ctx);
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
        // if we don't have a style yet, use the default
        if (_styleConfigs[DEFAULT] == null) {
            setStyleConfig(getDefaultStyleConfig());
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
        return _tooltip.createComponent(_ctx, tiptext);
    }

    /**
     * Applies our configured transform.
     */
    protected void applyTransform ()
    {
        // normal translation
        float hwidth = _width/2f, hheight = _height/2f;
        GL11.glTranslatef(_x + hwidth, _y + hheight, 0f);

        // offset transform
        int type = _offset.getType();
        if (type != Transform2D.IDENTITY) {
            if (type == Transform2D.AFFINE || type == Transform2D.GENERAL) {
                log.warning("Unsupported offset transform type.", "offset", _offset);

            } else { // type == Transform2D.RIGID || type == Transform2D.UNIFORM
                Vector2f translation = _offset.getTranslation();
                GL11.glTranslatef(translation.x, translation.y, 0f);
                GL11.glRotatef(FloatMath.toDegrees(_offset.getRotation()), 0f, 0f, 1f);
                if (type == Transform2D.UNIFORM) {
                    float scale = _offset.getScale();
                    GL11.glScalef(scale, scale, 1f);
                }
            }
        }

        // centering translation
        GL11.glTranslatef(-hwidth, -hheight, 0f);
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
     * Returns the name of the default config to be used for all instances of this component.
     * Derived classes will likely want to override this method and set up a default config for
     * their type of component.
     */
    protected String getDefaultStyleConfig ()
    {
        return "Default/Component";
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
     * Returns the fallback to use for the specified state if no explicit style was given.
     */
    protected int getFallbackState (int state)
    {
        return DEFAULT;
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
        return _enabled && _visible;
    }

    /**
     * Updates the mouse cursor with the supplied cursor.
     */
    protected void updateCursor (Cursor cursor)
    {
        if (isAdded()) {
            getWindow().getRoot().setCursor(cursor);
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
        return (_parent == null) ? getFirstDescendantFocus() : _parent.getNextFocus(this);
    }

    /**
     * Searches for the previous component that should receive the keyboard focus. If such a
     * component can be found, it will be returned. If no other focusable component can be found
     * and this component is focusable, this component will be returned. Otherwise, null will be
     * returned.
     */
    protected Component getPreviousFocus ()
    {
        return (_parent == null) ? getLastDescendantFocus() : _parent.getPreviousFocus(this);
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
        _rect.width = Math.max(_rect.width, 0);
        _rect.height = Math.max(_rect.height, 0);
        renderer.setScissor(_rect);
        return store;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The component's style configurations for each state. */
    protected StyleConfig[] _styleConfigs = new StyleConfig[getStateCount()];

    /** Our tooltip details. */
    protected Tooltip _tooltip = new Tooltip();

    protected Container _parent;
    protected Dimension _preferredSize;
    protected int _x, _y, _width, _height;
    protected CopyOnWriteArrayList<ComponentListener> _listeners;
    protected HashMap<String, Object> _properties;

    protected boolean _valid, _enabled = true, _visible = true, _hoverable = true, _hover;
    protected float _alpha = 1f;

    protected Color4f[] _colors = new Color4f[getStateCount()];
    protected Insets[] _insets = new Insets[getStateCount()];
    protected Border[] _borders = new Border[getStateCount()];
    protected Background[] _backgrounds = new Background[getStateCount()];
    protected Cursor _cursor;

    /** Handler for data transfer operations. */
    protected TransferHandler _transferHandler;

    /** Optional transformation offset. */
    protected Transform2D _offset;

    /** Temporary storage for scissor box. */
    protected static Rectangle _rect = new Rectangle();

    protected static final int STATE_COUNT = 3;
    protected static final String[] STATE_PCLASSES = { null, "Hover", "Disabled" };
}
