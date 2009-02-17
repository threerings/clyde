//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.util.ArrayList;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.layout.LayoutManager;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

import static com.threerings.opengl.gui.Log.*;

/**
 * A user interface element that is meant to contain other interface
 * elements.
 */
public class Container extends Component
{
    /**
     * Creates a container with no layout manager. One should subsequently
     * be set via a call to {@link #setLayoutManager}.
     */
    public Container (GlContext ctx)
    {
        super(ctx);
    }

    /**
     * Creates a container with the supplied layout manager.
     */
    public Container (GlContext ctx, LayoutManager layout)
    {
        super(ctx);
        setLayoutManager(layout);
    }

    /**
     * Configures this container with an entity that will set the size and
     * position of its children.
     */
    public void setLayoutManager (LayoutManager layout)
    {
        _layout = layout;
    }

    /**
     * Returns the layout manager configured for this container.
     */
    public LayoutManager getLayoutManager ()
    {
        return _layout;
    }

    /**
     * Adds a child to this container.
     */
    public void add (Component child)
    {
        add(child, null);
    }

    /**
     * Adds a child to this container at the specified position.
     */
    public void add (int index, Component child)
    {
        add(index, child, null);
    }

    /**
     * Adds a child to this container with the specified layout
     * constraints.
     */
    public void add (Component child, Object constraints)
    {
        add(_children.size(), child, constraints);
    }

    /**
     * Adds a child to this container at the specified position, with the
     * specified layout constraints.
     */
    public void add (int index, Component child, Object constraints)
    {
        if (_layout != null) {
            _layout.addLayoutComponent(child, constraints);
        }
        _children.add(index, child);
        child.setParent(this);

        // if we're already part of the hierarchy, call wasAdded() on our
        // child; otherwise when our parent is added, everyone will have
        // wasAdded() called on them
        if (isAdded()) {
            child.wasAdded();
        }

        // we need to be relayed out
        invalidate();
    }

    /**
     * Removes the child at a specific position from this container.
     */
    public void remove (int index)
    {
    	Component child = getComponent(index);
	    _children.remove(index);
        if (_layout != null) {
            _layout.removeLayoutComponent(child);
        }
        child.setParent(null);

        // if we're part of the hierarchy we call wasRemoved() on the
        // child now (which will be propagated to all of its children)
        if (isAdded()) {
            child.wasRemoved();
        }

        // we need to be relayed out
        invalidate();
    }

    /**
     * Replaces a given old component with a new component (if the old component exits).
     *
     * @return true if the old component was replaced, false otherwise.
     */
    public boolean replace (Component oldc, Component newc)
    {
        int idx = _children.indexOf(oldc);
        if (idx >= 0) {
            Object constraints = (_layout == null) ? null : _layout.getConstraints(oldc);
            remove(idx);
            add(idx, newc, constraints);
            return true;
        }
        return false;
    }

    /**
     * Removes the specified child from this container.
     */
    public void remove (Component child)
    {
        if (!_children.remove(child)) {
            // if the component was not our child, stop now
            return;
        }
        if (_layout != null) {
            _layout.removeLayoutComponent(child);
        }
        child.setParent(null);

        // if we're part of the hierarchy we call wasRemoved() on the
        // child now (which will be propagated to all of its children)
        if (isAdded()) {
            child.wasRemoved();
        }

        // we need to be relayed out
        invalidate();
    }

    /**
     * Returns the number of components contained in this container.
     */
    public int getComponentCount ()
    {
        return _children.size();
    }

    /**
     * Returns the <code>index</code>th component from this container.
     */
    public Component getComponent (int index)
    {
        return _children.get(index);
    }

    /**
     * Returns the index of the specified component in this container or -1 if the component count
     * not be found.
     */
    public int getComponentIndex (Component component)
    {
        return _children.indexOf(component);
    }

