//
// $Id$

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;
import java.io.FileFilter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.util.prefs.Preferences;

import javax.annotation.Nullable;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.ResourceUtil;
import com.threerings.util.ToolUtil;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.util.FieldCache;

import com.threerings.editor.util.EditorContext;

import com.threerings.export.util.ExportFileUtil;

import com.threerings.tudey.data.TudeySceneModel;

/**
 * Utilitiies for searching for and in ConfigReferences.
 */
public class ConfigSearcher extends JFrame
{
    /**
     * Detects a config reference.
     */
    public interface Detector
    {
        /**
         * Detect if the config reference meets some criteria.
         * The type may or may not be provided, and your detector will
         * probably want to err on false positives when there is no type.
         */
        public boolean apply (ConfigReference<?> ref, @Nullable Class<?> typeIfKnown);
    }

    /**
     * Detects attributes of a config reference.
     */
    public interface AttributeDetector<T>
    {
        /**
         * Detect certain attributes on a config reference.
         * The type may or may not be provided, and your detector will
         * probably want to err on false positives when there is no type.
         */
        public Multiset<T> apply (ConfigReference<?> ref, @Nullable Class<?> typeIfKnown);
    }

    /**
     * Detects attributes and formats the results.
     */
    public interface SearchReporter<T> extends AttributeDetector<T>
    {
        /**
         * Format the label for the result according to the non-empty accumulated attributes.
         */
        public String formatLabel (String label, Multiset<T> accumulatedAttrs);
    }

    /**
     * Find if anything satisfies the predicate in the specified object and any sub-objects.
     */
    public static boolean find (Object val, Detector detector)
    {
        return !findAttributes(val, new AttributeDetectorAdapter(detector)).isEmpty();
    }

    /**
     * Count how many configs satisfy the predicate in the specified object and any sub-objects.
     */
    public static int count (Object val, Detector detector)
    {
        return findAttributes(val, new AttributeDetectorAdapter(detector)).size();
    }

    /**
     * Find all the attributes in the specified object and any sub-objects.
     */
    public static <T> Multiset<T> findAttributes (Object val, AttributeDetector<T> detector)
    {
        return findAttributes(val, null, detector);
    }

    /**
     * Find all the attributes in the specified object and any sub-objects.
     */
    public static <T> Multiset<T> findAttributes (
            Object val, java.lang.reflect.Type valType, AttributeDetector<T> detector)
    {
        return findAttributes(val, valType, detector, Sets.newIdentityHashSet());
    }

    /**
     * A simple binary attribute regarding how well an object matched an attribute detector.
     */
    public enum Presence
    {
        /** A match was found. */
        MATCH,

        /** A possible match was found (the typeIfKnown was null, but otherwise it matched). */
        POSSIBLE_MATCH,
        ;

        /**
         * Get a reporter that reports presence.
         */
        public static SearchReporter<Presence> getReporter (
                final Class<?> clazz, final Predicate<? super ConfigReference<?>> pred)
        {
            return new SearchReporter<Presence>() {
                @Override
                public Multiset<Presence> apply (ConfigReference<?> ref, @Nullable Class<?> type)
                {
                    boolean fuzzy = (type == null);
                    return ((fuzzy || type.equals(clazz)) && pred.apply(ref))
                        ? (fuzzy ? RESULT_POSSIBLE_MATCH : RESULT_MATCH)
                        : RESULT_NONE;
                }
                @Override
                public String formatLabel (String label, Multiset<Presence> attrs)
                {
                    int possible = attrs.count(POSSIBLE_MATCH);
                    return attrs.count(MATCH) + ": " +
                            ((possible == 0) ? "" : ("(" + possible + " maybe): ")) +
                            label;
                }
            };
        }

