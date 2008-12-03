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
@EditorTypes({ HandlerConfig.Startup.class, HandlerConfig.Tick.class })
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

    /** The action to take in response to the event. */
    @Editable(nullable=true)
    public ActionConfig action;

    /**
     * Returns the name of the server-side logic class for this handler.
     */
    public abstract String getLogicClassName ();
}