    /**
     * Removes all children of this container.
     */
    public void removeAll ()
    {
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            remove(getComponent(ii));
        }
    }

    // documentation inherited
    public void setAlpha (float alpha)
    {
        super.setAlpha(alpha);

        // set our children's alpha values accordingly
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            getComponent(ii).setAlpha(alpha);
        }
    }

    // documentation inherited
    public void setEnabled (boolean enabled)
    {
        super.setEnabled(enabled);

        // enable or disable our children accordingly
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            getComponent(ii).setEnabled(enabled);
        }
    }

    // documentation inherited
    public Component getHitComponent (int mx, int my)
    {
        // if we're not within our bounds, we don't need to check our children
        if (super.getHitComponent(mx, my) != this) {
            return null;
        }

        // translate the coordinate into our children's coordinates
        mx -= _x;
        my -= _y;

        Component hit = null;
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            Component child = getComponent(ii);
            if ((hit = child.getHitComponent(mx, my)) != null) {
                return hit;
            }
        }
        return this;
    }

    // documentation inherited
    public void validate ()
    {
        if (!_valid) {
            layout(); // lay ourselves out

            // now validate our children
            applyOperation(new ChildOp() {
                public void apply (Component child) {
                    child.validate();
                }
            });

            _valid = true; // finally mark ourselves as valid
        }
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/Container";
    }

    // documentation inherited
    protected void layout ()
    {
        if (_layout != null) {
            _layout.layoutContainer(this);
        }
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        // render our children
        for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
            getComponent(ii).render(renderer);
        }
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        if (_layout != null) {
            return _layout.computePreferredSize(this, whint, hhint);
        } else {
            return super.computePreferredSize(whint, hhint);
        }
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // call wasAdded() on all of our existing children; if they are added later (after we are
        // added), they will automatically have wasAdded() called on them at that time
        applyOperation(new ChildOp() {
            public void apply (Component child) {
                child.wasAdded();
            }
        });
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // call wasRemoved() on all of our children
        applyOperation(new ChildOp() {
            public void apply (Component child) {
                child.wasRemoved();
            }
        });
    }

    /**
     * Returns the next component that should receive focus in this container given the current
     * focus owner. If the supplied current focus owner is null, the container should return its
     * first focusable component. If the container has no focusable components following the
     * current focus, it should call {@link #getNextFocus()} to search further up the hierarchy.
     */
    protected Component getNextFocus (Component current)
    {
        int idx = getComponentIndex(current);
        Component focus;
        for (int ii = idx + 1, nn = getComponentCount(); ii < nn; ii++) {
            if ((focus = getComponent(ii).getFirstDescendantFocus()) != null) {
                return focus;
            }
        }
        return getNextFocus();
    }

    /**
     * Returns the previous component that should receive focus in this container given the current
     * focus owner. If the supplied current focus owner is null, the container should return its
     * last focusable component. If the container has no focusable components before the current
     * focus, it should call {@link #getPreviousFocus()} to search further up the hierarchy.
     */
    protected Component getPreviousFocus (Component current)
    {
        int idx = (current == null) ? getComponentCount() : getComponentIndex(current);
        Component focus;
        for (int ii = idx - 1; ii >= 0; ii--) {
            if ((focus = getComponent(ii).getLastDescendantFocus()) != null) {
                return focus;
            }
        }
        return getPreviousFocus();
    }

    @Override // documentation inherited
    protected Component getFirstDescendantFocus ()
    {
        if (acceptsFocus()) {
            return this;
        }
        for (int ii = 0, nn = getComponentCount(); ii < nn; ii++) {
            Component desc = getComponent(ii).getFirstDescendantFocus();
            if (desc != null) {
                return desc;
            }
        }
        return null;
    }

    @Override // documentation inherited
    protected Component getLastDescendantFocus ()
    {
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            Component desc = getComponent(ii).getLastDescendantFocus();
            if (desc != null) {
                return desc;
            }
        }
        return super.getLastDescendantFocus();
    }

    /**
     * Applies an operation to all of our children.
     */
    protected void applyOperation (ChildOp op)
    {
        for (Component child : _children) {
            try {
                op.apply(child);
            } catch (Exception e) {
                log.warning("Child operation choked.", "op", op, "child", child, e);
            }
        }
    }

    /** Used in {@link #wasAdded} and {@link #wasRemoved}. */
    protected static interface ChildOp
    {
        public void apply (Component child);
    }

    protected ArrayList<Component> _children = new ArrayList<Component>();
    protected LayoutManager _layout;
}
