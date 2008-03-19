//
// $Id$

package com.threerings.opengl.gui.util;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.CommandEvent;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;

import static com.threerings.opengl.gui.Log.*;

/**
 * Provides some OpenGL GUI equivalents to the static methods in {@link Controller}.
 */
public class ControllerUtil
{
    /** An action listener that uses {@link #postAction} to forward the event to a controller. */
    public static final ActionListener DISPATCHER = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            postAction(event);
        }
    };

    /**
     * Posts a new {@link ActionEvent} with the supplied parameters to the controller hierarchy.
     */
    public static void postAction (Component source, String action)
    {
        postAction(new ActionEvent(source, 0L, 0, action));
    }

    /**
     * Posts a new {@link CommandEvent} with the supplied parameters to the controller hierarchy.
     */
    public static void postAction (Component source, String action, Object argument)
    {
        postAction(new CommandEvent(source, 0L, 0, action, argument));
    }

    /**
     * Attempts to find a {@link Controller} to handle the specified event by rising up through
     * the component hierarchy starting at the event's target.  Unlike
     * {@link Controller#postAction}, this method handles the event immediately, rather than
     * posting an action on the AWT thread.
     */
    public static void postAction (ActionEvent event)
    {
        Object source = event.getSource();
        if (!(source instanceof Component)) {
            log.warning("Tried to dispatch action on non-component [event=" + event + "].");
            return;
        }
        String action = event.getAction();
        Object argument = (event instanceof CommandEvent) ?
            ((CommandEvent)event).getArgument() : null;
        for (Component comp = (Component)source; comp != null; comp = comp.getParent()) {
            if (!(comp instanceof ControllerProvider)) {
                continue;
            }
            Controller ctrl = ((ControllerProvider)comp).getController();
            if (ctrl.handleAction(source, action, argument)) {
                return;
            }
        }
        log.warning("Unable to find controller to process action [event=" + event + "].");
    }
}
