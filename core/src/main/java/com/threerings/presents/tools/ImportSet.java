//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.presents.tools;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.StringUtil;

/**
 * Manages a set of strings to be used as a set of imports. Provides useful functions for
 * manipulating the set and sorts results.
 *
 * <p>Some methods in this class use a variable length String parameter 'replace'. This is a
 * convenience for easily specifying multiple find/replace pairs. For example, to replace "Foo"
 * with "Bar" and "123" with "ABC", a function can be called with the 4 arguments "Foo", "Bar",
 * "123", "ABC" for the String... replace argument.
 *
 * <p>A few methods also use a "pattern" string parameter that is used to match a class name.
 * This is a dumbed down regular expression (to avoid many \.) where "*" means .* and no other
 * characters have special meaning. The pattern is also implicitly enclosed with ^$ so that the
 * pattern must match the class name in its entirety. Callers will mostly use this to specify a
 * prefix like "something*" or a suffix like "*something".
 */
public class ImportSet
    implements Cloneable
{
    /**
     * Adds the given class' name to the set of imports.
     * @param clazz the class to add
     */
    public void add (Class<?> clazz)
    {
        _imports.add(clazz.getName());
    }

    /**
     * Adds the given name to the set of imports.
     * @param name the name to add
     */
    public void add (String name)
    {
        if (name != null) {
            _imports.add(name);
        }
    }

    /**
     * Adds all the imports from another import set into this one.
     * @param other the import set whose imports should be added
     */
    public void addAll (ImportSet other)
    {
        _imports.addAll(other._imports);
    }

    /**
     * Adds a class' name to the imports but first performs the given list of search/replaces as
     * described above.
     * @param clazz the class whose name is munged and added
     * @param replace array of pairs to search/replace on the name before adding
     */
    public void addMunged (Class<?> clazz, String... replace)
    {
        String name = clazz.getName();
        for (int ii = 0; ii < replace.length; ii += 2) {
            name = name.replace(replace[ii], replace[ii+1]);
        }
        _imports.add(name);
    }

    @Override
    public ImportSet clone ()
    {
        try {
            ImportSet newSet = (ImportSet)super.clone();
            newSet._pushed = Lists.newArrayList();
            newSet._imports = Sets.newHashSet(this._imports);
            return newSet;

        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse); // we are Cloneable
        }
    }

    /**
     * Gets rid of primitive and java.lang imports.
     */
    public void removeGlobals ()
    {
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String name = i.next();
            if (name.indexOf('.') == -1) {
                i.remove();
            } else if (name.startsWith("java.lang")) {
                i.remove();
            }
        }
    }

    /**
     * Gets rid of array imports.
     */
    public int removeArrays ()
    {
        return removeAll("[*");
    }

    /**
     * Remove all classes that are in the same package.
     * @param pkg package to remove
     */
    public void removeSamePackage (String pkg)
    {
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String name = i.next();
            if (name.startsWith(pkg) &&
                name.indexOf('.', pkg.length() + 1) == -1) {
                    i.remove();
            }
        }
    }

    /**
     * Replaces inner class imports (those with a '$') with an import of the parent class.
     */
    public void swapInnerClassesForParents ()
    {
        ImportSet declarers = new ImportSet();
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String name = i.next();
            int dollar = name.indexOf('$');
            if (dollar >= 0) {
                i.remove();
                declarers.add(name.substring(0, dollar));
            }
        }

        addAll(declarers);
    }

    /**
     * Replace all inner classes' separator characters ('$') with an underscore ('_') for use
     * when generating ActionScript.
     */
    public void translateInnerClasses ()
    {
        ImportSet inner = new ImportSet();
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String name = i.next();
            int dollar = name.indexOf('$');
            if (dollar >= 0) {
                i.remove();
                inner.add(name.replace('$', '_'));
            }
        }

        addAll(inner);
    }

    /**
     * Inserts imports for the non-primitive classes contained in all array imports.
     */
    public void translateClassArrays ()
    {
        ImportSet arrayTypes = new ImportSet();
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String name = i.next();
            int bracket = name.lastIndexOf('[');
            if (bracket != -1 &&
                name.charAt(bracket + 1) == 'L') {
                arrayTypes.add(name.substring(bracket + 2, name.length() - 1));
            }
        }

        addAll(arrayTypes);
    }

    /**
     * Temporarily remove one import matching the given pattern. The most recently pushed pattern
     * can be re-added using <code>popIn</code>. If there is no match, a null value is pushed so
     * that popIn can still be called.
     * @param pattern to match
     */
    public void pushOut (String pattern)
    {
        Pattern pat = makePattern(pattern);
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String imp = i.next();
            if (pat.matcher(imp).matches()) {
                i.remove();
                _pushed.add(imp);
                return;
            }
        }
        _pushed.add(null);
    }

    /**
     * Re-adds the most recently popped import to the set. If a null value was pushed, does
     * nothing.
     * @throws IndexOutOfBoundsException if there is nothing to pop
     */
    public void popIn ()
    {
        String front = _pushed.remove(_pushed.size() - 1);
        if (front != null) {
            _imports.add(front);
        }
    }

    public void remove (String name)
    {
        _imports.remove(name);
    }

    /**
     * Removes the name of a class from the imports.
     * @param clazz the class whose name should be removed
     */
    public void remove (Class<?> clazz)
    {
        _imports.remove(clazz.getName());
    }

    /**
     * Replaces any import exactly each find string with the corresponding replace string.
     * See the description above.
     * @param replace array of pairs for search/replace
     */
    public void replace (String... replace)
    {
        HashSet<String> toAdd = Sets.newHashSet();
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String name = i.next();
            for (int j = 0; j < replace.length; j += 2) {
                if (name.equals(replace[j])) {
                    toAdd.add(replace[j + 1]);
                    i.remove();
                    break;
                }
            }
        }
        _imports.addAll(toAdd);
    }

    /**
     * Replace the import matchings a class name.
     * @param pattern the class whose name to match
     * @param replacement string to use instead
     */
    public void replace (Class<?> pattern, String replacement)
    {
        replace(pattern.getName(), replacement);
    }

    /**
     * Remove all imports matching the given pattern.
     * @param pattern the dumbed down regex to match (see description above)
     * @return the number of imports removed
     */
    public int removeAll (String pattern)
    {
        Pattern pat = makePattern(pattern);
        int removed = 0;
        Iterator<String> i = _imports.iterator();
        while (i.hasNext()) {
            String name = i.next();
            if (pat.matcher(name).matches()) {
                i.remove();
                ++removed;
            }
        }
        return removed;
    }

    /**
     * Adds a new munged import for each existing import that matches a pattern. The new entry is
     * a copy of the old entry but modified according to the given find/replace pairs (see
     * description above).
     * @param pattern to qualify imports to duplicate
     * @param replace pairs to find/replace on the new import
     */
    public void duplicateAndMunge (String pattern, String... replace)
    {
        Pattern pat = makePattern(pattern);
        HashSet<String> toMunge = Sets.newHashSet();
        for (String name : _imports) {
            if (pat.matcher(name).matches()) {
                toMunge.add(name);
            }
        }
        for (String name : toMunge) {
            String newname = name;
            for (int ii = 0; ii < replace.length; ii += 2) {
                newname = newname.replace(replace[ii], replace[ii + 1]);
            }
            _imports.add(newname);
        }
    }

    /**
     * Converts the set of imports to groups of class names, according to conventional package
     * ordering and spacing. Within each group, sorting is alphabetical.
     */
    public List<List<String>> toGroups ()
    {
        List<String> list = Lists.newArrayList(_imports);
        Collections.sort(list, new Comparator<String>() {
            public int compare (String class1, String class2) {
                return ComparisonChain.start()
                    .compare(findImportGroup(class1), findImportGroup(class2))
                    .compare(class1, class2)
                    .result();
            }
        });
        List<List<String>> result = Lists.newArrayList();
        List<String> current = null;
        int lastGroup = -2;
        for (String imp : list) {
            int group = findImportGroup(imp);
            if (group != lastGroup) {
                if (current == null || !current.isEmpty()) {
                    result.add(current = Lists.<String>newArrayList());
                }
                lastGroup = group;
            }
            current.add(imp);
        }
        return result;
    }

    /**
     * Convert the set of imports to a sorted list, ready to be output to a generated file.
     * @return the sorted list of imports
     */
    public List<String> toList ()
    {
        ComparableArrayList<String> list = new ComparableArrayList<String>();
        list.addAll(_imports);
        list.sort();
        return list;
    }

    @Override
    public String toString ()
    {
        return StringUtil.toString(_imports);
    }

    /**
     * Find the index of the best import group to use for the specified import.
     */
    protected int findImportGroup (String imp)
    {
        int bestGroup = -1;
        int bestPrefixLength = -1;
        int ii = -1;
        for (String prefix : getImportGroups()) {
            ii++;
            if (!imp.startsWith(prefix)) {
                continue;
            }
            if (prefix.length() > bestPrefixLength) {
                bestPrefixLength = prefix.length();
                bestGroup = ii;
            }
        }
        return bestGroup;
    }

    /**
     * Return an iterable over the import prefixes that should be grouped together.
     */
    protected Iterable<String> getImportGroups ()
    {
        return IMPORT_GROUPS;
    }

    /**
     * Create a real regular expression from the dumbed down input.
     * @param input the dumbed down wildcard expression
     * @return the calculated regular expression
     */
    protected static Pattern makePattern (String input)
    {
        StringBuilder pattern = new StringBuilder('^');

        while (true) {
            String[] parts = _splitter.split(input, 2);
            pattern.append(Pattern.quote(parts[0]));
            if (parts.length == 1) {
                break;
            }
            int length = parts[0].length();
            String wildcard = input.substring(length, length + 1);
            if (wildcard.equals("*")) {
                pattern.append(".*");
            } else {
                System.err.println("Bad wildcard " + wildcard);
            }
            input = parts[1];
        }

        pattern.append("$");
        return Pattern.compile(pattern.toString());
    }

    protected HashSet<String> _imports = Sets.newHashSet();
    protected List<String> _pushed = Lists.newArrayList();

    protected static final Pattern _splitter = Pattern.compile("\\*");

    protected static final List<String> IMPORT_GROUPS = Lists.newArrayList(
        "flash",
        "fl",
        "java",
        "javax",
        "android",
        "",
        "com.samskivert",
        "com.samskivert.swing",
        "com.samskivert.servlet",
        "com.samskivert.velocity",
        "com.samskivert.jdbc",

        "com.samskivert.depot",

        "com.threerings.io",// Narya
        "com.threerings.no",
        "com.threerings.util",
        "com.threerings.presents",
        "com.threerings.crowd",
        "com.threerings.admin",
        "com.threerings.bureau",

        "com.threering.tudey",

        "com.threerings.display",// Aspirin
        "com.threerings.geom",
        "com.threerings.media",
        "com.threerings.text",
        "com.threerings.ui",

        "com.threerings.flashbang",

        "com.threerings.samsara",

        "com.threerings.cast", // Nenya
        "com.threerings.resource",
        "com.threerings.miso",
        "com.threerings.jme",
        "com.threerings.openal",
        "com.threerings.tools",

        "com.threerings.lembas",

        "com.threerings.parlor", // Vilya
        "com.threerings.puzzle",
        "com.threerings.whirled",
        "com.threerings.micasa",
        "com.threerings.stage",
        "com.threerings.stats",

        "com.threerings.orth",

        "com.threerings",
        "com.threerings.piracy",
        "com.threerings.ppa",
        "com.threerings.yohoho",
        "com.threerings.who",
        "com.threerings.projectx");
}
