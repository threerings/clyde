//
// $Id$

package com.threerings.tudey.client;

import com.threerings.opengl.GlView;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.scene.HashScene;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.data.SceneGlobals;
import com.threerings.tudey.data.SceneGlobals.EnvironmentModel;
import com.threerings.tudey.data.TudeySceneModel;

/**
 * Displays a view of a Tudey scene.
 */
public class TudeySceneView extends GlView
{
    /**
     * Creates a new scene view.
     */
    public TudeySceneView (GlContext ctx)
    {
        _ctx = ctx;
        _scene = new HashScene(ctx, 64f, 6);
        _scene.setParentScope(ctx.getScope());
    }

    /**
     * Sets the scene model for this view.
     */
    public void setSceneModel (TudeySceneModel model)
    {
        _sceneModel = model;
        updateEnvironment();
    }

    /**
     * Notes that the state of the scene globals has changed.
     */
    public void globalsChanged ()
    {
        updateEnvironment();
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        _scene.tick(elapsed);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        _scene.enqueue();
    }

    /**
     * Updates the environment in response to a change in the environment model list.
     */
    protected void updateEnvironment ()
    {
        EnvironmentModel[] environmentModels = _sceneModel.globals.environmentModels;
        Model[] omodels = _environment;
        _environment = new Model[environmentModels.length];
        for (int ii = 0; ii < _environment.length; ii++) {
            EnvironmentModel envmod = environmentModels[ii];
            Model model = (omodels == null || omodels.length <= ii) ? null : omodels[ii];
            if (model == null) {
                _scene.add(model = new Model(_ctx, envmod.model));
            } else {
                model.setConfig(envmod.model);
            }
            _environment[ii] = model;
            model.setLocalTransform(envmod.transform);
            model.updateBounds();
        }
        if (omodels != null) {
            for (int ii = _environment.length; ii < omodels.length; ii++) {
                _scene.remove(omodels[ii]);
                omodels[ii].dispose();
            }
        }
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The OpenGL scene. */
    protected HashScene _scene;

    /** The scene model. */
    protected TudeySceneModel _sceneModel;

    /** The environment models. */
    protected Model[] _environment;
}
