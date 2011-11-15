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
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.util.DeepObject;

import com.threerings.opengl.effect.Easing;
import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.TextComponent;
import com.threerings.opengl.gui.ToggleButton;
import com.threerings.opengl.gui.UserInterface;
import com.threerings.opengl.gui.UserInterface.Script;
import com.threerings.opengl.gui.UserInterface.ConfigScript;

/**
 * Represents a single script action.
 */
@EditorTypes({
    ActionConfig.PlaySound.class, ActionConfig.SetEnabled.class, ActionConfig.SetVisible.class,
    ActionConfig.SetAlpha.class, ActionConfig.FadeAlpha.class, ActionConfig.SetSelected.class,
    ActionConfig.SetText.class, ActionConfig.SetStyle.class, ActionConfig.SetConfig.class,
    ActionConfig.RunScript.class, ActionConfig.RequestFocus.class, ActionConfig.Wait.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable
{
    /**
     * Plays a sound effect.
     */
    public static class PlaySound extends ActionConfig
    {
        /** The sound to play. */
        @Editable(editor="resource", nullable=true)
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String sound;

        @Override // documentation inherited
        public void execute (UserInterface iface, ConfigScript script)
        {
            if (sound != null) {
                iface.getRoot().playSound(sound);
            }
        }
    }

    /**
     * Base class for actions that act on a target component identified by tag.
     */
    public static abstract class Targeted extends ActionConfig
    {
        /** The tag of the target component. */
        @Editable(hgroup="t")
        public String target = "";

        @Override // documentation inherited
        public void execute (UserInterface iface, ConfigScript script)
        {
            for (Component comp : iface.getComponents(target)) {
                apply(iface, comp);
            }
        }

        /**
         * Applies the action to the specified component.
         */
        public abstract void apply (UserInterface iface, Component comp);
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
        public void apply (UserInterface iface, Component comp)
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
        public void apply (UserInterface iface, Component comp)
        {
            comp.setVisible(visible);
        }
    }

    /**
     * Set's a component's transparency.
     */
    public static class SetAlpha extends Targeted
    {
        /** The alpha value to set. */
        @Editable(min=0, max=1, step=0.01, hgroup="t")
        public float alpha = 1f;

        @Override // documentation inherited
        public void apply (UserInterface iface, Component comp)
        {
            comp.setAlpha(alpha);
        }
    }

    /**
     * Fades a component's transparency over time.
     */
    public static class FadeAlpha extends Targeted
    {
        /** The interval over which to fade. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float interval = 1f;

        /** The starting alpha value. */
        @Editable(min=0, max=1, step=0.01, hgroup="s")
        public float start;

        /** The ending alpha value. */
        @Editable(min=0, max=1, step=0.01, hgroup="s")
        public float end = 1f;

        /** The type of easing to use, if any. */
        @Editable
        public Easing easing = new Easing.None();

        @Override // documentation inherited
        public void apply (UserInterface iface, final Component comp)
        {
            iface.addScript(iface.new TickableScript() {
                @Override public void tick (float elapsed) {
                    if ((_time += elapsed) < interval) {
                        comp.setAlpha(FloatMath.lerp(start, end,
                            easing.getTime(_time / interval)));
                    } else {
                        remove();
                    }
                }
                @Override public void cleanup () {
                    super.cleanup();
                    comp.setAlpha(end);
                }
                protected float _time;
            });
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
        public void apply (UserInterface iface, Component comp)
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
        public void apply (UserInterface iface, Component comp)
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
        public void apply (UserInterface iface, Component comp)
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
        public void apply (UserInterface iface, Component comp)
        {
            if (comp instanceof UserInterface) {
                ((UserInterface)comp).setConfig(userInterface);
            }
        }
    }

    /**
     * Runs a script on a user interface.
     */
    public static class RunScript extends Targeted
    {
        /** The script to run on the interface. */
        @Editable(nullable=true)
        public ConfigReference<InterfaceScriptConfig> interfaceScript;

        @Override // documentation inherited
        public void apply (UserInterface iface, Component comp)
        {
            if (comp instanceof UserInterface) {
                ((UserInterface)comp).runScript(interfaceScript);
            }
        }
    }

    /**
     * Requests focus on a component.
     */
    public static class RequestFocus extends Targeted
    {
        @Override // documentation inherited
        public void apply (UserInterface iface, Component comp)
        {
            comp.requestFocus();
        }
    }

    /**
     * Waits for an event to occur before continuing.
     */
    public static class Wait extends ActionConfig
    {
        /** The event to wait for. */
        @Editable
        public EventConfig event = new EventConfig.Action();

        @Override // documentation inherited
        public void execute (UserInterface iface, final ConfigScript script)
        {
            // pause the script
            script.setPaused(true);

            // and unpause when the handler calls back
            final Script[] wscript = new Script[1];
            iface.addScript(wscript[0] = event.addHandler(iface, new Runnable() {
                public void run () {
                    wscript[0].remove();
                    script.setPaused(false);
                }
            }));
        }
    }

    /**
     * Executes the action on the specified interface/script.
     */
    public abstract void execute (UserInterface iface, ConfigScript script);
}
