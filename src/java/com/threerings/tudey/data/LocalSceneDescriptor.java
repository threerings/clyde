//
// $Id$

package com.threerings.tudey.data;

import com.threerings.util.ResourceUtil;

import com.threerings.export.BinaryImporter;

import static com.threerings.ClydeLog.*;

/**
 * Contains the name of the scene to load locally on the client.
 */
public class LocalSceneDescriptor extends SceneDescriptor
{
    public LocalSceneDescriptor (String name)
    {
        _name = name;
    }

    @Override // documentation inherited
    public TudeySceneModel getSceneModel ()
    {
        try {
            BinaryImporter in = new BinaryImporter(
                ResourceUtil.getResourceAsStream("level/" + _name + ".level"));
            TudeySceneModel scene = (TudeySceneModel)in.readObject();
            in.close();
            return scene;
        } catch (Exception e) {
            log.warning("Failed to open scene [name=" + _name + ", error=" + e + "].");
            return new TudeySceneModel();
        }
    }

    /** The scene name. */
    protected String _name;
}
