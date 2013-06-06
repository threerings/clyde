//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.server.logic;

import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.util.ArrayUtil;

import com.threerings.config.ConfigReference;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;
import com.threerings.util.ShallowObject;

import com.threerings.opengl.model.config.ModelConfig;

import com.threerings.tudey.config.ActionConfig;
import com.threerings.tudey.config.ConditionConfig;
import com.threerings.tudey.config.ExpressionConfig;
import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.config.RegionConfig;
import com.threerings.tudey.config.TargetConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

/**
 * Handles the server-side processing for some entity.
 */
public abstract class Logic extends ShallowObject
{
    /**
     * An interface for objects interested in updates to the logic's shape (as returned by
     * {@link #getShape}).
     */
    public interface ShapeObserver
    {
        /**
         * Notes that the logic's shape is about to change.
         */
        public void shapeWillChange (Logic source);

        /**
         * Notes that the logic's shape has changed.
         */
        public void shapeDidChange (Logic source);
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr)
    {
        _scenemgr = scenemgr;
    }

    /**
     * Returns a reference to the scene manager that owns the logic.
     */
    public TudeySceneManager getSceneManager ()
    {
        return _scenemgr;
    }

    /**
     * Returns the tags for this logic, if any.
     */
    public String[] getTags ()
    {
        return ArrayUtil.EMPTY_STRING;
    }

    /**
     * Checks whether this logic object corresponds to a default entrance.
     */
    public boolean isDefaultEntrance ()
    {
        return false;
    }

    /**
     * Determines whether this logic object is still active in the scene.
     */
    public boolean isActive ()
    {
        return true;
    }

    /**
     * Determines whether this logic object is "visible" to the specified pawn.
     */
    public boolean isVisible (PawnLogic pawn)
    {
        return true;
    }

    /**
     * Returns the entity key for this logic, if any.
     */
    public EntityKey getEntityKey ()
    {
        return null;
    }

    /**
     * Convenience method to retrieve the translation and rotation in a new transform.
     */
    public Transform2D getTransform ()
    {
        return getTransform(new Transform2D());
    }

    /**
     * Convenience method to retrieve the translation and rotation in a transform.
     *
     * @return a reference to the result transform, for chaining.
     */
    public Transform2D getTransform (Transform2D result)
    {
        return result.set(getTranslation(), getRotation());
    }

    /**
     * Returns the transform to use when a portal.
     */
    public Transform2D getPortalTransform ()
    {
        return getTransform();
    }

    /**
     * Returns the translation of this logic for the purpose of spawning actors, etc.
     */
    public Vector2f getTranslation ()
    {
        return Vector2f.ZERO;
    }

    /**
     * Returns the rotation of this logic for the purpose of spawning actors, etc.
     */
    public float getRotation ()
    {
        return 0f;
    }

    /**
     * Returns a reference to this logic's shape, or returns <code>null</code> for none.
     */
    public Shape getShape ()
    {
        return null;
    }

    /**
     * Returns a patrol path for this logic.
     */
    public Vector2f[] getPatrolPath ()
    {
        Shape shape = getShape();
        return (shape == null) ? null : shape.getPerimeterPath();
    }

    /**
     * Adds an observer for changes to the logic's shape.
     */
    public void addShapeObserver (ShapeObserver observer)
    {
        // nothing by default; the shape never changes
    }

    /**
     * Removes a shape observer.
     */
    public void removeShapeObserver (ShapeObserver observer)
    {
        // nothing by default
    }

    /**
     * Returns a reference to the model associated with this logic, if any.
     */
    public ConfigReference<ModelConfig> getModel ()
    {
        return null;
    }

    /**
     * Sends a generic "signal" to the logic.
     *
     * @param timestamp the signal timestamp.
     * @param source the source of the signal.
     */
    public void signal (int timestamp, Logic source, String name)
    {
        // nothing by default
    }

    /**
     * Sets the value of a variable.
     *
     * @param timestamp the set timestamp.
     * @param source the source of the request.
     */
    public void setVariable (int timestamp, Logic source, String name, Object value)
    {
        if (_variables == null) {
            _variables = Maps.newHashMap();
        }
        _variables.put(name, value);
    }

    /**
     * Retrieves the value of a variable (or <code>null</code> if unset).
     */
    public Object getVariable (String name)
    {
        return (_variables == null) ? null : _variables.get(name);
    }

    /**
     * Notifies the logic of a client request.
     *
     * @param timestamp the request timestamp.
     * @param source the source of the request.
     */
    public void request (int timestamp, PawnLogic source, String name)
    {
        // nothing by default
    }

    /**
     * Resolve the appropriate source target logic.
     */
    public Logic resolveTarget ()
    {
        return this;
    }

    /**
     * Transfers state from the specified source logic.  Where relevant, this also takes the place
     * of addition/startup notification.
     */
    public void transfer (Logic source, Map<Object, Object> refs)
    {
        _variables = (source._variables == null) ? null : Maps.newHashMap(source._variables);
    }

    /**
     * Creates a handler with the supplied configuration and source.
     */
    protected HandlerLogic createHandler (HandlerConfig config, Logic source)
    {
        // create the logic instance
        HandlerLogic logic = (HandlerLogic)_scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /**
     * Creates an action with the supplied configuration and source.
     */
    protected ActionLogic createAction (ActionConfig config, Logic source)
    {
        // create the logic instance
        ActionLogic logic = (ActionLogic)_scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /**
     * Creates and returns a target logic object.
     */
    protected TargetLogic createTarget (TargetConfig config, Logic source)
    {
        // create the logic instance
        TargetLogic logic = (TargetLogic)_scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /**
     * Creates and returns a condition logic object.
     */
    protected ConditionLogic createCondition (ConditionConfig config, Logic source)
    {
        // create the logic instance
        ConditionLogic logic = (ConditionLogic)_scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /**
     * Creates and returns an expression logic object.
     */
    protected ExpressionLogic createExpression (ExpressionConfig config, Logic source)
    {
        // create the logic instance
        ExpressionLogic logic = (ExpressionLogic)_scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /**
     * Creates and returns a region logic object.
     */
    protected RegionLogic createRegion (RegionConfig config, Logic source)
    {
        // create the logic instance
        RegionLogic logic = (RegionLogic)_scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /** The scene manager. */
    protected TudeySceneManager _scenemgr;

    /** The lazily initialized variable map. */
    protected Map<String, Object> _variables;
}
