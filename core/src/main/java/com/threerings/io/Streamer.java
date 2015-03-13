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

package com.threerings.io;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import java.io.IOException;

import com.google.common.base.Defaults;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ByteEnum;
import com.samskivert.util.ByteEnumUtil;
import com.samskivert.util.ClassUtil;
import com.samskivert.util.QuickSort;

import static com.threerings.NaryaLog.log;

/**
 * Handles the streaming of {@link Streamable} instances as well as a set of basic object types
 * (see {@link ObjectOutputStream}). An instance of {@link Streamer} is created for each distinct
 * class that implements {@link Streamable}. The {@link Streamer} reflects on the streamed class
 * and caches the information necessary to efficiently read and write objects of the class in
 * question.
 */
public abstract class Streamer
{
    /**
     * Returns true if the supplied target class can be streamed using a streamer.
     */
    public synchronized static boolean isStreamable (Class<?> target)
    {
        // if we have not yet initialized ourselves, do so now
        maybeInit();

        // if we already have a streamer, or it's an enum, it's good
        if (_streamers.containsKey(target) || target.isEnum()) {
            return true;
        }

        // arrays are streamable, let's check the component type
        if (target.isArray()) {
            return isStreamable(target.getComponentType());
        }

        // otherwise it must be Streamable, or an Iterable or Map
        return Streamable.class.isAssignableFrom(target) ||
            Iterable.class.isAssignableFrom(target) ||
            Map.class.isAssignableFrom(target);
    }

    /**
     * Returns the class that should be used when streaming this object. In general that is the
     * object's natural class, but for enum values, that might be its declaring class as enums use
     * classes in a way that would otherwise pollute our id to class mapping space.
     */
    public static Class<?> getStreamerClass (Object object)
    {
        return (object instanceof Enum<?>) ?
            ((Enum<?>)object).getDeclaringClass() : object.getClass();
    }

    /**
     * If the specified class is not Streamable and is a Collection type, return the
     * most specific supported Collection interface type; otherwise return null.
     */
    public static Class<?> getCollectionClass (Class<?> clazz)
    {
        if (Streamable.class.isAssignableFrom(clazz)) {
            // the class is natively streamable, let's ignore it
            return null;
        }
        for (Class<?> collClass : BasicStreamers.CollectionStreamer.SPECIFICITY_ORDER) {
            if (collClass.isAssignableFrom(clazz)) {
                return collClass;
            }
        }
        return null;
    }

