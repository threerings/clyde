//
// $Id$

package com.threerings.tudey.tools;

import com.threerings.crowd.data.BodyObject;

import com.threerings.config.ConfigReference;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Scene manager for tools.
 */
public class ToolSceneManager extends TudeySceneManager
{
    @Override // documentation inherited
    protected ConfigReference<ActorConfig> getPawnConfig (BodyObject body)
    {
        return new ConfigReference<ActorConfig>("Character/PC/Editor");
    }
}
