//
// $Id$

package com.threerings.opengl.mod.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.editor.Property;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.mod.Animation;
import com.threerings.opengl.mod.AnimationObserver;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.DebugBounds;

/**
 * A simple model viewer application.
 */
public class ModelViewer extends GlCanvasTool
    implements ChangeListener, ConfigUpdateListener<ModelConfig>, AnimationObserver
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new ModelViewer(args.length > 0 ? args[0] : null).startup();
    }

    /**
     * Creates the model viewer with (optionally) the path to a model to load.
     */
    public ModelViewer (String model)
    {
        super("viewer");

        // set the title
        _frame.setTitle(_msgs.get("m.title"));

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("configs", KeyEvent.VK_C, KeyEvent.VK_G));
        edit.add(createMenuItem("resources", KeyEvent.VK_R, KeyEvent.VK_R));
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_showBounds = createCheckBoxMenuItem("bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(_showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_C, KeyEvent.VK_M));
        _showCompass.setSelected(true);
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("recenter", KeyEvent.VK_R, KeyEvent.VK_C));

        // add the track panel container
        _tpanels = GroupLayout.makeVBox(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        _frame.add(_tpanels, BorderLayout.SOUTH);
        _tpanels.setVisible(false);

        // configure the config editor
        ModelConfig.Derived impl = new ModelConfig.Derived();
        if (model != null) {
            String path = _rsrcmgr.getResourcePath(new File(model));
            if (path != null) {
                impl.model = new ConfigReference<ModelConfig>(path);
            }
        }
        _epanel.setObject(impl);
        _epanel.addChangeListener(this);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        // let the config know that it was updated
        _model.getConfig().wasUpdated();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ModelConfig> event)
    {
        // update the track panels
        Animation[] anims = _model.getAnimations();
        if (anims.length == 0) {
            _tpanels.setVisible(false);
            return;
        }
        _tpanels.setVisible(true);
        for (int ii = 0, nn = _tpanels.getComponentCount(); ii < nn; ii++) {
            ((TrackPanel)_tpanels.getComponent(ii)).updateAnimations();
        }
    }

    // documentation inherited from interface AnimationObserver
    public boolean animationStarted (Animation animation)
    {
        updateTrackControls();
        return true;
    }

    // documentation inherited from interface AnimationObserver
    public boolean animationStopped (Animation animation, boolean completed)
    {
        updateTrackControls();
        return true;
    }

    @Override // documentation inherited
    protected JComponent createCanvasContainer ()
    {
        JSplitPane pane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, true, _canvas, _epanel = new EditorPanel(this));
        _canvas.setMinimumSize(new Dimension(1, 1));
        pane.setResizeWeight(1.0);
        pane.setOneTouchExpandable(true);
        return pane;
    }

    @Override // documentation inherited
    protected DebugBounds createBounds ()
    {
        return new DebugBounds(this) {
            protected void draw () {
                _model.updateWorldBounds();
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

        // set up the model
        ModelConfig config = new ModelConfig();
        config.addListener(this);
        config.implementation = (ModelConfig.Derived)_epanel.getObject();
        _model = new Model(this, config);
        _model.setParentScope(this);
        _model.addAnimationObserver(this);

        // add the initial track panel
        _tpanels.setVisible(_model.getAnimations().length > 0);
        _tpanels.add(new TrackPanel(false));
    }

    @Override // documentation inherited
    protected void updateView ()
    {
        super.updateView();
        long time = System.currentTimeMillis();
        float elapsed = (_lastTick == 0L) ? 0f : (time - _lastTick) / 1000f;
        _model.tick(elapsed);
        _lastTick = time;
    }

    @Override // documentation inherited
    protected void enqueueView ()
    {
        super.enqueueView();
        _model.enqueue();
    }

    /**
     * Updates all of the track controls.
     */
    protected void updateTrackControls ()
    {
        for (int ii = 0, nn = _tpanels.getComponentCount(); ii < nn; ii++) {
            ((TrackPanel)_tpanels.getComponent(ii)).updateControls();
        }
    }

    /**
     * A single panel for running animations.
     */
    protected class TrackPanel extends JPanel
        implements ActionListener
    {
        /**
         * Creates a new animation panel.
         *
         * @param removable whether or not the panel is removable (the first panel is not).
         */
        public TrackPanel (boolean removable)
        {
            add(new JLabel(_msgs.get("m.animation")));
            add(_box = new JComboBox(_model.getAnimations()));
            _box.addActionListener(this);
            add(new Spacer(10, 1));
            add(_start = new JButton(_msgs.get("m.start")));
            _start.addActionListener(this);
            add(_stop = new JButton(_msgs.get("m.stop")));
            _stop.addActionListener(this);
            add(new Spacer(10, 1));

            // depending on whether we can remove this panel, add the remove or add button
            if (removable) {
                add(_remove = new JButton(_msgs.get("m.remove_track")));
                _remove.addActionListener(this);
            } else {
                add(_add = new JButton(_msgs.get("m.add_track")));
                _add.addActionListener(this);
            }

            // update the controls
            updateControls();
        }

        /**
         * Updates the list of animations.
         */
        public void updateAnimations ()
        {
            _box.setModel(new DefaultComboBoxModel(_model.getAnimations()));
            updateControls();
        }

        /**
         * Updates the controls in response to a change in the selected animation.
         */
        public void updateControls ()
        {
            Animation animation = (Animation)_box.getSelectedItem();
            _start.setEnabled(animation != null);
            _stop.setEnabled(animation != null && animation.isPlaying());
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Object source = event.getSource();
            if (source == _box) {
                updateControls();
            } else if (source == _start) {
                ((Animation)_box.getSelectedItem()).start();
            } else if (source == _stop) {
                ((Animation)_box.getSelectedItem()).stop();
            } else if (source == _add) {
                _tpanels.add(new TrackPanel(true));
                _frame.validate();
            } else if (source == _remove) {
                _tpanels.remove(this);
                _frame.validate();
            }
        }

        /** The combo box containing the selectable animations. */
        protected JComboBox _box;

        /** The start and stop buttons. */
        protected JButton _start, _stop;

        /** The add and remove buttons (only one of which will be used). */
        protected JButton _add, _remove;
    }

    /** The editor panel we use to edit the model configuration. */
    protected EditorPanel _epanel;

    /** The container for the animation track panels. */
    protected JPanel _tpanels;

    /** The model being viewed. */
    protected Model _model;

    /** The time of the last tick. */
    protected long _lastTick;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ModelViewer.class);
}
