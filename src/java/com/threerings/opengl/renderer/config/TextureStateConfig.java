//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.TextureState;
import com.threerings.opengl.util.GlContext;

/**
 * Configurable texture state.
 */
public class TextureStateConfig extends DeepObject
    implements Exportable
{
    /** The texture unit configurations. */
    @Editable
    public TextureUnitConfig[] units = new TextureUnitConfig[0];

    /**
     * Adds the state's update references to the provided set.
     */
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
        for (TextureUnitConfig unit : units) {
            refs.add(TextureConfig.class, unit.texture);
        }
    }

    /**
     * Determines whether this state is supported by the hardware.
     */
    public boolean isSupported (GlContext ctx)
    {
        if (units.length > ctx.getRenderer().getMaxTextureImageUnits()) {
            return false;
        }
        for (TextureUnitConfig unit : units) {
            if (!unit.isSupported(ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Populates the relevant portion of the supplied descriptor.
     */
    public void populateDescriptor (PassDescriptor desc)
    {
        desc.texCoordSets = new int[units.length];
        for (int ii = 0; ii < units.length; ii++) {
            desc.texCoordSets[ii] = units[ii].coordSet;
        }
    }

    /**
     * Returns the corresponding texture state.
     */
    public TextureState getState (GlContext ctx)
    {
        if (units.length == 0) {
            return TextureState.DISABLED;
        }
        TextureUnit[] sunits = new TextureUnit[units.length];
        for (int ii = 0; ii < units.length; ii++) {
            sunits[ii] = units[ii].createUnit(ctx);
        }
        return new TextureState(sunits);
    }
}
