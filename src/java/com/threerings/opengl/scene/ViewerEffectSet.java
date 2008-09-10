//
// $Id$

package com.threerings.opengl.scene;

import java.util.HashSet;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.Color4f;

/**
 * A set of viewer effects.
 */
public class ViewerEffectSet extends HashSet<ViewerEffect>
{
    /**
     * Returns the background color for this effect set.
     *
     * @param bounds the bounds used to resolve conflicts.
     */
    public Color4f getBackgroundColor (Box bounds)
    {
        Color4f closestColor = null;
        float cdist = Float.MAX_VALUE;
        for (ViewerEffect effect : this) {
            Color4f color = effect.getBackgroundColor();
            if (color != null) {
                float distance = effect.getBounds().getExtentDistance(bounds);
                if (closestColor == null || distance < cdist) {
                    closestColor = color;
                    cdist = distance;
                }
            }
        }
        return closestColor;
    }
}
