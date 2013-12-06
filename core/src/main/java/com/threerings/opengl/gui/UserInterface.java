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

package com.threerings.opengl.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.config.ManagedConfig;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.MutableBoolean;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;

import com.threerings.opengl.gui.config.InterfaceScriptConfig;
import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.opengl.gui.event.ComponentListener;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlContextWrapper;
import com.threerings.opengl.util.Tickable;

import static com.threerings.opengl.gui.Log.log;

/**
 * A user interface component configured from a resource.
 */
public class UserInterface extends Container
{
    /**
     * An interface that can be implemented by a controller class, specified by name
     * in the UserInterfaceConfig, to automatically wire-up functionality to a UI.
     */
    public interface Controller
    {
        /**
         * Initialize the controller and return true on success.
         * Return false if the controller cannot be used due to being in the editor, or whatever.
         */
        boolean init (GlContext ctx, UserInterface ui);

        /**
         * Called when the controller should activate/deactivate. This may change
         * if the UI is added or removed to the hierarchy, made visible or invisible, or
         * enabled or disabled, as well as possible future enhancements.
         *
         * Your implementation should be idempotent and capable of ignoring repeat requests
         * to set active or inactive if already so.
         */
        void setActive (boolean active);
    }

    /**
     * Represents a script running on the interface.
     */
    public abstract class Script
    {
        /**
         * Called after the script has been added to perform any necessary initialization.
         */
        public void init ()
        {
            // nothing by default
        }

        /**
         * Called when the script is stopped to perform any necessary cleanup.
         */
        public void cleanup ()
        {
            // nothing by default
        }

        /**
         * Removes the script.
         */
        public void remove ()
        {
            _scripts.remove(this);
            cleanup();
        }
    }

    /**
     * A script that runs actions only once.
     */
    public class InitScript extends Script
            implements ConfigUpdateListener<InterfaceScriptConfig>
    {
        public InitScript (InterfaceScriptConfig config)
        {
            if ((_config = config) != null) {
                _config.addListener(this);
            }
            updateFromConfig();
        }

        @Override
        public void init ()
        {
            if (_original == null) {
                return;
            }

            // Init scripts are fire-and-forget.
            for (int ii = 0; ii < _original.actions.length; ii++) {
                _original.actions[ii].action.execute(UserInterface.this, null);
            }
        }

        @Override
        public void cleanup ()
        {
            super.cleanup();
            if (_config != null) {
                _config.removeListener(this);
            }
        }

        // documentation inherited from interface ConfigUpdateListener
        public void configUpdated (ConfigEvent<InterfaceScriptConfig> event)
        {
            updateFromConfig();
        }

        /**
         * Updates the state in response to a change in the config.
         */
        protected void updateFromConfig ()
        {
            if (_config != null &&
                    (_original = _config.getOriginal(_ctx.getConfigManager())) == null) {
                _original = new InterfaceScriptConfig.Original();
            }
        }

        /** The script configuration. */
        protected InterfaceScriptConfig _config;

        /** The original configuration. */
        protected InterfaceScriptConfig.Original _original;
    }

    /**
     * A script that should be ticked at every frame.
     */
    public abstract class TickableScript extends Script
        implements Tickable
    {
        @Override
        public void init ()
        {
            _scriptRoot = getRoot();
            _scriptRoot.addTickParticipant(this);
            tick(0f); // tick once to initialize
        }

        @Override
        public void cleanup ()
        {
            _scriptRoot.removeTickParticipant(this);
        }

        /** The root used by this script. */
        protected transient Root _scriptRoot;
    }

