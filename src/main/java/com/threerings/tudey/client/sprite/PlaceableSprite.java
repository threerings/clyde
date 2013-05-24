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

package com.threerings.tudey.client.sprite;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.scene.Scene;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.util.ShapeConfigElement;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents a placeable entry.
 */
public class PlaceableSprite extends EntrySprite
    implements ConfigUpdateListener<PlaceableConfig>
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Returns the model for this implementation, or <code>null</code> for none.
         */
        public Model getModel ()
        {
            return null;
        }

        /**
         * Returns the sprite's floor flags.
         */
        public int getFloorFlags ()
        {
            return 0x0;
        }

        /**
         * Determines whether the implementation is hoverable.
         */
        public boolean isHoverable ()
        {
            return false;
        }

        /**
         * Determines whether the implementation is clickable.
         */
        public boolean isClickable ()
        {
            return false;
        }

        /**
         * Returns the implementation's tooltip text, or <code>null</code> for none.
         */
        public String getTooltipText ()
        {
            return null;
        }

        /**
         * Returns the implementation's tooltip timeout, or -1 to use the default.
         */
        public float getTooltipTimeout ()
        {
            return -1f;
        }

        /**
         * Returns the implementation's tooltip window style.
         */
        public String getTooltipWindowStyle ()
        {
            return "Default/TooltipWindow";
        }

        /**
         * Creates a tooltip component for the implementation (will only be called if
         * {@link #getTooltipText} returns true).
         */
        public Component createTooltipComponent (String tiptext)
        {
            return null;
        }

        /**
         * Dispatches an event on the implementation.
         *
         * @return true if the implementation handled the event, false if it should be handled
         * elsewhere.
         */
        public boolean dispatchEvent (Event event)
        {
            return false;
        }

        /**
         * Updates the implementation to match the entry state.
         */
        public void update (PlaceableEntry entry)
        {
            // nothing by default
        }

        @Override
        public String getScopeName ()
        {
            return "impl";
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (TudeyContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
            _scene.add(_model = new Model(ctx));
            _model.setUserObject(parentScope);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PlaceableConfig.Original config)
        {
            _model.setConfig(getModelConfig(config));

            // update the footprint
            boolean selected = ((PlaceableSprite)_parentScope).isSelected();
            if (selected && _footprint == null) {
                _footprint = new ShapeConfigElement(_ctx);
                _footprint.getColor().set(SELECTED_COLOR);
                _footprint.setConfig(config.shape, true);
                _scene.add(_footprint);
            } else if (!selected && _footprint != null) {
                _scene.remove(_footprint);
                _footprint = null;
            }
        }

        @Override
        public Component createTooltipComponent (String tiptext)
        {
            return Component.createDefaultTooltipComponent(_ctx, tiptext);
        }

        @Override
        public Model getModel ()
        {
            return _model;
        }

        @Override
        public void update (PlaceableEntry entry)
        {
            _model.setLocalTransform(entry.transform);
            if (_footprint != null) {
                _footprint.setTransform(entry.transform);
            }
        }

        @Override
        public void dispose ()
        {
            super.dispose();
            _scene.remove(_model);
            if (_footprint != null) {
                _scene.remove(_footprint);
            }
        }

        /**
         * Retrieves the model config reference from the configuration.
         */
        protected ConfigReference<ModelConfig> getModelConfig (PlaceableConfig.Original config)
        {
            return config.model;
        }

        /** The renderer context. */
        protected TudeyContext _ctx;

        /** The model. */
        protected Model _model;

        /** The footprint. */
        protected ShapeConfigElement _footprint;

        /** The scene to which we add our model/footprint. */
        @Bound
        protected Scene _scene;
    }

    /**
     * A prop implementation.
     */
    public static class Prop extends Original
    {
        /**
         * Creates a new prop implementation.
         */
        public Prop (TudeyContext ctx, Scope parentScope, PlaceableConfig.Prop config)
        {
            this(ctx, parentScope);
            setConfig(config);
        }

        @Override
        public void setConfig (PlaceableConfig.Original config)
        {
            super.setConfig(config);
            _config = (PlaceableConfig.Prop)config;
        }

        @Override
        public int getFloorFlags ()
        {
            return _config.floorFlags;
        }

        /**
         * Creates a new prop implementation (for use by subclasses).
         */
        protected Prop (TudeyContext ctx, Scope parentScope)
        {
            super(ctx, parentScope);
        }

        /** The prop configuration. */
        protected PlaceableConfig.Prop _config;
    }

    /**
     * A prop that may be clicked to perform some action.
     */
    public static class ClickableProp extends Prop
    {
        /**
         * Creates a new clickable prop implementation.
         */
        public ClickableProp (
            TudeyContext ctx, Scope parentScope, PlaceableConfig.ClickableProp config)
        {
            super(ctx, parentScope, config);
            _model.setColorState(_cstate);
        }

        @Override
        public void setConfig (PlaceableConfig.Original config)
        {
            super.setConfig(config);
            _config = (PlaceableConfig.ClickableProp)config;
            if (_cstate == null) {
                _cstate = new ColorState();
            }
            _defaultAnim = (_config.defaultAnimation == null) ?
                null : _model.createAnimation(_config.defaultAnimation);
            _hoverAnim = (_config.hoverAnimation == null) ?
                null : _model.createAnimation(_config.hoverAnimation);
            setHover(_hover);
        }

        @Override
        public boolean isHoverable ()
        {
            return true;
        }

        @Override
        public boolean isClickable ()
        {
            return true;
        }

        @Override
        public boolean dispatchEvent (Event event)
        {
            if (!(event instanceof MouseEvent)) {
                return false;
            }
            int type = ((MouseEvent)event).getType();
            if (type == MouseEvent.MOUSE_ENTERED) {
                setHover(true);
            } else if (type == MouseEvent.MOUSE_EXITED) {
                setHover(false);
            } else if (type == MouseEvent.MOUSE_PRESSED) {
                _config.action.execute(_ctx, _view, (PlaceableSprite)_parentScope);
            } else {
                return false;
            }
            return true;
        }

        /**
         * Sets the hover state.
         */
        protected void setHover (boolean hover)
        {
            // update the color
            _hover = hover;
            _cstate.getColor().set(hover ? _config.hoverColor : _config.defaultColor);
            _cstate.setDirty(true);

            // update the animations
            Animation active, inactive;
            if (hover) {
                active = _hoverAnim;
                inactive = _defaultAnim;
            } else {
                active = _defaultAnim;
                inactive = _hoverAnim;
            }
            if (inactive != null && inactive.isPlaying()) {
                inactive.stop();
            }
            if (active != null && !active.isPlaying()) {
                active.start();
            }
        }

        /** The prop configuration. */
        protected PlaceableConfig.ClickableProp _config;

        /** The color state. */
        protected ColorState _cstate;

        /** The default and hover animations, if any. */
        protected Animation _defaultAnim, _hoverAnim;

        /** Whether or not the hover state is active. */
        protected boolean _hover;

        /** The containing view. */
        @Bound
        protected TudeySceneView _view;
    }

    /**
     * A marker implementation.
     */
    public static class Marker extends Original
    {
        /**
         * Creates a new marker implementation.
         */
        public Marker (TudeyContext ctx, Scope parentScope, PlaceableConfig.Original config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }
    }

    /**
     * Creates a new placeable sprite.
     */
    public PlaceableSprite (TudeyContext ctx, TudeySceneView view, PlaceableEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    /**
     * Returns a reference to the sprite implementation.
     */
    public Implementation getImplementation ()
    {
        return _impl;
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PlaceableConfig> event)
    {
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override
    public int getFloorFlags ()
    {
        return _impl.getFloorFlags();
    }

    @Override
    public boolean isHoverable ()
    {
        return _impl.isHoverable();
    }

    @Override
    public boolean isClickable ()
    {
        return _impl.isClickable();
    }

    @Override
    public String getTooltipText ()
    {
        return _impl.getTooltipText();
    }

    @Override
    public float getTooltipTimeout ()
    {
        return _impl.getTooltipTimeout();
    }

    @Override
    public String getTooltipWindowStyle ()
    {
        return _impl.getTooltipWindowStyle();
    }

    @Override
    public Component createTooltipComponent (String tiptext)
    {
        return _impl.createTooltipComponent(tiptext);
    }

    @Override
    public boolean dispatchEvent (Event event)
    {
        return _impl.dispatchEvent(event);
    }

    @Override
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override
    public void update (Entry entry)
    {
        setConfig((_entry = (PlaceableEntry)entry).placeable);
        _impl.update(_entry);
    }

    @Override
    public Model getModel ()
    {
        return _impl.getModel();
    }

    @Override
    public void setSelected (boolean selected)
    {
        super.setSelected(selected);
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        _impl.dispose();
        if (_config != null) {
            _config.removeListener(this);
        }
    }

    /**
     * Sets the configuration of this placeable.
     */
    protected void setConfig (ConfigReference<PlaceableConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(PlaceableConfig.class, ref));
    }

    /**
     * Sets the configuration of this placeable.
     */
    protected void setConfig (PlaceableConfig config)
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
     * Updates the placeable to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        PlaceableConfig.Original original = (_config == null) ?
            null : _config.getOriginal(_ctx.getConfigManager());
        original = (original == null) ? PlaceableConfig.NULL_ORIGINAL : original;
        Implementation nimpl = original.getSpriteImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /** The scene entry. */
    protected PlaceableEntry _entry;

    /** The placeable configuration. */
    protected PlaceableConfig _config = INVALID_CONFIG;

    /** The placeable implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An invalid config used to force an initial update. */
    protected static PlaceableConfig INVALID_CONFIG = new PlaceableConfig();

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
