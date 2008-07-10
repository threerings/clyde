//
// $Id$

package com.threerings.opengl.util;

import java.awt.Font;

import com.threerings.opengl.gui.text.CharacterTextFactory;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Displays rendering statistics.
 */
public class Stats extends SimpleOverlay
    implements Renderable
{
    /**
     * Creates a new stats display.
     */
    public Stats (GlContext ctx)
    {
        super(ctx);

        // create the label (empty for now)
        _textFactory = CharacterTextFactory.getInstance(
            new Font("Dialog", Font.PLAIN, 12), true);
        _stats = _textFactory.createText("", Color4f.WHITE, 0, 0, Color4f.BLACK, true);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        super.enqueue();
        _frameCount++;

        // update the stats if the required interval has passed
        long now = System.currentTimeMillis(), interval = now - _lastUpdate;
        if (interval >= REPORT_INTERVAL) {
            int fps = (int)((_frameCount * 1000) / interval);
            Renderer renderer = _ctx.getRenderer();
            _stats = _textFactory.createText(
                fps + " fps (" + "batches: " + renderer.getBatchCount() + "; " + "primitives: " +
                renderer.getPrimitiveCount() + "; textures: " + renderer.getTextureCount() + ")",
                Color4f.WHITE, 0, 0, Color4f.BLACK, true);
            _lastUpdate = now;
            _frameCount = 0;
        }
    }

    @Override // documentation inherited
    protected void draw ()
    {
        _stats.render(_ctx.getRenderer(), 16, 16, 1f);
    }

    /** Used to create text objects. */
    protected CharacterTextFactory _textFactory;

    /** The stats display text. */
    protected Text _stats;

    /** The time at which we last updated the stats. */
    protected long _lastUpdate = System.currentTimeMillis();

    /** The number of frames rendered since the last update. */
    protected int _frameCount;

    /** The interval at which we update the stats. */
    protected static final long REPORT_INTERVAL = 1000L;
}
