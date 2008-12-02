//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * A logic object associated with a scene entry.
 */
public class EntryLogic extends Logic
{
    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, Entry entry)
    {
        super.init(scenemgr);
        _entry = entry;
    }

    /**
     * Notes that the entry has been removed from the scene.
     */
    public void removed ()
    {
        // nothing by default
    }

    /** The scene entry. */
    protected Entry _entry;
}
