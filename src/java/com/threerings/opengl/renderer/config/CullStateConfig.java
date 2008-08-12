//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.CullState;

/**
 * Configurable cull state.
 */
public class CullStateConfig extends DeepObject
    implements Exportable
{
    /** Cull face constants. */
    public enum Face
    {
        DISABLED(-1),
        FRONT(GL11.GL_FRONT),
        BACK(GL11.GL_BACK),
        FRONT_AND_BACK(GL11.GL_FRONT_AND_BACK);

        public int getConstant ()
        {
            return _constant;
        }

        Face (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** The cull face. */
    @Editable(hgroup="f")
    public Face face = Face.BACK;

    /** If true, do not use a shared instance. */
    @Editable(hgroup="f")
    public boolean uniqueInstance;

    /**
     * Returns the corresponding color state.
     */
    public CullState getState ()
    {
        return uniqueInstance ?
            new CullState(face.getConstant()) :
            CullState.getInstance(face.getConstant());
    }
}
