//
// $Id$

package com.threerings.tudey.tools;

import com.google.inject.Singleton;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.server.persist.DummySceneRepository;

/**
 * Returns a single scene model for all scene requests.
 */
@Singleton
public class ToolSceneRepository extends DummySceneRepository
{
    /**
     * Sets the scene model to return.
     */
    public void setSceneModel (SceneModel model)
    {
        _sceneModel = model;
    }

    @Override // documentation inherited
    public SceneModel loadSceneModel (int sceneId)
    {
        return _sceneModel;
    }

    /** The scene model to return. */
    protected SceneModel _sceneModel;
}
