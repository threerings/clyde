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
