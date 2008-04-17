//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.Streamable;
import com.threerings.export.Exportable;

/**
 * Superclass for objects stored in the scene.
 */
public abstract class SceneDescriptor
    implements Streamable, Exportable
{
    /**
     * Returns the scene model associated with this descriptor.
     */
    public abstract TudeySceneModel getSceneModel ();
}
