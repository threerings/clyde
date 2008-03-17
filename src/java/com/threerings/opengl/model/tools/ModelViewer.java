//
// $Id$

package com.threerings.opengl.model.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FileInputStream;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.lwjgl.opengl.GL11;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.editor.swing.DraggableSpinner;
import com.threerings.export.BinaryImporter;

import com.threerings.opengl.GlCanvasApp;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.AnimationObserver;
import com.threerings.opengl.model.ArticulatedModel;
import com.threerings.opengl.model.ArticulatedModel.AnimationTrack;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.Compass;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.Grid;

import static java.util.logging.Level.*;
import static com.threerings.opengl.Log.*;

/**
 * A simple model viewer application.
 */
public class ModelViewer extends GlCanvasApp
    implements ActionListener
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new ModelViewer(args.length > 0 ? args[0] : null).start();
    }

    /**
     * Creates the model viewer with (optionally) the path to a model to load.
     */
    public ModelViewer (String model)
    {
        _msgs = _msgmgr.getBundle("viewer");
        _initModel = (model == null) ? null : new File(model);

        // set the title
        updateTitle();

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);
        file.add(createMenuItem("open", KeyEvent.VK_O, KeyEvent.VK_O));
        file.addSeparator();
        file.add(_reload = createMenuItem("reload", KeyEvent.VK_R, KeyEvent.VK_R));
        _reload.setEnabled(false);
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(createMenuItem("toggle_bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(createMenuItem("toggle_compass", KeyEvent.VK_C, KeyEvent.VK_M));
        view.add(createMenuItem("toggle_stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(_variants = createMenu("variants", KeyEvent.VK_V));
        view.add(createMenuItem("recenter", KeyEvent.VK_R, KeyEvent.VK_C));

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("model_dir", null));
        _chooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                String fstr = file.toString().toLowerCase();
                return file.isDirectory() || fstr.endsWith(".dat") || fstr.endsWith(".properties");
            }
            public String getDescription () {
                return _msgs.get("m.model_files");
            }
        });

        // create the animation panel
        _apanel = new JPanel(new BorderLayout());
        _apanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 5, 5, 5)));
        _apanel.setVisible(false);
        _frame.add(_apanel, BorderLayout.SOUTH);

        // add the track panel container
        _tcont = new JPanel(new GridBagLayout());
        _tcont.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        _apanel.add(_tcont, BorderLayout.CENTER);

        // add the header labels
        _tcont.add(new JLabel(_msgs.get("m.animation")), createConstraints(0, 1, true));
        _tcont.add(new Spacer(5, 1), createConstraints(1, 1, true));
        _tcont.add(new JLabel(_msgs.get("m.priority")), createConstraints(2, 1, true));
        _tcont.add(new JLabel(_msgs.get("m.transition")), createConstraints(3, 1, true));
        _tcont.add(new JLabel(_msgs.get("m.weight")), createConstraints(4, 1, true));
        _tcont.add(new JLabel(_msgs.get("m.blend")), createConstraints(5, 1, true));
        _tcont.add(new JLabel(_msgs.get("m.speed")), createConstraints(6, 1, true));
        _tcont.add(new Spacer(5, 1), createConstraints(7, 1, true));
        _tcont.add(new JLabel(_msgs.get("m.controls")), createConstraints(8, 3, true));

        // add the first track panel
        new TrackPanel(false).add();

        // add the button panel
        JPanel bpanel = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.BOTTOM, GroupLayout.NONE);
        _apanel.add(bpanel, BorderLayout.EAST);
        bpanel.add(_atrack = new JButton(_msgs.get("m.add_track")));
        _atrack.addActionListener(this);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object source = event.getSource();
        if (source == _atrack) {
            new TrackPanel(true).add();
            return;
        }
        String action = event.getActionCommand();
        if (action.equals("open")) {
            open();
        } else if (action.equals("reload")) {
            open(_file);
        } else if (action.equals("quit")) {
            System.exit(0);
        } else if (action.equals("toggle_bounds")) {
            _bounds = (_bounds == null) ? createBounds() : null;
        } else if (action.equals("toggle_compass")) {
            _compass = (_compass == null) ? new Compass(this) : null;
        } else if (action.equals("toggle_stats")) {
            _renderer.setShowStats(!_renderer.getShowStats());
        } else if (action.equals("recenter")) {
            ((OrbitCameraHandler)_camhand).getTarget().set(Vector3f.ZERO);
        }
    }

    /**
     * Creates a menu with the specified name and mnemonic.
     */
    protected JMenu createMenu (String name, int mnemonic)
    {
        JMenu menu = new JMenu(_msgs.get("m." + name));
        menu.setMnemonic(mnemonic);
        return menu;
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator.
     */
    protected JMenuItem createMenuItem (String action, int mnemonic, int accelerator)
    {
        JMenuItem item = new JMenuItem(_msgs.get("m." + action), mnemonic);
        item.setActionCommand(action);
        item.addActionListener(this);
        if (accelerator != -1) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator, KeyEvent.CTRL_MASK));
        }
        return item;
    }

    /**
     * (Re)creates the debug bounds renderer.
     */
    protected DebugBounds createBounds ()
    {
        return new DebugBounds(this) {
            protected void draw () {
                if (_model != null) {
                    _model.updateWorldBounds();
                    _model.drawBounds();
                }
            }
        };
    }

    @Override // documentation inherited
    protected CameraHandler createCameraHandler ()
    {
        // add an orbiter to move the camera with the mouse
        OrbitCameraHandler camhand = new OrbitCameraHandler(this);
        new MouseOrbiter(camhand).addTo(_canvas);
        return camhand;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        // clear to gray
        _renderer.setClearColor(Color4f.GRAY);

        // create the reference grid
        _grid = new Grid(this, 65, 1f);
        _grid.getColor().set(0.2f, 0.2f, 0.2f, 1f);

        // attempt to load the model file specified on the command line
        if (_initModel != null) {
            open(_initModel);
        }
    }

    @Override // documentation inherited
    protected void updateScene ()
    {
        long time = System.currentTimeMillis();
        float elapsed = (_lastTick == 0L) ? 0f : (time - _lastTick) / 1000f;
        if (_model != null) {
            _model.tick(elapsed);
        }
        _lastTick = time;
    }

    @Override // documentation inherited
    protected void renderScene ()
    {
        // clear the previous frame
        _renderer.clearFrame();
        
        // queue up the grid
        _grid.enqueue();

        // and the model (if any)
        if (_model != null) {
            _model.enqueue();
        }

        // and maybe the bounding box(es)
        if (_bounds != null) {
            _bounds.enqueue();
        }

        // and the compass
        if (_compass != null) {
            _compass.enqueue();
        }

        // render the contents of the queues
        _renderer.renderFrame();
    }

    /**
     * Brings up the open model dialog.
     */
    protected void open ()
    {
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            open(_chooser.getSelectedFile());
        }
        _prefs.put("model_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to open the specified model file.
     */
    protected void open (File file)
    {
        Model nmodel = readModel(file);
        if (nmodel == null) {
            return;
        }
        (_model = nmodel).init(this, file.getParent().toString());
        _file = file;
        _reload.setEnabled(true);
        updateTitle();

        // set the editor attachments
        if (_model instanceof ArticulatedModel) {
            loadAnimations();
            setAttachments();
        } else {
            _apanel.setVisible(false);
        }

        // configure the variants menu
        _variants.removeAll();
        ButtonGroup vgroup = new ButtonGroup();
        addVariantMenuItem(vgroup, null).setSelected(true);
        String[] variants = StringUtil.parseStringArray(
            _model.getProperties().getProperty("variants", ""));
        for (String variant : variants) {
            addVariantMenuItem(vgroup, variant);
        }
    }

    /**
     * Creates, adds, and returns a menu item for the specified variant.
     */
    protected JCheckBoxMenuItem addVariantMenuItem (ButtonGroup group, final String variant)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(
            variant == null ? _msgs.get("m.default") : variant);
        item.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _model.createSurfaces(variant);
            }
        });
        group.add(item);
        _variants.add(item);
        return item;
    }

    /**
     * Updates the title based on the file.
     */
    protected void updateTitle ()
    {
        String title = _msgs.get("m.title");
        if (_file != null) {
            title = title + ": " + _file;
        }
        _frame.setTitle(title);
    }

    /**
     * Attempts to load the animations listed in the model's configuration.
     */
    protected void loadAnimations ()
    {
        // load the animation data
        ArrayList<AnimationData> animData = new ArrayList<AnimationData>();
        Properties props = _model.getProperties();
        String[] anims = StringUtil.parseStringArray(
            props.getProperty("viewer_animations", ""));
        String suffix = getSuffix(_file);
        for (String anim : anims) {
            AnimationData data = new AnimationData(anim);
            if (data.load()) {
                animData.add(data);
            }
        }
        _animData = (AnimationData[])animData.toArray(new AnimationData[animData.size()]);

        // initialize the existing track panels
        for (int ii = 0, nn = _tpanels.size(); ii < nn; ii++) {
            _tpanels.get(ii).init();
        }

        // show the animation panel
        _apanel.setVisible(true);
    }

    /**
     * Sets the attachments configured for the editor.
     */
    protected void setAttachments ()
    {
        Properties props = _model.getProperties();
        String[] points = StringUtil.parseStringArray(
            props.getProperty("attachment_points", ""));
        for (String point : points) {
            String attachment = props.getProperty(point + ".viewer_attachment");
            if (attachment == null) {
                continue;
            }
            String suffix = getSuffix(_file);
            File file = new File(_file.getParent(), attachment + suffix);
            Model model = readModel(file);
            if (model != null) {
                model.init(this, file.getParent().toString());
                ((ArticulatedModel)_model).attach(point, model);
            }
        }
    }

    /**
     * Reads a model from the specified file.
     */
    protected static Model readModel (File file)
    {
        String fstr = file.toString().toLowerCase();
        try {
            if (fstr.endsWith(".properties")) {
                return ModelReader.read(file);
            } else { // fstr.endsWith(".dat")
                BinaryImporter in = new BinaryImporter(new FileInputStream(file));
                return (Model)in.readObject();
            }
        } catch (Exception e) {
            log.log(WARNING, "Failed to load model.", e);
        }
        return null;
    }

    /**
     * Reads an animation from the specified file.
     */
    protected static Animation readAnimation (File file)
    {
        String fstr = file.toString().toLowerCase();
        try {
            if (fstr.endsWith(".properties")) {
                return AnimationReader.read(file);
            } else { // fstr.endsWith(".dat")
                BinaryImporter in = new BinaryImporter(new FileInputStream(file));
                return (Animation)in.readObject();
            }
        } catch (Exception e) {
            log.log(WARNING, "Failed to load animation.", e);
        }
        return null;
    }

    /**
     * Returns the suffix of the specified file, including the period.
     */
    protected static String getSuffix (File file)
    {
        String name = file.toString();
        return name.substring(name.lastIndexOf('.'));
    }

    /**
     * Creates and returns a set of grid bag constraints for one of the track panel components.
     */
    protected static GridBagConstraints createConstraints (int x)
    {
        return createConstraints(x, 1, false);
    }

    /**
     * Creates and returns a set of grid bag constraints.
     */
    protected static GridBagConstraints createConstraints (int x, int width, boolean header)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridwidth = width;
        if (x >= 2 && x <= 6 && !header) {
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        }
        constraints.insets.left = constraints.insets.right = 2;
        constraints.insets.top = 5;
        return constraints;
    }

    /**
     * A panel to control a single animation track.
     */
    protected class TrackPanel extends JPanel
        implements ActionListener, ChangeListener, AnimationObserver
    {
        public TrackPanel (boolean removable)
        {
            _removable = removable;
        }

        /**
         * Adds the components of this panel.
         */
        public void add ()
        {
            // add to the outer list
            _tpanels.add(this);

            _tcont.add(_abox = new JComboBox(), createConstraints(0));

            _tcont.add(_priority = new DraggableSpinner(0, null, null, 1),
                createConstraints(2));
            _priority.addChangeListener(this);
            _tcont.add(_transition = new DraggableSpinner(0f, 0f, null, 0.01f),
                createConstraints(3));
            _tcont.add(_weight = new DraggableSpinner(1f, 0f, 1f, 0.01f),
                createConstraints(4));
            _weight.addChangeListener(this);
            _tcont.add(_blend = new DraggableSpinner(0f, 0f, null, 0.01f),
                createConstraints(5));
            _tcont.add(_speed = new DraggableSpinner(1f, 0f, null, 0.01f),
                createConstraints(6));
            _speed.addChangeListener(this);

            _tcont.add(_start = new JButton(_msgs.get("m.start")),
                createConstraints(8));
            _start.addActionListener(this);
            _tcont.add(_stop = new JButton(_msgs.get("m.stop")),
                createConstraints(9));
            _stop.addActionListener(this);

            if (_removable) {
                _tcont.add(_remove = new JButton(
                    _msgs.get(_removable ? "m.remove_track" : "m.add_track")),
                    createConstraints(10));
                ((JButton)_remove).addActionListener(this);
            } else {
                _tcont.add(_remove = new Spacer(1, 1), createConstraints(10));
            }

            // initialize if we have a model already
            if (_model != null) {
                init();
            }

            // make sure we're correctly laid out
            _frame.validate();
        }

        /**
         * Initializes the panel with the list of animations.
         */
        public void init ()
        {
            _abox.removeAllItems();
            for (AnimationData data : _animData) {
                _abox.addItem(new AnimationItem(data));
            }
            _start.setEnabled(_animData.length > 0);
            _stop.setEnabled(false);
            _track = null;
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Object source = event.getSource();
            if (source == _start) {
                if (_track != null) {
                    _track.stop();
                    _track.removeObserver(this);
                }
                _tracks = ((AnimationItem)_abox.getSelectedItem()).tracks;
                startAnimation(
                    _tracks[0], _transition.getFloatValue(),
                    _blend.getFloatValue(), (_tracks.length > 1) ? 0f : _blend.getFloatValue());
                _stop.setEnabled(true);

            } else if (source == _stop) {
                _track.stop();

            } else if (source == _remove) {
                if (_track != null) {
                    _track.stop();
                    _track.removeObserver(this);
                }
                remove();
                _frame.validate();
            }
        }

        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            if (_track == null) {
                return;
            }
            Object source = event.getSource();
            if (source == _priority) {
                _track.setPriority(_priority.getIntValue());
            } else if (source == _weight) {
                _track.setWeight(_weight.getFloatValue());
            } else { // source == _speed
                _track.setSpeed(_speed.getFloatValue());
            }
        }

        // documentation inherited from interface AnimationObserver
        public boolean animationCancelled (AnimationTrack track)
        {
            // if we cancelled the cycle, run the stop animation
            if (_tracks.length == 3 && track == _tracks[1]) {
                startAnimation(
                    _tracks[2], 1f / _tracks[2].getAnimation().getFrameRate(),
                    0f, _blend.getFloatValue());
            } else {
                _stop.setEnabled(false);
                _track = null;
            }
            return false;
        }

        // documentation inherited from interface AnimationObserver
        public boolean animationCompleted (AnimationTrack track)
        {
            // start the next animation in the sequence, if any
            int nidx = ListUtil.indexOf(_tracks, track) + 1;
            if (nidx < _tracks.length) {
                startAnimation(
                    _tracks[nidx], 1f / _tracks[2].getAnimation().getFrameRate(), 0f, 0f);
            } else {
                _stop.setEnabled(false);
                _track = null;
            }
            return false;
        }

        /**
         * Starts the specified animation with the current parameters.
         */
        protected void startAnimation (
            AnimationTrack track, float transition, float blendIn, float blendOut)
        {
            _track = track;
            _track.setPriority(_priority.getIntValue());
            _track.setSpeed(_speed.getFloatValue());
            if (_track.getAnimation().isLooping()) {
                _track.loop(transition, _weight.getFloatValue(), blendIn);
            } else {
                _track.play(transition, _weight.getFloatValue(), blendIn, blendOut);
            }
            _track.addObserver(this);
        }

        /**
         * Removes the components of this panel.
         */
        protected void remove ()
        {
            // remove from the outer list
            _tpanels.remove(this);

            _tcont.remove(_abox);
            _tcont.remove(_priority);
            _tcont.remove(_transition);
            _tcont.remove(_weight);
            _tcont.remove(_blend);
            _tcont.remove(_speed);
            _tcont.remove(_start);
            _tcont.remove(_stop);
            _tcont.remove(_remove);
        }

        /** Whether or not we can remove this panel. */
        protected boolean _removable;

        /** Holds the animations. */
        protected JComboBox _abox;

        /** The priority spinner. */
        protected DraggableSpinner _priority;

        /** The transition spinner. */
        protected DraggableSpinner _transition;

        /** The weight spinner. */
        protected DraggableSpinner _weight;

        /** The blend spinner. */
        protected DraggableSpinner _blend;

        /** The speed spinner. */
        protected DraggableSpinner _speed;

        /** The animation start button. */
        protected JButton _start;

        /** The animation stop button. */
        protected JButton _stop;

        /** The remove button (or a spacer standing in for it). */
        protected Component _remove;

        /** The animation tracks currently being played. */
        protected AnimationTrack[] _tracks;

        /** The animation track, if an animation is currently running. */
        protected AnimationTrack _track;
    }

    /**
     * Stores the shared animation data.
     */
    protected class AnimationData
    {
        /** The visible name of the animation. */
        public String name;

        /** The animations to play (either a single animation or a three part loop). */
        public Animation[] anims;

        public AnimationData (String name)
        {
            this.name = name;
        }

        /**
         * Attempts to load the animation, returning true if successful.
         */
        public boolean load ()
        {
            Properties props = _model.getProperties();
            boolean compound = Boolean.parseBoolean(props.getProperty(name + ".compound"));
            anims = new Animation[compound ? 3 : 1];
            String path = props.getProperty(name, name);
            String suffix = File.separator + "animation" + getSuffix(_file);
            File parent = _file.getParentFile();
            if (compound) {
                anims[0] = readAnimation(new File(parent, path + "_start" + suffix));
                anims[1] = readAnimation(new File(parent, path + "_cycle" + suffix));
                anims[2] = readAnimation(new File(parent, path + "_stop" + suffix));
            } else {
                anims[0] = readAnimation(new File(parent, path + suffix));
            }
            for (Animation anim : anims) {
                if (anim == null) {
                    return false;
                }
                anim.init();
            }
            return true;
        }
    }

    /**
     * An animation in the combo box.
     */
    protected class AnimationItem
    {
        /** The visible name of the animation. */
        public String name;

        /** The animation tracks to play (either a single track or a three part loop). */
        public AnimationTrack[] tracks;

        public AnimationItem (AnimationData data)
        {
            name = data.name;
            tracks = new AnimationTrack[data.anims.length];
            for (int ii = 0; ii < tracks.length; ii++) {
                tracks[ii] = ((ArticulatedModel)_model).createAnimationTrack(data.anims[ii]);
                tracks[ii].setOverride(false); // never override
            }
        }

        @Override // documentation inherited
        public String toString ()
        {
            return name;
        }
    }

    /** The viewer message bundle. */
    protected MessageBundle _msgs;

    /** The file to attempt to load on initialization, if any. */
    protected File _initModel;

    /** The reload menu item. */
    protected JMenuItem _reload;

    /** The variants menu. */
    protected JMenu _variants;

    /** The file chooser for opening model files. */
    protected JFileChooser _chooser;

    /** The animation control panel. */
    protected JPanel _apanel;

    /** Holds the track panels. */
    protected JPanel _tcont;

    /** Adds a new track panel. */
    protected JButton _atrack;

    /** The reference grid. */
    protected Grid _grid;

    /** The bounds display. */
    protected DebugBounds _bounds;

    /** The coordinate system compass. */
    protected Compass _compass;

    /** The loaded model file. */
    protected File _file;

    /** The loaded model, if any. */
    protected Model _model;

    /** The active animation panels. */
    protected ArrayList<TrackPanel> _tpanels = new ArrayList<TrackPanel>();

    /** The animation data. */
    protected AnimationData[] _animData;

    /** The time of the last tick. */
    protected long _lastTick;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ModelViewer.class);
}