    /**
     * Represents a script controlled by a configuration.
     */
    public class ConfigScript extends TickableScript
        implements ConfigUpdateListener<InterfaceScriptConfig>
    {
        /**
         * Creates a new script.
         */
        public ConfigScript (InterfaceScriptConfig config)
        {
            if ((_config = config) != null) {
                _config.addListener(this);
            }
            updateFromConfig();
        }

        /**
         * Pauses or unpauses the script.
         */
        public void setPaused (boolean paused)
        {
            if (_paused == paused) {
                return; // no-op
            }
            if (_paused = paused) {
                _scriptRoot.removeTickParticipant(this);
            } else {
                _scriptRoot.addTickParticipant(this);
            }
        }

        // documentation inherited from interface ConfigUpdateListener
        public void configUpdated (ConfigEvent<InterfaceScriptConfig> event)
        {
            updateFromConfig();
        }

        // documentation inherited from interface Tickable
        public void tick (float elapsed)
        {
            if (_dropTick) {
                _dropTick = false;
                return;
            }
            _time += elapsed;
            executeActions();
            if (_paused) {
                return;
            }

            // check for loop or completion
            if (_original.loopDuration > 0f) {
                if (_time >= _original.loopDuration) {
                    _time %= _original.loopDuration;
                    _aidx = 0;
                    executeActions();
                }
            } else if (_aidx >= _original.actions.length) {
                remove();
            }
        }

        @Override
        public void cleanup ()
        {
            super.cleanup();
            if (_config != null) {
                _config.removeListener(this);
            }
        }

        @Override
        public void init ()
        {
            super.init();
            // We drop our first tick after initialization, since it includes an elapsed time from
            // before the script was running
            _dropTick = true;
        }

        /**
         * Updates the state in response to a change in the config.
         */
        protected void updateFromConfig ()
        {
            if (_config == null ||
                    (_original = _config.getOriginal(_ctx.getConfigManager())) == null) {
                _original = new InterfaceScriptConfig.Original();
            }
        }

        /**
         * Executes all actions scheduled before or at the current time.
         */
        protected void executeActions ()
        {
            for (; _aidx < _original.actions.length &&
                    _original.actions[_aidx].time <= _time && !_paused; _aidx++) {
                _original.actions[_aidx].action.execute(UserInterface.this, this);
            }
        }

        /** The script configuration. */
        protected InterfaceScriptConfig _config;

        /** The original configuration. */
        protected InterfaceScriptConfig.Original _original;

        /** The amount of time elapsed. */
        protected float _time;

        /** The index of the next action. */
        protected int _aidx;

        /** If set, we're paused waiting for something to happen. */
        protected boolean _paused;

        /** If we drop the next tick. */
        protected boolean _dropTick;
    }

    /**
     * Creates a new user interface.
     */
    public UserInterface (GlContext ctx)
    {
        this(ctx, (UserInterfaceConfig)null);
    }

    /**
     * Creates a new interface with the named configuration.
     */
    public UserInterface (GlContext ctx, String name)
    {
        this(ctx, ctx.getConfigManager().getConfig(UserInterfaceConfig.class, name));
    }

    /**
     * Creates a new interface with the named configuration and arguments.
     */
    public UserInterface (
        GlContext ctx, String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        this(ctx, ctx.getConfigManager().getConfig(
            UserInterfaceConfig.class, name, firstKey, firstValue, otherArgs));
    }

    /**
     * Creates a new interface with the referenced configuration.
     */
    public UserInterface (GlContext ctx, ConfigReference<UserInterfaceConfig> ref)
    {
        this(ctx, ctx.getConfigManager().getConfig(UserInterfaceConfig.class, ref));
    }

    /**
     * Creates a new interface with the given configuration.
     */
    public UserInterface (GlContext ctx, UserInterfaceConfig config)
    {
        this(ctx, config, ctx.getScope());
    }

    /**
     * Creates a new interface for use with the InterfaceTester.
     */
    public UserInterface (GlContext ctx, UserInterfaceConfig config, Scope parentScope)
    {
        super(ctx, new BorderLayout());
        _scope.setParentScope(parentScope);

        _ctx = new GlContextWrapper(ctx) {
            public ConfigManager getConfigManager () {
                return (_config == null) ?
                    _wrapped.getConfigManager() : _config.getConfigManager();
            }
        };
        setConfig(config);
    }

