//
// $Id$

package com.threerings.opengl.util;

import java.io.IOException;
import java.util.Properties;

import com.samskivert.util.SoftCache;

import com.threerings.opengl.material.Material;

import static com.threerings.opengl.Log.*;

/**
 * Caches loaded materials.
 */
public class MaterialCache
{
    /**
     * Creates a new material cache.
     */
    public MaterialCache (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Retrieves the material with the supplied properties.
     */
    public Material getMaterial (Properties props)
    {
        Material material = _materials.get(props);
        if (material == null) {
            // it may be a reference to another material
            String name = props.getProperty("material");
            if (name != null) {
                material = loadMaterial(name);
            } else {
                material = Material.createMaterial(_ctx, props);
            }
            if (material != null) {
                _materials.put(props, material);
            }
        }
        return material;
    }

    /**
     * Loads the shared material with the specified name.
     */
    protected Material loadMaterial (String name)
    {
        Properties props = new Properties();
        String path = "materials/" + name;
        try {
            props.load(GlUtil.getInputStream(_ctx, path + "/material.properties"));
        } catch (IOException e) {
            log.warning("Failed to read material properties [name=" + name + "].", e);
            return null;
        }
        GlUtil.normalizeProperties(path, props);
        return Material.createMaterial(_ctx, props);
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The set of loaded materials. */
    protected SoftCache<Properties, Material> _materials = new SoftCache<Properties, Material>();
}
