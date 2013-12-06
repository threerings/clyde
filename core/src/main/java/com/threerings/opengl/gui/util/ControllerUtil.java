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

package com.threerings.opengl.gui.util;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;

import static com.threerings.opengl.gui.Log.log;

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
        postAction(source, action, null);
    }

    /**
     * Posts a new {@link ActionEvent} with the supplied parameters to the controller hierarchy.
     */
    public static void postAction (Component source, String action, Object argument)
    {
        postAction(new ActionEvent(source, 0L, 0, action, argument));
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
        Object argument = event.getArgument();
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
