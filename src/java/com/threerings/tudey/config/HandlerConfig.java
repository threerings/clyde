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
    HandlerConfig.Intersection.class, HandlerConfig.Interaction.class })
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
     * The intersection event handler.
     */
    public static class Intersection extends HandlerConfig
    {
        /** The amount to expand the intersection shape. */
        @Editable(step=0.01, hgroup="e")
        public float expansion;

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
     * The interaction event handler.
     */
    public static class Interaction extends HandlerConfig
    {
        /** The amount to expand the interaction shape. */
        @Editable(step=0.01)
        public float expansion;

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
