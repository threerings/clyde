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

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.samskivert.util.StringUtil;

/**
 * Utility methods used by our various source code generating tasks.
 */
public class GenUtil extends com.samskivert.util.GenUtil
{
    /** A regular expression for matching the package declaration. */
    public static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+(\\S+)\\W");

    /** A regular expression for matching the class or interface declaration. */
    public static final Pattern NAME_PATTERN =
        Pattern.compile("^\\s*public\\s+(?:(?:abstract|final)\\s+)*(@?interface|class|enum)\\s+([\\w$]+)");

    /**
     * Returns the name of the supplied class as it would appear in ActionScript code using the
     * class (no package prefix, arrays specified as Array<code>Array</code>).
     */
    public static String simpleASName (Class<?> clazz)
    {
        if (clazz.isArray()) {
            Class<?> compoType = clazz.getComponentType();
            if (Byte.TYPE.equals(compoType)) {
                return "ByteArray";
            } else if (Object.class.equals(compoType)) {
                return "Array";
            } else {
                return "TypedArray /* of " + compoType + " */";
            }
        } else if (clazz == Boolean.TYPE) {
            return "Boolean";
        } else if (clazz == Byte.TYPE || clazz == Character.TYPE ||
                   clazz == Short.TYPE || clazz == Integer.TYPE) {
            return "int";
        } else if (clazz == Long.TYPE) {
            return "Long";
        } else if (clazz == Float.TYPE || clazz == Double.TYPE) {
            return "Number";
        } else {
            String cname = clazz.getName();
            Package pkg = clazz.getPackage();
            int offset = (pkg == null) ? 0 : pkg.getName().length()+1;
            String name = cname.substring(offset);
            return name.replace('$', '_');
        }
    }

    /**
     * "Boxes" the supplied argument, ie. turning an <code>int</code> into an <code>Integer</code>
     * object.
     */
    public static String boxArgument (Class<?> clazz, String name)
    {
        if (clazz == Boolean.TYPE) {
            return "Boolean.valueOf(" + name + ")";
        } else if (clazz == Byte.TYPE) {
            return "Byte.valueOf(" + name + ")";
        } else if (clazz == Character.TYPE) {
            return "Character.valueOf(" + name + ")";
        } else if (clazz == Short.TYPE) {
            return "Short.valueOf(" + name + ")";
        } else if (clazz == Integer.TYPE) {
            return "Integer.valueOf(" + name + ")";
        } else if (clazz == Long.TYPE) {
            return "Long.valueOf(" + name + ")";
        } else if (clazz == Float.TYPE) {
            return "Float.valueOf(" + name + ")";
        } else if (clazz == Double.TYPE) {
            return "Double.valueOf(" + name + ")";
        } else {
            return name;
        }
    }

    /**
     * "Unboxes" the supplied argument, ie. turning an <code>Integer</code> object into an
     * <code>int</code>.
     */
    public static String unboxArgument (Type type, String name)
    {
        if (Boolean.TYPE.equals(type)) {
            return "((Boolean)" + name + ").booleanValue()";
        } else if (Byte.TYPE.equals(type)) {
            return "((Byte)" + name + ").byteValue()";
        } else if (Character.TYPE.equals(type)) {
            return "((Character)" + name + ").charValue()";
        } else if (Short.TYPE.equals(type)) {
            return "((Short)" + name + ").shortValue()";
        } else if (Integer.TYPE.equals(type)) {
            return "((Integer)" + name + ").intValue()";
        } else if (Long.TYPE.equals(type)) {
            return "((Long)" + name + ").longValue()";
        } else if (Float.TYPE.equals(type)) {
            return "((Float)" + name + ").floatValue()";
        } else if (Double.TYPE.equals(type)) {
            return "((Double)" + name + ").doubleValue()";
        } else if (Object.class.equals(type)) {
            return name; // no need to cast object
        } else if (type instanceof Class<?>) {
            return "(" + simpleName(type) + ")" + name;
        } else {
            return "this.<" + simpleName(type) + ">cast(" + name + ")";
        }
    }

    /**
     * "Boxes" the supplied argument, ie. turning an <code>int</code> into an <code>Integer</code>
     * object.
     */
    public static String boxASArgument (Class<?> clazz, String name)
    {
        return boxASArgumentAndGatherImports(clazz, name, new ImportSet());
    }

    /**
     * Calculates the set of imports required for the code to box the given class in actionscript.
     * For example, for <code>int</code>, <code>Integer.class</code> will be returned.
     */
    public static ImportSet getASBoxImports (Class<?> clazz)
    {
        ImportSet imports = new ImportSet();
        boxASArgumentAndGatherImports(clazz, "foo", imports);
        return imports;
    }

