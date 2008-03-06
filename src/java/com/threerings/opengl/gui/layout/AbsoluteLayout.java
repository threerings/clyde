//
// $Id$

package com.threerings.opengl.gui.layout;

import java.util.HashMap;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Container;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Point;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Lays out components at absolute coordinate and with (optional) absolute
 * sizes. <em>Note:</em> the components are laid out in a coordinate system
 * defined from the inside of the insets of the container rather than from the
 * very edge of the container.
 */
public class AbsoluteLayout extends LayoutManager
{
    public AbsoluteLayout ()
    {
        this(false);
    }

    /**
     * @param flipped If true, will treat the y coordinates as 0 for the top
     * and height for the bottom.
     */
    public AbsoluteLayout (boolean flipped)
    {
        _flipped = flipped;
    }

    // documentation inherited
    public void addLayoutComponent (Component comp, Object constraints)
    {
        // various sanity checking
        if (constraints instanceof Point) {
            Point p = (Point)constraints;
            if (p.x < 0 || p.y < 0) {
                throw new IllegalArgumentException(
                    "Components must be laid out at positive coords: " + p);
            }

        } else if (constraints instanceof Rectangle) {
            Rectangle r = (Rectangle)constraints;
            if (r.x < 0 || r.y < 0) {
                throw new IllegalArgumentException(
                    "Components must be laid out at positive coords: " + r);
            }
            if (r.width < 0 || r.height < 0) {
                throw new IllegalArgumentException(
                    "Constraints must specify positive dimensions: " + r);
            }

        } else {
            throw new IllegalArgumentException(
                "Components must be added to an AbsoluteLayout with " +
                "Point or Rectangle constraints.");
        }

        _spots.put(comp, constraints);
    }

    // documentation inherited
    public void removeLayoutComponent (Component comp)
    {
        _spots.remove(comp);
    }

    // documentation inherited
    public Dimension computePreferredSize (
        Container target, int whint, int hhint)
    {
        // determine the largest rectangle that contains all of the components
        Rectangle rec = new Rectangle();
        for (int ii = 0, cc = target.getComponentCount(); ii < cc; ii++) {
            Component comp = target.getComponent(ii);
            if (!comp.isVisible()) {
                continue;
            }
            Object cons = _spots.get(comp);
            if (cons instanceof Point) {
                Point p = (Point)cons;
                Dimension d = comp.getPreferredSize(-1, -1);
                rec.add(p.x, p.y, d.width, d.height);
            } else if (cons instanceof Rectangle) {
                Rectangle r = (Rectangle)cons;
                rec.add(r.x, r.y, r.width, r.height);
            }
        }
        return new Dimension(rec.x + rec.width, rec.y + rec.height);
    }

    // documentation inherited
    public void layoutContainer (Container target)
    {
        Insets insets = target.getInsets();
        int height = target.getHeight();
        for (int ii = 0, cc = target.getComponentCount(); ii < cc; ii++) {
            Component comp = target.getComponent(ii);
            if (!comp.isVisible()) {
                continue;
            }
            Object cons = _spots.get(comp);
            if (cons instanceof Point) {
                Point p = (Point)cons;
                Dimension d = comp.getPreferredSize(-1, -1);
                comp.setBounds(insets.left + p.x,
                        (_flipped ? height - d.height - insets.top - p.y :
                                   insets.bottom + p.y), d.width, d.height);
            } else if (cons instanceof Rectangle) {
                Rectangle r = (Rectangle)cons;
                comp.setBounds(insets.left + r.x,
                        (_flipped ? height - r.height - insets.top - r.y :
                                   insets.bottom + r.y), r.width, r.height);
            }
        }
    }

    protected boolean _flipped;
    protected HashMap<Component, Object> _spots =
        new HashMap<Component, Object>();
}
