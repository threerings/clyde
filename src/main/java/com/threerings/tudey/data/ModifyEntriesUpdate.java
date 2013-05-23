//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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

    @Override
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
