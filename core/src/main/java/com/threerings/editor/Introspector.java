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

package com.threerings.editor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;

import static com.threerings.editor.Log.log;

/**
 * Provides access to the editable properties of objects.
 */
public class Introspector
{
    /**
     * Returns an array containing the categories to which the supplied class's properties are
     * assigned.
     */
    public static String[] getCategories (Class<?> clazz)
    {
        String[] categories = _categories.get(clazz);
        if (categories == null) {
            categories = ArrayUtil.EMPTY_STRING;
            for (Property prop : getProperties(clazz)) {
                String category = prop.getAnnotation().category();
                if (!ListUtil.contains(categories, category)) {
                    categories = ArrayUtil.append(categories, category);
                }
            }
            if (DynamicallyEditable.class.isAssignableFrom(clazz)) {
                if (!ListUtil.contains(categories, "")) {
                    categories = ArrayUtil.append(categories, "");
                }
            }
            _categories.put(clazz, categories);
        }
        return categories;
    }

    /**
     * Returns an array containing both the static and the dynamic properties of the specified
     * object.
     */
    public static Property[] getProperties (Object object)
    {
        Property[] sprops = getProperties(object.getClass());
        if (!(object instanceof DynamicallyEditable)) {
            return sprops;
        }
        Property[] dprops = ((DynamicallyEditable)object).getDynamicProperties();
        if (sprops.length == 0) {
            return dprops;
        } else if (dprops.length == 0) {
            return sprops;
        } else {
            return ArrayUtil.concatenate(sprops, dprops);
        }
    }

    /**
     * Returns an array containing the editable properties of the supplied class.
     */
    public static Property[] getProperties (Class<?> clazz)
    {
        Property[] properties = _properties.get(clazz);
        if (properties == null) {
            _properties.put(clazz, properties = createProperties(clazz));
        }
        return properties;
    }

    /**
     * Returns the message bundle to use when translating the supplied class's properties.
     */
    public static String getMessageBundle (Class<?> clazz)
    {
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        if (clazz.isPrimitive()) {
            return EditorMessageBundle.DEFAULT;
        }
        String bundle = _bundles.get(clazz);
        if (bundle == null) {
            _bundles.put(clazz, bundle = findMessageBundle(clazz));
        }
        return bundle;
    }

    /**
     * Finds the editor message bundle for the supplied class.
     *
     * <P><em>NOTE</em>: if the class itself doesn't have an EditorMessageBundle annotation,
     * this code attempts to find the annotation on the package, climbing up the package
     * hierarchy one level at a time.
     */
    protected static String findMessageBundle (Class<?> clazz)
    {
        EditorMessageBundle annotation = clazz.getAnnotation(EditorMessageBundle.class);
        if (annotation != null) {
            return annotation.value();
        }
        Class<?> eclazz = clazz.getEnclosingClass();
        if (eclazz != null) {
            return getMessageBundle(eclazz);
        }
        String name = clazz.getName();
        int idx;
        while ((idx = name.lastIndexOf('.')) != -1) {
            name = name.substring(0, idx);
            try {
                // Ideally: Package p = Package.getPackage(name);
                // But: asking for a Package only works if a class in that package has already been
                // resolved, and there's no way to force resolution of all packages.
                // Instead, we load the "class" called package-info, and can look for
                // annotations there.
                Class<?> c = Class.forName(name + ".package-info");
                annotation = c.getAnnotation(EditorMessageBundle.class);
                if (annotation != null) {
                    return annotation.value();
                }
            } catch (ClassNotFoundException cnfe) {
                // expected
            }
        }
        return EditorMessageBundle.DEFAULT;
    }

    /**
     * Creates and returns the list of properties for the supplied class.
     */
    protected static Property[] createProperties (Class<?> clazz)
    {
        // get the list of properties
        ArrayList<Property> properties = new ArrayList<Property>();
        createProperties(clazz, properties);

        // sort the properties by weight
        Collections.sort(properties, WEIGHT_COMP);

        // if the class has a property order attribute, move the listed properties
        // to the beginning in the desired order
        PropertyOrder order = clazz.getAnnotation(PropertyOrder.class);
        if (order != null) {
            int index = 0;
            for (String name : order.value()) {
                int oindex = index;
                for (int ii = index, nn = properties.size(); ii < nn; ii++) {
                    Property property = properties.get(ii);
                    if (property.getName().equals(name)) {
                        properties.remove(ii);
                        properties.add(index++, property);
                        break;
                    }
                }
                if (index == oindex) {
                    log.warning("Missing property in order annotation [class=" +
                        clazz.getName() + ", property=" + name + "].");
                }
            }
        }

        // return an array with the results
        return properties.toArray(new Property[properties.size()]);
    }

    /**
     * Retrieves all {@link Editable} properties of the specified class.
     */
    protected static void createProperties (Class<?> clazz, ArrayList<Property> properties)
    {
        // prepend the superclass properties
        Class<?> sclazz = clazz.getSuperclass();
        if (sclazz != null) {
            Collections.addAll(properties, getProperties(sclazz));
        }

        // find the getters and setters
        HashMap<String, Method> unpaired = new HashMap<String, Method>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isSynthetic() || !method.isAnnotationPresent(Editable.class)) {
                continue;
            }
            String name = method.getName();
            boolean getter;
            if (name.startsWith("set")) {
                name = name.substring(3);
                getter = false;
            } else {
                if (name.startsWith("get")) {
                    name = name.substring(3);
                } else if (name.startsWith("is")) {
                    name = name.substring(2);
                } else {
                    log.warning("Invalid method name for editable property [class=" + clazz +
                        ", name=" + name + "].");
                    continue;
                }
                getter = true;
            }
            Method omethod = unpaired.remove(name);
            if (omethod != null) {
                Method gmethod = getter ? method : omethod;
                Method smethod = getter ? omethod : method;
                Class<?> rtype = gmethod.getReturnType();
                Class<?>[] ptypes = smethod.getParameterTypes();
                if (ptypes.length != 1 || ptypes[0] != rtype) {
                    log.warning("Mismatched types on getter/setter [class=" + clazz +
                        ", getter=" + gmethod + ", setter=" + smethod + "].");
                    continue;
                }
                properties.add(new MethodProperty(gmethod, smethod));
            } else {
                unpaired.put(name, method);
            }
        }
        if (!unpaired.isEmpty()) {
            log.warning("Found unmatched getters/setters [class=" + clazz + ", methods=" +
                unpaired.values() + "].");
        }

        // add all editable fields
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Editable.class)) {
                properties.add(new FieldProperty(field));
            }
        }
    }

    /** Cached category lists. */
    protected static HashMap<Class<?>, String[]> _categories = new HashMap<Class<?>, String[]>();

    /** Cached property lists. */
    protected static HashMap<Class<?>, Property[]> _properties = new HashMap<Class<?>, Property[]>();

    /** Cached editor bundle mappings. */
    protected static HashMap<Class<?>, String> _bundles = new HashMap<Class<?>, String>();

    /** Sorts properties by increasing weight. */
    protected static final Comparator<Property> WEIGHT_COMP = new Comparator<Property>() {
        public int compare (Property p1, Property p2) {
            return Double.compare(p1.getAnnotation().weight(), p2.getAnnotation().weight());
        }
    };
}
