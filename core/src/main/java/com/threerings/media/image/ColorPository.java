//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
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

package com.threerings.media.image;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import java.text.ParseException;

import java.awt.Color;

import com.google.common.collect.Lists;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.CompiledConfig;

import com.threerings.resource.ResourceManager;

import static com.threerings.media.Log.log;

/**
 * A repository of image recoloration information. It was called the recolor repository but the
 * re-s cancelled one another out.
 */
public class ColorPository implements Serializable
{
    /**
     * Used to store information on a class of colors. These are public to simplify the XML
     * parsing process, so pay them no mind.
     */
    public static class ClassRecord implements Serializable, Comparable<ClassRecord>
    {
        /** An integer identifier for this class. */
        public int classId;

        /** The name of the color class. */
        public String name;

        /** The source color to use when recoloring colors in this class. */
        public Color source;

        /** Data identifying the range of colors around the source color
         * that will be recolored when recoloring using this class. */
        public float[] range;

        /** The default starting legality value for this color class. See
         * {@link ColorRecord#starter}. */
        public boolean starter;

        /** The default colorId to use for recoloration in this class, or
         * 0 if there is no default defined. */
        public int defaultId;

        /** A table of target colors included in this class. */
        public HashIntMap<ColorRecord> colors = new HashIntMap<ColorRecord>();

        /** Used when parsing the color definitions. */
        public void addColor (ColorRecord record) {
            // validate the color id
            if (record.colorId > 127) {
                log.warning("Refusing to add color record; colorId > 127",
                    "class", this, "record", record);
            } else if (colors.containsKey(record.colorId)) {
                log.warning("Refusing to add duplicate colorId",
                    "class", this, "record", record, "existing", colors.get(record.colorId));
            } else {
                record.cclass = this;
                colors.put(record.colorId, record);
            }
        }

        /**
         * Translates a color identified in string form into the id that should be used to look up
         * its information. Throws an exception if no color could be found that associates with
         * that name.
         *
         * FIXME: This lookup could be sped up a lot with some cached data tables if it looked
         * like this function would get called in time critical situations.
         */
        public int getColorId (String name)
            throws ParseException
        {
            // Check if the string is itself a number
            try {
                int id = Integer.parseInt(name);
                if (colors.containsKey(id)) {
                    return id;
                }
            } catch (NumberFormatException e) {
                // Guess it must be something else
            }

            // Look for name matches among all colors
            for (ColorRecord color : colors.values()) {
                if (color.name.equalsIgnoreCase(name)) {
                    return color.colorId;
                }
            }

            // That input wasn't a color
            throw new ParseException("No color named '" + name + "'", 0);
        }

        /** Returns a random starting id from the entries in this class. */
        public ColorRecord randomStartingColor () {
            return randomStartingColor(RandomUtil.rand);
        }

        /** Returns a random starting id from the entries in this class. */
        public ColorRecord randomStartingColor (Random rand) {
            // figure out our starter ids if we haven't already
            if (_starters == null) {
                ArrayList<ColorRecord> list = Lists.newArrayList();
                for (ColorRecord color : colors.values()) {
                    if (color.starter) {
                        list.add(color);
                    }
                }
                _starters = list.toArray(new ColorRecord[list.size()]);
            }

            // sanity check
            if (_starters.length < 1) {
                log.warning("Requested random starting color from colorless component class",
                    "class", this);
                return null;
            }

            // return a random entry from the array
            return _starters[RandomUtil.getInt(_starters.length, rand)];
        }

        /**
         * Get the default ColorRecord defined for this color class, or null if none.
         */
        public ColorRecord getDefault () {
            return colors.get(defaultId);
        }

        // from interface Comparable<ClassRecord>
        public int compareTo (ClassRecord other) {
            return name.compareTo(other.name);
        }

        @Override
        public String toString () {
            return "[id=" + classId + ", name=" + name + ", source=#" +
                Integer.toString(source.getRGB() & 0xFFFFFF, 16) +
                ", range=" + StringUtil.toString(range) +
                ", starter=" + starter + ", colors=" +
                StringUtil.toString(colors.values().iterator()) + "]";
        }

        protected transient ColorRecord[] _starters;

        /** Increase this value when object's serialized state is impacted
         * by a class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 2;
    }

    /**
     * Used to store information on a particular color. These are public to simplify the XML
     * parsing process, so pay them no mind.
     */
    public static class ColorRecord implements Serializable, Comparable<ColorRecord>
    {
        /** The colorization class to which we belong. */
        public ClassRecord cclass;

