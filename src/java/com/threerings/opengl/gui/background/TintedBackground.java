//
// $Id$

package com.threerings.opengl.gui.background;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Displays a partially transparent solid color in the background.
 */
public class TintedBackground extends Background
{
    /**
     * Creates a tinted background with the specified color.
     */
    public TintedBackground (Color4f color)
    {
        _color.set(color);
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
        super.render(renderer, x, y, width, height, alpha);

        float a = _color.a * alpha;
        renderer.setColorState(_color.r * a, _color.g * a, _color.b * a, a);
        renderer.setTextureState(null);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    protected Color4f _color = new Color4f();
}
