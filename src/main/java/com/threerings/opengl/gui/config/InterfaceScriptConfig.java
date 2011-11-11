//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.TextComponent;
import com.threerings.opengl.gui.ToggleButton;
import com.threerings.opengl.gui.UserInterface;

/**
 * A script used to control a user interface.
 */
public class InterfaceScriptConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the script.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The loop duration, or zero for unlooped. */
        @Editable(min=0.0, step=0.01)
        public float loopDuration;

        /** The actions of which the script is composed. */
        @Editable
        public TimedAction[] actions = new TimedAction[0];
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The script reference. */
        @Editable(nullable=true)
        public ConfigReference<InterfaceScriptConfig> interfaceScript;
    }

    /**
     * An action to perform after a specific time interval.
     */
    public static class TimedAction extends DeepObject
        implements Exportable
    {
        /** The time at which to perform the action. */
        @Editable(min=0, step=0.01)
        public float time;

        /** The action to perform. */
        @Editable
        public Action action = new PlaySound();
    }

    /**
     * Represents a single script action.
     */
    @EditorTypes({
        PlaySound.class, SetEnabled.class, SetVisible.class, SetSelected.class,
        SetText.class, SetStyle.class, SetConfig.class, RequestFocus.class })
    public static abstract class Action extends DeepObject
        implements Exportable
    {
        /**
         * Executes the action on the specified interface.
         */
        public abstract void execute (UserInterface iface);
    }

    /**
     * Plays a sound effect.
     */
    public static class PlaySound extends Action
    {
        /** The sound to play. */
        @Editable(editor="resource", nullable=true)
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String sound;

        @Override // documentation inherited
        public void execute (UserInterface iface)
        {
            if (sound != null) {
                iface.getRoot().playSound(sound);
            }
        }
    }

    /**
     * Base class for actions that act on a target component identified by tag.
     */
    public static abstract class Targeted extends Action
    {
        /** The tag of the target component. */
        @Editable(hgroup="t")
        public String target = "";

        @Override // documentation inherited
        public void execute (UserInterface iface)
        {
            for (Component comp : iface.getComponents(target)) {
                apply(comp);
            }
        }

        /**
         * Applies the action to the specified component.
         */
        public abstract void apply (Component comp);
    }

    /**
     * Enables or disables a component.
     */
    public static class SetEnabled extends Targeted
    {
        /** Whether or not the component should be enabled. */
        @Editable(hgroup="t")
        public boolean enabled = true;

        @Override // documentation inherited
        public void apply (Component comp)
        {
            comp.setEnabled(enabled);
        }
    }

    /**
     * Renders a component visible or invisible.
     */
    public static class SetVisible extends Targeted
    {
        /** Whether or not the component should be visible. */
        @Editable(hgroup="t")
        public boolean visible = true;

        @Override // documentation inherited
        public void apply (Component comp)
        {
            comp.setVisible(visible);
        }
    }

    /**
     * Selects or unselects a component.
     */
    public static class SetSelected extends Targeted
    {
        /** Whether or not the component should be selected. */
        @Editable(hgroup="t")
        public boolean selected = true;

        @Override // documentation inherited
        public void apply (Component comp)
        {
            if (comp instanceof ToggleButton) {
                ((ToggleButton)comp).setSelected(selected);
            }
        }
    }

    /**
     * Sets the text content of a component.
     */
    public static class SetText extends Targeted
    {
        /** The text content. */
        @Editable(hgroup="t")
        public String text = "";

        @Override // documentation inherited
        public void apply (Component comp)
        {
            if (comp instanceof TextComponent) {
                ((TextComponent)comp).setText(text);
            }
        }
    }

    /**
     * Sets the style of a component.
     */
    public static class SetStyle extends Targeted
    {
        /** The new user interface config. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> style;

        @Override // documentation inherited
        public void apply (Component comp)
        {
            comp.setStyleConfig(style);
        }
    }

    /**
     * Sets the configuration of a user interface.
     */
    public static class SetConfig extends Targeted
    {
        /** The new user interface config. */
        @Editable(nullable=true)
        public ConfigReference<UserInterfaceConfig> userInterface;

        @Override // documentation inherited
        public void apply (Component comp)
        {
            if (comp instanceof UserInterface) {
                ((UserInterface)comp).setConfig(userInterface);
            }
        }
    }

    /**
     * Requests focus on a component.
     */
    public static class RequestFocus extends Targeted
    {
        @Override // documentation inherited
        public void apply (Component comp)
        {
            comp.requestFocus();
        }
    }

    /** The actual script implementation. */
    @Editable
    public Implementation implementation = new Original();
}
