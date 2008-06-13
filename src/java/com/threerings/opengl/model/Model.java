//
// $Id$

package com.threerings.opengl.model;

import java.util.Properties;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.PropertiesUtil;

import com.threerings.math.Box;
import com.threerings.math.Frustum;
import com.threerings.math.Ray;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;

import com.threerings.export.Exportable;

import com.threerings.opengl.material.Material;
import com.threerings.opengl.material.Surface;
import com.threerings.opengl.material.SurfaceHost;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlUtil;
import com.threerings.opengl.util.Intersectable;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

/**
 * A 3D model.
 */
public abstract class Model
    implements SurfaceHost, Tickable, Intersectable, Renderable, Cloneable, Exportable
{
    /**
     * Creates a new model.
     */
    public Model (Properties props)
    {
        _props = props;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Model ()
    {
    }

    /**
     * Prepares the model for rendering.
     *
     * @param path the root path of the model (used to resolve relative resource paths).
     */
    public void init (GlContext ctx, String path)
    {
        _ctx = ctx;
        _path = path;
        _transform = new Transform(Transform.UNIFORM);
        _localBounds = new Box();
        _worldBounds = new Box();
        _cstate = new ColorState();
        _fstate = FogState.DISABLED;
        _lstate = LightState.DISABLED;
        _mstate = new MaterialState();

        // let subclasses perform custom initialization
        didInit();
    }

    /**
     * Sets this model's cache key.
     */
    public void setKey (Object key)
    {
        _key = key;
    }

    /**
     * Returns this model's cache key.
     */
    public Object getKey ()
    {
        return _key;
    }

    /**
     * Returns the path of this model.
     */
    public String getPath ()
    {
        return _path;
    }

    /**
     * Returns the properties that define this model's configuration.
     */
    public Properties getProperties ()
    {
        return _props;
    }

    /**
     * Returns the local bounds of the model.
     */
    public Box getLocalBounds ()
    {
        return _localBounds;
    }

    /**
     * Returns a reference to the model's root transform.
     */
    public Transform getTransform ()
    {
        return _transform;
    }

    /**
     * Returns a reference to the model's color.  If you change this value, be sure to call
     * {@link #updateSurfaces} to propagate the change to the model surfaces.
     */
    public Color4f getColor ()
    {
        return _cstate.getColor();
    }

    /**
     * Returns the bounds of the model in world space.
     */
    public Box getWorldBounds ()
    {
        return _worldBounds;
    }

    /**
     * Updates the model's bounding volume to reflect its current transform.
     */
    public void updateWorldBounds ()
    {
        _localBounds.transform(_transform, _worldBounds);
    }

    /**
     * Determines whether this model's bounding volume intersects the view frustum.
     */
    public boolean boundsIntersectFrustum ()
    {
        Frustum frustum = _ctx.getRenderer().getCamera().getWorldVolume();
        return frustum.getIntersectionType(_worldBounds) != Frustum.IntersectionType.NONE;
    }

    /**
     * (Re)creates the model's surfaces using the specified variant configuration (or null
     * for the default).
     */
    public abstract void createSurfaces (String variant);

    /**
     * Updates the surfaces after a change to one of the model's parameters.  Generally speaking,
     * this is used for non-trivial changes, as opposed to changes that happen every frame.
     */
    public void updateSurfaces ()
    {
        // copy color to material ambient/diffuse
        _cstate.setDirty(true);
        _mstate.getFrontAmbient().set(_cstate.getColor());
        _mstate.getFrontDiffuse().set(_cstate.getColor());
        _mstate.setDirty(true);
    }

    /**
     * Determines whether this model will ever require a per-frame call to {@link #tick}.
     */
    public abstract boolean requiresTick ();

    /**
     * Resets the state of this model.  Used when models are reused from a pool.
     */
    public void reset ()
    {
        // nothing by default
    }

    /**
     * Draws the bounds of the model in immediate mode for debugging purposes.
     */
    public void drawBounds ()
    {
        DebugBounds.draw(_worldBounds, Color4f.WHITE);
    }

    // documentation inherited from interface SurfaceHost
    public ColorState getColorState ()
    {
        return _cstate;
    }

    /**
     * Sets the shared fog state.
     */
    public void setFogState (FogState fstate)
    {
        _fstate = fstate;
    }

    // documentation inherited from interface SurfaceHost
    public FogState getFogState ()
    {
        return _fstate;
    }

    /**
     * Sets the shared light state.
     */
    public void setLightState (LightState lstate)
    {
        _lstate = lstate;
    }

    // documentation inherited from interface SurfaceHost
    public LightState getLightState ()
    {
        return _lstate;
    }

    // documentation inherited from interface SurfaceHost
    public MaterialState getMaterialState ()
    {
        return _mstate;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // nothing by default
    }

    @Override // documentation inherited
    public Object clone ()
    {
        Model omodel = null;
        try {
            omodel = (Model)super.clone();
        } catch (CloneNotSupportedException e) {
            return null; // should never happen
        }
        omodel._transform = new Transform(_transform);
        omodel._localBounds = new Box(_localBounds);
        omodel._worldBounds = new Box(_worldBounds);
        omodel._cstate = new ColorState(_cstate.getColor());
        omodel._mstate = new MaterialState(
            _mstate.getFrontAmbient(), _mstate.getFrontDiffuse(), _mstate.getFrontSpecular(),
            _mstate.getFrontEmission(), _mstate.getFrontShininess(),
            _mstate.getBackAmbient(), _mstate.getBackDiffuse(), _mstate.getBackSpecular(),
            _mstate.getBackEmission(), _mstate.getBackShininess(),
            _mstate.getColorMaterialMode(), _mstate.getColorMaterialFace(),
            _mstate.getTwoSide(), _mstate.getLocalViewer(), _mstate.getSeparateSpecular(),
            _mstate.getFlatShading());
        return omodel;
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
    }

    /**
     * Fetches or creates the material for the specified variant corresponding to the given
     * texture name.
     */
    protected Material getMaterial (String variant, String texture)
    {
        // extract the subproperties, treating the texture as a diffuse texture path
        // by default
        Properties tprops;
        if (texture == null) {
            tprops = new Properties();
        } else {
            Properties props = (variant == null) ?
                _props : PropertiesUtil.getFilteredProperties(_props, variant);
            tprops = PropertiesUtil.getSubProperties(props, texture);
            if (!tprops.containsKey("diffuse")) {
                tprops.setProperty("diffuse", props.getProperty(texture, texture));
            }
        }

        // normalize the subproperties and retrieve from cache
        GlUtil.normalizeProperties(_path, tprops);
        return _ctx.getMaterialCache().getMaterial(tprops);
    }

    /**
     * Enqueues this model with the given modelview transform.  Used to enqueue attached models.
     */
    protected abstract void enqueue (Transform modelview);

    /** The model properties. */
    protected Properties _props;

    /** The renderer context. */
    protected transient GlContext _ctx;

    /** The cache key. */
    protected transient Object _key;

    /** The model path. */
    protected transient String _path;

    /** The untransformed bounds of the model. */
    protected transient Box _localBounds;

    /** The model's transform in world space. */
    protected transient Transform _transform;

    /** The bounds of the model in world space. */
    protected transient Box _worldBounds;

    /** The model's common color state. */
    protected transient ColorState _cstate;

    /** The model's common fog state. */
    protected transient FogState _fstate;

    /** The model's common light state. */
    protected transient LightState _lstate;

    /** The model's common material state. */
    protected transient MaterialState _mstate;
}
