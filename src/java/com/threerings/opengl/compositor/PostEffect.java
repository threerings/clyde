//
// $Id$

package com.threerings.opengl.compositor;

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
    implements ConfigUpdateListener<PostEffectConfig>
{
    /**
     * Creates a new post effect.
     */
    protected PostEffect (GlContext ctx, Scope parentScope, PostEffectConfig config)
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

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PostEffectConfig> event)
    {
        updateFromConfig();
    }

    /**
     * Updates the effect to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        String scheme = ScopeUtil.resolve(_parentScope, "renderScheme", (String)null);
        Technique technique = _config.getTechnique(_ctx, scheme);
        if (technique == null) {
            log.warning("No technique available to render post effect.",
                "config", _config.getName(), "scheme", scheme);
        }

    }

    /** The application context. */
    protected GlContext _ctx;

    /** The post effect configuration. */
    protected PostEffectConfig _config;
}
