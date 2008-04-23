//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.Streamable;
import com.threerings.export.Exportable;

/**
 * An abstract representation of the scene model for transmission to clients.
 */
public abstract class SceneDescriptor
    implements Streamable, Exportable
{
    /**
     * Returns the scene model associated with this descriptor.
     */
    public abstract TudeySceneModel getSceneModel ();
}
