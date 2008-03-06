//
// $Id$

package com.threerings.opengl.effect;

import com.threerings.opengl.renderer.Color4f;

/**
 * The alpha mode, which determines how to convert colors to premultiplied alpha format.
 */
public enum AlphaMode
{
    /** Multiply by alpha and set alpha to one. */
    OPAQUE {
        public Color4f apply (Color4f color) {
            float a = color.a;
            return color.set(color.r*a, color.g*a, color.b*a, 1f);
        }
    },

    /** Multiply by alpha and leave alpha unchanged. */
    TRANSLUCENT {
        public Color4f apply (Color4f color) {
            float a = color.a;
            return color.set(color.r*a, color.g*a, color.b*a, a);
        }
    },

    /** Multiply by alpha and set alpha to zero. */
    ADDITIVE {
        public Color4f apply (Color4f color) {
            float a = color.a;
            return color.set(color.r*a, color.g*a, color.b*a, 0f);
        }
    },

    /** Leave everything unchanged. */
    PREMULTIPLIED {
        public Color4f apply (Color4f color) {
            return color;
        }
    };

    /**
     * Applies the computed color's alpha according to this mode's rules.
     *
     * @return a reference to the color, for chaining.
     */
    public abstract Color4f apply (Color4f color);
}
