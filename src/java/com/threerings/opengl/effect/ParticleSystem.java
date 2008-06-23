//
// $Id$

package com.threerings.opengl.effect;

import java.io.File;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.QuickSort;

import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.probs.ColorFunctionVariable;
import com.threerings.probs.FloatFunctionVariable;
import com.threerings.probs.FloatVariable;
import com.threerings.probs.QuaternionVariable;
import com.threerings.probs.VectorVariable;
import com.threerings.util.Copyable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;
import com.threerings.util.DeepUtil;

import com.threerings.opengl.material.Material;
import com.threerings.opengl.material.ParticleHost;
import com.threerings.opengl.material.ParticleHost.Alignment;
import com.threerings.opengl.material.Surface;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.Camera;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlUtil;

/**
 * A particle system effect.
 */
public class ParticleSystem extends Model
{
    /**
     * Controls the order in which layers are rendered within the system.
     */
    public static class GroupPriority extends DeepObject
        implements Exportable
    {
        /** The priority group to which the layer belongs. */
        @Editable(min=0)
        public int group;

        /** The priority within the group. */
        @Editable
        public int priority;
    }

    /**
     * A single layer of the effect.
     */
    public class Layer
        implements ParticleHost, Cloneable, Copyable, Exportable
    {
        /** The name of this layer. */
        public String name;

        /** Whether or not the layer is visible. */
        public boolean visible = true;

        /** Whether or not to align particles' orientations with their velocities. */
        @Editable(category="appearance", weight=-2)
        public Alignment alignment = Alignment.BILLBOARD;

        /** The number of texture divisions in the S direction. */
        @Editable(category="appearance", min=1)
        public int textureDivisionsS = 1;

        /** The number of texture divisions in the T direction. */
        @Editable(category="appearance", min=1)
        public int textureDivisionsT = 1;

        /** Determines how colors are modified according to their alpha values. */
        @Editable(category="appearance")
        public AlphaMode alphaMode = AlphaMode.TRANSLUCENT;

        /** Whether or not to sort the particles by depth before rendering. */
        @Editable(category="appearance")
        public boolean depthSort;

        /** The render priority (higher priority layers are rendered above lower priority ones). */
        @Editable(category="appearance")
        public GroupPriority priorityMode;

        /** Controls the particles' change of color over their lifespans. */
        @Editable(category="appearance", mode="alpha")
        public ColorFunctionVariable color =
            new ColorFunctionVariable.Fixed(new ColorFunction.Constant());

        /** Controls the particles' change of size over their lifespans. */
        @Editable(category="appearance", min=0.0, step=0.01)
        public FloatFunctionVariable size =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0.1f));

        /** Controls the particles' change of length over their lifespans. */
        @Editable(category="appearance", min=0.0, step=0.01)
        public FloatFunctionVariable length =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0.1f));

        /** Controls the particles' change of texture frame over their lifespans. */
        @Editable(category="appearance", min=0.0)
        public FloatFunctionVariable frame =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0f));

        /** The emitter transform. */
        @Editable(category="origin", mode="rigid", step=0.01)
        public Transform3D transform = new Transform3D();

        /** Controls the particles' initial positions. */
        @Editable(category="origin")
        public Placer placer = new Placer.Point();

        /** Controls the particles' initial orientations. */
        @Editable(category="origin")
        public QuaternionVariable orientation = new QuaternionVariable.Constant();

        /** Whether or not to move particles with the emitter. */
        @Editable(category="origin")
        public boolean moveParticlesWithEmitter;

        /** Controls the particles' initial directions. */
        @Editable(category="emission")
        public Shooter shooter = new Shooter.Cone();

        /** Controls the particles' initial speeds. */
        @Editable(category="emission", min=0.0, step=0.01)
        public FloatVariable speed = new FloatVariable.Uniform(1f, 1.5f);

        /** Controls the particles' initial angular velocities. */
        @Editable(category="emission", scale=Math.PI/180.0)
        public VectorVariable angularVelocity = new VectorVariable.Constant();

        /** Whether or not to rotate the velocities with the emitter. */
        @Editable(category="emission")
        public boolean rotateVelocitiesWithEmitter = true;

        /** Controls the number of particles emitted at each frame. */
        @Editable(category="flow")
        public Counter counter = new Counter.Unlimited();

        /** Whether or not to respawn dead particles. */
        @Editable(category="flow")
        public boolean respawnDeadParticles = true;

        /** Controls the particles' lifespans. */
        @Editable(category="world", min=0.0, step=0.01)
        public FloatVariable lifespan = new FloatVariable.Uniform(1f, 1.5f);

        /** The time at which to start the layer. */
        @Editable(category="world", min=0.0, step=0.01)
        public float startTime;

        /** The layer's speed multiplier. */
        @Editable(category="world", min=0.0, step=0.01)
        public float timeScale = 1f;

        /** The layer's influences. */
        @Editable(category="influences")
        public Influence[] influences = new Influence[0];

        public Layer (String name)
        {
            this.name = name;
        }

        public Layer ()
        {
        }

        /**
         * Sets the render mode for the particles.
         */
        @Editable(category="appearance", weight=-4)
        public void setRenderMode (RenderMode mode)
        {
            _renderMode = mode;
            createGeometry();
        }

        /**
         * Returns the particle render mode.
         */
        @Editable
        public RenderMode getRenderMode ()
        {
            return _renderMode;
        }

        /**
         * Sets the maximum number of particles in the layer.
         */
        @Editable(category="appearance", min=0, weight=-3)
        public void setParticleCount (int count)
        {
            _count = count;
            createParticles();
        }

        /**
         * Returns the number of particles in the layer.
         */
        @Editable
        public int getParticleCount ()
        {
            return _count;
        }

        /**
         * Sets the texture to use.
         */
        @Editable(category="appearance", weight=-1)
        @FileConstraints(
            description="m.texture_files",
            extensions={".properties", ".png", ".jpg"},
            directory="texture_dir")
        public void setTexture (File texture)
        {
            _texture = (texture == null) ? null : GlUtil.relativizePath(
                _path, texture.toString()).replace(File.separatorChar, '/');
            createSurface();
        }

        /**
         * Returns the texture being used.
         */
        @Editable
        public File getTexture ()
        {
            return (_texture == null) ? null :
                new File(_path, _texture.replace('/', File.separatorChar));
        }

        /**
         * Returns a reference to the camera rendering this layer.
         */
        public Camera getCamera ()
        {
            return _ctx.getRenderer().getCamera();
        }

        /**
         * Initializes the layer.
         */
        public void init ()
        {
            _tstate = new TransformState();
            createParticles();
        }

        /**
         * (Re)creates the surface for this layer.
         */
        public void createSurface (String variant, HashMap<String, Material> materials)
        {
            String texture = null;
            Material material = materials.get(texture);
            if (material == null) {
                material = getMaterial(variant, texture);
            }
            _surface = material.createSurface(_geom);
            _surface.setHost(this);
        }

        /**
         * Updates the layer surface.
         */
        public void updateSurface ()
        {
            _surface.update();
        }

        /**
         * Resets the layer to its initial state.
         */
        public void reset ()
        {
            // clear the elapsed time
            _total = 0f;

            // reset the counter
            counter.reset();

            // reset the counts
            _living = 0;
            _preliving = _count;
        }

        /**
         * Updates the current particle state based on the elapsed time in seconds.
         */
        public void tick (float elapsed)
        {
            if ((_total += elapsed) < startTime) {
                return;
            }
            elapsed *= timeScale;

            // update the world transform and its inverse
            _transform.compose(transform, _worldTransform).invert(_worldTransformInv);

            // tick the influences
            for (Influence influence : influences) {
                influence.tick(this, elapsed);
            }

            // update the living particles and the bounds
            _bounds.setToEmpty();
            float msize = 0f;
            for (int ii = 0; ii < _living; ii++) {
                Particle particle = _particles[ii];
                if (particle.tick(elapsed)) {
                    // apply the influences
                    for (Influence influence : influences) {
                        influence.apply(particle);
                    }
                    // add to bounds
                    _bounds.addLocal(particle.getPosition());
                    msize = Math.max(msize, particle.getSize());

                } else {
                    // move this particle to the end of the list
                    if (ii != --_living) {
                        _particles[ii] = _particles[_living];
                        _particles[_living] = particle;
                        ii--; // update the swapped particle on the next iteration
                    }
                    // then to the end of the preliving list
                    if (_preliving != 0) {
                        int idx = _living + _preliving;
                        _particles[_living] = _particles[idx];
                        _particles[idx] = particle;
                    }
                }
            }

            // find out how many particles the counter thinks we should emit
            int count = counter.count(elapsed,
                respawnDeadParticles ? (_count - _living) : _preliving);

            // spawn those particles
            for (int ii = _living, nn = _living + count; ii < nn; ii++) {
                Particle particle = _particles[ii];
                placer.place(this, particle);
                orientation.getValue(particle.getOrientation());
                vectorToLayer(
                    shooter.shoot(particle).multLocal(speed.getValue()),
                    rotateVelocitiesWithEmitter);
                angularVelocity.getValue(particle.getAngularVelocity());
                particle.init(
                    lifespan.getValue(),
                    alphaMode, color, size,
                    (_geom.getSegments() > 0) ? length : null,
                    (textureDivisionsS > 1 || textureDivisionsT > 1) ? frame : null);
                _living++;
                _preliving = Math.max(_preliving - 1, 0);
                _bounds.addLocal(particle.getPosition());
                msize = Math.max(msize, particle.getSize());
            }

            // expand the bounds (TODO: account for tails)
            if (_bounds.isEmpty()) {
                return;
            }
            float amount = _gradius * msize;
            _bounds.expandLocal(amount, amount, amount);

            // add layer bounds to the system bounds
            if (moveParticlesWithEmitter) {
                _bounds.transformLocal(_worldTransform);
            }
            _worldBounds.addLocal(_bounds);

            // and the group bounds, if applicable
            if (priorityMode != null) {
                int group = priorityMode.group;
                if (_groups.length <= group) {
                    LayerGroup[] ogroups = _groups;
                    _groups = new LayerGroup[group + 1];
                    System.arraycopy(ogroups, 0, _groups, 0, ogroups.length);
                    for (int ii = ogroups.length; ii < _groups.length; ii++) {
                        _groups[ii] = new LayerGroup();
                    }
                }
                _groups[group].bounds.addLocal(_bounds);
            }
        }

        /**
         * Transforms a point in-place from world space or emitter space into the space of
         * the layer (either world space or emitter space, depending on the value of
         * {@link #moveParticlesWithEmitter}).
         *
         * @param emitter if true, transform from emitter space (else from world space).
         * @return a reference to the transformed point, for chaining.
         */
        public Vector3f pointToLayer (Vector3f point, boolean emitter)
        {
            return moveParticlesWithEmitter ?
                (emitter ? point : _worldTransformInv.transformPointLocal(point)) :
                (emitter ? _worldTransform.transformPointLocal(point) : point);
        }

        /**
         * Transforms a vector in-place from world space or emitter space into the space of
         * the layer (either world space or emitter space, depending on the value of
         * {@link #moveParticlesWithEmitter}).
         *
         * @param emitter if true, transform from emitter space (else from world space).
         * @return a reference to the transformed vector, for chaining.
         */
        public Vector3f vectorToLayer (Vector3f vector, boolean emitter)
        {
            return moveParticlesWithEmitter ?
                (emitter ? vector : _worldTransformInv.transformVectorLocal(vector)) :
                (emitter ? _worldTransform.transformVectorLocal(vector) : vector);
        }

        /**
         * Enqueues the layer for rendering.
         */
        public void enqueue (Transform3D modelview)
        {
            if (!visible || _living == 0) {
                return;
            }
            // update the transform state
            if (moveParticlesWithEmitter) {
                modelview.compose(transform, _tstate.getModelview());
            } else {
                _tstate.getModelview().set(_ctx.getRenderer().getCamera().getViewTransform());
            }
            _tstate.setDirty(true);

            // sort by depth if so required (TODO: radix or incremental sort?)
            if (depthSort) {
                Transform3D xform = _tstate.getModelview();
                for (int ii = 0; ii < _living; ii++) {
                    Particle particle = _particles[ii];
                    particle.depth = xform.transformPointZ(particle.getPosition());
                }
                QuickSort.sort(_particles, 0, _living - 1, DEPTH_COMP);
            }

            // update the depth of the layer
            if (priorityMode == null) {
                _bounds.getCenter(_center);
                _depth = _ctx.getRenderer().getCamera().getViewTransform().transformPointZ(_center);
            } else {
                _depth = _groups[priorityMode.group].depth + priorityMode.priority*0.0001f;
            }

            // enqueue the surface
            _surface.enqueue();
        }

        /**
         * Draws the layer's bounds.
         */
        public void drawBounds ()
        {
            if (visible && _living > 0) {
                DebugBounds.draw(_bounds, Color4f.GRAY);
            }
        }

        /**
         * Returns whether this layer has no particles alive or yet to be spawned.
         */
        public boolean isComplete ()
        {
            return _living == 0 && _preliving == 0 && !respawnDeadParticles;
        }

        // documentation inherited from interface SurfaceHost
        public Transform3D getModelview ()
        {
            return _tstate.getModelview();
        }

        // documentation inherited from interface SurfaceHost
        public ColorState getColorState ()
        {
            return _cstate;
        }

        // documentation inherited from interface SurfaceHost
        public FogState getFogState ()
        {
            return _fstate;
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

        // documentation inherited from interface SurfaceHost
        public TransformState getTransformState ()
        {
            return _tstate;
        }

        // documentation inherited from interface ParticleHost
        public Particle[] getParticles ()
        {
            return _particles;
        }

        // documentation inherited from interface ParticleHost
        public int getLiving ()
        {
            return _living;
        }

        // documentation inherited from interface ParticleHost
        public Alignment getAlignment ()
        {
            return alignment;
        }

        // documentation inherited from interface ParticleHost
        public int getTextureDivisionsS ()
        {
            return textureDivisionsS;
        }

        // documentation inherited from interface ParticleHost
        public int getTextureDivisionsT ()
        {
            return textureDivisionsT;
        }

        // documentation inherited from interface ParticleHost
        public float getDepth ()
        {
            return _depth;
        }

        @Override // documentation inherited
        public Object clone ()
        {
            return copy(null);
        }

        // documentation inherited from interface Copyable
        public Object copy (Object dest)
        {
            Layer clayer = (Layer)DeepUtil.copy(this, dest);
            clayer.init();
            return clayer;
        }

        /**
         * Creates a clone of this layer for the specified system.
         */
        public Layer clone (ParticleSystem osystem)
        {
            // shallow copy the parameters
            Layer olayer = osystem.createLayer(name);
            olayer.visible = visible;
            olayer.alignment = alignment;
            olayer.textureDivisionsS = textureDivisionsS;
            olayer.textureDivisionsT = textureDivisionsT;
            olayer.alphaMode = alphaMode;
            olayer.depthSort = depthSort;
            olayer.priorityMode = priorityMode;
            olayer.color = color;
            olayer.size = size;
            olayer.length = length;
            olayer.frame = frame;
            olayer.transform = transform;
            olayer.placer = placer;
            olayer.orientation = orientation;
            olayer.moveParticlesWithEmitter = moveParticlesWithEmitter;
            olayer.shooter = shooter;
            olayer.speed = speed;
            olayer.angularVelocity = angularVelocity;
            olayer.rotateVelocitiesWithEmitter = rotateVelocitiesWithEmitter;
            olayer.counter = counter;
            olayer.respawnDeadParticles = respawnDeadParticles;
            olayer.lifespan = lifespan;
            olayer.startTime = startTime;
            olayer.timeScale = timeScale;
            olayer.influences = influences;
            olayer._renderMode = _renderMode;
            olayer._count = _count;
            olayer._texture = _texture;
            olayer._worldTransform = new Transform3D(_worldTransform);
            olayer._worldTransformInv = new Transform3D(_worldTransformInv);
            olayer._bounds = new Box(_bounds);
            olayer._particles = new Particle[_count];
            for (int ii = 0; ii < _count; ii++) {
                olayer._particles[ii] = new Particle();
            }
            olayer._preliving = _count;
            olayer._geom = _geom;
            olayer._gradius = _gradius;
            olayer._tstate = new TransformState(); // have to set this before calling setHost
            olayer._surface = (Surface)_surface.clone();
            olayer._surface.setHost(olayer);
            return olayer;
        }

        /**
         * (Re)creates the particle array.
         */
        protected void createParticles ()
        {
            Particle[] oparticles = _particles;
            _particles = new Particle[_count];
            for (int ii = 0; ii < _count; ii++) {
                _particles[ii] = (oparticles != null && oparticles.length > ii) ?
                    oparticles[ii] : new Particle();
            }
            if (oparticles == null) {
                _living = 0;
                _preliving = _count;
            } else {
                _living = Math.min(_living, _count);
                _preliving = Math.min(_living + _preliving, _count) - _living;
            }
            createGeometry();
        }

        /**
         * (Re)creates the geometry.
         */
        protected void createGeometry ()
        {
            _geom = _renderMode.createGeometry(_count);
            Box gbounds = _geom.getBounds();
            _gradius = Math.max(
                gbounds.getMinimumExtent().length(), gbounds.getMaximumExtent().length());
            createSurface();
        }

        /**
         * (Re)creates the surface.
         */
        protected void createSurface ()
        {
            Properties props = new Properties();
            if (_texture != null) {
                if (_texture.toLowerCase().endsWith(".properties")) {
                    props.setProperty("material", _texture);
                } else {
                    props.setProperty("diffuse", _texture);
                }
            }
            GlUtil.normalizeProperties(_path, props);
            _surface = _ctx.getMaterialCache().getMaterial(props).createSurface(_geom);
            _surface.setHost(this);
        }

        /** The particle render mode. */
        protected RenderMode _renderMode = new RenderMode.Points();

        /** The maximum number of particles. */
        protected int _count = 100;

        /** The path to the texture. */
        protected String _texture;

        /** The system's transform in world space. */
        @DeepOmit
        protected transient Transform3D _worldTransform = new Transform3D();

        /** The inverse of the world space transform. */
        @DeepOmit
        protected transient Transform3D _worldTransformInv = new Transform3D();

        /** The bounds of the layer. */
        @DeepOmit
        protected transient Box _bounds = new Box();

        /** The particles in the layer (first the living particles, then the pre-living particles,
         * then the dead particles). */
        @DeepOmit
        protected transient Particle[] _particles;

        /** The number of particles currently alive. */
        @DeepOmit
        protected transient int _living;

        /** The number of particles currently "pre-alive." */
        @DeepOmit
        protected transient int _preliving;

        /** The layer's particle geometry. */
        @DeepOmit
        protected transient ParticleGeometry _geom;

        /** The radius of the geometry. */
        @DeepOmit
        protected transient float _gradius;

        /** The particle surface. */
        @DeepOmit
        protected transient Surface _surface;

        /** The layer's transform state. */
        @DeepOmit
        protected transient TransformState _tstate;

        /** The total time elapsed since reset. */
        @DeepOmit
        protected transient float _total;

        /** The view space depth. */
        @DeepOmit
        protected transient float _depth;
    }

    /**
     * Creates a new particle system.
     */
    public ParticleSystem ()
    {
        super(new Properties());
    }

    /**
     * Sets the path used to resolve textures.
     */
    public void setPath (String path)
    {
        _path = path;
    }

    /**
     * Creates a new layer for this system.
     */
    public Layer createLayer (String name)
    {
        return new Layer(name);
    }

    /**
     * Returns a reference to the list of layers.
     */
    public ArrayList<Layer> getLayers ()
    {
        return _layers;
    }

    /**
     * Sets whether to update the system when it is enqueued for rendering, rather than
     * in the {@link #tick} method.
     */
    public void setUpdateOnEnqueue (boolean updateOnEnqueue)
    {
        _updateOnEnqueue = updateOnEnqueue;
    }

    /**
     * Checks whether the system is updated when enqueued.
     */
    public boolean getUpdateOnEnqueue ()
    {
        return _updateOnEnqueue;
    }

    /**
     * Returns whether this system has no particles alive or yet to be spawned.
     */
    public boolean isComplete ()
    {
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            if (!_layers.get(ii).isComplete()) {
                return false;
            }
        }
        return true;
    }

    // documentation inherited from interface SurfaceHost
    public Transform3D getModelview ()
    {
        return _modelview;
    }

    // documentation inherited from interface SurfaceHost
    public TransformState getTransformState ()
    {
        return TransformState.IDENTITY;
    }

    // documentation inherited from interface Intersectable
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        return false;
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the modelview transform and the transform hierarchy
        Transform3D view = _ctx.getRenderer().getCamera().getViewTransform();
        view.compose(_transform, _modelview);

        // update the group depths
        for (LayerGroup group : _groups) {
            group.depth = view.transformPointZ(group.bounds.getCenter(_center));
        }

        // enqueue the layers
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            _layers.get(ii).enqueue(_modelview);
        }
    }

    @Override // documentation inherited
    public void updateWorldBounds ()
    {
        // world bounds are updated in tick
    }

    @Override // documentation inherited
    public void createSurfaces (String variant)
    {
        // create the layer surfaces
        HashMap<String, Material> materials = new HashMap<String, Material>();
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            _layers.get(ii).createSurface(variant, materials);
        }
    }

    @Override // documentation inherited
    public boolean requiresTick ()
    {
        return true;
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        // if specified, wait until we actually enqueue the system before updating
        if (_updateOnEnqueue) {
            _elapsed = elapsed;
        } else {
            update(elapsed);
        }
    }

    @Override // documentation inherited
    public void reset ()
    {
        // reset the layers
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            _layers.get(ii).reset();
        }
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        // draw the layer bounds first, then the system bounds
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            _layers.get(ii).drawBounds();
        }
        super.drawBounds();
    }

    @Override // documentation inherited
    public Object clone ()
    {
        ParticleSystem osystem = (ParticleSystem)super.clone();
        osystem._layers = new ArrayList<Layer>(_layers.size());
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            osystem._layers.add(_layers.get(ii).clone(osystem));
        }
        osystem._modelview = new Transform3D(_modelview);
        osystem._groups = new LayerGroup[0];
        return osystem;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        // initialize the layers
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            _layers.get(ii).init();
        }
    }

    @Override // documentation inherited
    protected void enqueue (Transform3D modelview)
    {
        // update the world transform
        Camera camera = _ctx.getRenderer().getCamera();
        camera.getWorldTransform().compose(modelview, _transform);

        // update the group depths
        Transform3D view = camera.getViewTransform();
        for (LayerGroup group : _groups) {
            group.depth = view.transformPointZ(group.bounds.getCenter(_center));
        }

        // update the layers if specified
        if (_updateOnEnqueue) {
            update(_elapsed);
            _elapsed = 0f;
        }

        // enqueue the layers
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            _layers.get(ii).enqueue(modelview);
        }
    }

    /**
     * Updates the layers.
     */
    protected void update (float elapsed)
    {
        // reset the bounds
        _worldBounds.setToEmpty();
        for (LayerGroup group : _groups) {
            group.bounds.setToEmpty();
        }

        // tick the layers (they will expand the bounds)
        for (int ii = 0, nn = _layers.size(); ii < nn; ii++) {
            _layers.get(ii).tick(elapsed);
        }
    }

    /**
     * Contains information on a group of layers for sorting purposes.
     */
    protected static class LayerGroup
    {
        /** The combined bounds of the group. */
        public Box bounds = new Box();

        /** The base depth of the group. */
        public float depth;
    }

    /** The effect layers. */
    protected ArrayList<Layer> _layers = new ArrayList<Layer>();

    /** The modelview transform. */
    protected transient Transform3D _modelview = new Transform3D();

    /** Layer groups mapped by index. */
    protected transient LayerGroup[] _groups = new LayerGroup[0];

    /** Whether or not we wait until we are enqueued before updating the layers. */
    protected transient boolean _updateOnEnqueue;

    /** The elapsed time passed to the {@link #tick} method. */
    protected transient float _elapsed;

    /** Used to compute the bounding box centers. */
    protected transient Vector3f _center = new Vector3f();

    /** Sorts particles by decreasing depth. */
    protected static final Comparator<Particle> DEPTH_COMP = new Comparator<Particle>() {
        public int compare (Particle p1, Particle p2) {
            return Float.compare(p1.depth, p2.depth);
        }
    };
}
