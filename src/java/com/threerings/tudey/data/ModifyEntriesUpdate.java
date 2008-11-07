//
// $Id$

package com.threerings.tudey.data;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * A scene update that is broadcast when entries have been added to, removed from, or updated
 * within the scene.
 */
public class ModifyEntriesUpdate extends SceneUpdate
{
    /** The entries added to the scene. */
    public Entry[] added;

    /** The entries updated within the scene. */
    public Entry[] updated;

    /** The keys of the entries removed from the scene. */
    public Object[] removed;

    /**
     * Initializes this update with all necessary data.
     *
     * @param added the objects added to the scene, or <code>null</code> for
     * none
     * @param removed the objects removed from the scene, or <code>null</code>
     * for none
     */
    public void init (
        int targetId, int targetVersion, Entry[] added, Entry[] updated, Object[] removed)
    {
        super.init(targetId, targetVersion);
        this.added = added;
        this.updated = updated;
        this.removed = removed;
    }

    @Override // documentation inherited
    public void apply (SceneModel model)
    {
        super.apply(model);

        // add the new entries
        TudeySceneModel tsmodel = (TudeySceneModel)model;
        for (Entry entry : added) {
            tsmodel.addEntry(entry);
        }

        // update the updated entries
        for (Entry entry : updated) {
            tsmodel.updateEntry(entry);
        }

        // remove the removed entries
        for (Object key : removed) {
            tsmodel.removeEntry(key);
        }
    }
}
