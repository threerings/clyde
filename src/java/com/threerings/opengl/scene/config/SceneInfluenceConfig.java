//
// $Id$

package com.threerings.opengl.scene.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.config.FogStateConfig;
import com.threerings.opengl.renderer.config.LightConfig;
import com.threerings.opengl.scene.SceneInfluence;

/**
 * The configuration of an influence.
 */
@EditorTypes({
    SceneInfluenceConfig.AmbientLight.class, SceneInfluenceConfig.Fog.class,
    SceneInfluenceConfig.Light.class })
public abstract class SceneInfluenceConfig extends DeepObject
    implements Exportable
{
    /**
     * Represents the influence of ambient light.
     */
    public static class AmbientLight extends SceneInfluenceConfig
    {
        /** The ambient light color. */
        @Editable
        public Color4f color = new Color4f(0.2f, 0.2f, 0.2f, 1f);
    }

    /**
     * Represents the influence of fog.
     */
    public static class Fog extends SceneInfluenceConfig
    {
        /** The fog state. */
        @Editable
        public FogStateConfig fogState = new FogStateConfig.Linear();
    }

    /**
     * Represents the influence of a light.
     */
    public static class Light extends SceneInfluenceConfig
    {
        /** The light config. */
        @Editable
        public LightConfig light = new LightConfig.Directional();
    }
}