        /** A unique colorization identifier (used in fingerprints). */
        public int colorId;

        /** The name of the target color. */
        public String name;

        /** Data indicating the offset (in HSV color space) from the
         * source color to recolor to this color. */
        public float[] offsets;

        /** Tags this color as a legal starting color or not. This is a shameful copout, placing
         * application-specific functionality into a general purpose library class. */
        public boolean starter;

        /**
         * Returns a value that is the composite of our class id and color id which can be used
         * to identify a colorization record. This value will always be a positive integer that
         * fits into 16 bits.
         */
        public int getColorPrint () {
            return ((cclass.classId << 8) | colorId);
        }

        /**
         * Returns the data in this record configured as a colorization instance.
         */
        public Colorization getColorization () {
//             if (_zation == null) {
//                 _zation = new Colorization(getColorPrint(), cclass.source,
//                                            cclass.range, offsets);
//             }
//             return _zation;
            return new Colorization(getColorPrint(), cclass.source, cclass.range, offsets);
        }

        // from interface Comparable<ColorRecord>
        public int compareTo (ColorRecord other) {
            return name.compareTo(other.name);
        }

        @Override
        public String toString () {
            return "[id=" + colorId + ", name=" + name +
                ", offsets=" + StringUtil.toString(offsets) + ", starter=" + starter + "]";
        }

        /** Our data represented as a colorization. */
        protected transient Colorization _zation;

        /** Increase this value when object's serialized state is impacted
         * by a class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 2;
    }

    /**
     * Returns an iterator over all color classes in this pository.
     */
    public Iterator<ClassRecord> enumerateClasses ()
    {
        return _classes.values().iterator();
    }

    public Collection<ClassRecord> getClasses ()
    {
        return _classes.values();
    }

    /**
     * Returns an array containing the records for the colors in the specified class.
     */
    public ColorRecord[] enumerateColors (String className)
    {
        // make sure the class exists
        ClassRecord record = getClassRecord(className);
        if (record == null) {
            return null;
        }

        // create the array
        ColorRecord[] crecs = new ColorRecord[record.colors.size()];
        Iterator<ColorRecord> iter = record.colors.values().iterator();
        for (int ii = 0; iter.hasNext(); ii++) {
            crecs[ii] = iter.next();
        }
        return crecs;
    }

    /**
     * Returns an array containing the ids of the colors in the specified class.
     */
    public int[] enumerateColorIds (String className)
    {
        // make sure the class exists
        ClassRecord record = getClassRecord(className);
        if (record == null) {
            return null;
        }

        int[] cids = new int[record.colors.size()];
        Iterator<ColorRecord> crecs = record.colors.values().iterator();
        for (int ii = 0; crecs.hasNext(); ii++) {
            cids[ii] = crecs.next().colorId;
        }
        return cids;
    }

    /**
     * Returns true if the specified color is legal for use at character creation time. false is
     * always returned for non-existent colors or classes.
     */
    public boolean isLegalStartColor (int colorPrint)
    {
        ColorRecord color = getColorRecord(colorPrint >> 8, colorPrint & 0xFF);
        return (color == null) ? false : color.starter;
    }

    /**
     * Returns true if the specified color is legal for use at character creation time. false is
     * always returned for non-existent colors or classes.
     */
    public boolean isLegalStartColor (int classId, int colorId)
    {
        ColorRecord color = getColorRecord(classId, colorId);
        return (color == null) ? false : color.starter;
    }

    /**
     * Returns a random starting color from the specified color class.
     */
    public ColorRecord getRandomStartingColor (String className)
    {
        return getRandomStartingColor(className, RandomUtil.rand);
    }
    /**
     * Returns a random starting color from the specified color class.
     */
    public ColorRecord getRandomStartingColor (String className, Random rand)
    {
        //  make sure the class exists
        ClassRecord record = getClassRecord(className);
        return (record == null) ? null : record.randomStartingColor(rand);
    }

    /**
     * Looks up a colorization by id.
     */
    public Colorization getColorization (int classId, int colorId)
    {
        ColorRecord color = getColorRecord(classId, colorId);
        return (color == null) ? null : color.getColorization();
    }

    /**
     * Looks up a colorization by color print.
     */
    public Colorization getColorization (int colorPrint)
    {
        return getColorization(colorPrint >> 8, colorPrint & 0xFF);
    }

