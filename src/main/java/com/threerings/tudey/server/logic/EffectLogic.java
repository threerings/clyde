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

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

/**
 * Handles an effect on the server.
 */
public class EffectLogic extends Logic
{
    /**
     * Initializes the logic.
     *
     * @param translation an offset from the target's translation, or null, or the absolute
     * translation if there's no target.
     * @param rotation an offset from the target's rotation, or the absolute rotation if there's
     * no target.
     */
    public void init (
        TudeySceneManager scenemgr, ConfigReference<EffectConfig> ref,
        EffectConfig.Original config, int timestamp, Logic target,
        Vector2f translation, float rotation)
    {
        super.init(scenemgr);
        _config = config;
        _target = target;

        EntityKey targetKey;
        if (target != null) {
            targetKey = target.getEntityKey();
            translation = (translation == null)
                ? target.getTranslation()
                : translation.add(target.getTranslation());
            rotation = FloatMath.normalizeAngle(rotation + target.getRotation());

        } else {
            targetKey = null;
        }

        _effect = createEffect(ref, timestamp, targetKey, translation, rotation);
        _effect.init(scenemgr.getConfigManager());
        _shape = config.shape.getShape().transform(new Transform2D(translation, rotation));
        _action = (config.action == null) ? null : createAction(config.action, this);

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Returns a reference to the effect fired.
     */
    public Effect getEffect ()
    {
        return _effect;
    }

    /**
     * Returns a reference to the shape of the effect.
     */
    public Shape getShape ()
    {
        return _shape;
    }

    @Override
    public boolean isVisible (PawnLogic pawn)
    {
        return !_config.targetOnly || _target == pawn;
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _effect.getTranslation();
    }

    @Override
    public float getRotation ()
    {
        return _effect.getRotation();
    }

    /**
     * Creates the effect for this logic.
     */
    protected Effect createEffect (
        ConfigReference<EffectConfig> ref, int timestamp,
        EntityKey target, Vector2f translation, float rotation)
    {
        return _config.createEffect(ref, timestamp, target, translation, rotation);
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // execute the configured action, if any
        if (_action != null) {
            _action.execute(_effect.getTimestamp(), (_target == null) ? this : _target);
        }
    }

    // TODO: a way to call removed() on the action

    /** The effect configuration. */
    protected EffectConfig.Original _config;

    /** The effect fired. */
    protected Effect _effect;

    /** The target of the effect (if any). */
    protected Logic _target;

    /** The shape of the effect. */
    protected Shape _shape;

    /** The effect action, if any. */
    protected ActionLogic _action;
}
