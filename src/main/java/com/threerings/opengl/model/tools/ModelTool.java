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

package com.threerings.opengl.model.tools;

import java.awt.event.ActionEvent;

import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.SimpleScene;
import com.threerings.opengl.util.DebugBounds;

/**
 * Base class for tools that view or manipulate a single model (like the model viewer and
 * particle editor).
 */
public abstract class ModelTool extends GlCanvasTool
{
    /**
     * Represents a model in the environment.
     */
    public static class EnvironmentModel extends DeepObject
        implements Exportable
    {
        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The transform of the model. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();
    }

    /**
     * Creates a new tool application.
     *
     * @param msgs the name of the application message bundle.
     */
    public ModelTool (String msgs)
    {
        super(msgs);
    }

    @Override
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("environment")) {
            ((ModelToolPrefs)_eprefs).updateEnvironment();
        } else if (action.equals("reset")) {
            _model.reset();
        } else {
            super.actionPerformed(event);
        }
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // adjust the limits on the camera handler
        ((OrbitCameraHandler)_camhand).setCoordLimits(
            -FloatMath.HALF_PI, +FloatMath.HALF_PI, 2f, 50f);

        // set up the scene
        _scene = new SimpleScene(this);
        _scene.setParentScope(this);

        // initialize the environment
        ((ModelToolPrefs)_eprefs).updateEnvironment();
    }

    @Override
    protected DebugBounds createBounds ()
    {
        return new DebugBounds(this) {
            protected void draw () {
                _model.drawBounds();
            }
        };
    }

    @Override
    protected void updateView (float elapsed)
    {
        super.updateView(elapsed);
        _scene.tick(elapsed);
    }

    @Override
    protected void compositeView ()
    {
        super.compositeView();
        _scene.composite();
    }

    /**
     * The preferences for model tools.
     */
    protected class ModelToolPrefs extends CanvasToolPrefs
    {
        public ModelToolPrefs (Preferences prefs)
        {
            super(prefs);

            // set up the environment models
            _environmentModels = (EnvironmentModel[])getPref(
                "environment_models", new EnvironmentModel[0]);
        }

        /**
         * Sets the environment models to include in the scene.
         */
        @Editable(weight=8)
        public void setEnvironmentModels (EnvironmentModel[] models)
        {
            putPref("environment_models", _environmentModels = models);
            updateEnvironment();
        }

        /**
         * Returns the environment models included in the scene.
         */
        @Editable
        public EnvironmentModel[] getEnvironmentModels ()
        {
            return _environmentModels;
        }

        /**
         * Updates the environment in response to a change in the environment model list.
         */
        public void updateEnvironment ()
        {
            Model[] omodels = _environment;
            _environment =
                new Model[_showEnvironment.isSelected() ? _environmentModels.length : 0];
            for (int ii = 0; ii < _environment.length; ii++) {
                EnvironmentModel envmod = _environmentModels[ii];
                Model model = (omodels == null || omodels.length <= ii) ? null : omodels[ii];
                if (model == null) {
                    _scene.add(model = new Model(ModelTool.this));
                }
                model.setConfig(envmod.model);
                _environment[ii] = model;
                model.setLocalTransform(envmod.transform);
            }
            if (omodels != null) {
                for (int ii = _environment.length; ii < omodels.length; ii++) {
                    _scene.remove(omodels[ii]);
                    omodels[ii].dispose();
                }
            }
        }

        /** The environment models to include in the scene. */
        protected EnvironmentModel[] _environmentModels;
    }

    /** Environment toggle. */
    protected JCheckBoxMenuItem _showEnvironment;

    /** The model scene. */
    protected SimpleScene _scene;

    /** The environment models. */
    protected Model[] _environment;

    /** The model being viewed. */
    protected Model _model;
}
