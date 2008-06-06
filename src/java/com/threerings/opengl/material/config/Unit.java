//
// $Id$

package com.threerings.opengl.material.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.Transform;

import com.threerings.opengl.renderer.Color4f;

/**
 * Describes the configuration of a single texture unit.
 */
public class Unit extends DeepObject
    implements Exportable
{
    /** The texture environment. */
    @Editable
    public Environment env = new Environment.Modulate();

    /** The texture environment color. */
    @Editable(mode="alpha")
    public Color4f envColor = new Color4f(0f, 0f, 0f, 0f);

    /** The texture level of detail bias. */
    @Editable(step=0.01)
    public float lodBias;

    /** The texture transform. */
    @Editable
    public Transform transform = new Transform();
}
