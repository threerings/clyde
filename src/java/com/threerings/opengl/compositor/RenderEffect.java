//
// $Id$

package com.threerings.opengl.compositor;

import java.lang.Comparable;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;

import com.threerings.opengl.compositor.config.RenderEffectConfig;
import com.threerings.opengl.compositor.config.RenderEffectConfig.Technique;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

/**
 * Handles a render effect.
 */
public class RenderEffect extends SimpleScope
    implements ConfigUpdateListener<RenderEffectConfig>, Comparable<RenderEffect>
{
    /**
     * Creates a new render effect.
     */
    public RenderEffect (GlContext ctx, Scope parentScope, RenderEffectConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this effect.
     */
    public void setConfig (RenderEffectConfig config)
    {
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Returns the effect's priority.
     */
    public int getPriority ()
    {
        return _priority;
    }

    /**
     * Renders this effect.
     *
     * @param idx the effect's index within the compositor list.
     */
    public void render (int idx)
    {
        _ctx.getCompositor().renderPrevious(idx);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<RenderEffectConfig> event)
    {
        updateFromConfig();
    }

    // documentation inherited from interface Comparable
    public int compareTo (RenderEffect other)
    {
        return _priority - other._priority;
    }

    /**
     * Updates the effect to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // get the effect priority
        _priority = (_config == null) ? 0 : _config.getPriority(_ctx);

        // find a technique to render the effect
        String scheme = ScopeUtil.resolve(_parentScope, "renderScheme", (String)null);
        Technique technique = (_config == null) ?
            NOOP_TECHNIQUE : _config.getTechnique(_ctx, scheme);
        if (technique == null) {
            log.warning("No technique available to render effect.",
                "config", _config.getName(), "scheme", scheme);
            technique = NOOP_TECHNIQUE;
        }

    }

    /** The application context. */
    protected GlContext _ctx;

    /** The render effect configuration. */
    protected RenderEffectConfig _config;

    /** The priority of the effect. */
    protected int _priority;

    /** A technique that does nothing. */
    protected static final Technique NOOP_TECHNIQUE = new Technique();
}
