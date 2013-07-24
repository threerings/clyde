//
// $Id$

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.ToolUtil;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;

import com.threerings.editor.util.EditorContext;

import com.threerings.export.util.ExportFileUtil;

import com.threerings.tudey.data.TudeySceneModel;

/**
 * Utilitiies for searching for and in ConfigReferences.
 */
public class ConfigSearcher extends JFrame
{
    /**
     * Find if anything satisfies the predicate in the specified object any any sub-objects.
     */
    public static boolean find (Object val, Predicate<? super ConfigReference<?>> detector)
    {
        return find(val, detector, Sets.newIdentityHashSet());
    }

    /**
     * Find all the attributes in the specified object and any sub-objects.
     */
    public static <T> Iterable<T> findAttributes (
        Object val, Function<? super ConfigReference<?>, ? extends Iterable<T>> detector)
    {
        return findAttributes(val, detector, Sets.newIdentityHashSet());
    }

    /**
     * Find the path to the search config from the specified starting point.
     * Experimental.
     */
    public static String findPath (Object val, Predicate<? super ConfigReference<?>> detector)
    {
        return findPath(val, detector, Sets.newIdentityHashSet());
    }

    /**
     * Represents something found while searching.
     */
    public static abstract class Result
    {
        /**
         * Get the display label for this result.
         */
        public String getLabel ()
        {
            return _label;
        }

        /**
         * Open the result for viewing/editing by the user.
         */
        public abstract void onClick ();

        /**
         * Construct a result with the specified label.
         */
        public Result (String label)
        {
            _label = label;
        }

        /** The label for this result. */
        protected String _label;
    }

    /**
     * A Domain within which we search for configs.
     */
    public interface Domain
    {
        /**
         * The label for this domain.
         */
        public String getLabel ();

        /**
         * Null results should be returned to allow the UI/interaction updates.
         */
        public Iterator<Result> getResults (Predicate<? super ConfigReference<?>> detector);
    }

    /**
     * A search domain that searches all managed configs in the ConfigManger.
     */
    public static class ConfigDomain 
        implements Domain
    {
        /**
         * Create a ConfigDomain.
         */
        public ConfigDomain (EditorContext ctx)
        {
            _ctx = ctx;
        }

        @Override
        public String getLabel ()
        {
            return "CONFIGS";
        }

        @Override
        public Iterator<Result> getResults (final Predicate<? super ConfigReference<?>> detector)
        {
            return new AbstractIterator<Result>() {
                protected Result computeNext () {
                    while (!_cfgIterator.hasNext()) {
                        if (!_groupIterator.hasNext()) {
                            return endOfData();
                        }
                        _currentGroup = _groupIterator.next();
                        _cfgIterator = _currentGroup.getConfigs().iterator();
                    }
                    final ManagedConfig cfg = _cfgIterator.next();
                    final ConfigGroup<?> group = _currentGroup;
                    return find(cfg, detector)
                        ? new Result(group.getName() + ": " + cfg.getName()) {
                                public void onClick () {
                                    BaseConfigEditor
                                        .createEditor(_ctx, group.getConfigClass(), cfg.getName())
                                        .setVisible(true);
                                }
                            }
                        : null;
                }
                protected Iterator<? extends ManagedConfig> _cfgIterator =
                        Iterators.emptyIterator();
                protected Iterator<ConfigGroup> _groupIterator =
                        _ctx.getConfigManager().getGroups().iterator();
                protected ConfigGroup<?> _currentGroup;
            };
        }

        /** The editor context. */
        protected EditorContext _ctx;
    }

