//
// $Id$

package com.threerings.opengl.material;

import java.util.Properties;

import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

/**
 * Generates surfaces for rendering meshes.
 */
public abstract class Material
{
    /**
     * Creates a material from the specified properties.
     */
    public static Material createMaterial (GlContext ctx, Properties props)
    {
        // instantiate the configured class
        String mclass = props.getProperty("class", DefaultMaterial.class.getName());
        try {
            Material material = (Material)Class.forName(mclass).newInstance();
            material.init(ctx, props);
            return material;
        } catch (Exception e) {
            log.warning("Failed to instantiate material class [class=" + mclass + "].", e);
            return null;
        }
    }

    /**
     * Initializes this material.
     */
    public void init (GlContext ctx, Properties props)
    {
        _ctx = ctx;
        _props = props;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Override to perform custom initialization.
     */
    public void didInit ()
    {
    }

    /**
     * Creates a surface using this material to render the supplied geometry.
     */
    public abstract Surface createSurface (Geometry geom);

    /** The renderer context. */
    protected GlContext _ctx;

    /** The material properties. */
    protected Properties _props;
}
