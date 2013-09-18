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

package com.threerings.opengl.gui.config;

import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.StringUtil;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.UserInterface;
import com.threerings.opengl.gui.UserInterface.Script;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.ComponentListener;

/**
 * An event to be handled in a script.
 */
@EditorTypes({ EventConfig.Action.class, EventConfig.Any.class })
public abstract class EventConfig extends DeepObject
    implements Exportable
{
    /**
     * Superclass of the targeted events.
     */
    public static abstract class Targeted extends EventConfig
    {
        /** The tag of the target component. */
        @Editable(hgroup="t")
        public String target = "";

        @Override
        public Script addHandler (UserInterface iface, Runnable runnable)
        {
            final ComponentListener listener = createListener(runnable);
            final List<Component> comps = Lists.newArrayList(iface.getComponents(target));
            return iface.new Script() {
                @Override public void init () {
                    for (Component comp : comps) {
                        comp.addListener(listener);
                    }
                }
                @Override public void cleanup () {
                    for (Component comp : comps) {
                        comp.removeListener(listener);
                    }
                }
            };
        }

        /**
         * Creates the listener that will execute the runnable when the event fires.
         */
        protected abstract ComponentListener createListener (Runnable runnable);
    }

    /**
     * Waits for an action to fire on the targeted components.
     */
    public static class Action extends Targeted
    {
        /** The name of the action, or blank for any. */
        @Editable(hgroup="t")
        public String action = "";

        @Override
        protected ComponentListener createListener (final Runnable runnable)
        {
            return new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    if (StringUtil.isBlank(action) || action.equals(event.getAction())) {
                        runnable.run();
                    }
                }
            };
        }
    }

    /**
     * Waits for any of the contained sub-events to fire.
     */
    public static class Any extends EventConfig
    {
        /** The contained sub-events. */
        @Editable
        public EventConfig[] events = new EventConfig[0];

        @Override
        public Script addHandler (UserInterface iface, Runnable runnable)
        {
            final Script[] scripts = new Script[events.length];
            for (int ii = 0; ii < events.length; ii++) {
                scripts[ii] = events[ii].addHandler(iface, runnable);
            }
            return iface.new Script() {
                @Override public void init () {
                    for (Script script : scripts) {
                        script.init();
                    }
                }
                @Override public void cleanup () {
                    for (Script script : scripts) {
                        script.cleanup();
                    }
                }
            };
        }
    }

    /**
     * Adds a handler that will execute the supplied runnable when the event fires.
     *
     * @return a script handle that can be used to remove the handler.
     */
    public abstract Script addHandler (UserInterface iface, Runnable runnable);
}
