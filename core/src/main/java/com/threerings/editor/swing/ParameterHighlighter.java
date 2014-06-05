//
// $Id$

package com.threerings.editor.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.base.Preconditions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.swing.util.SwingUtil;

import com.threerings.editor.swing.editors.ObjectEditor;
import com.threerings.editor.swing.editors.PathTableArrayListEditor;

import static com.threerings.editor.Log.log;

/**
 * Assists the top-level editor with showing parameterized properties.
 */
public class ParameterHighlighter
{
    /**
     * Construct a ParameterHighlighter on the top-level editor.
     */
    public ParameterHighlighter (BaseEditorPanel editor)
    {
        _editor = editor;
        editor.addContainerListener(_containerListener);
    }

    protected void registerDescendant (Component c)
    {
        if (!_comps.add(c)) {
            throw new RuntimeException("NOPE");
        }
        if (c instanceof Container) {
            ((Container)c).addContainerListener(_containerListener);
        }
        if (c instanceof BasePropertyEditor) {
            BasePropertyEditor pe = (BasePropertyEditor)c;
            String path = _editor.getComponentPath(pe, false);
            if (path.startsWith(".")) {
                path = path.substring(1);
            }
            if (_pathToEditor.containsKey(path)) {
                // We already registered a parent editor, let's continue to use that for
                // labelling purposes...
                log.warning("Skipper!", "path", path, "pe", pe, "class", pe.getClass());
                return;
            }
            _pathToEditor.put(path, pe);
            _editorToPath.put(pe, path);
//            log.info("Registered", "path", path, "pe", pe);
            if (pe instanceof PathTableArrayListEditor) {
                PathTableArrayListEditor paths = (PathTableArrayListEditor)pe;
                _params.put(paths, new ParamterWatcher(paths));
            }
        }
    }

    protected void unregisterDescendant (Component c)
    {
        if (!_comps.remove(c)) {
            log.warning("WHAT?", new Exception());
        }
        if (c instanceof Container) {
            ((Container)c).removeContainerListener(_containerListener);
        }
        if (c instanceof BasePropertyEditor) {
            BasePropertyEditor pe = (BasePropertyEditor)c;
            String path = Preconditions.checkNotNull(_editorToPath.remove(pe));
            _pathToEditor.remove(path);
            if (pe instanceof PathTableArrayListEditor) {
                ParamterWatcher watcher = _params.remove(pe);
                if (watcher == null) {
                    log.warning("What?: " + pe);
                } else {
                    watcher.shutdown();
                }
            }
        }
    }

    /** The editor we're supporting. */
    protected BaseEditorPanel _editor;

    // TEMP: for debugging
    protected java.util.Set<Component> _comps = com.google.common.collect.Sets.newIdentityHashSet();

    /** Maps a property editor the path of its property. */
    protected Map<BasePropertyEditor, String> _editorToPath = Maps.newIdentityHashMap();

    /** Maps the path back to the property editor. */
    protected Map<String, BasePropertyEditor> _pathToEditor = Maps.newHashMap();

    /** Maps a parameter path editor to the object tracking parameter changes. */
    protected Map<PathTableArrayListEditor, ParamterWatcher> _params = Maps.newIdentityHashMap();

    /** Listens for container events and registers all descendants. */
    protected ContainerListener _containerListener = new ContainerListener() {
            public void componentAdded (ContainerEvent e) {
                SwingUtil.applyToHierarchy(e.getChild(), new SwingUtil.ComponentOp() {
                    public void apply (Component comp) {
                        registerDescendant(comp);
                    }
                });
            }
            public void componentRemoved (ContainerEvent e) {
                SwingUtil.applyToHierarchy(e.getChild(), new SwingUtil.ComponentOp() {
                    public void apply (Component comp) {
                        unregisterDescendant(comp);
                    }
                });
            }
        };

    /**
     * Listens on both the name editor and path editor of a parameter, and updates
     * any property editors referenced by those paths to have the name of the parameter
     * provided to them.
     */
    protected class ParamterWatcher
        implements ChangeListener
    {
        /**
         * Construct a ParameterWatcher.
         */
        public ParamterWatcher (PathTableArrayListEditor paths)
        {
            PropertyEditor name = null;
            for (Container p = paths; p != null; p = p.getParent()) {
                if (p instanceof EditorPanel) {
                    name = ((EditorPanel)p).getPropertyEditor("name");
                    break;
                }
            }
            _pathsEditor = paths;
            _nameEditor = name;
            _pathsEditor.addChangeListener(this);
            _nameEditor.addChangeListener(this);
            update();
        }

        /**
         * Shut down the watcher.
         */
        public void shutdown ()
        {
            _pathsEditor.removeChangeListener(this);
            _nameEditor.removeChangeListener(this);
            update(ImmutableSet.<BasePropertyEditor>of());
        }

        // from ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            update();
        }

        /**
         * Update any new and previous property editors referenced by the paths with the
         * appropriate parameter name.
         */
        protected void update ()
        {
            Set<BasePropertyEditor> targets = Sets.newIdentityHashSet();
            for (int row = 0,
                    rowCount = _pathsEditor.getRowCount(), colCount = _pathsEditor.getColumnCount();
                    row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    String path = (String)_pathsEditor.getValueAt(row, col);
                    BasePropertyEditor pe = _pathToEditor.get(path);
                    if (pe == null) {
                        log.warning("Unfound: " + path);
                    } else {
                        targets.add(pe);
                    }
                }
            }
            update(targets);
        }

        /**
         * Update with the new set of targets.
         */
        protected void update (Set<BasePropertyEditor> newTargets)
        {
            // null out any targets no longer present
            for (BasePropertyEditor pe : Sets.difference(_pathTargets, newTargets)) {
                pe.setParameterLabel(null);
            }
            // assign the new targets and update the name
            _pathTargets = newTargets;
            String label = String.valueOf(_nameEditor.getProperty().get(_nameEditor.getObject()));
            for (BasePropertyEditor pe : newTargets) {
                pe.setParameterLabel(label);
            }
        }

        /** The editor for the paths. */
        protected final PathTableArrayListEditor _pathsEditor;

        /** The editor for the name. */
        protected final PropertyEditor _nameEditor;

        /** The current set of property editors referenced by the paths. */
        protected Set<BasePropertyEditor> _pathTargets = ImmutableSet.of();

    } // end: class ParameterWatcher
}
