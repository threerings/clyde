//
// $Id$

package com.threerings.opengl.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.google.common.collect.Maps;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.SoftCache;

import com.threerings.math.Transform3D;

import com.threerings.export.BinaryImporter;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Animation.Frame;
import com.threerings.opengl.model.ArticulatedModel;
import com.threerings.opengl.model.ArticulatedModel.Node;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.SkinMesh;
import com.threerings.opengl.model.StaticModel;
import com.threerings.opengl.model.VisibleMesh;

import static com.threerings.opengl.Log.*;

/**
 * Caches loaded models and animations and maintains a pool of reusable models.
 */
public class ModelCache
{
    /**
     * Creates a new model cache.
     */
    public ModelCache (GlContext ctx)
    {
        _ctx = ctx;
        ERROR_MODEL.setData(
            ERROR_MODEL.createNode(null, false, new Transform3D(), new Node[0]),
            new SkinMesh[0], null);
        ERROR_MODEL.init(ctx, "error");
        ERROR_ANIM.init();
    }

    /**
     * Returns an instance of the default variant of the named model, fetching one from the pool
     * if one is available (otherwise creating a new instance).  When finished with the model, call
     * {@link #returnToPool} to return it to the pool.
     */
    public Model getFromPool (String name)
    {
        return getFromPool(name, null);
    }

    /**
     * Returns an instance of the specified variant of the named model, fetching one from the pool
     * if one is available (otherwise creating a new instance).  When finished with the model, call
     * {@link #returnToPool} to return it to the pool.
     */
    public Model getFromPool (String name, String variant)
    {
        ModelKey key = new ModelKey(name, variant);
        ArrayList<Model> instances = _pool.get(key);
        if (instances == null) {
            return createInstance(name, variant);
        }
        int size = instances.size();
        Model instance = instances.remove(size - 1);
        if (size == 1) {
            _pool.remove(key);
        }
        instance.reset();
        return instance;
    }

    /**
     * Returns a model to the pool for reuse.
     */
    public void returnToPool (Model model)
    {
        ModelKey key = (ModelKey)model.getKey();
        ArrayList<Model> instances = _pool.get(key);
        if (instances == null) {
            _pool.put(key, instances = new ArrayList<Model>());
        }
        instances.add(model);
    }

    /**
     * Clears out the model pool.
     */
    public void clearPool ()
    {
        _pool.clear();
    }

    /**
     * Creates an instance of the default variant of the named model.
     */
    public Model createInstance (String name)
    {
        return createInstance(name, null);
    }

    /**
     * Creates an instance of the specified variant of the named model.
     */
    public Model createInstance (String name, String variant)
    {
        Model prototype = getPrototype(name, variant);
        return (prototype == null) ? null : (Model)prototype.clone();
    }

    /**
     * Fetches the prototype of the default variant of the named model.
     */
    public Model getPrototype (String name)
    {
        return getPrototype(name, null);
    }

    /**
     * Fetches the prototype of the specified variant of the named model.
     */
    public Model getPrototype (String name, String variant)
    {
        ModelKey key = new ModelKey(name, variant);
        Model prototype = _prototypes.get(key);
        if (prototype == null) {
            if (variant == null) {
                try {
                    BinaryImporter in = new BinaryImporter(
                        GlUtil.getInputStream(_ctx, name + ".dat"));
                    _prototypes.put(key, prototype = (Model)in.readObject());
                    int sidx = name.lastIndexOf('/');
                    prototype.init(_ctx, name.substring(0, Math.max(0, sidx)));
                    prototype.setKey(key);

                } catch (IOException e) {
                    log.warning("Failed to load model [name=" + name + "].", e);
                    prototype = ERROR_MODEL;
                }
            } else {
                _prototypes.put(key, prototype = createInstance(name));
                prototype.createSurfaces(variant);
            }
        }
        return prototype;
    }

    /**
     * Fetches the animation with the specified name.
     */
    public Animation getAnimation (String name)
    {
        Animation anim = _animations.get(name);
        if (anim == null) {
            try {
                BinaryImporter in = new BinaryImporter(
                    GlUtil.getInputStream(_ctx, name + "/animation.dat"));
                _animations.put(name, anim = (Animation)in.readObject());
                anim.init();

            } catch (IOException e) {
                log.warning("Failed to load animation [name=" + name + "].", e);
                anim = ERROR_ANIM;
            }
        }
        return anim;
    }

    /**
     * Identifies a loaded model.
     */
    protected static class ModelKey
    {
        /** The model name. */
        public String name;

        /** The model variant (null for the default variant). */
        public String variant;

        public ModelKey (String name, String variant)
        {
            this.name = name;
            this.variant = variant;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return name.hashCode() ^ (variant == null ? 0 : variant.hashCode());
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            ModelKey okey = (ModelKey)other;
            return name.equals(okey.name) && ObjectUtil.equals(variant, okey.variant);
        }
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The set of loaded model prototypes, mapped by name and variant. */
    protected SoftCache<ModelKey, Model> _prototypes = new SoftCache<ModelKey, Model>();

    /** The set of pooled instances, mapped by name and variant. */
    protected HashMap<ModelKey, ArrayList<Model>> _pool = Maps.newHashMap();

    /** The set of loaded animations, mapped by name. */
    protected SoftCache<String, Animation> _animations = new SoftCache<String, Animation>();

    /** An empty model to return on error. */
    protected static final ArticulatedModel ERROR_MODEL = new ArticulatedModel(new Properties());

    /** An empty animation to return on error. */
    protected static final Animation ERROR_ANIM = new Animation(
        new Properties(), 1f, new String[0],
        new Frame[] { new Frame(new Transform3D[0]), new Frame(new Transform3D[0]) });
}
