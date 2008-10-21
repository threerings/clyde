//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.samskivert.util.IntTuple;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.client.util.GridBox;
import com.threerings.tudey.config.GroundConfig;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.CoordSet;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The ground brush tool.
 */
public class GroundBrush extends ConfigTool<GroundConfig>
{
    /**
     * Creates the ground brush tool.
     */
    public GroundBrush (SceneEditor editor)
    {
        super(editor, GroundConfig.class, new GroundReference());
    }

    @Override // documentation inherited
    public void init ()
    {
        _inner = new GridBox(_editor);
        _inner.getColor().set(0f, 1f, 0f, 1f);
        _outer = new GridBox(_editor);
        _outer.getColor().set(0f, 0.5f, 0f, 1f);
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        updateCursor();
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        if (_cursorVisible) {
            _inner.enqueue();
            _outer.enqueue();
        }
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        int button = event.getButton();
        boolean paint = (button == MouseEvent.BUTTON1), erase = (button == MouseEvent.BUTTON3);
        if ((paint || erase) && _cursorVisible) {
            paintGround(erase, true);
        }
    }

    @Override // documentation inherited
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        if (_cursorVisible) {
            _rotation = (_rotation + event.getWheelRotation()) & 0x03;
        }
    }

    /**
     * Updates the entry transform and cursor visibility based on the location of the mouse cursor.
     */
    protected void updateCursor ()
    {
        if (!(_cursorVisible = getMousePlaneIntersection(_isect) && !_editor.isControlDown())) {
            return;
        }
        GroundReference gref = (GroundReference)_eref;
        int iwidth = TudeySceneMetrics.getTileWidth(gref.width, gref.height, _rotation);
        int iheight = TudeySceneMetrics.getTileHeight(gref.width, gref.height, _rotation);
        int owidth = iwidth + 2, oheight = iheight + 2;

        int x = Math.round(_isect.x - iwidth*0.5f), y = Math.round(_isect.y - iheight*0.5f);
        _inner.getRegion().set(x, y, iwidth, iheight);
        _outer.getRegion().set(x - 1, y - 1, owidth, oheight);

        int elevation = _editor.getGrid().getElevation();
        _inner.setElevation(elevation);
        _outer.setElevation(elevation);

        // if we are dragging, consider performing another paint operation
        boolean paint = _editor.isFirstButtonDown(), erase = _editor.isThirdButtonDown();
        if ((paint || erase) && !_inner.getRegion().equals(_lastPainted)) {
            paintGround(erase, false);
        }
    }

    /**
     * Paints the cursor region with ground.
     *
     * @param erase if true, erase the region by painting with the null ground type.
     * @param revise if true, replace existing ground tiles with different variants.
     */
    protected void paintGround (boolean erase, boolean revise)
    {
        ConfigManager cfgmgr = _editor.getConfigManager();
        GroundConfig config = cfgmgr.getConfig(GroundConfig.class, _eref.getReference());
        GroundConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
        Rectangle iregion = _inner.getRegion(), oregion = _outer.getRegion();
        _lastPainted.set(iregion);

        // if no config, just erase the inner region
        if (original == null) {
            removeEntries(iregion);
            return;
        }

        // if erasing, erase the outer region and update the edges
        if (erase) {
            removeEntries(oregion);
            CoordSet coords = new CoordSet(oregion);
            coords.removeAll(iregion);
            updateEdges(coords, revise, original);
            return;
        }

        // find the coordinates that need to be painted
        CoordSet coords = new CoordSet();
        CoordSet border;
        if (original.extendEdge) {
            int maxx = oregion.x + oregion.width - 1;
            int maxy = oregion.y + oregion.height - 1;
            coords.addAll(oregion);
            coords.remove(oregion.x, oregion.y);
            coords.remove(maxx, oregion.y);
            coords.remove(maxx, maxy);
            coords.remove(oregion.x, maxy);
            border = coords.getBorder();
        } else {
            coords.addAll(iregion);
            border = new CoordSet(oregion);
            border.removeAll(iregion);
        }
        if (!revise) {
            // remove anything that's already a floor tile
            for (Iterator<Coord> it = coords.iterator(); it.hasNext(); ) {
                Coord coord = it.next();
                if (original.isFloor(_scene.getTileEntry(coord.x, coord.y))) {
                    it.remove();
                }
            }
        }

        // cover floor tiles randomly until filled in
        Rectangle region = new Rectangle();
        while (!coords.isEmpty()) {
            coords.getLargestRegion(region);
            TileEntry entry = original.createFloor(cfgmgr, region.width, region.height);
            if (entry == null) {
                break; // no appropriate tiles
            }
            Coord coord = entry.getLocation();
            coords.pickRandom(region.width, region.height, coord);
            TileConfig.Original tconfig = entry.getConfig(cfgmgr);
            int twidth = entry.getWidth(tconfig);
            int theight = entry.getHeight(tconfig);
            entry.elevation = _editor.getGrid().getElevation();
            addEntry(entry, region.set(coord.x, coord.y, twidth, theight));
            coords.removeAll(region);
        }

        // find the border tiles that need to be updated
        updateEdges(border, revise, original);
    }

    /**
     * Updates the edge tiles in the specified coordinate set.
     */
    protected void updateEdges (CoordSet coords, boolean revise, GroundConfig.Original original)
    {
        // divide the coordinates up by case/rotation pairs
        HashMap<IntTuple, CoordSet> sets = new HashMap<IntTuple, CoordSet>();
        for (Coord coord : coords) {
            TileEntry entry = _scene.getTileEntry(coord.x, coord.y);
            if (original.isFloor(entry)) {
                continue; // if it's already floor, leave it alone
            }
            // classify the tile based on its surroundings
            int pattern = GroundConfig.createPattern(
                original.isFloor(_scene.getTileEntry(coord.x, coord.y + 1)),
                original.isFloor(_scene.getTileEntry(coord.x - 1, coord.y + 1)),
                original.isFloor(_scene.getTileEntry(coord.x - 1, coord.y)),
                original.isFloor(_scene.getTileEntry(coord.x - 1, coord.y - 1)),
                original.isFloor(_scene.getTileEntry(coord.x, coord.y - 1)),
                original.isFloor(_scene.getTileEntry(coord.x + 1, coord.y - 1)),
                original.isFloor(_scene.getTileEntry(coord.x + 1, coord.y)),
                original.isFloor(_scene.getTileEntry(coord.x + 1, coord.y + 1)));
            IntTuple tuple = original.getCaseRotations(pattern);
            if (tuple != null && (revise || !original.isEdge(entry, tuple))) {
                CoordSet set = sets.get(tuple);
                if (set == null) {
                    sets.put(tuple, set = new CoordSet());
                }
                set.add(coord);
            }
        }

        // add edge tiles as appropriate
        ConfigManager cfgmgr = _editor.getConfigManager();
        Rectangle region = new Rectangle();
    OUTER:
        for (Map.Entry<IntTuple, CoordSet> entry : sets.entrySet()) {
            IntTuple tuple = entry.getKey();
            CoordSet set = entry.getValue();
            while (!set.isEmpty()) {
                set.getLargestRegion(region);
                TileEntry tentry = original.createEdge(cfgmgr, tuple, region.width, region.height);
                if (tentry == null) {
                    continue OUTER; // no appropriate tiles
                }
                Coord coord = tentry.getLocation();
                set.pickRandom(region.width, region.height, coord);
                TileConfig.Original tconfig = tentry.getConfig(cfgmgr);
                int twidth = tentry.getWidth(tconfig);
                int theight = tentry.getHeight(tconfig);
                tentry.elevation = _editor.getGrid().getElevation();
                addEntry(tentry, region.set(coord.x, coord.y, twidth, theight));
                set.removeAll(region);
            }
        }
    }

    /**
     * Adds the specified entry, removing any entries underneath it.
     */
    protected void addEntry (TileEntry entry, Rectangle region)
    {
        removeEntries(region);
        _scene.addEntry(entry);
    }

    /**
     * Removes all entries within the specified region.
     */
    protected void removeEntries (Rectangle region)
    {
        ArrayList<TileEntry> results = new ArrayList<TileEntry>();
        _scene.getTileEntries(region, results);
        for (int ii = 0, nn = results.size(); ii < nn; ii++) {
            _scene.removeEntry(results.get(ii).getKey());
        }
    }

    /**
     * Allows us to edit the ground reference.
     */
    protected static class GroundReference extends EditableReference<GroundConfig>
    {
        /** The ground reference. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> ground;

        /** The width of the brush. */
        @Editable(min=1, hgroup="d")
        public int width = 1;

        /** The height of the brush. */
        @Editable(min=1, hgroup="d")
        public int height = 1;

        @Override // documentation inherited
        public ConfigReference<GroundConfig> getReference ()
        {
            return ground;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<GroundConfig> ref)
        {
            ground = ref;
        }
    }

    /** The inner and outer cursors. */
    protected GridBox _inner, _outer;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The rotation of the cursor. */
    protected int _rotation;

    /** The last painted region. */
    protected Rectangle _lastPainted = new Rectangle();

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
