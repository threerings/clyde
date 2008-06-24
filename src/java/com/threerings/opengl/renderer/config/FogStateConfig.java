//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.FogState;

/**
 * Configurable fog state.
 */
@EditorTypes({
    FogStateConfig.Disabled.class, FogStateConfig.Linear.class,
    FogStateConfig.Exponential.class })
public abstract class FogStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Explicitly disables the fog.
     */
    public static class Disabled extends FogStateConfig
    {
        @Override // documentation inherited
        public FogState getState ()
        {
            return FogState.DISABLED;
        }
    }

    /**
     * Superclass of the enabled states.
     */
    public static abstract class Enabled extends FogStateConfig
    {
        /** The fog color. */
        @Editable(hgroup="p")
        public Color4f color = new Color4f(0f, 0f, 0f, 0f);
    }

    /**
     * Linear fog.
     */
    public static class Linear extends Enabled
    {
        /** The fog start distance. */
        @Editable(min=0, step=0.1, hgroup="p")
        public float start;

        /** The fog end distance. */
        @Editable(min=0, step=0.1, hgroup="p")
        public float end = 1f;

        public Linear (Enabled other)
        {
            color.set(other.color);
        }

        public Linear ()
        {
        }

        @Override // documentation inherited
        public FogState getState ()
        {
            return new FogState(GL11.GL_LINEAR, start, end, color);
        }
    }

    /**
     * Exponential fog.
     */
    public static class Exponential extends Enabled
    {
        /** The fog density. */
        @Editable(min=0, step=0.001, hgroup="p")
        public float density = 1f;

        /** Whether or not to square the exponential function. */
        @Editable(hgroup="p")
        public boolean squared;

        public Exponential (Enabled other)
        {
            color.set(other.color);
        }

        public Exponential ()
        {
        }

        @Override // documentation inherited
        public FogState getState ()
        {
            return new FogState(squared ? GL11.GL_EXP2 : GL11.GL_EXP, density, color);
        }
    }

    /**
     * Returns the corresponding fog state.
     */
    public abstract FogState getState ();
}
