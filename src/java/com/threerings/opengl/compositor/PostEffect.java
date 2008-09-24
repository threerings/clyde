//
// $Id$

package com.threerings.opengl.compositor;

import java.lang.Comparable;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;

import com.threerings.opengl.compositor.config.PostEffectConfig;
import com.threerings.opengl.compositor.config.PostEffectConfig.Technique;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

/**
 * Handles a post effect.
 */
public class PostEffect extends SimpleScope
    implements ConfigUpdateListener<PostEffectConfig>, Comparable<PostEffect>
{
    /**
     * Creates a new post effect.
     */
    public PostEffect (GlContext ctx, Scope parentScope, PostEffectConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this effect.
     */
    public void setConfig (PostEffectConfig config)
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
     * Renders this post effect.
     *
     * @param idx the effect's index within the compositor list.
     */
    public void render (int idx)
    {
        _ctx.getCompositor().renderPrevious(idx);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PostEffectConfig> event)
    {
        updateFromConfig();
    }

    // documentation inherited from interface Comparable
    public int compareTo (PostEffect other)
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
            log.warning("No technique available to render post effect.",
                "config", _config.getName(), "scheme", scheme);
            technique = NOOP_TECHNIQUE;
        }

    }

    /** The application context. */
    protected GlContext _ctx;

    /** The post effect configuration. */
    protected PostEffectConfig _config;

    /** The priority of the effect. */
    protected int _priority;

    /** A technique that does nothing. */
    protected static final Technique NOOP_TECHNIQUE = new Technique();
}
