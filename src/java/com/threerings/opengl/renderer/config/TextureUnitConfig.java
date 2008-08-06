//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.util.DeepObject;

import com.threerings.math.Transform3D;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
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
    @Editable(nullable=true)
    public ConfigReference<TextureConfig> texture;

    /** The texture environment. */
    @Editable
    public TextureEnvironmentConfig environment = new TextureEnvironmentConfig.Modulate();

    /** The texture coordinate set to use. */
    @Editable(min=0)
    public int coordSet;

    /** The texture coordinate generation function for the s coordinate. */
    @Editable(nullable=true)
    public TextureCoordGenConfig coordGenS;

    /** The texture coordinate generation function for the t coordinate. */
    @Editable(nullable=true)
    public TextureCoordGenConfig coordGenT;

    /** The texture coordinate generation function for the r coordinate. */
    @Editable(nullable=true)
    @EditorTypes({
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class,
        TextureCoordGenConfig.NormalMap.class, TextureCoordGenConfig.ReflectionMap.class })
    public TextureCoordGenConfig coordGenR;

    /** The texture coordinate generation function for the q coordinate. */
    @Editable(nullable=true)
    @EditorTypes({
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class })
    public TextureCoordGenConfig coordGenQ;

    /** The texture transform. */
    @Editable(step=0.01)
    public Transform3D transform = new Transform3D();

    /**
     * Checks whether the unit configuration is supported.
     */
    public boolean isSupported (GlContext ctx)
    {
        if (!(environment.isSupported() &&
            (coordGenS == null || coordGenS.isSupported()) &&
            (coordGenT == null || coordGenT.isSupported()) &&
            (coordGenR == null || coordGenR.isSupported()) &&
            (coordGenQ == null || coordGenQ.isSupported()))) {
            return false;
        }
        TextureConfig config = getTextureConfig(ctx);
        return config == null || config.isSupported(ctx);
    }

    /**
     * Creates the texture unit corresponding to this configuration.
     */
    public TextureUnit createUnit (GlContext ctx)
    {
        TextureUnit unit = new TextureUnit();
        TextureConfig config = getTextureConfig(ctx);
        if (config != null) {
            unit.texture = config.getTexture(ctx);
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

    /**
     * Returns the configuration of the unit texture.
     */
    protected TextureConfig getTextureConfig (GlContext ctx)
    {
        return (texture == null) ?
            null : ctx.getConfigManager().getConfig(TextureConfig.class, texture);
    }
}
