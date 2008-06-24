//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.state.LightState;

/**
 * Configurable light state.
 */
@EditorTypes({ LightStateConfig.Disabled.class, LightStateConfig.Enabled.class })
public abstract class LightStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Explicitly disables lighting.
     */
    public static class Disabled extends LightStateConfig
    {
        @Override // documentation inherited
        public LightState getState ()
        {
            return LightState.DISABLED;
        }
    }

    /**
     * Enables lighting.
     */
    public static class Enabled extends LightStateConfig
    {
        /** The global ambient light intensity. */
        @Editable
        public Color4f globalAmbient = new Color4f(0.2f, 0.2f, 0.2f, 1f);

        /** The individual light configurations. */
        @Editable
        public LightConfig[] lights = new LightConfig[0];

        @Override // documentation inherited
        public LightState getState ()
        {
            Light[] slights = new Light[lights.length];
            for (int ii = 0; ii < lights.length; ii++) {
                slights[ii] = lights[ii].createLight();
            }
            return new LightState(slights, globalAmbient);
        }
    }

    /**
     * Returns the corresponding light state.
     */
    public abstract LightState getState ();
}
