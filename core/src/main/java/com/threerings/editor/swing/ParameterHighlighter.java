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

import com.google.common.collect.ImmutableMap;
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

    /**
     * Register a newly-seen descendant.
     */
    protected void registerDescendant (Component c)
    {
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
                return;
            }
            _pathToEditor.put(path, pe);
            _editorToPath.put(pe, path);
//            log.info("+", "path", path, "pe", pe.getClass());
            if (pe instanceof PathTableArrayListEditor) {
                PathTableArrayListEditor paths = (PathTableArrayListEditor)pe;
                _params.put(paths, new ParameterWatcher(paths));
            }
        }
    }

    /**
     * Unregister a removed descendant.
     */
    protected void unregisterDescendant (Component c)
    {
        if (c instanceof Container) {
            ((Container)c).removeContainerListener(_containerListener);
        }
        if (c instanceof BasePropertyEditor) {
            BasePropertyEditor pe = (BasePropertyEditor)c;
            String path = _editorToPath.remove(pe);
            if (path != null) {
                _pathToEditor.remove(path);
//                log.info("-", "path", path);
            }
            if (pe instanceof PathTableArrayListEditor) {
                ParameterWatcher watcher = _params.remove(pe);
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

    /** Maps a property editor the path of its property. */
    protected Map<BasePropertyEditor, String> _editorToPath = Maps.newIdentityHashMap();

    /** Maps the path back to the property editor. */
    protected Map<String, BasePropertyEditor> _pathToEditor = Maps.newHashMap();

    /** Maps a parameter path editor to the object tracking parameter changes. */
    protected Map<PathTableArrayListEditor, ParameterWatcher> _params = Maps.newIdentityHashMap();

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
    protected class ParameterWatcher
        implements ChangeListener
    {
        /**
         * Construct a ParameterWatcher.
         */
        public ParameterWatcher (PathTableArrayListEditor paths)
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
            update(ImmutableMap.<BasePropertyEditor, String>of());
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
            Map<BasePropertyEditor, String> targets = Maps.newIdentityHashMap();
            for (int row = 0,
                    rowCount = _pathsEditor.getRowCount(), colCount = _pathsEditor.getColumnCount();
                    row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    String path = (String)_pathsEditor.getValueAt(row, col);
                    BasePropertyEditor pe = _pathToEditor.get(path);
                    String pathInfo = "";
                    while ((pe == null) && path.endsWith("]")) {
                        int lastBracket = path.lastIndexOf("[");
                        pathInfo = path.substring(lastBracket) + pathInfo;
                        path = path.substring(0, lastBracket);
                        pe = _pathToEditor.get(path);
                    }
                    if (pe != null) {
                        targets.put(pe, pathInfo);
                    }
                }
            }
            update(targets);
        }

        /**
         * Update with the new set of targets.
         */
        protected void update (Map<BasePropertyEditor, String> newTargets)
        {
            // null out any targets no longer present
            for (BasePropertyEditor pe : Sets.difference(_lastTargets, newTargets.keySet())) {
                pe.setParameterLabel(null, "");
            }
            // assign the new targets and update the name
            String label = String.valueOf(_nameEditor.getProperty().get(_nameEditor.getObject()));
            for (Map.Entry<BasePropertyEditor, String> entry : newTargets.entrySet()) {
                entry.getKey().setParameterLabel(label, entry.getValue());
            }
            _lastTargets = newTargets.keySet();
        }

        /** The editor for the paths. */
        protected final PathTableArrayListEditor _pathsEditor;

        /** The editor for the name. */
        protected final PropertyEditor _nameEditor;

        /** The current set of property editors referenced by the paths, mapped to pathInfo. */
        /** The targets that were updated during the last update. */
        protected Set<BasePropertyEditor> _lastTargets = ImmutableSet.of();

    } // end: class ParameterWatcher
}