    /**
     * Obtains a {@link Streamer} that can be used to read and write objects of the specified
     * target class. {@link Streamer} instances are shared among all {@link ObjectInputStream}s and
     * {@link ObjectOutputStream}s.
     *
     * @param target the class that is desired to be streamed. This should be the result of a call
     * to {@link #getStreamerClass} if the caller has an instance they wish to stream.
     *
     * @throws IOException when a streamer is requested for an object that does not implement
     * {@link Streamable} and is not one of the basic object types (@see {@link
     * ObjectOutputStream}).
     */
    public synchronized static Streamer getStreamer (final Class<?> target)
        throws IOException
    {
        // if we have not yet initialized ourselves, do so now
        maybeInit();

        Streamer stream = _streamers.get(target);
        if (stream == null) {
            // Get or create a streamer for the class, and cache it.
            // First, see if it's a collection type...
            Class<?> collClass = getCollectionClass(target);
            if (collClass != null) {
                stream = getStreamer(collClass);

            // otherwise make sure it's a streamable class
            } else if (!isStreamable(target)) {
                throw new IOException(
                    "Requested to stream invalid class '" + target.getName() + "'");

            } else {
                // create a new streamer for the class
                if (ObjectInputStream.STREAM_DEBUG) {
                    log.info("Creating a streamer for '" + target.getName() + "'.");
                }

                // create our streamer in a privileged block so that it can introspect on the to be
                // streamed class
                try {
                    stream = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Streamer>() {
                            public Streamer run () throws IOException {
                                return create(target);
                            }
                        });
                } catch (PrivilegedActionException pae) {
                    throw (IOException) pae.getCause();
                }
            }

            // cache the streamer by the class type
            _streamers.put(target, stream);
        }
        return stream;
    }

    /**
     * Writes the supplied object to the specified stream.
     *
     * @param object the instance to be written to the stream.
     * @param out the stream to which to write the instance.
     * @param useWriter whether or not to use the custom <code>writeObject</code> if one exists.
     */
    public abstract void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
        throws IOException;

    /**
     * Creates a blank object that can subsequently be read by this streamer.  Data may be read
     * from the input stream as a result of this method (in the case of arrays, the length of the
     * array must be read before creating the array).
     */
    public abstract Object createObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException;

    /**
     * Reads and populates the fields of the supplied object from the specified stream.
     *
     * @param object the instance to be read from the stream.
     * @param in the stream from which to read the instance.
     * @param useReader whether or not to use the custom <code>readObject</code> if one exists.
     */
    public abstract void readObject (Object object, ObjectInputStream in, boolean useReader)
        throws IOException, ClassNotFoundException;

    @Override
    public final String toString ()
    {
        return toStringHelper().toString();
    }

    /**
     * Overrideable to add more information to this class' toString() representation.
     */
    protected Objects.ToStringHelper toStringHelper ()
    {
        // no extra details in the base class
        return Objects.toStringHelper(this);
    }

    /**
     * The constructor used by the basic streamers.
     */
    protected Streamer ()
    {
    }

    /**
     * Create the appropriate Streamer for a newly-seen class.
     */
    protected static Streamer create (Class<?> target)
        throws IOException
    {
        // validate that the class is really streamable
        boolean isInner = false, isStatic = Modifier.isStatic(target.getModifiers());
        try {
            isInner = (target.getDeclaringClass() != null);
        } catch (Throwable t) {
            log.warning("Failure checking innerness of class",
                "class", target.getName(), "error", t);
        }
        if (isInner && !isStatic) {
            throw new IllegalArgumentException(
                "Cannot stream non-static inner class: " + target.getName());
        }

        // create streamers for array types
        if (target.isArray()) {
            Class<?> componentType = target.getComponentType();
            if (Modifier.isFinal(componentType.getModifiers())) {
                Streamer delegate = Streamer.getStreamer(componentType);
                if (delegate != null) {
                    return new FinalArrayStreamer(componentType, delegate);
                } // else: error, below

            } else if (isStreamable(componentType)) {
                return new ArrayStreamer(componentType);
            }
            String errmsg = "Aiya! Streamer created for array type but we have no registered " +
                "streamer for the element type [type=" + target.getName() + "]";
            throw new RuntimeException(errmsg);
        }

        // create streamers for enum types
        if (target.isEnum()) {
            switch (ENUM_POLICY) {
            case NAME_WITH_BYTE_ENUM:
            case ORDINAL_WITH_BYTE_ENUM:
                if (ByteEnum.class.isAssignableFrom(target)) {
                    return new ByteEnumStreamer(target);
                }
                break;

            default:
                // we do not care if it is a ByteEnum, we move on...
                break;
            }

            switch (ENUM_POLICY) {
            case NAME_WITH_BYTE_ENUM:
            case NAME:
                return new NameEnumStreamer(target);

            default:
                List<?> universe = ImmutableList.copyOf(target.getEnumConstants());
                int maxOrdinal = universe.size() - 1;
                if (maxOrdinal <= Byte.MAX_VALUE) {
                    return new ByteOrdEnumStreamer(target, universe);

                } else if (maxOrdinal <= Short.MAX_VALUE) {
                    return new ShortOrdEnumStreamer(target, universe);

                } else {
                    return new IntOrdEnumStreamer(target, universe);
                }
            }
        }

        // create Streamers for other types
        Method reader = null;
        Method writer = null;
        try {
            reader = target.getMethod(READER_METHOD_NAME, READER_ARGS);
        } catch (NoSuchMethodException nsme) {
            // nothing to worry about, we just don't have one
        }
        try {
            writer = target.getMethod(WRITER_METHOD_NAME, WRITER_ARGS);
        } catch (NoSuchMethodException nsme) {
            // nothing to worry about, we just don't have one
        }

        // if there is no reader and no writer, we can do a simpler thing
        if ((reader == null) && (writer == null)) {
            return new ClassStreamer(target);
        } else {
            return new CustomClassStreamer(target, reader, writer);
        }
    }

    /**
     * A streamer that streams the fields of a class.
     */
    protected static class ClassStreamer extends Streamer
    {
        /** Constructor. */
        protected ClassStreamer (Class<?> target)
        {
            _target = target;
            initConstructor();
            _marshallers = createMarshallers();
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            int fcount = _fields.length;
            for (int ii = 0; ii < fcount; ii++) {
                Field field = _fields[ii];
                FieldMarshaller fm = _marshallers[ii];
                try {
                    if (ObjectInputStream.STREAM_DEBUG) {
                        log.info("Writing field",
                            "class", _target.getName(), "field", field.getName());
                    }
                    fm.writeField(field, object, out);
                } catch (Exception e) {
                    String errmsg = "Failure writing streamable field [class=" + _target.getName() +
                        ", field=" + field.getName() + "]";
                    throw (IOException) new IOException(errmsg).initCause(e);
                }
            }
        }

        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            try {
                if (ObjectInputStream.STREAM_DEBUG) {
                    log.info(in.hashCode() + ": Creating object '" + _target.getName() + "'.");
                }
                return _ctor.newInstance(_ctorArgs);

            } catch (InvocationTargetException ite) {
                String errmsg = "Error instantiating object [type=" + _target.getName() + "]";
                throw (IOException) new IOException(errmsg).initCause(ite.getCause());

            } catch (InstantiationException ie) {
                String errmsg = "Error instantiating object [type=" + _target.getName() + "]";
                throw (IOException) new IOException(errmsg).initCause(ie);

            } catch (IllegalAccessException iae) {
                String errmsg = "Error instantiating object [type=" + _target.getName() + "]";
                throw (IOException) new IOException(errmsg).initCause(iae);
            }
        }

        @Override
        public void readObject (Object object, ObjectInputStream in, boolean useReader)
            throws IOException, ClassNotFoundException
        {
            int fcount = _fields.length;
            for (int ii = 0; ii < fcount; ii++) {
                Field field = _fields[ii];
                FieldMarshaller fm = _marshallers[ii];
                try {
                    if (ObjectInputStream.STREAM_DEBUG) {
                        log.info(in.hashCode() + ": Reading field '" + field.getName() + "' " +
                                 "with " + fm + ".");
                    }
                    // gracefully deal with objects that have had new fields added to their class
                    // definition
                    if (in.available() > 0) {
                        fm.readField(field, object, in);
                    } else {
                        log.info("Streamed instance missing field (probably newly added)",
                                 "class", _target.getName(), "field", field.getName());
                    }
                } catch (Exception e) {
                    String errmsg = "Failure reading streamable field [class=" + _target.getName() +
                        ", field=" + field.getName() + ", error=" + e + "]";
                    throw (IOException) new IOException(errmsg).initCause(e);
                }
            }

            if (ObjectInputStream.STREAM_DEBUG) {
                log.info(in.hashCode() + ": Read object '" + object + "'.");
            }
        }

        /**
         * Locates the appropriate constructor for creating instances.
         */
        protected void initConstructor ()
        {
            // if we have a zero argument constructor, we have to use that one
            Constructor<?>[] ctors = _target.getDeclaredConstructors();
            for (Constructor<?> ctor : ctors) {
                if (ctor.getParameterTypes().length == 0) {
                    _ctor = ctor;
                    _ctorArgs = ArrayUtil.EMPTY_OBJECT;
                    return;
                }
            }

            // otherwise there should be a single non-zero-argument constructor, which we'll call
            // with zero-valued arguments at unstreaming time, which will then be overwritten by
            // readObject()
            if (ctors.length != 1) {
                throw new RuntimeException(
                    "Streamable closure classes must have either a zero-argument constructor " +
                    "or a single argument-taking constructor; multiple argument-taking " +
                    "constructors are not allowed [class=" + _target.getName() + "]");
            }
            _ctor = ctors[0];
            _ctor.setAccessible(true);

            // we pass bogus arguments to it (because unstreaming will overwrite our bogus
            // values with the real values)
            Class<?>[] ptypes = _ctor.getParameterTypes();
            _ctorArgs = new Object[ptypes.length];
            for (int ii = 0; ii < ptypes.length; ii++) {
                // this will be the appropriately typed zero, or null
                _ctorArgs[ii] = Defaults.defaultValue(ptypes[ii]);
            }
        }

        /**
         * Creates and returns the reading and writing marshallers.
         */
        protected FieldMarshaller[] createMarshallers ()
        {
            // reflect on all the object's fields
            List<Field> fields = Lists.newArrayList();
            // this will read all non-static, non-transient fields into our fields list
            ClassUtil.getFields(_target, fields);

            // Checks whether or not we should stream the fields in alphabetical order.
            // This ensures cross-JVM compatibility since Class.getDeclaredFields() does not
            // define an order. Due to legacy issues, this is not used by default.
            if (SORT_FIELDS) {
                QuickSort.sort(fields, FIELD_NAME_ORDER);
            }

            // remove all marked with NotStreamable, and if we're a streamable closure, remove any
            // anonymous enclosing class reference
            Predicate<Field> filter = Streamable.Closure.class.isAssignableFrom(_target) ?
                IS_STREAMCLOSURE : IS_STREAMABLE;
            _fields = Iterables.toArray(Iterables.filter(fields, filter), Field.class);
            int fcount = _fields.length;

            // obtain field marshallers for all of our fields
            FieldMarshaller[] marshallers = new FieldMarshaller[fcount];
            for (int ii = 0; ii < fcount; ii++) {
                marshallers[ii] = FieldMarshaller.getFieldMarshaller(_fields[ii]);
                if (marshallers[ii] == null) {
                    String errmsg = "Unable to marshall field [class=" + _target.getName() +
                        ", field=" + _fields[ii].getName() +
                        ", type=" + _fields[ii].getType().getName() + "]";
                    throw new RuntimeException(errmsg);
                }
                if (ObjectInputStream.STREAM_DEBUG) {
                    log.info("Using " + marshallers[ii] + " for " + _target.getName() + "." +
                             _fields[ii].getName() + ".");
                }
            }
            return marshallers;
        }

        @Override
        protected Objects.ToStringHelper toStringHelper ()
        {
            return super.toStringHelper()
                .add("target", _target.getName())
                .add("fcount", (_fields == null) ? 0 : _fields.length);
        }

        /** The class for which this streamer instance is configured. */
        protected Class<?> _target;

        /** The constructor we use to create instances. */
        protected Constructor<?> _ctor;

        /** The arguments we pass to said constructor (empty or all null/zero). */
        protected Object[] _ctorArgs;

        /** The non-transient, non-static public fields that we will stream when requested. */
        protected Field[] _fields;

        /** Field marshallers for each field that will be read or written in our objects. */
        protected FieldMarshaller[] _marshallers;
    } // end: static class ClassStreamer

    /**
     * Extends basic class streaming with support for customized streaming.
     */
    protected static class CustomClassStreamer extends ClassStreamer
    {
        /** Constructor. */
        protected CustomClassStreamer (Class<?> target, Method reader, Method writer)
        {
            super(target);
            _reader = reader;
            _writer = writer;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            // if we're supposed to and one exists, use the writer method
            if (useWriter && _writer != null) {
                try {
                    if (ObjectInputStream.STREAM_DEBUG) {
                        log.info("Writing with writer", "class", _target.getName());
                    }
                    _writer.invoke(object, new Object[] { out });

                } catch (Throwable t) {
                    if (t instanceof InvocationTargetException) {
                        t = ((InvocationTargetException)t).getTargetException();
                    }
                    if (t instanceof IOException) {
                        throw (IOException)t;
                    }
                    String errmsg = "Failure invoking streamable writer " +
                        "[class=" + _target.getName() + "]";
                    throw (IOException) new IOException(errmsg).initCause(t);
                }
                return;
            }

            // otherwise, ensure the marshallers are initialized and call super
            if (_marshallers == null) {
                // this can race with other threads, but the worst that can happen is that the work
                // done in createMarshallers() is duplicated
                _marshallers = super.createMarshallers();
            }
            super.writeObject(object, out, useWriter);
        }

        @Override
        public void readObject (Object object, ObjectInputStream in, boolean useReader)
            throws IOException, ClassNotFoundException
        {
            // if we're supposed to and one exists, use the reader method
            if (useReader && _reader != null) {
                try {
                    if (ObjectInputStream.STREAM_DEBUG) {
                        log.info(in.hashCode() + ": Reading with reader '" + _target.getName() +
                            "." + _reader.getName() + "()'.");
                    }
                    _reader.invoke(object, new Object[] { in });

                } catch (Throwable t) {
                    if (t instanceof InvocationTargetException) {
                        t = ((InvocationTargetException)t).getTargetException();
                    }
                    if (t instanceof IOException) {
                        throw (IOException)t;
                    }
                    String errmsg = "Failure invoking streamable reader " +
                        "[class=" + _target.getName() + "]";
                    throw (IOException) new IOException(errmsg).initCause(t);
                }
                return;
            }

            if (ObjectInputStream.STREAM_DEBUG) {
                log.info(in.hashCode() + ": Reading '" + _target.getName() + "'.");
            }

            // otherwise, ensure the marshallers are iniitalized and call super
            if (_marshallers == null) {
                // this can race with other threads, but the worst that can happen is that the work
                // done in createMarshallers() is duplicated
                _marshallers = super.createMarshallers();
            }
            super.readObject(object, in, useReader);
        }

        @Override
        protected FieldMarshaller[] createMarshallers ()
        {
            // we will lazy-initialize the marshallers only if needed, so we don't call super
            // (It's possible there is only a writer method, but the object is never read from
            // clients, so don't get cute and set up the marshallers at construct time if one of
            // the methods is null).
            return null;
        }

        @Override
        protected Objects.ToStringHelper toStringHelper ()
        {
            return super.toStringHelper()
                .add("reader", _reader)
                .add("writer", _writer);
        }

        /** A reference to the <code>readObject</code> method if one is defined by our target. */
        protected Method _reader;

        /** A reference to the <code>writeObject</code> method if one is defined by our target. */
        protected Method _writer;
    } // end: static class CustomClassStreamer

    /**
     * A streamer for array types.
     */
    protected static class ArrayStreamer extends Streamer
    {
        /** Constructor. */
        protected ArrayStreamer (Class<?> componentType)
        {
            _componentType = componentType;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            int length = Array.getLength(object);
            out.writeInt(length);
            // write each array element with its own class identifier
            // because it could be any derived class of the array element type
            for (int ii = 0; ii < length; ii++) {
                out.writeObject(Array.get(object, ii));
            }
        }

        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            int length = in.readInt();
            if (ObjectInputStream.STREAM_DEBUG) {
                log.info(in.hashCode() + ": Creating array '" +
                    _componentType.getName() + "[" + length + "]'.");
            }
            return Array.newInstance(_componentType, length);
        }

        @Override
        public void readObject (Object object, ObjectInputStream in, boolean useReader)
            throws IOException, ClassNotFoundException
        {
            int length = Array.getLength(object);
            for (int ii = 0; ii < length; ii++) {
                if (ObjectInputStream.STREAM_DEBUG) {
                    log.info(in.hashCode() + ": Reading free element '" + ii + "'.");
                }
                Array.set(object, ii, in.readObject());
            }
        }

        @Override
        protected Objects.ToStringHelper toStringHelper ()
        {
            return super.toStringHelper()
                .add("componentType", _componentType.getName());
        }

        /** The class of our component type. */
        protected Class<?> _componentType;
    } // end: static class ArrayStreamer

    /**
     * A streamer for arrays with a final component type.
     */
    protected static class FinalArrayStreamer extends ArrayStreamer
    {
        /** Constructor. */
        protected FinalArrayStreamer (Class<?> componentType, Streamer delegate)
        {
            super(componentType);
            _delegate = delegate;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            int length = Array.getLength(object);
            out.writeInt(length);
            // The component class is final, we can be sure that all instances in the array will
            // be of the same class and thus can serialize things more efficiently.
            // Compute a mask indicating which elements are null and which are populated
            ArrayMask mask = new ArrayMask(length);
            for (int ii = 0; ii < length; ii++) {
                if (Array.get(object, ii) != null) {
                    mask.set(ii);
                }
            }
            // write that mask out to the stream
            mask.writeTo(out);

            // now write out the populated elements
            for (int ii = 0; ii < length; ii++) {
                Object element = Array.get(object, ii);
                if (element != null) {
                    out.writeBareObject(element, _delegate, useWriter);
                }
            }
        }

        @Override
        public void readObject (Object object, ObjectInputStream in, boolean useReader)
            throws IOException, ClassNotFoundException
        {
            int length = Array.getLength(object);
            // The component class is final, we can be sure that all instances in the array will
            // be of the same class and thus have serialized things more efficiently
            // Read in the nullness mask.
            ArrayMask mask = new ArrayMask();
            mask.readFrom(in);
            // now read in the array elements given that we know which elements to read
            for (int ii = 0; ii < length; ii++) {
                if (mask.isSet(ii)) {
                    if (ObjectInputStream.STREAM_DEBUG) {
                        log.info(in.hashCode() + ": Reading fixed element '" + ii + "'.");
                    }
                    Object element = _delegate.createObject(in);
                    in.readBareObject(element, _delegate, useReader);
                    Array.set(object, ii, element);
                } else if (ObjectInputStream.STREAM_DEBUG) {
                    log.info(in.hashCode() + ": Skipping null element '" + ii + "'.");
                }
            }
        }

        @Override
        protected Objects.ToStringHelper toStringHelper ()
        {
            return super.toStringHelper()
                .add("delegate", _delegate);
        }

        /** Our delegate streamer. */
        protected Streamer _delegate;
    } // end: static class FinalArrayStreamer

    /**
     * Base class for Enum streamers.
     */
    protected static abstract class EnumStreamer extends Streamer
    {
        /** Constructor. */
        protected EnumStreamer (Class<?> target)
        {
            @SuppressWarnings("unchecked")
            Class<EnumReader> eclass = (Class<EnumReader>)target;
            _eclass = eclass;
        }

        @Override
        public void readObject (Object object, ObjectInputStream in, boolean useReader)
            throws IOException, ClassNotFoundException
        {
            // nothing here: handled in createObject
        }

        @Override
        public Objects.ToStringHelper toStringHelper ()
        {
            return super.toStringHelper()
                .add("eclass", _eclass.getName());
        }

        /** Used to coerce the type system into quietude when reading enums from the wire. */
        protected static enum EnumReader implements ByteEnum {
            NOT_USED;
            public byte toByte () { return 0; }
        }

        /** Our enum class, not actually an EnumReader. */
        protected Class<EnumReader> _eclass;
    } // end: static abstract class EnumStreamer

    /**
     * Streams ByteEnums, if that's what's desired.
     */
    protected static class ByteEnumStreamer extends EnumStreamer
    {
        /** Constructor. */
        protected ByteEnumStreamer (Class<?> target)
        {
            super(target);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeByte(((ByteEnum) object).toByte());
        }

        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            return ByteEnumUtil.fromByte(_eclass, in.readByte());
        }
    } // end: static class ByteEnumStreamer

    /**
     * Streams enums by name.
     */
    protected static class NameEnumStreamer extends EnumStreamer
    {
        /** Constructor. */
        protected NameEnumStreamer (Class<?> target)
        {
            super(target);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeUTF(((Enum<?>)object).name());
        }

        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            return Enum.valueOf(_eclass, in.readUTF());
        }
    } // end: static class NameEnumStreamer

    /**
     * Base class for enum streamers that stream by ordinal.
     */
    protected static abstract class OrdEnumStreamer extends EnumStreamer
    {
        /** Constructor. */
        protected OrdEnumStreamer (Class<?> target, List<?> universe)
        {
            super(target);
            _universe = universe;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            int code = (object == null) ? -1 : ((Enum<?>)object).ordinal();
            writeCode(out, code);
        }

        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            int code = readCode(in);
            return (code == -1) ? null : _universe.get(code);
        }

        /** Write the ordinal code. */
        protected abstract void writeCode (ObjectOutputStream out, int code)
            throws IOException;

        /** Read the ordinal code. */
        protected abstract int readCode (ObjectInputStream in)
            throws IOException;

        /** The universe of this enum. */
        protected List<?> _universe;
    } // end: static abstract class OrdEnumStreamer

    /**
     * Streams enums by the byte value of their ordinal.
     */
    protected static class ByteOrdEnumStreamer extends OrdEnumStreamer
    {
        /** Constructor. */
        protected ByteOrdEnumStreamer (Class<?> target, List<?> universe)
        {
            super(target, universe);
        }

        @Override
        protected void writeCode (ObjectOutputStream out, int code)
            throws IOException
        {
            out.writeByte((byte)code);
        }

        @Override
        protected int readCode (ObjectInputStream in)
            throws IOException
        {
            return in.readByte();
        }
    } // end: static class ByteOrdEnumStreamer

    /**
     * Streams enums by the short value of their ordinal.
     */
    protected static class ShortOrdEnumStreamer extends OrdEnumStreamer
    {
        /** Constructor. */
        protected ShortOrdEnumStreamer (Class<?> target, List<?> universe)
        {
            super(target, universe);
        }

        @Override
        protected void writeCode (ObjectOutputStream out, int code)
            throws IOException
        {
            out.writeShort((short)code);
        }

        @Override
        protected int readCode (ObjectInputStream in)
            throws IOException
        {
            return in.readShort();
        }
    } // end: static class ShortOrdEnumStreamer

    /**
     * Streams enums by the int value of their ordinal.
     */
    protected static class IntOrdEnumStreamer extends OrdEnumStreamer
    {
        /** Constructor. */
        protected IntOrdEnumStreamer (Class<?> target, List<?> universe)
        {
            super(target, universe);
        }

        @Override
        protected void writeCode (ObjectOutputStream out, int code)
            throws IOException
        {
            out.writeInt(code);
        }

        @Override
        protected int readCode (ObjectInputStream in)
            throws IOException
        {
            return in.readInt();
        }
    } // end: static class IntOrdEnumStreamer

    /**
     * Initializes static state if necessary.
     */
    protected synchronized static void maybeInit ()
    {
        if (_streamers == null) {
            _streamers = Maps.newHashMap(BasicStreamers.BSTREAMERS);
        }
    }

    /** Contains the mapping from class names to configured streamer instances. */
    protected static Map<Class<?>, Streamer> _streamers;

    /** Should we sort fields in streamable classes? */
    protected static final boolean SORT_FIELDS =
        Boolean.getBoolean("com.threerings.io.streamFieldsAlphabetically");

    /** Our policy on handling enum classes. */
    protected static final EnumPolicy ENUM_POLICY = EnumPolicy.create();

    /** Compares fields by name. */
    protected static final Comparator<Field> FIELD_NAME_ORDER = new Comparator<Field>() {
        public int compare (Field arg0, Field arg1)
        {
            return arg0.getName().compareTo(arg1.getName());
        }
    };

    /**
     * The enum policy of this streamer, determined at start time by examining
     * a system property.
     */
    protected enum EnumPolicy
    {
        /** Stream enums using the name of the enum constant. */
        NAME,

        /** Use bytes if the enum is a ByteEnum, otherwise use the name. This is the OLD DEFAULT. */
        NAME_WITH_BYTE_ENUM,

        /** Stream using the ordinal: a byte, short, or int; depending on the size of the enum.
         *  I would like to change this to be the default. */
        ORDINAL,

        /** Use bytes if the enum is a ByteEnum, otherwise use the ordinal. */
        ORDINAL_WITH_BYTE_ENUM;

        /**
         * Create the static enum policy by checking the com.threerings.io.enumPolicy system prop.
         */
        public static EnumPolicy create ()
        {
            String policy = System.getProperty("com.threerings.io.enumPolicy");
            try {
                return valueOf(policy);
            } catch (Exception e) {
                return NAME_WITH_BYTE_ENUM;
            }
        }
    }

    /** The name of the custom reader method. */
    protected static final String READER_METHOD_NAME = "readObject";

    /** The argument list for the custom reader method. */
    protected static final Class<?>[] READER_ARGS = { ObjectInputStream.class };

    /** The name of the custom writer method. */
    protected static final String WRITER_METHOD_NAME = "writeObject";

    /** The argument list for the custom writer method. */
    protected static final Class<?>[] WRITER_ARGS = { ObjectOutputStream.class };

    /** Filters "NotStreamable" members from a field list. */
    protected static final Predicate<Field> IS_STREAMABLE = new Predicate<Field>() {
        public boolean apply (Field obj) {
            return (obj.getAnnotation(NotStreamable.class) == null);
        }
    };

    /** Filters "NotStreamable" members and enclosing class refs from a field list. */
    protected static final Predicate<Field> IS_STREAMCLOSURE = new Predicate<Field>() {
        public boolean apply (Field obj) {
            return IS_STREAMABLE.apply(obj) &&
                !(obj.isSynthetic() && obj.getName().startsWith("this$"));
        }
    };
}