    /**
     * An abstract class for creating a Domain that searches scene files.
     */
    public abstract static class TudeySceneDomain
        implements Domain
    {
        public TudeySceneDomain (EditorContext ctx, String label, File dir, String... subdirs)
        {
            _ctx = ctx;
            _label = label;
            _dir = validateDir(dir);
            if (subdirs.length == 0) {
                _dirs = ImmutableList.of(dir);

            } else {
                ImmutableList.Builder<File> builder = ImmutableList.builder();
                for (String s : subdirs) {
                    builder.add(validateDir(new File(dir, s)));
                }
                _dirs = builder.build();
            }
        }

        /**
         * Validate that the specified file is a Directory.
         */
        protected File validateDir (File dir)
        {
            Preconditions.checkArgument(dir.isDirectory(), "Invalid directory: %s", dir);
            return dir;
        }

        @Override
        public String getLabel ()
        {
            return _label;
        }

        @Override
        public Iterator<Result> getResults (final Predicate<? super ConfigReference<?>> detector)
        {
            Iterable<File> allFiles = Iterables.concat(
                Iterables.transform(_dirs,
                    new Function<File, Iterable<File>>() {
                        public Iterable<File> apply (File dir) {
                            return datFiles(dir);
                        }
                    }));

            return Iterables.transform(allFiles,
                new Function<File, Result>() {
                    public Result apply (File f) {
                        return resultForFile(f, detector);
                    }
                }).iterator();
        }

        /**
         * Find all .dat files beneath the specified directory.
         */
        protected Iterable<File> datFiles (File directory)
        {
            return Iterables.concat(
                Arrays.asList(directory.listFiles(DAT_FILTER)),
                Iterables.concat(
                    Iterables.transform(Arrays.asList(directory.listFiles(DIR_FILTER)),
                        new Function<File, Iterable<File>>() {
                            public Iterable<File> apply (File dir) {
                                return datFiles(dir); // recurse
                            }
                        })));
        }

        /**
         * Generate a Result for the specified file, or return null.
         */
        protected Result resultForFile (File file, Predicate<? super ConfigReference<?>> detector)
        {
            TudeySceneModel model;
            try {
                model = ExportFileUtil.readObject(file, TudeySceneModel.class);
            } catch (Exception e) {
                return null;
            }
            model.init(_ctx.getConfigManager());
            for (TudeySceneModel.Entry entry : model.getEntries()) {
                if (find(entry.getReference(), detector)) {
                    return new SceneResult(_dir, file);
                }
            }
            return null;
        }

        /**
         * Called to edit the scene. You must implement this.
         */
        protected abstract void editScene (File file);

        /** Our context. */
        protected EditorContext _ctx;

        /** The label for this domain. */
        protected String _label;

        /** The top-level directory from which we print relative filenames. */
        protected File _dir;

        /** The directories, under the top-level _dir, which are the roots for our search. */
        protected List<File> _dirs;

        /**
         * A Result from a scene.
         */
        protected class SceneResult extends Result
        {
            public SceneResult (File topDir, File file)
            {
                super(file.getAbsolutePath().substring(topDir.getAbsolutePath().length()));
                _file = file;
            }

            @Override
            public void onClick ()
            {
                editScene(_file);
            }

            /** The file representing the scene. */
            protected File _file;
        }

        /** A filter for finding directories, excluding .svn. */
        protected static final FileFilter DIR_FILTER = new FileFilter() {
            public boolean accept (File f) {
                return f.isDirectory() && !".svn".equals(f.getName());
            }
        };

        /** A filter for finding .dat files. */
        protected static final FileFilter DAT_FILTER = new FileFilter() {
            public boolean accept (File f) {
                return f.getName().endsWith(".dat");
            }
        };
    }