    /**
     * Looks up a colorization by name.
     */
    public Colorization getColorization (String className, int colorId)
    {
        ClassRecord crec = getClassRecord(className);
        if (crec != null) {
            ColorRecord color = crec.colors.get(colorId);
            if (color != null) {
                return color.getColorization();
            }
        }
        return null;
    }

    /**
     * Looks up a colorization by class and color names.
     */
    public Colorization getColorization (String className, String colorName)
    {
        ClassRecord crec = getClassRecord(className);
        if (crec != null) {
            int colorId = 0;
            try {
                colorId = crec.getColorId(colorName);
            } catch (ParseException pe) {
                log.info("Error getting colorization by name", "error", pe);
                return null;
            }

            ColorRecord color = crec.colors.get(colorId);
            if (color != null) {
                return color.getColorization();
            }
        }
        return null;
    }

    /**
     * Loads up a colorization class by name and logs a warning if it doesn't exist.
     */
    public ClassRecord getClassRecord (String className)
    {
        Iterator<ClassRecord> iter = _classes.values().iterator();
        while (iter.hasNext()) {
            ClassRecord crec = iter.next();
            if (crec.name.equals(className)) {
                return crec;
            }
        }
        log.warning("No such color class", "class", className, new Exception());
        return null;
    }

    /**
     * Looks up the requested color record.
     */
    public ColorRecord getColorRecord (int classId, int colorId)
    {
        ClassRecord record = getClassRecord(classId);
        if (record == null) {
            // if they request color class zero, we assume they're just
            // decoding a blank colorprint, otherwise we complain
            if (classId != 0) {
                log.warning("Requested unknown color class",
                    "classId", classId, "colorId", colorId, new Exception());
            }
            return null;
        }
        return record.colors.get(colorId);
    }

    /**
     * Looks up the requested color record by class & color names.
     */
    public ColorRecord getColorRecord (String className, String colorName)
    {
        ClassRecord record = getClassRecord(className);
        if (record == null) {
            log.warning("Requested unknown color class",
                "className", className, "colorName", colorName, new Exception());
            return null;
        }

        int colorId = 0;
        try {
            colorId = record.getColorId(colorName);
        } catch (ParseException pe) {
            log.info("Error getting color record by name", "error", pe);
            return null;
        }

        return record.colors.get(colorId);
    }

    /**
     * Looks up the requested color class record.
     */
    public ClassRecord getClassRecord (int classId)
    {
        return _classes.get(classId);
    }

    /**
     * Adds a fully configured color class record to the pository. This is only called by the XML
     * parsing code, so pay it no mind.
     */
    public void addClass (ClassRecord record)
    {
        // validate the class id
        if (record.classId > 255) {
            log.warning("Refusing to add class; classId > 255 " + record + ".");
        } else {
            _classes.put(record.classId, record);
        }
    }

    /**
     * Loads up a serialized ColorPository from the supplied resource manager.
     */
    public static ColorPository loadColorPository (ResourceManager rmgr)
    {
        try {
            return loadColorPository(rmgr.getResource(CONFIG_PATH));
        } catch (IOException ioe) {
            log.warning("Failure loading color pository", "path", CONFIG_PATH, "error", ioe);
            return new ColorPository();
        }
    }

    /**
     * Loads up a serialized ColorPository from the supplied resource manager.
     */
    public static ColorPository loadColorPository (InputStream source)
    {
        try {
            return (ColorPository)CompiledConfig.loadConfig(source);
        } catch (IOException ioe) {
            log.warning("Failure loading color pository", "ioe", ioe);
            return new ColorPository();
        }
    }

    /**
     * Serializes and saves color pository to the supplied file.
     */
    public static void saveColorPository (ColorPository posit, File root)
    {
        File path = new File(root, CONFIG_PATH);
        try {
            CompiledConfig.saveConfig(path, posit);
        } catch (IOException ioe) {
            log.warning("Failure saving color pository", "path", path, "error", ioe);
        }
    }

    /** Our mapping from class names to class records. */
    protected HashIntMap<ClassRecord> _classes = new HashIntMap<ClassRecord>();

    /** Increase this value when object's serialized state is impacted by
     * a class change (modification of fields, inheritance). */
    private static final long serialVersionUID = 1;

    /**
     * The path (relative to the resource directory) at which the
     * serialized recolorization repository should be loaded and stored.
     */
    protected static final String CONFIG_PATH = "config/media/colordefs.dat";
}
