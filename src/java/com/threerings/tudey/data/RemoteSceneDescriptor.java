//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.Streamable;
import com.threerings.export.Exportable;

/**
 * Contains the entire scene model.
 */
public class RemoteSceneDescriptor extends SceneDescriptor
{
    public RemoteSceneDescriptor (TudeySceneModel model)
    {
        _model = model;
    }

    @Override // documentation inherited
    public TudeySceneModel getSceneModel ()
    {
        return _model;
    }

    /** The model to transmit. */
    protected TudeySceneModel _model;
}
