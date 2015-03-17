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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.samskivert.util.Logger;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.TransportHint;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.net.Transport;

/**
 * A base Ant task for generating invocation service related marshalling and unmarshalling
 * classes.
 */
public abstract class InvocationTask extends GenTask
{
    /** Used to keep track of invocation service method listener arguments. */
    public class ListenerArgument
    {
        public Class<?> listener;

        public ListenerArgument (int index, Class<?> listener) {
            this.listener = listener;
            _index = index;
        }

        public String getMarshaller () {
            String name = GenUtil.simpleName(listener);
            // handle ye olde special case
            if (name.equals("InvocationService.InvocationListener")) {
                return "ListenerMarshaller";
            }
            name = name.replace("Service", "Marshaller");
            return name.replace("Listener", "Marshaller");
        }

        public String getActionScriptMarshaller () {
            // handle ye olde special case
            String name = listener.getName();
            if (name.endsWith("InvocationService$InvocationListener")) {
                return "InvocationMarshaller_ListenerMarshaller";
            } else {
                return getMarshaller().replace('.', '_');
            }
        }

        public int getIndex () {
            return _index+1;
        }

        protected int _index;
    }

    /**
     * Creates a new service method and adds its basic imports to a set.
     * @param method the method to create
     * @param imports will be filled with the types required by the method
     */
    public ServiceMethod createAndGatherImports (Method method, ImportSet imports)
    {
        ServiceMethod sm = new ServiceMethod(method);
        sm.gatherImports(imports);
        return sm;
    }

    /** Used to keep track of invocation service methods or listener methods. */
    public class ServiceMethod implements Comparable<ServiceMethod>
    {
        public Method method;
        public List<ListenerArgument> listenerArgs = Lists.newArrayList();

        /**
         * Creates a new service method.
         * @param method the method to inspect
         */
        public ServiceMethod (Method method) {
            this.method = method;

            // if this method has listener arguments, we need to add listener argument info for them
            Class<?>[] args = method.getParameterTypes();
            for (int ii = 0; ii < args.length; ii++) {
                Class<?> arg = args[ii];
                while (arg.isArray()) {
                    arg = arg.getComponentType();
                }

                if (_ilistener.isAssignableFrom(arg)) {
                    listenerArgs.add(new ListenerArgument(ii, arg));
                }
            }
        }

        public void gatherImports (ImportSet imports) {
            // we need to look through our arguments and note any needed imports in the supplied
            // table
            for (Type type : method.getGenericParameterTypes()) {
                addImportsForType(type, imports);
            }

            // import Transport if used
            if (!StringUtil.isBlank(getTransport())) {
                imports.add(Transport.class);
            }
        }

        public String getCode () {
            return StringUtil.unStudlyName(method.getName()).toUpperCase();
        }

        public String getSenderMethodName () {
            String mname = method.getName();
            if (mname.startsWith("received")) {
                return "send" + mname.substring("received".length());
            } else {
                return mname;
            }
        }

        public String typeParams () {
            List<String> params = Lists.newArrayList();
            for (Type type : method.getGenericParameterTypes()) {
                collectTypeParams(type, params);
            }
            // the trailing space in '> ' is needed
            return params.isEmpty() ? "" : StringUtil.toString(params, "<", "> ");
        }

        public String getArgList () {
            StringBuilder buf = new StringBuilder();
            Type[] ptypes = method.getGenericParameterTypes();
            for (int ii = 0; ii < ptypes.length; ii++) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                String simpleName = GenUtil.simpleName(ptypes[ii]);
                if (method.isVarArgs() && ii == ptypes.length - 1) {
                    // Switch [] with ... for varargs
                    buf.append(simpleName.substring(0, simpleName.length() - 2)).append("...");
                } else {
                    buf.append(simpleName);
                }
                buf.append(" arg").append(ii+1);
            }
            return buf.toString();
        }

