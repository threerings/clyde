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

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;

import com.threerings.opengl.effect.Easing;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.ViewerEffect;
import com.threerings.opengl.scene.config.ViewerEffectConfig;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;

/**
 * Configurations for Tudey-specific viewer effects.
 */
@EditorTypes({ TudeyViewerEffectConfig.Camera.class })
public abstract class TudeyViewerEffectConfig extends ViewerEffectConfig
{
    /**
     * Adds a camera to the view.
     */
    public static class Camera extends TudeyViewerEffectConfig
    {
        /** The transition to use when switching to or from the camera. */
        @Editable(min=0.0, step=0.01)
        public float transition;

        /** The easing to use for the transition. */
        @Editable
        public Easing easing = new Easing.None();

        /** The camera configuration. */
        @Editable
        public CameraConfig camera = new CameraConfig();

        /** More camera configurations!  For backwards compatibility we keep the original. */
        @Editable
        public CameraConfig[] cameras = new CameraConfig[0];

        @Override
        public void preload (GlContext ctx)
        {
            // Do nothing
        }

        @Override
        public ViewerEffect getViewerEffect (GlContext ctx, Scope scope, ViewerEffect effect)
        {
            final TudeySceneView view = ScopeUtil.resolve(
                scope, "view:this", null, TudeySceneView.class);
            if (view == null || !ScopeUtil.resolve(scope, "cameraEnabled", true)) {
                return getNoopEffect(effect);
            }
            class CameraEffect extends ViewerEffect {
                public void setConfig (Camera camera) {
                    if (_activated) {
                        view.removeCameraConfig(_camcfg, 0f, null);
                        for (CameraConfig cc : _camcfgs) {
                            view.removeCameraConfig(cc, 0f, null);
                        }
                    }
                    _transition = camera.transition;
                    _easing = camera.easing;
                    _camcfg = camera.camera;
                    _camcfgs = camera.cameras;
                    if (_activated) {
                        view.addCameraConfig(_camcfg, 0f, null);
                        for (CameraConfig cc : _camcfgs) {
                            view.addCameraConfig(cc, 0f, null);
                        }
                    }
                }
                public void activate (Scene scene) {
                    _activated = true;
                    view.addCameraConfig(_camcfg, _transition, _easing);
                    for (CameraConfig cc : _camcfgs) {
                        view.addCameraConfig(cc, 0f, null);
                    }
                }
                public void deactivate () {
                    view.removeCameraConfig(_camcfg, _transition, _easing);
                    for (CameraConfig cc : _camcfgs) {
                        view.removeCameraConfig(cc, 0f, null);
                    }
                    _activated = false;
                }
                protected float _transition = transition;
                protected Easing _easing = easing;
                protected CameraConfig _camcfg = camera;
                protected CameraConfig[] _camcfgs = cameras;
                protected boolean _activated;
            }
            if (effect instanceof CameraEffect) {
                ((CameraEffect)effect).setConfig(this);
            } else {
                effect = new CameraEffect();
            }
            return effect;
        }
    }
}
