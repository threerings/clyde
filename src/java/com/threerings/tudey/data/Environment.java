//
// $Id$

package com.threerings.tudey.data;

import com.threerings.editor.Editable;

import com.threerings.opengl.renderer.Color4f;

/**
 * Represents the global environment of the scene.
 */
public class Environment extends SceneElement
{
    /** The ambient light intensity. */
    @Editable
    public Color4f ambientLight = new Color4f();
}
