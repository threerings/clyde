//
// $Id$

package com.threerings.opengl.gui.layout;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Container;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Handles horizontally laid out groups.
 *
 * @see GroupLayout
 */
public class HGroupLayout extends GroupLayout
{
    // documentation inherited
    public Dimension computePreferredSize (Container target, int whint, int hhint)
    {
        DimenInfo info = computeDimens(target, true, whint, hhint);
        Dimension dims = new Dimension();

        if (_policy == STRETCH) {
            dims.width = info.maxfreewid * (info.count - info.numfix) + info.fixwid;
        } else if (_policy == EQUALIZE) {
            dims.width = info.maxwid * info.count;
        } else { // NONE or CONSTRAIN
            dims.width = info.totwid;
        }

        dims.width += (info.count - 1) * _gap;
        dims.height = info.maxhei;

        return dims;
    }

    // documentation inherited
    public void layoutContainer (Container target)
    {
        // adjust the bounds width and height to account for the insets
        Rectangle b = target.getBounds();
        Insets insets = target.getInsets();
        b.width -= insets.getHorizontal();
        b.height -= insets.getVertical();

        DimenInfo info = computeDimens(target, true, b.width, b.height);
        int nk = target.getComponentCount();
        int sx, sy;
        int totwid, totgap = _gap * (info.count-1);
        int freecount = info.count - info.numfix;

        // when stretching, there is the possibility that a pixel or more will be lost to rounding
        // error. we account for that here and assign the extra space to the first free component
        int freefrac = 0;

        // do the on-axis policy calculations
        int defwid = 0;
        float conscale = 1f;
        if (_policy == STRETCH) {
            if (freecount > 0) {
                int freewid = b.width - info.fixwid - totgap;
                defwid = freewid / freecount;
                freefrac = freewid % freecount;
                totwid = b.width;
            } else {
                totwid = info.fixwid + totgap;
            }

        } else if (_policy == EQUALIZE) {
            defwid = info.maxwid;
            totwid = info.fixwid + defwid * freecount + totgap;

        } else if (_policy == CONSTRAIN) {
            totwid = info.totwid + totgap;
            // if we exceed the width available, we must constrain
            if (totwid > b.width) {
                conscale = (b.width-totgap) / (float)info.totwid;
                totwid = b.width;
            }

        } else { // NONE
            totwid = info.totwid + totgap;
        }

        // do the off-axis policy calculations
        int defhei = 0;
        if (_offpolicy == STRETCH) {
            defhei = b.height;
        } else if (_offpolicy == EQUALIZE) {
            defhei = info.maxhei;
        }

        // do the justification-related calculations
        if (_justification == LEFT || _justification == TOP) {
            sx = insets.left;
        } else if (_justification == CENTER) {
            sx = insets.left + (b.width - totwid)/2;
        } else { // RIGHT or BOTTOM
            sx = insets.left + b.width - totwid;
        }

        // do the layout
        for (int i = 0; i < nk; i++) {
            // skip non-visible kids
            if (info.dimens[i] == null) {
                continue;
            }

            Component child = target.getComponent(i);
            int newwid, newhei;

            if (_policy == NONE || isFixed(child)) {
                newwid = info.dimens[i].width;
            } else if (_policy == CONSTRAIN) {
                newwid = Math.max(1, (int)(conscale * info.dimens[i].width));
            } else {
                newwid = defwid + freefrac;
                // clear out the extra pixels the first time they're used
                freefrac = 0;
            }

            if (_offpolicy == NONE) {
                newhei = info.dimens[i].height;
            } else if (_offpolicy == CONSTRAIN) {
                newhei = Math.min(info.dimens[i].height, b.height);
            } else {
                newhei = defhei;
            }

            // determine our off-axis position
            if (_offjust == RIGHT || _offjust == TOP) {
                sy = insets.bottom + b.height - newhei;
            } else if (_offjust == LEFT || _offjust == BOTTOM) {
                sy = insets.bottom;
            } else { // CENTER
                sy = insets.bottom + (b.height - newhei)/2;
            }

            child.setBounds(sx, sy, newwid, newhei);
            sx += child.getWidth() + _gap;
        }
    }
}