    /**
     * "Unboxes" the supplied argument, ie. turning an <code>Integer</code> object into an
     * <code>int</code>.
     */
    public static String unboxASArgument (Class<?> clazz, String name)
    {
        if (clazz == Boolean.TYPE) {
            return "(" + name + " as Boolean)";
        } else if (clazz == Byte.TYPE ||
                   clazz == Character.TYPE ||
                   clazz == Short.TYPE ||
                   clazz == Integer.TYPE) {
            return "(" + name + " as int)";
        } else if (clazz == Long.TYPE) {
            return "(" + name + " as Long)";
        } else if (clazz == Float.TYPE ||
                   clazz == Double.TYPE) {
            return "(" + name + " as Number)";
        } else {
            return "(" + name + " as " + simpleASName(clazz) + ")";
        }
    }

    /**
     * Potentially clones the supplied argument if it is the type that needs such treatment.
     */
    public static String cloneArgument (Class<?> dsclazz, Field field, String name)
    {
        Class<?> clazz = field.getType();
        if (clazz.isArray() || dsclazz.equals(clazz)) {
            return "(" + name + " == null) ? null : " + name + ".clone()";
        } else if (dsclazz.isAssignableFrom(clazz)) {
            return "(" + name + " == null) ? null : " +
                "(" + simpleName(field) + ")" + name + ".clone()";
        } else {
            return name;
        }
    }

    /**
     * Reads in the supplied source file and locates the package and class or interface name and
     * returns a fully qualified class name.
     */
    public static String readClassName (File source)
        throws IOException
    {
        // load up the file and determine it's package and classname
        String pkgname = null, name = null;
        BufferedReader bin = new BufferedReader(new FileReader(source));
        String line;
        while ((line = bin.readLine()) != null) {
            Matcher pm = PACKAGE_PATTERN.matcher(line);
            if (pm.find()) {
                pkgname = pm.group(1);
            }
            Matcher nm = NAME_PATTERN.matcher(line);
            if (nm.find()) {
                name = nm.group(2);
                break;
            }
        }
        bin.close();

        // make sure we found something
        if (name == null) {
            throw new IOException("Unable to locate class or interface name in " + source + ".");
        }

        // prepend the package name to get a name we can Class.forName()
        if (pkgname != null) {
            name = pkgname + "." + name;
        }

        return name;
    }

    /**
     * Return an "@Generated" annotation.
     *
     * @param clazz the class doing the code generation, NOT the generation target.
     * @param indent the number of spaces that the annotation is indented.
     * @param includeDate include the date?
     * @param comments joined by a space and used as explanatory comments.
     */
    public static String getGeneratedAnnotation (
        Class<?> clazz, int indent, boolean includeDate, String... comments)
    {
        final int LINE_LENGTH = 100;
        String comm = StringUtil.join(comments, " ");
        boolean hasComment = !StringUtil.isBlank(comm);
        String anno = "@Generated(value={\"" + clazz.getName() + "\"}";
        if (includeDate) {
            anno += ",";
            // ISO 8601 date
            String date = " date=\"" +
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()) + "\"";
            // wrap the date onto a new line if it's going to be too long, or if we're
            // adding a comment (which is also on a new line)
            if (hasComment || anno.length() + date.length() + 1 + indent > LINE_LENGTH) {
                // put the date on a new line, space it right
                date = "\n" + StringUtil.fill(' ', indent + 10) + date;
            }
            anno += date;
        }
        if (hasComment) {
            anno += ",\n" + StringUtil.fill(' ', indent + 11) + "comments=\"" + comm + "\"";
        }
        anno += ")";
        return anno;
    }

    protected static String boxASArgumentAndGatherImports (Class<?> clazz, String name,
            ImportSet imports)
    {
        if (clazz == Boolean.TYPE) {
            imports.add(Boolean.class);
            return "langBoolean.valueOf(" + name + ")";
        } else if (clazz == Byte.TYPE) {
            imports.add(Byte.class);
            return "Byte.valueOf(" + name + ")";
        } else if (clazz == Character.TYPE) {
            imports.add(Character.class);
            return "Character.valueOf(" + name + ")";
        } else if (clazz == Short.TYPE) {
            imports.add(Short.class);
            return "Short.valueOf(" + name + ")";
        } else if (clazz == Integer.TYPE) {
            imports.add(Integer.class);
            return "Integer.valueOf(" + name + ")";
        } else if (clazz == Long.TYPE) {
            imports.add(Long.class);
            return name; // Long is left as is
        } else if (clazz == Float.TYPE) {
            imports.add(Float.class);
            return "Float.valueOf(" + name + ")";
        } else if (clazz == Double.TYPE) {
            imports.add(Double.class);
            return "Double.valueOf(" + name + ")";
        } else {
            imports.add(name);
            return name;
        }
    }
}
