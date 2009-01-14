//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.tudey.config.ActorConfig;

/**
 * Controls an autonomous agent.
 */
public class AgentLogic extends MobileLogic
{
    @Override // documentation inherited
    public boolean tick (int timestamp)
    {
        // advance to current time
        super.tick(timestamp);

        // update the behavior
        _behavior.tick(timestamp);

        return true;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // initialize the behavior logic
        ActorConfig.Agent aconfig = (ActorConfig.Agent)_config;
        _behavior = (BehaviorLogic)_scenemgr.createLogic(aconfig.behavior.getLogicClassName());
        if (_behavior == null) {
            _behavior = new BehaviorLogic.Idle();
        }
        _behavior.init(_scenemgr, aconfig.behavior, this);
    }

    /** The agent's behavior logic. */
    protected BehaviorLogic _behavior;
}
