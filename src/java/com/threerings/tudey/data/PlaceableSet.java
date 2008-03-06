//
// $Id$

package com.threerings.tudey.data;

import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;

import static com.threerings.tudey.Log.*;

/**
 * Contains a set of placeables.
 */
public class PlaceableSet extends SimpleStreamableObject
    implements Exportable, Iterable<Placeable>
{
    /**
     * Creates an empty placeable set.
     */
    public PlaceableSet ()
    {
    }

    /**
     * Creates a set containing a single placeable.
     */
    public PlaceableSet (Placeable placeable)
    {
        add(placeable);
    }

    /**
     * Copy constructor.
     */
    public PlaceableSet (Collection<Placeable> other)
    {
        addAll(other);
    }

    /**
     * Retrieves the placeable with the specified id.
     *
     * @return the requested placeable, or <code>null</code> if there is no such placeable in the
     * set.
     */
    public Placeable get (int placeableId)
    {
        return _placeables.get(placeableId);
    }

    /**
     * Returns the number of placeables in the set.
     */
    public int size ()
    {
        return _placeables.size();
    }

    /**
     * Checks whether this set is empty.
     */
    public boolean isEmpty ()
    {
        return _placeables.isEmpty();
    }

    /**
     * Adds all the placeables in the provided array.
     */
    public void addAll (Placeable[] placeables)
    {
        for (Placeable placeable : placeables) {
            add(placeable);
        }
    }

    /**
     * Adds all the placeables in the provided iterable.
     */
    public void addAll (Iterable<Placeable> placeables)
    {
        for (Placeable placeable : placeables) {
            add(placeable);
        }
    }

    /**
     * Adds a placeable to the set.
     *
     * @return <code>true</code> if the placeable was added, <code>false</code> if it was already
     * in the set (in which case a warning will be logged).
     */
    public boolean add (Placeable placeable)
    {
        int placeableId = placeable.getPlaceableId();
        Placeable oplaceable = _placeables.put(placeableId, placeable);
        if (oplaceable != null) {
            // it was already there; log a warning and restore the old one
            log.warning("Refusing to add duplicate placeable [old=" +
                oplaceable + ", new=" + placeable + "].");
            _placeables.put(placeableId, oplaceable);
            return false;
        }
        return true;
    }

    /**
     * Removes a placeable from the set.
     *
     * @return <code>true</code> if the placeable was in the set.
     */
    public boolean remove (Placeable placeable)
    {
        return remove(placeable.getPlaceableId()) != null;
    }

    /**
     * Removes the placeable with the specified id from the set.
     *
     * @return the placeable removed, or <code>null</code> for none.
     */
    public Placeable remove (int placeableId)
    {
        return _placeables.remove(placeableId);
    }

    /**
     * Updates an element of the set.
     *
     * @return the old entry that was replaced, or <code>null</code> if the entry was not
     * present.
     */
    public Placeable update (Placeable placeable)
    {
        int placeableId = placeable.getPlaceableId();
        Placeable oplaceable = _placeables.put(placeableId, placeable);
        if (oplaceable == null) {
            // it wasn't there before, so remove the updated entry
            _placeables.remove(placeableId);
        }
        return oplaceable;
    }

    /**
     * Checks whether this set contains the specified placeable.
     */
    public boolean contains (Placeable placeable)
    {
        return contains(placeable.getPlaceableId());
    }

    /**
     * Checks whether this set contains a placeable with the specified id.
     */
    public boolean contains (int placeableId)
    {
        return _placeables.containsKey(placeableId);
    }

    /**
     * Clears out the set.
     */
    public void clear ()
    {
        _placeables.clear();
    }

    /**
     * Returns an iterator over the placeables in this set.
     */
    public Iterator<Placeable> iterator ()
    {
        return _placeables.values().iterator();
    }

    /**
     * Populates an array with the contents of this placeable set.
     *
     * @param array the array to populate, or <code>null</code> to create a new array.
     * @return the populated array.
     */
    public Placeable[] toArray (Placeable[] array)
    {
        if (array == null) {
            array = new Placeable[size()];
        }
        return _placeables.values().toArray(array);
    }

    /**
     * Removes any invalid pieces, logging warnings for each.
     */
    public void validate ()
    {
        for (Iterator<Placeable> it = iterator(); it.hasNext(); ) {
            Placeable placeable = it.next();
            if (!placeable.isValid()) {
                log.warning("Invalid placeable [placeable=" + placeable + "].");
                it.remove();
            }
        }
    }

    @Override // documentation inherited
    public String toString ()
    {
        return StringUtil.toString(iterator());
    }

    /**
     * Writes the fields of the object.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.writeObject(toArray(null));
    }

    /**
     * Writes the fields of the object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        out.write("placeables", toArray(null), Placeable[].class);
    }

    /**
     * Reads the fields of the object.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        addAll((Placeable[])in.readObject());
    }

    /**
     * Reads the fields of the object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        addAll(in.read("placeables", null, Placeable[].class));
    }

    /** The underlying map from placeable id to placeable. */
    protected HashIntMap<Placeable> _placeables = new HashIntMap<Placeable>();
}
