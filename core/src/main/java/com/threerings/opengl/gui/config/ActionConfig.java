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

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.BooleanExpression;
import com.threerings.expr.FloatExpression;
import com.threerings.expr.Function;
import com.threerings.expr.ObjectExpression;
import com.threerings.expr.Scope;
import com.threerings.expr.Transform2DExpression;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform2D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.effect.Easing;
import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.RenderableView;
import com.threerings.opengl.gui.TextComponent;
import com.threerings.opengl.gui.ToggleButton;
import com.threerings.opengl.gui.UserInterface;
import com.threerings.opengl.gui.UserInterface.Script;
import com.threerings.opengl.gui.UserInterface.ConfigScript;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.AnimationConfig;

import static com.threerings.opengl.gui.Log.log;

/**
 * Represents a single script action.
 */
@EditorTypes({
    ActionConfig.CallFunction.class, ActionConfig.PlaySound.class, ActionConfig.SetEnabled.class,
    ActionConfig.SetVisible.class, ActionConfig.SetHoverable.class, ActionConfig.SetAlpha.class,
    ActionConfig.FadeAlpha.class, ActionConfig.AnimateAlpha.class, ActionConfig.SetOffset.class,
    ActionConfig.MoveOffset.class, ActionConfig.AnimateOffset.class, ActionConfig.SetSelected.class,
    ActionConfig.SetText.class, ActionConfig.SetStyle.class, ActionConfig.SetConfig.class,
    ActionConfig.RunScript.class, ActionConfig.RunInitScript.class,
    ActionConfig.PlayAnimation.class, ActionConfig.RequestFocus.class, ActionConfig.Wait.class,
    ActionConfig.AddHandler.class, ActionConfig.Conditional.class, ActionConfig.Compound.class,
    ActionConfig.EmitEvent.class, ActionConfig.Log.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable
{
    /** An empty (and thus immutable and sharable) ActionConfig array. */
    public static final ActionConfig[] EMPTY_ARRAY = new ActionConfig[0];

    /**
     * Calls a scoped function.
     */
    public static class CallFunction extends ActionConfig
    {
        /** The name of the function to call. */
        @Editable
        public String name = "";

        @Override
        public void execute (UserInterface iface, ConfigScript script)
        {
            ScopeUtil.resolve(iface.getScope(), name, Function.NULL).call();
        }
    }

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

        @Override
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

        @Override
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

        @Override
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

        @Override
        public void apply (UserInterface iface, Component comp)
        {
            comp.setVisible(visible);
        }
    }

    /**
     * Renders a component visible or invisible.
     */
    public static class SetHoverable extends Targeted
    {
        /** Whether or not the component should be visible. */
        @Editable(hgroup="t")
        public boolean hoverable = true;

        @Override
        public void apply (UserInterface iface, Component comp)
        {
            comp.setHoverable(hoverable);
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

        @Override
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

        /** The actions to perform when animation is completed. */
        @Editable(nullable=true)
        public ActionConfig actionOnComplete;

        @Override
        public void apply (final UserInterface iface, final Component comp)
        {
            iface.addScript(iface.new TickableScript() {
                public void tick (float elapsed) {
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
                    if (actionOnComplete != null) {
                        // This will annoy the 'wait' action, but that
                        // shouldn't be used here anyway.
                        actionOnComplete.execute(iface, null);
                    }
                }
                protected float _time;
            });
        }
    }

    /**
     * Animates a component's transparency using an expression.
     */
    public static class AnimateAlpha extends Targeted
    {
        /** The duration of the animation, or zero to continue indefinitely. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float duration;

        /** The value expression. */
        @Editable
        public FloatExpression value = new FloatExpression.Constant();

        @Override
        public void apply (UserInterface iface, final Component comp)
        {
            final FloatExpression.Evaluator eval = value.createEvaluator(iface.getScope());
            iface.addScript(iface.new TickableScript() {
                public void tick (float elapsed) {
                    if (duration == 0f || (_time += elapsed) < duration) {
                        comp.setAlpha(eval.evaluate());
                    } else {
                        remove();
                    }
                }
                protected float _time;
            });
        }

        @Override
        public void invalidate ()
        {
            value.invalidate();
        }
    }

    /**
     * Sets a component's offset.
     */
    public static class SetOffset extends Targeted
    {
        /** The component offset. */
        @Editable(step=0.01)
        public Transform2D offset = new Transform2D();

        @Override
        public void apply (UserInterface iface, Component comp)
        {
            comp.setOffset(new Transform2D(offset));
        }
    }

    /**
     * Moves a component's offset.
     */
    public static class MoveOffset extends Targeted
    {
        /** The interval over which to move. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float interval = 1f;

        /** The starting offset. */
        @Editable(step=0.01)
        public Transform2D start = new Transform2D();

        /** The ending offset. */
        @Editable(step=0.01)
        public Transform2D end = new Transform2D();

        /** The type of easing to use, if any. */
        @Editable
        public Easing easing = new Easing.None();

        /** The actions to perform when animation is completed. */
        @Editable(nullable=true)
        public ActionConfig actionOnComplete;

        @Override
        public void apply (final UserInterface iface, final Component comp)
        {
            iface.addScript(iface.new TickableScript() {
                public void tick (float elapsed) {
                    if ((_time += elapsed) < interval) {
                        comp.setOffset(start.lerp(end, easing.getTime(_time / interval), _offset));
                    } else {
                        remove();
                    }
                }
                @Override public void cleanup () {
                    super.cleanup();
                    comp.setOffset(_offset.set(end));
                    if (actionOnComplete != null) {
                        // This will annoy the 'wait' action, but that
                        // shouldn't be used here anyway.
                        actionOnComplete.execute(iface, null);
                    }
                }
                protected float _time;
                protected Transform2D _offset = new Transform2D();
            });
        }
    }

    /**
     * Animates a component's offset using an expression.
     */
    public static class AnimateOffset extends Targeted
    {
        /** The duration of the animation, or zero to continue indefinitely. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float duration;

        /** The value expression. */
        @Editable
        public Transform2DExpression value = new Transform2DExpression.Constant();

        @Override
        public void apply (UserInterface iface, final Component comp)
        {
            final ObjectExpression.Evaluator<Transform2D> eval =
                value.createEvaluator(iface.getScope());
            iface.addScript(iface.new TickableScript() {
                public void tick (float elapsed) {
                    if (duration == 0f || (_time += elapsed) < duration) {
                        comp.setOffset(eval.evaluate());
                    } else {
                        remove();
                    }
                }
                protected float _time;
            });
        }

        @Override
        public void invalidate ()
        {
            value.invalidate();
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
        public void apply (UserInterface iface, Component comp)
        {
            if (comp instanceof UserInterface) {
                ((UserInterface)comp).runScript(interfaceScript);
            }
        }
    }

    /**
     * Runs a script on a user interface.
     */
    public static class RunInitScript extends Targeted
    {
        /** The script to run on the interface. */
        @Editable(nullable=true)
        public ConfigReference<InterfaceScriptConfig> interfaceScript;

        @Override
        public void apply (UserInterface iface, Component comp)
        {
            if (comp instanceof UserInterface) {
                ((UserInterface)comp).runInitScript(interfaceScript);
            }
        }
    }

    /**
     * Plays an animation on a model in a renderable view.
     */
    public static class PlayAnimation extends Targeted
    {
        /** The index of the model on which to play the animation. */
        @Editable(min=0, hgroup="t")
        public int index;

        /** The animation to play on the model. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;

        @Override
        public void apply (UserInterface iface, Component comp)
        {
            if (comp instanceof RenderableView) {
                Model[] models = ((RenderableView)comp).getConfigModels();
                if (index < models.length) {
                    models[index].createAnimation(animation).start();
                }
            }
        }
    }

    /**
     * Requests focus on a component.
     */
    public static class RequestFocus extends Targeted
    {
        @Override
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

        @Override
        public void execute (UserInterface iface, final ConfigScript script)
        {
            // sanity check, as actions can be executed outside of scripts
            if (script == null) {
                log.warning("Tried to perform wait action outside of script.");
                return;
            }

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
     * Adds a handler that will execute an action when an event occurs.
     */
    public static class AddHandler extends ActionConfig
    {
        /** The event to handle. */
        @Editable
        public EventConfig event = new EventConfig.Action();

        /** The actions to perform. */
        @Editable
        public ActionConfig[] actions = EMPTY_ARRAY;

        @Override
        public void execute (final UserInterface iface, final ConfigScript script)
        {
            iface.addScript(event.addHandler(iface, new Runnable() {
                public void run () {
                    for (ActionConfig action : actions) {
                        action.execute(iface, script);
                    }
                }
            }));
        }

        @Override
        public void invalidate ()
        {
            for (ActionConfig action : actions) {
                action.invalidate();
            }
        }
    }

    /**
     * Performs one of a number of sub-actions depending on conditions.
     */
    public static class Conditional extends ActionConfig
    {
        /** The cases. */
        @Editable
        public Case[] cases = new Case[0];

        /** The default action. */
        @Editable
        public ActionConfig defaultAction = new CallFunction();

        @Override
        public void execute (UserInterface iface, ConfigScript script)
        {
            Scope scope = iface.getScope();
            for (Case caze : cases) {
                if (caze.condition.createEvaluator(scope).evaluate()) {
                    caze.action.execute(iface, script);
                    return;
                }
            }
            defaultAction.execute(iface, script);
        }

        @Override
        public void invalidate ()
        {
            for (Case caze : cases) {
                caze.condition.invalidate();
                caze.action.invalidate();
            }
        }
    }

    /**
     * Combines an action with a condition.
     */
    public static class Case extends DeepObject
        implements Exportable
    {
        /** The condition for the case. */
        @Editable
        public BooleanExpression condition = new BooleanExpression.Constant(true);

        /** The action itself. */
        @Editable
        public ActionConfig action = new CallFunction();
    }

    /**
     * Performs a number of sub-actions.
     */
    public static class Compound extends ActionConfig
    {
        /** The actions to perform. */
        @Editable
        public ActionConfig[] actions = EMPTY_ARRAY;

        @Override
        public void execute (UserInterface iface, ConfigScript script)
        {
            for (ActionConfig action : actions) {
                action.execute(iface, script);
            }
        }

        @Override
        public void invalidate ()
        {
            for (ActionConfig action : actions) {
                action.invalidate();
            }
        }
    }

    /**
     * Emits an event on the interface.
     */
    public static class EmitEvent extends ActionConfig
    {
        /** The action name. */
        @Editable
        public String action = "";

        @Override
        public void execute (UserInterface iface, ConfigScript script)
        {
            log.info("Emit Event", "action", action);
            iface.dispatchEvent(new ActionEvent(iface, iface.getRoot().getTickStamp(), 0, action));
        }
    }

    /**
     * Prints a log message.
     */
    public static class Log extends ActionConfig
    {
        /** The log message. */
        @Editable
        public String message = "";

        @Override
        public void execute (UserInterface iface, ConfigScript script)
        {
            log.info("Script log action", "message", message);
        }
    }

    /**
     * Executes the action on the specified interface/script.
     */
    public abstract void execute (UserInterface iface, ConfigScript script);

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }
}
