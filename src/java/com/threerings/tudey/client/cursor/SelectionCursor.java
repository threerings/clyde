//
// $Id$

package com.threerings.tudey.client.cursor;

import java.util.ArrayList;
import java.util.HashMap;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * A cursor for a selection.
 */
public class SelectionCursor extends Cursor
{
    /**
     * Creates the cursor.
     */
    public SelectionCursor (GlContext ctx, TudeySceneView view)
    {
        super(ctx, view);
    }

    /**
     * Updates the cursor with new entry state.
     */
    public void update (Entry[] entries)
    {
        // find and create or update the corresponding cursors
        for (Entry entry : entries) {
            Object key = entry.getKey();
            EntryCursor cursor = _cursorsByKey.get(key);
            if (cursor == null) {
                _cursors.add(cursor = entry.createCursor(_ctx, (TudeySceneView)_parentScope));
                _cursorsByKey.put(key, cursor);
            } else {
                cursor.update(entry);
            }
        }
        // remove any cursors for which there is no longer an entry
        if (_cursors.size() > entries.length) {
            for (int ii = _cursors.size() - 1; ii >= 0; ii--) {
                Object key = _cursors.get(ii).getEntry().getKey();
                if (!containsKey(entries, key)) {
                    _cursors.remove(ii);
                    _cursorsByKey.remove(key);
                }
            }
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        for (int ii = 0, nn = _cursors.size(); ii < nn; ii++) {
            _cursors.get(ii).tick(elapsed);
        }
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        for (int ii = 0, nn = _cursors.size(); ii < nn; ii++) {
            _cursors.get(ii).enqueue();
        }
    }

    /**
     * Determines whether the supplied array of entries contains an entry with the specified key.
     */
    protected static boolean containsKey (Entry[] entries, Object key)
    {
        for (Entry entry : entries) {
            if (entry.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** The contained cursors. */
    protected ArrayList<EntryCursor> _cursors = new ArrayList<EntryCursor>();

    /** Maps entry keys to cursors. */
    protected HashMap<Object, EntryCursor> _cursorsByKey = new HashMap<Object, EntryCursor>();
}
