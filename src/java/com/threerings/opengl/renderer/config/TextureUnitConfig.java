//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.util.DeepObject;

import com.threerings.math.Transform;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;

import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.util.GlContext;

/**
 * Contains the configuration of a single texture unit.
 */
public class TextureUnitConfig extends DeepObject
    implements Exportable
{
    /** A reference to the texture to bind to the unit. */
    @Editable
    public ConfigReference<TextureConfig> texture;

    /** The texture environment. */
    @Editable
    public TextureEnvironmentConfig environment = new TextureEnvironmentConfig.Modulate();

    /** The texture coordinate generation function for the s coordinate. */
    @Editable(types={
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class,
        TextureCoordGenConfig.SphereMap.class, TextureCoordGenConfig.NormalMap.class,
        TextureCoordGenConfig.ReflectionMap.class })
    public TextureCoordGenConfig coordGenS;

    /** The texture coordinate generation function for the t coordinate. */
    @Editable(types={
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class,
        TextureCoordGenConfig.SphereMap.class, TextureCoordGenConfig.NormalMap.class,
        TextureCoordGenConfig.ReflectionMap.class })
    public TextureCoordGenConfig coordGenT;

    /** The texture coordinate generation function for the r coordinate. */
    @Editable(types={
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class,
        TextureCoordGenConfig.NormalMap.class, TextureCoordGenConfig.ReflectionMap.class })
    public TextureCoordGenConfig coordGenR;

    /** The texture coordinate generation function for the q coordinate. */
    @Editable(types={
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class })
    public TextureCoordGenConfig coordGenQ;

    /** The texture transform. */
    @Editable
    public Transform transform = new Transform();

    /**
     * Checks whether the unit configuration is supported.
     */
    public boolean isSupported ()
    {
        return environment.isSupported() &&
            (coordGenS == null || coordGenS.isSupported()) &&
            (coordGenT == null || coordGenT.isSupported()) &&
            (coordGenR == null || coordGenR.isSupported()) &&
            (coordGenQ == null || coordGenQ.isSupported());
    }

    /**
     * Creates the texture unit corresponding to this configuration.
     */
    public TextureUnit createUnit (GlContext ctx)
    {
        TextureUnit unit = new TextureUnit();
        if (texture != null) {
            TextureConfig config = ctx.getConfigManager().getConfig(TextureConfig.class, texture);
            if (config != null) {
                unit.texture = config.getTexture(ctx);
            }
        }
        environment.configure(unit);
        if (coordGenS != null) {
            unit.genModeS = coordGenS.getModeAndPlane(unit.genPlaneS);
        }
        if (coordGenT != null) {
            unit.genModeT = coordGenT.getModeAndPlane(unit.genPlaneT);
        }
        if (coordGenR != null) {
            unit.genModeR = coordGenR.getModeAndPlane(unit.genPlaneR);
        }
        if (coordGenQ != null) {
            unit.genModeQ = coordGenQ.getModeAndPlane(unit.genPlaneQ);
        }
        unit.transform.set(transform);
        return unit;
    }
}
