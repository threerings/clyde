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

package com.threerings.opengl.model.tools;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileSystemView;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.editor.swing.DraggableSpinner;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.util.ChangeBlock;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.ModelObserver;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;

import static com.threerings.opengl.Log.log;

/**
 * A simple model viewer application.
 */
public class ModelViewer extends ModelTool
    implements ChangeListener, ConfigUpdateListener<ModelConfig>, ModelObserver
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
        createFileMenuItems(file);

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("configs", KeyEvent.VK_C, KeyEvent.VK_G));
        edit.add(createMenuItem("resources", KeyEvent.VK_R, KeyEvent.VK_R));
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_autoReset = createCheckBoxMenuItem("auto_reset", KeyEvent.VK_A, KeyEvent.VK_E));
        view.addSeparator();
        view.add(_showEnvironment =
            createCheckBoxMenuItem("environment", KeyEvent.VK_E, KeyEvent.VK_V));
        _showEnvironment.setSelected(true);
        view.add(_showGrid = createCheckBoxMenuItem("grid", KeyEvent.VK_G, KeyEvent.VK_D));
        _showGrid.setSelected(true);
        view.add(_showBounds = createCheckBoxMenuItem("bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(_showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_C, KeyEvent.VK_M));
        _showCompass.setSelected(true);
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("refresh", KeyEvent.VK_F, KeyEvent.VK_F));
        view.addSeparator();
        view.add(createMenuItem("recenter", KeyEvent.VK_C, KeyEvent.VK_C));
        view.add(createMenuItem("reset", KeyEvent.VK_R, KeyEvent.VK_R, 0));

        JMenu tools = createMenu("tools", KeyEvent.VK_T);
        menubar.add(tools);
        tools.add(createMenuItem("save_snapshot", KeyEvent.VK_S, KeyEvent.VK_F12, 0));

        // configure the side panel
        _cpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _cpanel.setPreferredSize(new Dimension(350, 1));

        // add the config editor
        _cpanel.add(_epanel = new EditorPanel(this));

        // add the animation control container
        _apanel = GroupLayout.makeVBox(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        _apanel.setBorder(BorderFactory.createTitledBorder(_msgs.get("m.animations")));
        _cpanel.add(_apanel, GroupLayout.FIXED);
        _apanel.setVisible(false);

        // add the track panel container
        _tpanels = GroupLayout.makeVBox(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        _apanel.add(_tpanels);

        // add the animation controls
        JPanel buttons = new JPanel();
        _apanel.add(buttons);
        buttons.add(createButton("add_track"));
        buttons.add(_removeTrack = createButton("remove_track"));
        _removeTrack.setEnabled(false);

        // add the controls
        JPanel controls = new JPanel();
        _cpanel.add(controls, GroupLayout.FIXED);
        controls.add(new JLabel(_msgs.get("m.global_speed")));
        controls.add(_speedSpinner = new DraggableSpinner(1f, 0f, Float.MAX_VALUE, 0.01f));
        _speedSpinner.setMinimumSize(_speedSpinner.getPreferredSize());
        _speedSpinner.setMaximumSize(_speedSpinner.getPreferredSize());
        _speedSpinner.addChangeListener(this);

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
        if (event.getSource() == _epanel) {
            // let the config know that it was updated
            if (!_block.enter()) {
                return;
            }
            try {
                // ah-ha, this possible NPE is caused by 5bbfa84ad96e6d155324025d6f0a27cf1ac823a0
                // wherein I fire a property change in the property editor
                if (_model != null) {
                    _model.getConfig().wasUpdated();
                }
            } finally {
                _block.leave();
            }
        }
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ModelConfig> event)
    {
        // update the track panels
        Animation[] anims = _model.getAnimations();
        if (anims.length == 0) {
            _apanel.setVisible(false);
        } else {
            _apanel.setVisible(true);
            for (int ii = 0, nn = _tpanels.getComponentCount(); ii < nn; ii++) {
                ((TrackPanel)_tpanels.getComponent(ii)).updateAnimations();
            }
        }

        // update the editor panel
        if (!_block.enter()) {
            return;
        }
        try {
            _epanel.update();
            _epanel.validate();
        } finally {
            _block.leave();
        }
    }

    // documentation inherited from interface ModelObserver
    public boolean animationStarted (Animation animation)
    {
        updateTrackControls();
        return true;
    }

    // documentation inherited from interface ModelObserver
    public boolean animationStopped (Animation animation, boolean completed)
    {
        updateTrackControls();
        return true;
    }

    // documentation inherited from interface ModelObserver
    public boolean modelCompleted (Model model)
    {
        return true;
    }

    @Override
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("add_track")) {
            _tpanels.add(new TrackPanel());
            _removeTrack.setEnabled(true);
            SwingUtil.refresh(_cpanel);

        } else if (action.equals("remove_track")) {
            _tpanels.remove(_tpanels.getComponentCount() - 1);
            _removeTrack.setEnabled(_tpanels.getComponentCount() > 1);
            SwingUtil.refresh(_cpanel);

        } else if (action.equals("save_snapshot")) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            File file = new File(FileSystemView.getFileSystemView().getDefaultDirectory(),
                "viewer_" + fmt.format(new Date()) + ".png");
            try {
                ImageIO.write(createSnapshot(true), "png", file);
            } catch (IOException e) {
                log.warning("Failed to write snapshot.", "file", file, e);
            }
        } else {
            super.actionPerformed(event);
        }
    }

    protected void createFileMenuItems (JMenu file)
    {
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));
    }

    @Override
    protected JComponent createCanvasContainer ()
    {
        JSplitPane pane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, _canvas, _cpanel = GroupLayout.makeVStretchBox(5));
        _canvas.setMinimumSize(new Dimension(1, 1));
        pane.setResizeWeight(1.0);
        pane.setOneTouchExpandable(true);
        return pane;
    }

    @Override
    protected CanvasToolPrefs createEditablePrefs ()
    {
        return new ModelToolPrefs(_prefs);
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // set up the model
        ModelConfig config = new ModelConfig();
        config.init(_cfgmgr);
        config.implementation = (ModelConfig.Derived)_epanel.getObject();
        config.addListener(this);
        _scene.add(_model = new Model(this, config));
        _model.addObserver(this);

        // add the initial track panel
        _apanel.setVisible(_model.getAnimations().length > 0);
        _tpanels.add(new TrackPanel());
    }

    @Override
    protected void updateView ()
    {
        // scaled the elapsed time by the speed
        long nnow = System.currentTimeMillis();
        _elapsed += (nnow - _lastUpdate) * _speedSpinner.getFloatValue();
        _lastUpdate = nnow;

        // remove the integer portion for use as time increment
        long lelapsed = (long)_elapsed;
        _elapsed -= lelapsed;
        _now.value += lelapsed;

        updateView(lelapsed / 1000f);
    }

    @Override
    protected void updateView (float elapsed)
    {
        super.updateView(elapsed);
        if (_autoReset.isSelected() && _model.hasCompleted()) {
            _model.reset();
        }
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
         */
        public TrackPanel ()
        {
            add(_box = new JComboBox(_model.getAnimations()));
            _box.addActionListener(this);
            add(new Spacer(1, 1));
            add(_start = new JButton(_msgs.get("m.start")));
            _start.addActionListener(this);
            add(_stop = new JButton(_msgs.get("m.stop")));
            _stop.addActionListener(this);

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
            }
        }

        /** The combo box containing the selectable animations. */
        protected JComboBox _box;

        /** The start and stop buttons. */
        protected JButton _start, _stop;
    }

    /** The toggle for automatic reset. */
    protected JCheckBoxMenuItem _autoReset;

    /** The panel that holds the control bits. */
    protected JPanel _cpanel;

    /** The editor panel we use to edit the model configuration. */
    protected EditorPanel _epanel;

    /** The animation control container. */
    protected JPanel _apanel;

    /** The container for the animation track panels. */
    protected JPanel _tpanels;

    /** The remove track button. */
    protected JButton _removeTrack;

    /** The speed control spinner. */
    protected DraggableSpinner _speedSpinner;

    /** The time of the last update. */
    protected long _lastUpdate = System.currentTimeMillis();

    /** Accumulated elapsed time. */
    protected float _elapsed;

    /** Indicates that we should ignore any changes, because we're the one effecting them. */
    protected ChangeBlock _block = new ChangeBlock();

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ModelViewer.class);

    /** The format for the speed display. */
    protected static final DecimalFormat SPEED_FORMAT = new DecimalFormat("0.00x");
    static {
        SPEED_FORMAT.setMaximumFractionDigits(2);
    }
}
