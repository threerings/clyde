//
// $Id$

package com.threerings.tudey.util;

import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * Provides a generic interface for manipulating scene entries.
 */
public interface EntryManipulator
{
    /**
     * Adds an entry to the scene.
     */
    public void addEntry (Entry entry);

    /**
     * Updates an entry within the scene.
     */
    public void updateEntry (Entry entry);

    /**
     * Removes an entry from the scene.
     */
    public void removeEntry (Object key);
}
