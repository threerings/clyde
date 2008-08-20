//
// $Id$

package com.threerings.opengl.eff.tools;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import org.lwjgl.opengl.GL11;

import com.threerings.util.ToolUtil;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.SimpleTransformable;

/**
 * The particle editor application.
 */
public class ParticleEditor extends GlCanvasTool
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new ParticleEditor(args.length > 0 ? args[0] : null).startup();
    }

    /**
     * Creates the particle editor with (optionally) the path to a particle system to load.
     */
    public ParticleEditor (String particles)
    {
        super("particle");

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);
        file.add(createMenuItem("new", KeyEvent.VK_N, KeyEvent.VK_N));
        file.add(createMenuItem("open", KeyEvent.VK_O, KeyEvent.VK_O));
        file.addSeparator();
        file.add(createMenuItem("save", KeyEvent.VK_S, KeyEvent.VK_S));
        file.add(createMenuItem("save_as", KeyEvent.VK_A, KeyEvent.VK_A));
        file.add(_revert = createMenuItem("revert", KeyEvent.VK_R, KeyEvent.VK_R));
        _revert.setEnabled(false);
        file.addSeparator();
        file.add(createMenuItem("import", KeyEvent.VK_I, -1));
        file.add(createMenuItem("export", KeyEvent.VK_E, -1));
        file.addSeparator();
        file.add(createMenuItem("import_layers", KeyEvent.VK_L, KeyEvent.VK_L));
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_showGround = createCheckBoxMenuItem("ground", KeyEvent.VK_G, KeyEvent.VK_G));
        view.add(_showBounds = createCheckBoxMenuItem("bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(_showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_O, KeyEvent.VK_M));
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("reset", KeyEvent.VK_R, KeyEvent.VK_R, 0));
        view.add(createMenuItem("recenter", KeyEvent.VK_C, KeyEvent.VK_C));

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("particle_dir", null));
        _chooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".dat");
            }
            public String getDescription () {
                return _msgs.get("m.particle_files");
            }
        });

        // and the export chooser
        _exportChooser = new JFileChooser(_prefs.get("particle_export_dir", null));
        _exportChooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".xml");
            }
            public String getDescription () {
                return _msgs.get("m.xml_files");
            }
        });
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("new")) {
//            newParticles();
        } else if (action.equals("open")) {
//            open();
        } else if (action.equals("save")) {
//            if (_file != null) {
//                save(_file);
//            } else {
//                save();
//            }
        } else if (action.equals("save_as")) {
//            save();
        } else if (action.equals("revert")) {
//            open(_file);
        } else if (action.equals("import")) {
//            importParticles();
        } else if (action.equals("export")) {
//            exportParticles();
        } else if (action.equals("import_layers")) {
//            importLayers();
        } else if (action.equals("reset")) {
//            _particles.reset();
        } else if (action.equals("new_layer")) {
//            ((LayerTableModel)_ltable.getModel()).newLayer();
        } else if (action.equals("clone_layer")) {
//            ((LayerTableModel)_ltable.getModel()).cloneLayer();
        } else if (action.equals("delete_layer")) {
//            ((LayerTableModel)_ltable.getModel()).deleteLayer();
        } else {
            super.actionPerformed(event);
        }
    }

    @Override // documentation inherited
    protected DebugBounds createBounds ()
    {
        return new DebugBounds(this) {
            protected void draw () {
                _model.drawBounds();
            }
        };
    }

    @Override // documentation inherited
    protected ToolUtil.EditablePrefs createEditablePrefs ()
    {
        return new CanvasToolPrefs(_prefs);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // create the ground plane
        _ground = new SimpleTransformable(this) {
            protected RenderState[] createStates () {
                RenderState[] states = super.createStates();
                states[RenderState.COLOR_STATE] = new ColorState(
                    new Color4f(0.4f, 0.4f, 0.4f, 1f));
                return states;
            }
            protected void draw () {
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex3f(-32f, -32f, -0.01f);
                GL11.glVertex3f(+32f, -32f, -0.01f);
                GL11.glVertex3f(+32f, +32f, -0.01f);
                GL11.glVertex3f(-32f, +32f, -0.01f);
                GL11.glEnd();
            }
        };

        // set up the model
        _model = new Model(this);
        _model.setParentScope(this);
    }

    @Override // documentation inherited
    protected void updateView (float elapsed)
    {
        super.updateView(elapsed);
        _model.tick(elapsed);
    }

    @Override // documentation inherited
    protected void enqueueView ()
    {
        super.enqueueView();
        if (_showGround.isSelected()) {
            _ground.enqueue();
        }
        _model.enqueue();
    }

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The toggle for the ground view. */
    protected JCheckBoxMenuItem _showGround;

    /** The file chooser for opening and saving particle files. */
    protected JFileChooser _chooser;

    /** The file chooser for importing and exporting particle files. */
    protected JFileChooser _exportChooser;

    /** The ground plane. */
    protected SimpleTransformable _ground;

    /** The particle model. */
    protected Model _model;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ParticleEditor.class);
}

