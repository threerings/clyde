//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.Transform2D;
import com.threerings.util.DeepObject;

import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.config.ShapeConfig;

/**
 * Configurations for server-side event handlers.
 */
@EditorTypes({
    HandlerConfig.Startup.class, HandlerConfig.Shutdown.class,
    HandlerConfig.Tick.class, HandlerConfig.Timer.class,
    HandlerConfig.Signal.class, HandlerConfig.SignalStart.class,
    HandlerConfig.SignalStop.class, HandlerConfig.Intersection.class,
    HandlerConfig.IntersectionStart.class, HandlerConfig.IntersectionStop.class,
    HandlerConfig.ThresholdIntersectionCount.class, HandlerConfig.Request.class,
    HandlerConfig.ActorRemoved.class, HandlerConfig.VariableChanged.class })
public abstract class HandlerConfig extends DeepObject
    implements Exportable
{
    /**
     * The startup event handler.
     */
    public static class Startup extends HandlerConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Startup";
        }
    }

    /**
     * The shutdown event handler.
     */
    public static class Shutdown extends HandlerConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Shutdown";
        }
    }

    /**
     * The tick event handler.
     */
    public static class Tick extends HandlerConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Tick";
        }
    }

    /**
     * The timer event handler.
     */
    public static class Timer extends HandlerConfig
    {
        /** The timer interval, in seconds. */
        @Editable(min=0.0, step=0.1, hgroup="i")
        public float interval = 1f;

        /** The number of times to fire the timer (or zero for unlimited). */
        @Editable(min=0, hgroup="i")
        public int limit;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Timer";
        }
    }

    /**
     * The signal event handler.
     */
    public static class Signal extends HandlerConfig
    {
        /** The name of the signal of interest. */
        @Editable(hgroup="n")
        public String name = "";

        /** The amount of time that must elapse between firings. */
        @Editable(min=0.0, step=0.1, hgroup="n")
        public float refractoryPeriod;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Signal";
        }
    }

    /**
     * The signal start event handler (fired on the first tick that a signal is received).
     */
    public static class SignalStart extends HandlerConfig
    {
        /** The name of the signal of interest. */
        @Editable
        public String name = "";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$SignalStart";
        }
    }

    /**
     * The signal stop event handler (fired on the first tick that a signal stops being received).
     */
    public static class SignalStop extends HandlerConfig
    {
        /** The name of the signal of interest. */
        @Editable
        public String name = "";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$SignalStop";
        }
    }

    /**
     * Base class for the various intersection-related handlers.
     */
    public static abstract class BaseIntersection extends HandlerConfig
    {
        /** The shape to use for the intersection test. */
        @Editable
        public IntersectionShape shape = new DefaultShape();

        @Override // documentation inherited
        public void invalidate ()
        {
            super.invalidate();
            shape.invalidate();
        }
    }

    /**
     * The intersection event handler.
     */
    public static class Intersection extends BaseIntersection
    {
        /** The amount of time that must elapse between firings. */
        @Editable(min=0.0, step=0.1)
        public float refractoryPeriod;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Intersection";
        }
    }

    /**
     * The intersection start event handler.
     */
    public static class IntersectionStart extends BaseIntersection
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$IntersectionStart";
        }
    }

    /**
     * The intersection stop event handler.
     */
    public static class IntersectionStop extends BaseIntersection
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$IntersectionStop";
        }
    }

    /**
     * The intersection count event handler.
     */
    public static abstract class BaseIntersectionCount extends BaseIntersection
    {
        /** The condition that must be satisfied for a valid intersection. */
        @Editable
        public ConditionConfig condition = new ConditionConfig.InstanceOf();
    }

    /**
     * An threshold instsection count event handler.
     */
    public static class ThresholdIntersectionCount extends BaseIntersectionCount
    {
        /** The threshold value. */
        @Editable(min=0)
        public int threshold;

        /** The action to perform when we go under the threshold. */
        @Editable(nullable=true, weight=2)
        public ActionConfig underAction;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$ThresholdIntersectionCount";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            super.invalidate();
            if (underAction != null) {
                underAction.invalidate();
            }
        }
    }

    /**
     * The client request event handler.
     */
    public static class Request extends HandlerConfig
    {
        /** The name of the request of interest. */
        @Editable
        public String name = "";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Request";
        }
    }

    /**
     * An actor is removed event handler.
     */
    public static class ActorRemoved extends HandlerConfig
    {
        /** The targets we're observering. */
        @Editable
        public TargetConfig target = new TargetConfig.Tagged();

        /** If we're waiting for them all to be removed. */
        @Editable
        public boolean all = true;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$ActorRemoved";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Called when any variable is modified.
     */
    public static class VariableChanged extends HandlerConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$VariableChanged";
        }
    }

    /**
     * Base class for the intersection shapes.
     */
    @EditorTypes({ DefaultShape.class, TransformedShape.class })
    public static abstract class IntersectionShape extends DeepObject
        implements Exportable
    {
        /**
         * Returns the intersection shape based on the source shape and transform.
         *
         * @param result a result object to reuse if possible.
         */
        public abstract Shape getShape (Shape source, Transform2D transform, Shape result);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * Uses the source's shape with an optional expansion.
     */
    public static class DefaultShape extends IntersectionShape
    {
        /** The amount to expand the shape. */
        @Editable(step=0.01)
        public float expansion;

        @Override // documentation inherited
        public Shape getShape (Shape source, Transform2D transform, Shape result)
        {
            return (source == null || expansion == 0f) ? source : source.expand(expansion, result);
        }
    }

    /**
     * Uses a different shape, transformed by the source's transform.
     */
    public static class TransformedShape extends IntersectionShape
    {
        /** The shape to use. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Point();

        @Override // documentation inherited
        public Shape getShape (Shape source, Transform2D transform, Shape result)
        {
            return shape.getShape().transform(transform, result);
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            shape.invalidate();
        }
    }

    /** The action to take in response to the event. */
    @Editable(weight=1)
    public ActionConfig action = new ActionConfig.SpawnActor();

    /**
     * Returns the name of the server-side logic class for this handler.
     */
    public abstract String getLogicClassName ();

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        action.invalidate();
    }
}