    /**
     * Create a ConfigSearcher ui.
     */
    public ConfigSearcher (
        EditorContext ctx, String title, final Predicate<? super ConfigReference<?>> detector,
        final Iterable<Domain> domains)
    {
        _ctx = ctx;
        _content = GroupLayout.makeVBox(GroupLayout.NONE, GroupLayout.LEFT, GroupLayout.STRETCH);
        _status = new JLabel(".");
        add(new JScrollPane(_content), BorderLayout.CENTER);
        add(_status, BorderLayout.NORTH);
        setTitle(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(850, 600);
        SwingUtil.centerWindow(this);
        _eprefs.bindWindowBounds("ConfigSearcher.", this);
        setVisible(true);

        EventQueue.invokeLater(new Runnable() {
            public void run () {
                if (!ConfigSearcher.this.isShowing()) {
                    return; // the window's been dismissed
                }
                while (!_resultIterator.hasNext()) {
                    if (!_domainIterator.hasNext()) {
                        addLabel("DONE.");
                        _status.setText("");
                        return;
                    }
                    Domain domain = _domainIterator.next();
                    addLabel(domain.getLabel());
                    _resultIterator = domain.getResults(detector);
                }
                Result result = _resultIterator.next();
                if (result != null) {
                    addResult(result);
                }
                updateStatusLabel();
                EventQueue.invokeLater(this);
            }

            protected Iterator<Result> _resultIterator = Iterators.emptyIterator();
            protected Iterator<Domain> _domainIterator = domains.iterator();
        });
    }

    /**
     * Add a label for a domain to the ongoing search results.
     */
    protected void addLabel (String label)
    {
        _content.add(new JLabel(label));
        SwingUtil.refresh(_content);
    }

    /**
     * Add a non-null search result to the results window.
     */
    protected void addResult (final Result result)
    {
        JLabel label = new JLabel("  " + result.getLabel());
        label.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked (MouseEvent event) {
                result.onClick();
            }
        });
        _content.add(label);
        SwingUtil.refresh(_content);
    }

    /**
     * Update the status label to indicate continuing progress on the search.
     */
    protected void updateStatusLabel ()
    {
        long now = System.currentTimeMillis();
        if (now >= _nextStatusUpdate) {
            _status.setText(StringUtil.fill('.', 1 + (_status.getText().length() % 4)));
            _nextStatusUpdate = now + 800; // 800ms
        }
    }

    /** Our editor context. */
    protected EditorContext _ctx;

    /** The content panel where we place our search results. */
    protected JPanel _content;

    /** The status label that displays ongoing search progress. */
    protected JLabel _status;

    /** The time at which we'll next update the status label. */
    protected long _nextStatusUpdate;

    /** Preferences. */
    protected ToolUtil.EditablePrefs _eprefs =
        new ToolUtil.EditablePrefs(Preferences.userNodeForPackage(ConfigSearcher.class));

    /**
     * Internal helper for find.
     */
    protected static boolean find (
        Object val, Predicate<? super ConfigReference<?>> detector,
        Set<Object> seen)
    {
        if (val == null) {
            return false;
        }

        Class<?> c = val.getClass();
        if ((c == String.class) || c.isPrimitive() || Primitives.isWrapperType(c) ||
                !seen.add(val)) {
            return false;
        }

        // make a list of sub-fields
        if (c.isArray()) {
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                if (find(Array.get(val, ii), detector, seen)) {
                    return true;
                }
            }

        } else if (val instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            if (detector.apply(ref)) {
                return true;
            }
            for (Object value : ref.getArguments().values()) {
                if (find(value, detector, seen)) {
                    return true;
                }
            }

        } else {
            for (Field f : FIELDS.getUnchecked(c)) {
                Object o;
                try {
                    o = f.get(val);
                } catch (IllegalAccessException iae) {
                    continue;
                }
                if (find(o, detector, seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Internal helper for findPath.
     */
    protected static String findPath (
        Object val, Predicate<? super ConfigReference<?>> detector,
        Set<Object> seen)
    {
        if (val == null) {
            return null;
        }

        Class<?> c = val.getClass();
        if ((c == String.class) || c.isPrimitive() || Primitives.isWrapperType(c) ||
                !seen.add(val)) {
            return null;
        }

        // make a list of sub-fields
        String path;
        if (val instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            if (detector.apply(ref)) {
                return "this";
            }
            for (Map.Entry<String, Object> entry : ref.getArguments().entrySet()) {
                path = findPath(entry.getValue(), detector, seen);
                if (path != null) {
                    return "{" + entry.getKey() + "}." + path;
                }
            }

        } else if (c.isArray()) {
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                path = findPath(Array.get(val, ii), detector, seen);
                if (path != null) {
                    return "[" + ii + "]." + path;
                }
            }

        } else {
            for (Field f : FIELDS.getUnchecked(c)) {
                Object o;
                try {
                    o = f.get(val);
                } catch (IllegalAccessException iae) {
                    continue;
                }
                path = findPath(o, detector, seen);
                if (path != null) {
                    return f.getName() + "." + path;
                }
            }
        }
        return null;
    }

    /**
     * Internal helper for findAttributes.
     */
    protected static <T> Iterable<T> findAttributes (
        Object val, Function<? super ConfigReference<?>, ? extends Iterable<T>> detector,
        Set<Object> seen)
    {
        if (val == null) {
            return ImmutableList.<T>of();
        }

        Class<?> c = val.getClass();
        if ((c == String.class) || c.isPrimitive() || Primitives.isWrapperType(c) ||
                !seen.add(val)) {
            return ImmutableList.<T>of();
        }

        // make a list of sub-fields
        List<Iterable<T>> list = Lists.newArrayList();
        if (val instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            list.add(detector.apply(ref));
            for (Object value : ref.getArguments().values()) {
                list.add(findAttributes(value, detector, seen));
            }

        } else if (c.isArray()) {
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                list.add(findAttributes(Array.get(val, ii), detector, seen));
            }

        } else {
            for (Field f : FIELDS.getUnchecked(c)) {
                Object o;
                try {
                    o = f.get(val);
                } catch (IllegalAccessException iae) {
                    continue;
                }
                list.add(findAttributes(o, detector, seen));
            }
        }
        return Iterables.concat(list);
    }

    /** All the fields (and superfields...) of a class, cached. */
    protected static final LoadingCache<Class<?>, ImmutableList<Field>> FIELDS =
        CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(
            new CacheLoader<Class<?>, ImmutableList<Field>>() {
                public ImmutableList<Field> load (Class<?> clazz) {
                    ImmutableList.Builder<Field> builder = ImmutableList.builder();
                    // add recurse on superclass
                    Class<?> superClazz = clazz.getSuperclass();
                    if (superClazz != null) {
                        builder.addAll(FIELDS.getUnchecked(superClazz));
                    }
                    // get all fields of the specified class, and filter out the static ones..
                    for (Field f : clazz.getDeclaredFields()) {
                        // add all non-static fields; make them accessible
                        if (0 == (f.getModifiers() & (Modifier.STATIC|Modifier.TRANSIENT))) {
                            f.setAccessible(true);
                            builder.add(f);
                        }
                    }
                    return builder.build();
                }
            });
}