        public String getASArgList () {
            StringBuilder buf = new StringBuilder();
            Class<?>[] args = method.getParameterTypes();
            for (int ii = 0; ii < args.length; ii++) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append("arg").append(ii+1).append(" :");
                buf.append(GenUtil.simpleASName(args[ii]));
            }
            return buf.toString();
        }

         public String getASInvokeArgList () {
            StringBuilder buf = new StringBuilder();
            Class<?>[] args = method.getParameterTypes();
            for (int ii = 0; ii < args.length; ii++) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append("arg").append(ii+1);
            }
            return buf.toString();
        }

        public String getWrappedArgList () {
            StringBuilder buf = new StringBuilder();
            Class<?>[] args = method.getParameterTypes();
            for (int ii = 0; ii < args.length; ii++) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append(boxArgument(args[ii], ii+1));
            }
            return buf.toString();
        }

        public void gatherASWrappedArgListImports (ImportSet set) {
            Class<?>[] args = method.getParameterTypes();
            for (int ii = 0; ii < args.length; ii++) {
                set.addAll(GenUtil.getASBoxImports(args[ii]));
            }
        }

        public String getASWrappedArgList () {
            StringBuilder buf = new StringBuilder();
            Class<?>[] args = method.getParameterTypes();
            for (int ii = 0; ii < args.length; ii++) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                String index = String.valueOf(ii+1);
                String arg;
                if (_ilistener.isAssignableFrom(args[ii])) {
                    arg = GenUtil.boxASArgument(args[ii],  "listener" + index);
                } else {
                    arg = GenUtil.boxASArgument(args[ii],  "arg" + index);
                }
                buf.append(arg);
            }
            return buf.toString();
        }

        public boolean hasArgs () {
            return method.getParameterTypes().length > 0;
        }

        public boolean hasParameterizedArgs () {
            return Iterables.any(
                Arrays.asList(method.getGenericParameterTypes()), new Predicate<Type>() {
                public boolean apply (Type type) {
                    // TODO: might eventually need to handle generic arrays and wildcard types
                    return (type instanceof ParameterizedType);
                }
            });
        }

        public String getUnwrappedArgListAsListeners () {
            return getUnwrappedArgList(true);
        }

        public String getUnwrappedArgList () {
            return getUnwrappedArgList(false);
        }

        public String getUnwrappedArgList (boolean listenerMode) {
            StringBuilder buf = new StringBuilder();
            Type[] ptypes = method.getGenericParameterTypes();
            for (int ii = 0; ii < ptypes.length; ii++) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append(unboxArgument(ptypes[ii], ii, listenerMode));
            }
            return buf.toString();
        }

        public String getASUnwrappedArgListAsListeners () {
            return getASUnwrappedArgList(true);
        }

        public String getASUnwrappedArgList () {
            return getASUnwrappedArgList(false);
        }

        public String getASUnwrappedArgList (boolean listenerMode) {
            StringBuilder buf = new StringBuilder();
            Class<?>[] args = method.getParameterTypes();
            for (int ii = 0; ii < args.length; ii++) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                String arg;
                int argidx = ii;
                if (listenerMode && _ilistener.isAssignableFrom(args[ii])) {
                    arg = "listener" + argidx;
                } else {
                    arg = GenUtil.unboxASArgument(args[ii], "args[" + argidx + "]");
                }
                buf.append(arg);
            }
            return buf.toString();
        }

        public String getTransport () {
            TransportHint hint = method.getAnnotation(TransportHint.class);
            if (hint == null) {
                // inherit hint from interface annotation
                hint = method.getDeclaringClass().getAnnotation(TransportHint.class);
            }
            if (hint == null) {
                return "";
            }
            return ", Transport.getInstance(Transport.Type." +
                hint.type().name() + ", " + hint.channel() + ")";
        }

        // from interface Comparator<ServiceMethod>
        public int compareTo (ServiceMethod other) {
            return getCode().compareTo(other.getCode());
        }

        @Override // from Object
        public boolean equals (Object other) {
            return (other instanceof ServiceMethod) && compareTo((ServiceMethod)other) == 0;
        }

        @Override // from Object
        public int hashCode () {
            return getCode().hashCode();
        }

        protected void addImportsForType (Type type, ImportSet imports) {
            if (type instanceof Class<?>) {
                imports.add((Class<?>)type);
            } else if (type instanceof ParameterizedType) {
                imports.add((Class<?>)((ParameterizedType)type).getRawType());
                for (Type param : ((ParameterizedType)type).getActualTypeArguments()) {
                    addImportsForType(param, imports);
                }
            } else if (type instanceof WildcardType) {
                for (Type upper : ((WildcardType)type).getUpperBounds()) {
                    addImportsForType(upper, imports);
                }
                for (Type lower : ((WildcardType)type).getLowerBounds()) {
                    addImportsForType(lower, imports);
                }
            } else if (type instanceof GenericArrayType) {
                addImportsForType(((GenericArrayType)type).getGenericComponentType(), imports);
            } else if (type instanceof TypeVariable) {
                // nothing needed
            } else {
                System.err.println(Logger.format(
                    "Unhandled Type in adding imports for a service", "type", type, "typeClass",
                    type.getClass()));
            }
        }

        protected void collectTypeParams (Type type, List<String> params) {
            if (type instanceof TypeVariable) {
                String tvar = ((TypeVariable<?>)type).getName();
                if (!params.contains(tvar)) params.add(tvar);
            } else if (type instanceof ParameterizedType) {
                for (Type pt : ((ParameterizedType)type).getActualTypeArguments()) {
                    collectTypeParams(pt, params);
                }
            } else if (type instanceof WildcardType) {
                for (Type lb : ((WildcardType)type).getLowerBounds()) {
                    collectTypeParams(lb, params);
                }
                for (Type ub : ((WildcardType)type).getUpperBounds()) {
                    collectTypeParams(ub, params);
                }
            } // else nada
        }

        protected String boxArgument (Class<?> clazz, int index) {
            if (_ilistener.isAssignableFrom(clazz)) {
                return GenUtil.boxArgument(clazz,  "listener" + index);
            } else {
                return GenUtil.boxArgument(clazz,  "arg" + index);
            }
        }

        protected String unboxArgument (Type type, int index, boolean listenerMode) {
            if (listenerMode && (type instanceof Class<?>) &&
                _ilistener.isAssignableFrom((Class<?>)type)) {
                return "listener" + index;
            } else {
                return GenUtil.unboxArgument(type, "args[" + index + "]");
            }
        }
    }

    @Override
    public void execute ()
    {
        // resolve the InvocationListener and Client classes using our classloader
        _ilistener = loadClass(InvocationListener.class.getName());
        _iclient = loadClass(Client.class.getName());

        super.execute();
    }

    protected static <T> void checkedAdd (List<T> list, T value)
    {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    protected static String replacePath (String source, String oldstr, String newstr)
    {
        // replace only the last occurrance of 'oldstr' with 'newstr'; if we have a path like:
        // /client/src/foo/bar/client/FooService.java we don't want to replace the first /client
        // just the one that immediately precedes /FooService.java

        // TODO: this is error prone because we could have a project that opted not to have
        // client/data/server subdirs and instead just had a path like:
        // /client/src/foo/bar/FooService.java which would be improperly mangled

        String oldPath = oldstr.replace('/', File.separatorChar);
        String newPath = newstr.replace('/', File.separatorChar);
        int oldIdx = source.lastIndexOf(oldPath);
        return (oldIdx == -1) ? source :
            (source.substring(0, oldIdx) + newPath + source.substring(oldIdx+oldPath.length()));
    }

    /** {@link InvocationListener} resolved with the proper classloader so that we can compare it
     * to loaded derived classes. */
    protected Class<?> _ilistener;

    /** {@link Client} resolved with the proper classloader so that we can compare it to loaded
     * derived classes. */
    protected Class<?> _iclient;
}
