//
// $Id$

package com.threerings.tudey.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.export.Exportable;
import com.threerings.util.Copyable;
import com.threerings.util.DeepUtil;

import com.threerings.tudey.client.TudeySceneController;

/**
 * Place configuration for Tudey scenes.
 */
public class TudeySceneConfig extends PlaceConfig
    implements Exportable, Cloneable, Copyable
{
    // documentation inherited from interface Copyable
    public Object copy (Object dest)
    {
        return DeepUtil.copy(this, dest);
    }

    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new TudeySceneController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.tudey.server.TudeySceneManager";
    }

    @Override // documentation inherited
    public Object clone ()
    {
        return copy(null);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return DeepUtil.equals(this, other);
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return DeepUtil.hashCode(this);
    }
}