        protected static final ImmutableMultiset<Presence> RESULT_NONE =
                ImmutableMultiset.of();
        protected static final ImmutableMultiset<Presence> RESULT_MATCH =
                ImmutableMultiset.of(MATCH);
        protected static final ImmutableMultiset<Presence> RESULT_POSSIBLE_MATCH =
                ImmutableMultiset.of(POSSIBLE_MATCH);
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
        public <T> Iterator<Result> getResults (SearchReporter<T> detector);
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
        public <T> Iterator<Result> getResults (final SearchReporter<T> detector)
        {
            return new AbstractIterator<Result>() {
                protected Result computeNext () {
                    while (!_cfgIterator.hasNext()) {
                        if (!_groupIterator.hasNext()) {
                            return endOfData();
                        }
                        _currentGroup = _groupIterator.next();
                        _cfgIterator = _currentGroup.getRawConfigs().iterator();
                    }
                    final ManagedConfig cfg = _cfgIterator.next();
                    final ConfigGroup<?> group = _currentGroup;
                    Multiset<T> attrs = findAttributes(cfg, detector);
                    return attrs.isEmpty()
                        ? null
                        : new Result(
                            detector.formatLabel(group.getName() + ": " + cfg.getName(), attrs)) {
                                public void onClick () {
                                    BaseConfigEditor
                                        .createEditor(_ctx, group.getConfigClass(), cfg.getName())
                                        .setVisible(true);
                                }
                            };
                }
                protected Iterator<? extends ManagedConfig> _cfgIterator =
                        Iterators.emptyIterator();
                protected Iterator<ConfigGroup<?>> _groupIterator =
                        _ctx.getConfigManager().getGroups().iterator();
                protected ConfigGroup<?> _currentGroup;
            };
        }

        /** The editor context. */
        protected EditorContext _ctx;
    }