    /**
     * Returns a reference to the interface's scope.
     */
    public DynamicScope getScope ()
    {
        return _scope;
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (String name)
    {
        setConfig(_ctx.getConfigManager().getConfig(UserInterfaceConfig.class, name));
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (ConfigReference<UserInterfaceConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(UserInterfaceConfig.class, ref));
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (
        String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        setConfig(_ctx.getConfigManager().getConfig(
            UserInterfaceConfig.class, name, firstKey, firstValue, otherArgs));
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (UserInterfaceConfig config)
    {
        if (_config == config) {
            return;
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Returns a reference to the configuration.
     */
    public UserInterfaceConfig getConfig ()
    {
        return _config;
    }

    /**
     * Shortcut method for setting the enabled status of all components bearing the specified tag.
     */
    public void setEnabled (String tag, boolean enabled)
    {
        for (Component comp : getComponents(tag)) {
            comp.setEnabled(enabled);
        }
    }

    /**
     * Shortcut method for setting the visibility status of all components bearing the specified
     * tag.
     */
    public void setVisible (String tag, boolean visible)
    {
        for (Component comp : getComponents(tag)) {
            comp.setVisible(visible);
        }
    }

    /**
     * Shortcut method for setting the hoverable status of all components bearing the specified
     * tag.
     */
    public void setHoverable (String tag, boolean hoverable)
    {
        for (Component comp : getComponents(tag)) {
            comp.setHoverable(hoverable);
        }
    }

    /**
     * Shortcut method for setting the text of all {@link TextComponent}s bearing the specified
     * tag.
     */
    public void setText (String tag, String text)
    {
        for (TextComponent comp : getComponents(tag, TextComponent.class)) {
            comp.setText(text);
        }
    }

    /**
     * Shortcut method for retrieving the text of a tagged {@link TextComponent}.
     */
    public String getText (String tag)
    {
        // first look for an editable field...
        TextComponent comp = getComponent(tag, EditableTextComponent.class);
        if (comp == null) {
            comp = getComponent(tag, TextComponent.class);
            if (comp == null) {
                log.warning("Not a text component.", "tag", tag, "components", getComponents(tag));
                return "";
            }
        }
        return comp.getText();
    }

    /**
     * Shortcut method for retrieving the selected state of a tagged {@link ToggleButton}.
     */
    public boolean isSelected (String tag)
    {
        ToggleButton comp = getComponent(tag, ToggleButton.class);
        if (comp == null) {
            log.warning("Not a toggle button.", "tag", tag, "component", getComponent(tag));
            return false;
        }
        return comp.isSelected();
    }

    /**
     * Shortcut method to add a listener to all components with the specified tag.
     */
    public void addListener (String tag, ComponentListener listener)
    {
        for (Component comp : getComponents(tag)) {
            comp.addListener(listener);
        }
    }

    /**
     * Shortcut method to remove a listener from all components with the specified tag.
     */
    public void removeListener (String tag, ComponentListener listener)
    {
        for (Component comp : getComponents(tag)) {
            comp.removeListener(listener);
        }
    }

    /**
     * Shortcut method to remove all listeners from all components with the specified tag.
     */
    public void removeAllListeners (String tag)
    {
        for (Component comp : getComponents(tag)) {
            comp.removeAllListeners();
        }
    }

    /**
     * Sets the alpha for all components with the specified tag.
     */
    public void setAlpha (String tag, float alpha)
    {
        for (Component comp : getComponents(tag)) {
            comp.setAlpha(alpha);
        }
    }

    /**
     * Returns a reference to the first component registered with the specified tag, or
     * <code>null</code> if there are no such components.
     */
    public Component getComponent (String tag)
    {
        return Iterables.getFirst(getComponents(tag), null);
    }

    /**
     * Returns a reference to the first component registered with the specified tag and
     * implementing the specified class or interface, or <code>null</code> if there are
     * no such components.
     */
    public <C extends Component> C getComponent (String tag, Class<C> clazz)
    {
        return Iterables.getFirst(getComponents(tag, clazz), null);
    }

    /**
     * Returns the Components registered with the specified tag.
     */
    public <C extends Component> Iterable<C> getComponents (String tag, Class<C> clazz)
    {
        return Iterables.filter(getComponents(tag), clazz);
    }

    /**
     * Returns the Components registered with the specified tag.
     */
    public Iterable<Component> getComponents (String tag)
    {
        // a blank tag implies "this"
        if (StringUtil.isBlank(tag)) {
            return Collections.<Component>singletonList(this);
        }
        // parse simple paths
        int idx = tag.indexOf('/');
        if (idx == -1) {
            //return Iterables.unmodifiableIterable(_tagged.get(tag));
            return _tagged.get(tag);
        }
        // assemble all components in all sub-trees that have the correct path
        final String nextpath = tag.substring(idx + 1);
        tag = tag.substring(0, idx);
        return Iterables.concat(Iterables.transform(getComponents(tag, UserInterface.class),
            new Function<UserInterface, Iterable<Component>>() {
                public Iterable<Component> apply (UserInterface comp) {
                    return comp.getComponents(nextpath);
                }
            }));
    }

    /**
     * Get the first ComboBox with the specified tag.
     */
    public ComboBox<String> getComboBox (String tag)
    {
        @SuppressWarnings("unchecked") // safe because the UI editor will put Strings in it
        ComboBox<String> box = (ComboBox<String>)getComponent(tag, ComboBox.class);
        return box;
    }

    /**
     * Clear the items and return the first ComboBox with the specified tag.
     */
    public <T> ComboBox<T> getAndClearComboBox (String tag)
    {
        @SuppressWarnings("unchecked") // safe because we will clear it
        ComboBox<T> box = (ComboBox<T>)getComponent(tag, ComboBox.class);
        if (box != null) {
            box.clearItems();
        }
        return box;
    }

    /**
     * Returns a reference to the tagged component map.
     */
    public Map<String, Collection<Component>> getTagged ()
    {
        return _tagged.asMap();
    }

    /**
     * Replaces the component at the tag with a new component.
     */
    public boolean replace (String tag, Component newc)
    {
        List<Component> list = _tagged.get(tag);
        if (!list.isEmpty()) {
            Component oldc = list.get(0);
            if (oldc.getParent().replace(oldc, newc)) {
                list.set(0, newc);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the root reference stored when the interface was added.
     */
    public Root getRoot ()
    {
        return _root;
    }

    /**
     * Runs a script on the interface.
     */
    public void runScript (String name)
    {
        runScript(_ctx.getConfigManager().getConfig(InterfaceScriptConfig.class, name));
    }

    /**
     * Runs a script on the interface.
     */
    public void runScript (ConfigReference<InterfaceScriptConfig> ref)
    {
        runScript(_ctx.getConfigManager().getConfig(InterfaceScriptConfig.class, ref));
    }

    /**
     * Runs a script on the interface.
     */
    public void runScript (
        String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        runScript(_ctx.getConfigManager().getConfig(InterfaceScriptConfig.class,
            name, firstKey, firstValue, otherArgs));
    }

    /**
     * Runs a script on the interface.
     */
    public void runScript (InterfaceScriptConfig config)
    {
        addScript(new ConfigScript(config));
    }

    /**
     * Runs an initialization script on the interface.
     */
    public void runInitScript (ConfigReference<InterfaceScriptConfig> ref)
    {
        addScript(new InitScript(
            _ctx.getConfigManager().getConfig(InterfaceScriptConfig.class, ref)));
    }

    /**
     * Runs a script on the interface after the current tick.
     */
    public void runScriptLater (String name)
    {
        runScriptLater(_ctx.getConfigManager().getConfig(InterfaceScriptConfig.class, name));
    }

    /**
     * Runs a script on the interface after the current tick.
     */
    public void runScriptLater (
            String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        runScriptLater(_ctx.getConfigManager().getConfig(InterfaceScriptConfig.class,
                name, firstKey, firstValue, otherArgs));
    }

    /**
     * Runs a script on the interface after the current tick.
     */
    public void runScriptLater (InterfaceScriptConfig config)
    {
        if (_scriptQueue == null) {
            // Lazy-instantiate this so it's not sitting on every UI.
            _scriptQueue = new ScriptQueue();
        }
        _scriptQueue.add(config);
    }

    /**
     * Adds a script to the interface.
     */
    public void addScript (Script script)
    {
        if (_root == null && !(script instanceof InitScript)) {
            throw new IllegalStateException("Can't add script to non-added interface.");
        }
        _scripts.add(script);
        script.init();
    }

    /**
     * Clears all scripts running on the interface.
     */
    public void clearScripts ()
    {
        // clean up any remaining scripts
        for (Script script : _scripts) {
            script.cleanup();
        }
        _scripts.clear();
    }

    @Override
    public void setEnabled (boolean enabled)
    {
        super.setEnabled(enabled);
        checkController();
    }

    @Override
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        checkController();
    }

    @Override
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        if (event.getConfig() instanceof UserInterfaceConfig) {
            updateFromConfig();
        } else {
            super.configUpdated(event);
        }
    }

    @Override
    public Objects.ToStringHelper toStringHelper ()
    {
        return super.toStringHelper()
            .add("config", (_config == null) ? null : _config.getName());
    }

    @Override
    protected void wasAdded ()
    {
        super.wasAdded();
        Window window = getWindow();
        _root = (window == null) ? null : window.getRoot();
        if (_root == null) {
            return;
        }

        // play the addition sound, if any
        UserInterfaceConfig.Original original = getOriginal();
        if (original.addSound != null) {
            _root.playSound(original.addSound);
        }

        checkController();

        // perform the addition action, if any
        if (original.addAction != null) {
            original.addAction.execute(this, null);
        }
    }

    @Override
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_root == null) {
            return;
        }

        checkController();

        // play the removal sound, if any
        UserInterfaceConfig.Original original = getOriginal();
        if (original.removeSound != null) {
            _root.playSound(original.removeSound);
        }

        // perform the removal action, if any
        if (original.removeAction != null) {
            original.removeAction.execute(this, null);
        }

        clearScripts();

        _root = null;
    }

    /**
     * Updates the interface to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        _tagged.clear();

        if (_controller != null) {
            _controller.setActive(false);
        }
        _controller = null;

        Component ocomp = (getComponentCount() == 0) ? null : getComponent(0);
        Component ncomp = (_config == null) ? null : _config.getComponent(_ctx, _scope, ocomp);
        removeAll();
        if (ncomp != null) {
            add(ncomp, BorderLayout.CENTER);
        }

        UserInterfaceConfig.Original original = getOriginal();
        String controller = original.controller;
        if (!"".equals(controller)) {
            try {
                Controller c = (Controller)Class.forName(controller).newInstance();
                if (c.init(_ctx.getApp(), this)) {
                    _controller = c;
                    checkController();
                }
            } catch (ClassNotFoundException cnfe) {
                log.warning("Controller not found: " + controller);

            } catch (Exception e) {
                log.warning("Error initializing controller", "controller", controller, e);
                _controller = null;
            }
        }

        // perform the init action, if any
        if (original.initAction != null) {
            original.initAction.execute(this, null);
        }
    }

    /**
     * Convenience to get a non-null Original implementation.
     */
    protected UserInterfaceConfig.Original getOriginal ()
    {
        UserInterfaceConfig.Original orig = (_config == null) ? null : _config.getOriginal();
        if (orig == null) {
            orig = new UserInterfaceConfig.Original();
        }
        return orig;
    }

    /**
     * Registers a group of components mapped by tag.
     */
    @Scoped
    protected void registerComponents (Map<String, Collection<Component>> tagged)
    {
        for (Map.Entry<String, Collection<Component>> entry : tagged.entrySet()) {
            _tagged.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    /**
     * Registers a component with the specified tag.
     */
    @Scoped
    protected void registerComponent (String tag, Component comp)
    {
        _tagged.put(tag, comp);
    }

    /**
     * Check/set the controller's active state.
     */
    protected void checkController ()
    {
        if (_controller != null) {
            _controller.setActive(isShowing() && isEnabled());
        }
    }

    protected class UIScope
        extends DynamicScope
    {
        public UIScope (Object owner, String name)
        {
            super(owner, name, null);
        }

        @Override
        public <T> T get (String name, Class<T> clazz)
        {
            int dot = name.indexOf('.');
            if (dot != -1) {
                Component c = getComponent(name.substring(0, dot));
                Object value = null;
                if (c != null) {
                    String arg = name.substring(dot + 1);

                    if (arg.equals("visible")) {
                        value = new MutableBoolean(c.isVisible());
                    } else if (arg.equals("enabled")) {
                        value = new MutableBoolean(c.isEnabled());
                    } else if (arg.equals("added")) {
                        value = new MutableBoolean(c.isAdded());
                    } else if (arg.equals("hoverable")) {
                        value = new MutableBoolean(c.isHoverable());
                    } else if (arg.equals("selected") && c instanceof ToggleButton) {
                        value = new MutableBoolean(((ToggleButton)c).isSelected());
                    }
                }
                if (clazz.isInstance(value)) {
                    return clazz.cast(value);
                }
            }
            return super.get(name, clazz);
        }
    }

    protected class ScriptQueue
    {
        public void add (InterfaceScriptConfig config) {
            _queue.add(config);
            if (!_running) {
                _ctx.getApp().getRunQueue().postRunnable(_runnable);
                _running = true;
            }
        }

        /** The queue of scripts to add. */
        protected List<InterfaceScriptConfig> _queue = Lists.newArrayList();

        /** Whether the runnable has been posted. */
        protected boolean _running;

        /** The actual runnable. */
        protected Runnable _runnable = new Runnable() {
            public void run () {
                for (InterfaceScriptConfig config : _queue) {
                    runScript(config);
                }
                _queue.clear();
                _running = false;
            }
        };

    }

    /** The user interface scope. */
    protected DynamicScope _scope = new UIScope(this, "interface");

    /** The configuration of this interface. */
    protected UserInterfaceConfig _config;

    /** Our controller, if any. */
    protected Controller _controller;

    /** The sets of components registered under each tag. */
    protected ListMultimap<String, Component> _tagged = ArrayListMultimap.create();

    /** The root to which we were added. */
    protected Root _root;

    /** The scripts currently running on the interface. */
    protected List<Script> _scripts = Lists.newArrayList();

    /** The scripts queued to start execution on the next tick. */
    protected ScriptQueue _scriptQueue;

    /** A container for the interface epoch. */
    @Scoped
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());
}
