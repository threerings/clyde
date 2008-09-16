//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;

import com.threerings.tudey.client.cursor.PlaceableCursor;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;

/**
 * The placeable placer tool.
 */
public class Placer extends ConfigTool<PlaceableConfig>
{
    /**
     * Creates the placer tool.
     */
    public Placer (SceneEditor editor)
    {
        super(editor, PlaceableConfig.class, new PlaceableReference());
    }

    @Override // documentation inherited
    public void init ()
    {
        _cursor = new PlaceableCursor(_editor, _editor.getView(), _entry);
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        _cursor.tick(elapsed);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        _cursor.enqueue();
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
    }

    @Override // documentation inherited
    public void mouseEntered (MouseEvent event)
    {
    }

    @Override // documentation inherited
    public void mouseExited (MouseEvent event)
    {
    }

    @Override // documentation inherited
    public void mouseDragged (MouseEvent event)
    {
    }

    @Override // documentation inherited
    public void mouseMoved (MouseEvent event)
    {
    }

    @Override // documentation inherited
    public void mouseWheelMoved (MouseWheelEvent event)
    {

    }

    @Override // documentation inherited
    protected void referenceChanged (ConfigReference<PlaceableConfig> ref)
    {
        _entry.placeable = ref;
        _cursor.updateFromEntry();
    }

    /**
     * Allows us to edit the placeable reference.
     */
    protected static class PlaceableReference extends EditableReference<PlaceableConfig>
    {
        /** The placeable reference. */
        @Editable(nullable=true)
        public ConfigReference<PlaceableConfig> placeable;

        @Override // documentation inherited
        public ConfigReference<PlaceableConfig> getReference ()
        {
            return placeable;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<PlaceableConfig> ref)
        {
            placeable = ref;
        }
    }

    /** The prototype entry. */
    protected PlaceableEntry _entry = new PlaceableEntry();

    /** The cursor. */
    protected PlaceableCursor _cursor;
}