    /**
     * An abstract building-block class for creating a Domain that searches files.
     */
    public abstract static class FileDomain
        implements Domain
    {
        public FileDomain (EditorContext ctx, String label, File dir, String... subdirs)
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
        public <T> Iterator<Result> getResults (final SearchReporter<T> detector)
        {
            Iterable<File> allFiles = Iterables.concat(
                Iterables.transform(_dirs,
                    new Function<File, Iterable<File>>() {
                        public Iterable<File> apply (File dir) {
                            return findFiles(dir);
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
         * Find all the searchable files in the specified top-level directory.
         */
        protected Iterable<File> findFiles (File directory)
        {
            return findFiles(directory, DAT_FILTER);
        }

        /**
         * Find all the files matching the specified filter underneath the specified directory.
         */
        protected Iterable<File> findFiles (File directory, final FileFilter filter)
        {
            return Iterables.concat(
                Arrays.asList(directory.listFiles(filter)),
                Iterables.concat(
                    Iterables.transform(Arrays.asList(directory.listFiles(DIR_FILTER)),
                        new Function<File, Iterable<File>>() {
                            public Iterable<File> apply (File dir) {
                                return findFiles(dir, filter); // recurse
                            }
                        })));
        }

        /**
         * Generate a Result for the specified file, or return null.
         */
        protected <T> Result resultForFile (File file, SearchReporter<T> detector)
        {
            Multiset<T> attrs = attrsForFile(file, detector);
            return attrs.isEmpty()
                ? null
                : new FileResult(_dir, file, detector, attrs);
        }

        /**
         * Get the attributes for the object(s) in the specified file.
         */
        protected abstract <T> Multiset<T> attrsForFile (File file, SearchReporter<T> detector);

        /**
         * Called to edit the file. You must implement this.
         */
        protected abstract void openFile (File file);

        /** Our context. */
        protected EditorContext _ctx;

        /** The label for this domain. */
        protected String _label;

        /** The top-level directory from which we print relative filenames. */
        protected File _dir;

        /** The directories, under the top-level _dir, which are the roots for our search. */
        protected List<File> _dirs;

        /**
         * A Result from a file.
         */
        protected class FileResult extends Result
        {
            public <T> FileResult (
                    File topDir, File file, SearchReporter<T> det, Multiset<T> result)
            {
                super(det.formatLabel(
                    file.getAbsolutePath().substring(topDir.getAbsolutePath().length()),
                    result));
                _file = file;
            }

            @Override
            public void onClick ()
            {
                openFile(_file);
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
     * An abstract class for creating a Domain that searches scene files.
     */
    public abstract static class TudeySceneDomain extends FileDomain
    {
        public TudeySceneDomain (EditorContext ctx, String label, File dir, String... subdirs)
        {
            super(ctx, label, dir, subdirs);
        }

        @Override
        protected <T> Multiset<T> attrsForFile (File file, SearchReporter<T> detector)
        {
            TudeySceneModel model;
            try {
                model = ExportFileUtil.readObject(file, TudeySceneModel.class);
            } catch (Exception e) {
                return ImmutableMultiset.<T>of();
            }
            model.init(_ctx.getConfigManager());

            Multiset<T> attrs = null;
            for (TudeySceneModel.Entry entry : model.getEntries()) {
                attrs = addAll(attrs,
                        findAttributes(entry.getReference(), entry.getReferenceType(), detector));
            }
            return (attrs == null) ? ImmutableMultiset.<T>of() : attrs;
        }
    }

    /**
     * Create a ConfigSearcher ui.
     */
    public <T> ConfigSearcher (
        EditorContext ctx, String title, final SearchReporter<T> detector,
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
        _eprefs.bindWindowBounds("ConfigSearcher." + ResourceUtil.getPrefsPrefix(), this);
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
     * Internal helper for findAttributes.
     */
    protected static <T> Multiset<T> findAttributes (
        Object val, java.lang.reflect.Type valGenericType, AttributeDetector<T> detector,
        Set<Object> seen)
    {
        if (val == null) {
            return ImmutableMultiset.<T>of();
        }

        Class<?> c = val.getClass();
        if ((c == String.class) || c.isPrimitive() || Primitives.isWrapperType(c) ||
                !seen.add(val)) {
            return ImmutableMultiset.<T>of();
        }

        // make a list of sub-fields
        Multiset<T> attrs = null;
        if (val instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            Class<?> refType = asClass(getFirstGenericType(valGenericType));

            attrs = addAll(attrs, detector.apply(ref, refType));
            for (Object value : ref.getArguments().values()) {
                attrs = addAll(attrs, findAttributes(value, null, detector, seen));
            }

        } else if (c.isArray()) {
            java.lang.reflect.Type subType = (java.lang.reflect.Type)c.getComponentType();
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                attrs = addAll(attrs, findAttributes(Array.get(val, ii), subType, detector, seen));
            }

        } else if (val instanceof Collection) {
            java.lang.reflect.Type subType = getFirstGenericType(valGenericType);
            for (Object o : ((Collection)val)) {
                attrs = addAll(attrs, findAttributes(o, subType, detector, seen));
            }

        } else {
            for (Field f : _fieldCache.getFields(c)) {
                Object o;
                try {
                    o = f.get(val);
                } catch (IllegalAccessException iae) {
                    continue;
                }
                attrs = addAll(attrs, findAttributes(o, f.getGenericType(), detector, seen));
            }
        }
        return (attrs != null) ? attrs : ImmutableMultiset.<T>of();
    }

    /**
     * A helper for findAttributes as a small nod towards avoiding garbage creation.
     */
    protected static <T> Multiset<T> addAll (Multiset<T> accum, Multiset<T> toAdd)
    {
        if (!toAdd.isEmpty()) {
            if (accum == null) {
                accum = HashMultiset.create();
            }
            accum.addAll(toAdd);
        }
        return accum;
    }

    /**
     * Turn the Type into a class, if possible.
     */
    protected static Class<?> asClass (java.lang.reflect.Type type)
    {
        if (type instanceof Class) {
            return (Class)type;
        }
        // TODO: more

        return null;
    }

    /**
     * Get the first generic type argument of the specified type.
     */
    protected static java.lang.reflect.Type getFirstGenericType (java.lang.reflect.Type type)
    {
        if (type instanceof ParameterizedType) {
            java.lang.reflect.Type[] args = ((ParameterizedType)type).getActualTypeArguments();
            return (args.length > 0) ? args[0] : null;
        }
        // TODO: more?

        return null;
    }

    /**
     * Adapt a simple detector to be used as an AttributeDetector.
     */
    protected static class AttributeDetectorAdapter
        implements AttributeDetector<Boolean>
    {
        public AttributeDetectorAdapter (Detector det)
        {
            _det = det;
        }

        @Override
        public Multiset<Boolean> apply (ConfigReference<?> ref, Class<?> typeIfKnown)
        {
            return _det.apply(ref, typeIfKnown) ? YES : NO;
        }

        protected final Detector _det;
        protected static final Multiset<Boolean> NO = ImmutableMultiset.of();
        protected static final Multiset<Boolean> YES = ImmutableMultiset.of(true);
    }

    /** All the fields (and superfields...) of a class, cached. */
    protected static final FieldCache _fieldCache = new FieldCache();
}
