//
// $Id$

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Configurations for server-side event handlers.
 */
@EditorTypes({
    HandlerConfig.Startup.class, HandlerConfig.Tick.class,
    HandlerConfig.Timer.class, HandlerConfig.Signal.class,
    HandlerConfig.SignalStart.class, HandlerConfig.SignalStop.class,
    HandlerConfig.Intersection.class, HandlerConfig.IntersectionStart.class,
    HandlerConfig.IntersectionStop.class, HandlerConfig.Interaction.class })
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
        /** The amount to expand the intersection shape. */
        @Editable(step=0.01, hgroup="e")
        public float expansion;
    }

    /**
     * The intersection event handler.
     */
    public static class Intersection extends BaseIntersection
    {
        /** The amount of time that must elapse between firings. */
        @Editable(min=0.0, step=0.1, hgroup="e")
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
     * The interaction event handler.
     */
    public static class Interaction extends BaseIntersection
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.HandlerLogic$Interaction";
        }
    }

    /** The action to take in response to the event. */
    @Editable(weight=1)
    public ActionConfig action = new ActionConfig.SpawnActor();

    /**
     * Returns the name of the server-side logic class for this handler.
     */
    public abstract String getLogicClassName ();
}
