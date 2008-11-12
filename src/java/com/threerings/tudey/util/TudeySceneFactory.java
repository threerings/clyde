//
// $Id$

package com.threerings.tudey.util;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.util.SceneFactory;

/**
 * Creates Tudey scene objects.
 */
public class TudeySceneFactory
    implements SceneFactory
{
    // documentation inherited from interface SceneFactory
    public Scene createScene (SceneModel model, PlaceConfig config)
    {
        return new SceneImpl(model, config);
    }
}
