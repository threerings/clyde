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

package com.threerings.resource;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.StreamUtil;
import com.samskivert.net.PathUtil;
import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;
import com.samskivert.util.WeakObserverList;

import static com.threerings.resource.Log.log;

/**
 * The resource manager is responsible for maintaining a repository of resources that are
 * synchronized with a remote source. This is accomplished in the form of sets of jar files
 * (resource bundles) that contain resources and that are updated from a remote resource repository
 * via HTTP.  These resource bundles are organized into resource sets. A resource set contains one
 * or more resource bundles and is defined much like a classpath.
 *
 * <p> The resource manager can load resources from the default resource set, and can make
 * available named resource sets to entities that wish to do their own resource loading. If the
 * resource manager fails to locate a resource in the default resource set, it falls back to
 * loading the resource via the classloader (which will search the classpath).
 *
 * <p> Applications that wish to make use of resource sets and their associated bundles must call
 * {@link #initBundles} after constructing the resource manager, providing the path to a resource
 * definition file which describes these resource sets. The definition file will be loaded and the
 * resource bundles defined within will be loaded relative to the resource directory.  The bundles
 * will be cached in the user's home directory and only reloaded when the source resources have
 * been updated. The resource definition file looks something like the following:
 *
 * <pre>
 * resource.set.default = sets/misc/config.jar: \
 *                        sets/misc/icons.jar
 * resource.set.tiles = sets/tiles/ground.jar: \
 *                      sets/tiles/objects.jar: \
 *                      /global/resources/tiles/ground.jar: \
 *                      /global/resources/tiles/objects.jar
 * resource.set.sounds = sets/sounds/sfx.jar: \
 *                       sets/sounds/music.jar: \
 *                       /global/resources/sounds/sfx.jar: \
 *                       /global/resources/sounds/music.jar
 * </pre>
 *
 * <p> All resource set definitions are prefixed with <code>resource.set.</code> and all text
 * following that string is considered to be the name of the resource set. The resource set named
 * <code>default</code> is the default resource set and is the one that is searched for resources
 * is a call to {@link #getResource(String)}.
 *
 * <p> When a resource is loaded from a resource set, the set is searched in the order that entries
 * are specified in the definition.
 */
public class ResourceManager
{
    /**
     * Provides facilities for notifying an observer of the resource unpacking process.
     */
    public interface InitObserver
    {
        /**
         * Indicates a percent completion along with an estimated time remaining in seconds.
         */
        public void progress (int percent, long remaining);

        /**
         * Indicates that there was a failure unpacking our resource bundles.
         */
        public void initializationFailed (Exception e);
    }

    /**
     * An adapter that wraps an {@link InitObserver} and routes all method invocations to the AWT
     * thread.
     */
    public static class AWTInitObserver implements InitObserver
    {
        public AWTInitObserver (InitObserver obs) {
            _obs = obs;
        }

        public void progress (final int percent, final long remaining) {
            EventQueue.invokeLater(new Runnable() {
                public void run () {
                    _obs.progress(percent, remaining);
                }
            });
        }

        public void initializationFailed (final Exception e) {
            EventQueue.invokeLater(new Runnable() {
                public void run () {
                    _obs.initializationFailed(e);
                }
            });
        }

        protected InitObserver _obs;
    }

    /**
     * Notifies observers of modifications to resources (as indicated by a change to their
     * {@link File#lastModified} property).
     */
    public interface ModificationObserver
    {
        /**
         * Notes that a resource has been modified.
         *
         * @param path the path of the resource.
         * @param lastModified the resource's new timestamp.
         */
        public void resourceModified (String path, long lastModified);
    }

    /**
     * Transforms a regular resource path into a locale-specific path. The returned path
     * does not need to represent a valid resource. The ResourceManager will attempt to use
     * the locale-specific path, but if it fails will fall back to the generic path.
     */
    public interface LocaleHandler
    {
        /**
         * Return a locale-specific path, or null if the specified path cannot or need not
         * be transformed.
         */
        String getLocalePath (String path);
    }

    /**
     * Constructs a resource manager which will load resources via the classloader, prepending
     * <code>resourceRoot</code> to their path.
     *
     * @param resourceRoot the path to prepend to resource paths prior to attempting to load them
     * via the classloader. When resources are bundled into the default resource bundle, they don't
     * need this prefix, but if they're to be loaded from the classpath, it's likely that they'll
     * live in some sort of <code>resources</code> directory to isolate them from the rest of the
     * files in the classpath. This is not a platform dependent path (forward slash is always used
     * to separate path elements).
     */
    public ResourceManager (String resourceRoot)
    {
        this(resourceRoot, ResourceManager.class.getClassLoader());
    }

    /**
     * Creates a resource manager with the specified class loader via which to load classes. See
     * {@link #ResourceManager(String)} for further documentation.
     */
    public ResourceManager (String resourceRoot, ClassLoader loader)
    {
        this(resourceRoot, null, loader);
    }

    /**
     * Creates a resource manager with a root path to resources over the network. See
     * {@link #ResourceManager(String)} for further documentation.
     */
    public ResourceManager (String resourceRoot, String networkResourceRoot)
    {
        this(resourceRoot, networkResourceRoot, ResourceManager.class.getClassLoader());
    }

    /**
     * Creates a resource manager with a root path to resources over the network and the specified
     * class loader via which to load classes. See {@link #ResourceManager(String)} for further
     * documentation.
     */
    public ResourceManager (String fileResourceRoot, String networkResourceRoot, ClassLoader loader)
    {
        _rootPath = fileResourceRoot;
        _networkRootPath = networkResourceRoot;
        _loader = loader;

        // check a system property to determine if we should unpack our bundles, but don't freak
        // out if we fail to read it
        try {
            _unpack = !Boolean.getBoolean("no_unpack_resources");
        } catch (SecurityException se) {
            // no problem, we're in a sandbox so we definitely won't be unpacking
        }

        // get our resource directory from resource_dir if possible
        initResourceDir(null);
    }

    /**
     * Registers a protocol handler with URL to handle <code>resource:</code> URLs. The URLs take
     * the form: <pre>resource://bundle_name/resource_path</pre> Resources from the default bundle
     * can be loaded via: <pre>resource:///resource_path</pre>
     */
    public void activateResourceProtocol ()
    {
        // set up a URL handler so that things can be loaded via urls with the 'resource' protocol
        try {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run () {
                    Handler.registerHandler(ResourceManager.this);
                    return null;
                }
            });
        } catch (SecurityException se) {
            log.info("Running in sandbox. Unable to bind rsrc:// handler.");
        }
    }

    /**
     * Configure a default LocaleHandler with the specified prefix.
     */
    public void setLocalePrefix (final String prefix)
    {
        setLocaleHandler(
            new LocaleHandler() {
                public String getLocalePath (String path) {
                    return PathUtil.appendPath(prefix, path);
                }
            });
    }

    /**
     * Configure a custom LocaleHandler.
     */
    public void setLocaleHandler (LocaleHandler localeHandler)
    {
        _localeHandler = localeHandler;
    }

    /**
     * Configures whether we unpack our resource bundles or not. This must be called before {@link
     * #initBundles}. One can also pass the <code>-Dno_unpack_resources=true</code> system property
     * to disable resource unpacking.
     */
    public void setUnpackResources (boolean unpackResources)
    {
        _unpack = unpackResources;
    }

    /**
     * Initializes the bundle sets to be made available by this resource manager.  Applications
     * that wish to make use of resource bundles should call this method after constructing the
     * resource manager.
     *
     * @param resourceDir the base directory to which the paths in the supplied configuration file
     * are relative. If this is null, the system property <code>resource_dir</code> will be used,
     * if available.
     * @param configPath the path (relative to the resource dir) of the resource definition file.
     * @param initObs a bundle initialization observer to notify of unpacking progress and success
     * or failure, or <code>null</code> if the caller doesn't care to be informed; note that in the
     * latter case, the calling thread will block until bundle unpacking is complete.
     *
     * @exception IOException thrown if we are unable to read our resource manager configuration.
     */
    public void initBundles (String resourceDir, String configPath, InitObserver initObs)
        throws IOException
    {
        // reinitialize our resource dir if it was specified
        if (resourceDir != null) {
            initResourceDir(resourceDir);
        }

        // load up our configuration
        Properties config = loadConfig(configPath);

        // resolve the configured resource sets
        List<ResourceBundle> dlist = Lists.newArrayList();
        Enumeration<?> names = config.propertyNames();
        while (names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (!key.startsWith(RESOURCE_SET_PREFIX)) {
                continue;
            }
            String setName = key.substring(RESOURCE_SET_PREFIX.length());
            String resourceSetType = config.getProperty(RESOURCE_SET_TYPE_PREFIX + setName,
                FILE_SET_TYPE);
            resolveResourceSet(setName, config.getProperty(key), resourceSetType, dlist);
        }

        // if an observer was passed in, then we do not need to block the caller
        final boolean[] shouldWait = new boolean[] { false };
        if (initObs == null) {
            // if there's no observer, we'll need to block the caller
            shouldWait[0] = true;
            initObs = new InitObserver() {
                public void progress (int percent, long remaining) {
                    if (percent >= 100) {
                        synchronized (this) {
                            // turn off shouldWait, in case we reached 100% progress before the
                            // calling thread even gets a chance to get to the blocking code, below
                            shouldWait[0] = false;
                            notify();
                        }
                    }
                }
                public void initializationFailed (Exception e) {
                    synchronized (this) {
                        shouldWait[0] = false;
                        notify();
                    }
                }
            };
        }

        // start a thread to unpack our bundles
        Unpacker unpack = new Unpacker(dlist, initObs);
        unpack.start();

        if (shouldWait[0]) {
            synchronized (initObs) {
                if (shouldWait[0]) {
                    try {
                        initObs.wait();
                    } catch (InterruptedException ie) {
                        log.warning("Interrupted while waiting for bundles to unpack.");
                    }
                }
            }
        }
    }

    /**
     * (Re)initializes the directory to search for resource files.
     *
     * @param resourceDir the directory path, or <code>null</code> to set the resource dir to
     * the value of the <code>resource_dir</code> system property.
     */
    public void initResourceDir (String resourceDir)
    {
        // if none was specified, check the resource_dir system property
        if (resourceDir == null) {
            try {
                resourceDir = System.getProperty("resource_dir");
            } catch (SecurityException se) {
                // no problem
            }
        }

        // if we found no resource directory, don't use one
        if (resourceDir == null) {
            return;
        }

        // make sure there's a trailing slash
        if (!resourceDir.endsWith(File.separator)) {
            resourceDir += File.separator;
        }
        _rdir = new File(resourceDir);
    }

    /**
     * Given a path relative to the resource directory, the path is properly jimmied (assuming we
     * always use /) and combined with the resource directory to yield a {@link File} object that
     * can be used to access the resource.
     *
     * @return a file referencing the specified resource or null if the resource manager was never
     * configured with a resource directory.
     */
    public File getResourceFile (String path)
    {
        if (_rdir == null) {
            return null;
        }
        if ('/' != File.separatorChar) {
            path = path.replace('/', File.separatorChar);
        }
        // first try a locale-specific file
        String localePath = getLocalePath(path);
        if (localePath != null) {
            File file = new File(_rdir, localePath);
            if (file.exists()) {
                return file;
            }
        }
        return new File(_rdir, path);
    }

    /**
     * Given a file within the resource directory, returns a resource path that can be passed to
     * {@link #getResourceFile} to locate the resource.
     *
     * @return a path referencing the specified resource or null if either the resource manager
     * was never configured with a resource directory or the file is not contained within the
     * resource directory.
     */
    public String getResourcePath (File file)
    {
        if (_rdir == null) {
            return null;
        }
        try {
            String parent = _rdir.getCanonicalPath();
            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String child = file.getCanonicalPath();
            if (!child.startsWith(parent)) {
                return null;
            }
            String path = child.substring(parent.length());
            return (File.separatorChar == '/') ? path : path.replace(File.separatorChar, '/');

        } catch (IOException e) {
            log.warning("Failed to determine resource path", "file", file, e);
            return null;
        }
    }

    /**
     * Checks to see if the specified bundle exists, is unpacked and is ready to be used.
     */
    public boolean checkBundle (String path)
    {
        File bfile = getResourceFile(path);
        return (bfile == null) ? false : new FileResourceBundle(bfile, true, _unpack).isUnpacked();
    }

    /**
     * Resolve the specified bundle (the bundle file must already exist in the appropriate place on
     * the file system) and return it on the specified result listener. Note that the result
     * listener may be notified before this method returns on the caller's thread if the bundle is
     * already resolved, or it may be notified on a brand new thread if the bundle requires
     * unpacking.
     */
    public void resolveBundle (String path, final ResultListener<FileResourceBundle> listener)
    {
        File bfile = getResourceFile(path);
        if (bfile == null) {
            String errmsg = "ResourceManager not configured with resource directory.";
            listener.requestFailed(new IOException(errmsg));
            return;
        }

        final FileResourceBundle bundle = new FileResourceBundle(bfile, true, _unpack);
        if (bundle.isUnpacked()) {
            if (bundle.sourceIsReady()) {
                listener.requestCompleted(bundle);
            } else {
                String errmsg = "Bundle initialization failed.";
                listener.requestFailed(new IOException(errmsg));
            }
            return;
        }

        // start a thread to unpack our bundles
        ArrayList<ResourceBundle> list = Lists.newArrayList();
        list.add(bundle);
        Unpacker unpack = new Unpacker(list, new InitObserver() {
            public void progress (int percent, long remaining) {
                if (percent == 100) {
                    listener.requestCompleted(bundle);
                }
            }
            public void initializationFailed (Exception e) {
                listener.requestFailed(e);
            }
        });
        unpack.start();
    }

    /**
     * Returns the class loader being used to load resources if/when there are no resource bundles
     * from which to load them.
     */
    public ClassLoader getClassLoader ()
    {
        return _loader;
    }

    /**
     * Configures the class loader this manager should use to load resources if/when there are no
     * bundles from which to load them.
     */
    public void setClassLoader (ClassLoader loader)
    {
        _loader = loader;
    }

    /**
     * Fetches a resource from the local repository.
     *
     * @param path the path to the resource (ie. "config/miso.properties"). This should not begin
     * with a slash.
     *
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public InputStream getResource (String path)
        throws IOException
    {
        String localePath = getLocalePath(path);
        InputStream in;

        // first look for this resource in our default resource bundle
        for (ResourceBundle bundle : _default) {
            // Try a localized version first.
            if (localePath != null) {
                in = bundle.getResource(localePath);
                if (in != null) {
                    return in;
                }
            }
            // If that didn't work, try generic.
            in = bundle.getResource(path);
            if (in != null) {
                return in;
            }
        }

        // fallback next to an unpacked resource file
        File file = getResourceFile(path);
        if (file != null && file.exists()) {
            return new FileInputStream(file);
        }

        // if we still didn't find anything, try the classloader; first try a locale-specific file
        if (localePath != null) {
            in = getInputStreamFromClasspath(PathUtil.appendPath(_rootPath, localePath));
            if (in != null) {
                return in;
            }
        }

        // if we didn't find that, try locale-neutral
        in = getInputStreamFromClasspath(PathUtil.appendPath(_rootPath, path));
        if (in != null) {
            return in;
        }

        // if we still haven't found it, we throw an exception
        throw new FileNotFoundException("Unable to locate resource [path=" + path + "]");
    }

    /**
     * Fetches and decodes the specified resource into a {@link BufferedImage}.
     *
     * @exception FileNotFoundException thrown if the resource could not be located in any of the
     * bundles in the specified set, or if the specified set does not exist.
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public BufferedImage getImageResource (String path)
        throws IOException
    {
        String localePath = getLocalePath(path);

        // first look for this resource in our default resource bundle
        for (ResourceBundle bundle : _default) {
            // try a localized version first
            BufferedImage image;
            if (localePath != null) {
                image = bundle.getImageResource(localePath, false);
                if (image != null) {
                    return image;
                }
            }
            // if we didn't find that, try generic
            image = bundle.getImageResource(path, false);
            if (image != null) {
                return image;
            }
        }

        // fallback next to an unpacked resource file
        File file = getResourceFile(path);
        if (file != null && file.exists()) {
            return loadImage(file, path.endsWith(FastImageIO.FILE_SUFFIX));
        }

        // if we still didn't find anything, try the classloader
        InputStream in;
        if (localePath != null) {
            in = getInputStreamFromClasspath(PathUtil.appendPath(_rootPath, localePath));
            if (in != null) {
                return loadImage(in);
            }
        }
        in = getInputStreamFromClasspath(PathUtil.appendPath(_rootPath, path));
        if (in != null) {
            return loadImage(in);
        }

        // if we still haven't found it, we throw an exception
        throw new FileNotFoundException("Unable to locate image resource [path=" + path + "]");
    }

    /**
     * Returns an input stream from which the requested resource can be loaded. <em>Note:</em> this
     * performs a linear search of all of the bundles in the set and returns the first resource
     * found with the specified path, thus it is not extremely efficient and will behave
     * unexpectedly if you use the same paths in different resource bundles.
     *
     * @exception FileNotFoundException thrown if the resource could not be located in any of the
     * bundles in the specified set, or if the specified set does not exist.
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public InputStream getResource (String rset, String path)
        throws IOException
    {
        // grab the resource bundles in the specified resource set
        ResourceBundle[] bundles = getResourceSet(rset);
        if (bundles == null) {
            throw new FileNotFoundException(
                "Unable to locate resource [set=" + rset + ", path=" + path + "]");
        }

        String localePath = getLocalePath(path);
        // look for the resource in any of the bundles
        for (ResourceBundle bundle : bundles) {
            InputStream in;
            // Try a localized version first.
            if (localePath != null) {
                in = bundle.getResource(localePath);
                if (in != null) {
                    return in;
                }
            }
            // If we didn't find that, try a generic.
            in = bundle.getResource(path);
            if (in != null) {
                return in;
            }
        }

        throw new FileNotFoundException(
            "Unable to locate resource [set=" + rset + ", path=" + path + "]");
    }

    /**
     * Fetches and decodes the specified resource into a {@link BufferedImage}.
     *
     * @exception FileNotFoundException thrown if the resource could not be located in any of the
     * bundles in the specified set, or if the specified set does not exist.
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public BufferedImage getImageResource (String rset, String path)
        throws IOException
    {
        // grab the resource bundles in the specified resource set
        ResourceBundle[] bundles = getResourceSet(rset);
        if (bundles == null) {
            throw new FileNotFoundException(
                "Unable to locate image resource [set=" + rset + ", path=" + path + "]");
        }

        String localePath = getLocalePath(path);
        // look for the resource in any of the bundles
        for (ResourceBundle bundle : bundles) {
            BufferedImage image;
            // try a localized version first
            if (localePath != null) {
                image = bundle.getImageResource(localePath, false);
                if (image != null) {
                    return image;
                }
            }
            // if we didn't find that, try generic
            image = bundle.getImageResource(path, false);
            if (image != null) {
                return image;
            }
        }

        throw new FileNotFoundException(
            "Unable to locate image resource [set=" + rset + ", path=" + path + "]");
    }

    /**
     * Returns a reference to the resource set with the specified name, or null if no set exists
     * with that name. Services that wish to load their own resources can allow the resource
     * manager to load up a resource set for them, from which they can easily load their resources.
     */
    public ResourceBundle[] getResourceSet (String name)
    {
        return _sets.get(name);
    }

    /**
     * Adds a modification observer for the specified resource.  Note that only a weak reference to
     * the observer will be retained, and thus this will not prevent the observer from being
     * garbage-collected.
     */
    public void addModificationObserver (String path, ModificationObserver obs)
    {
        ObservedResource resource = _observed.get(path);
        if (resource == null) {
            File file = getResourceFile(path);
            if (file == null) {
                return; // only resource files will ever be modified
            }
            _observed.put(path, resource = new ObservedResource(file));
        }
        resource.observers.add(obs);
    }

    /**
     * Removes a modification observer from the list maintained for the specified resource.
     */
    public void removeModificationObserver (String path, ModificationObserver obs)
    {
        ObservedResource resource = _observed.get(path);
        if (resource != null) {
            resource.observers.remove(obs);
        }
    }

    /**
     * Checks all observed resources for changes to their {@link File#lastModified} properties,
     * notifying their listeners if the files have been modified since the last call to this
     * method.
     */
    public void checkForModifications ()
    {
        for (Iterator<Map.Entry<String, ObservedResource>> it = _observed.entrySet().iterator();
                it.hasNext(); ) {
            Map.Entry<String, ObservedResource> entry = it.next();
            ObservedResource resource = entry.getValue();
            if (resource.checkForModification(entry.getKey())) {
                it.remove();
            }
        }
    }

    /**
     * Loads the configuration properties for our resource sets.
     */
    protected Properties loadConfig (String configPath)
        throws IOException
    {
        Properties config = new Properties();
        try {
            config.load(new FileInputStream(new File(_rdir, configPath)));
        } catch (Exception e) {
            String errmsg = "Unable to load resource manager config [rdir=" + _rdir +
                ", cpath=" + configPath + "]";
            log.warning(errmsg + ".", e);
            throw new IOException(errmsg);
        }
        return config;
    }

    /**
     * If we have a full list of the resources available, we return it.  A return value of null
     *  means that we do not know what's available and we'll have to try all possibilities.  This
     *  is fine for most applications.
     */
    public HashSet<String> getResourceList ()
    {
        return null;
    }

    /**
     * Loads up a resource set based on the supplied definition information.
     */
    protected void resolveResourceSet (
        String setName, String definition, String setType, List<ResourceBundle> dlist)
    {
        List<ResourceBundle> set = Lists.newArrayList();
        StringTokenizer tok = new StringTokenizer(definition, ":");
        while (tok.hasMoreTokens()) {
            set.add(createResourceBundle(setType, tok.nextToken().trim(), dlist));
        }

        // convert our array list into an array and stick it in the table
        ResourceBundle[] setvec = set.toArray(new ResourceBundle[set.size()]);
        _sets.put(setName, setvec);

        // if this is our default resource bundle, keep a reference to it
        if (DEFAULT_RESOURCE_SET.equals(setName)) {
            _default = setvec;
        }
    }

    /**
     * Creates a ResourceBundle based on the supplied definition information.
     */
    protected ResourceBundle createResourceBundle (String setType, String path,
        List<ResourceBundle> dlist)
    {
        if (setType.equals(FILE_SET_TYPE)) {
            FileResourceBundle bundle =
                createFileResourceBundle(getResourceFile(path), true, _unpack);
            if (!bundle.isUnpacked() || !bundle.sourceIsReady()) {
                dlist.add(bundle);
            }
            return bundle;
        } else if (setType.equals(NETWORK_SET_TYPE)) {
            // ARMHACK
            assert false;
            return null;
            //return createNetworkResourceBundle(_networkRootPath, path, getResourceList());
        } else {
            throw new IllegalArgumentException("Unknown set type: " + setType);
        }
    }

    /**
     * Creates an appropriate bundle for fetching resources from files.
     */
    protected FileResourceBundle createFileResourceBundle (
        File source, boolean delay, boolean unpack)
    {
        return new FileResourceBundle(source, delay, unpack);
    }

    // ARMHACK
//    /**
//     * Creates an appropriate bundle for fetching resources from the network.
//     */
//    protected ResourceBundle createNetworkResourceBundle (
//        String root, String path, Set<String> rsrcList)
//    {
//        return new NetworkResourceBundle(root, path, rsrcList);
//    }

    /**
     * Returns an InputStream from this manager's classloader for the given path.
     */
    protected InputStream getInputStreamFromClasspath (final String fullyQualifiedPath)
    {
        return AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
            public InputStream run () {
                return _loader.getResourceAsStream(fullyQualifiedPath);
            }
        });
    }

    /**
     * Transform the path into a locale-specific one, or return null.
     */
    protected String getLocalePath (String path)
    {
        return (_localeHandler == null) ? null : _localeHandler.getLocalePath(path);
    }

    /**
     * Loads an image from the supplied file. Supports {@link FastImageIO} files and formats
     * supported by {@link ImageIO} and will load the appropriate one based on the useFastIO param.
     */
    protected static BufferedImage loadImage (File file, boolean useFastIO)
        throws IOException
    {
        if (file == null) {
            return null;
        } else if (useFastIO) {
            return FastImageIO.read(file);
        }
        return ImageIO.read(file);
    }

    /**
     * Loads an image from the given input stream. Supports formats supported by {@link ImageIO}
     * as well as {@link FastImageIO} based on the useFastIO param.
     */
    public static BufferedImage loadImage (InputStream iis, boolean useFastIO)
        throws IOException
    {
        if (iis == null) {
            return null;
        } else if (useFastIO) {
            return FastImageIO.read(iis);
        }
        return ImageIO.read(iis);
    }

    /**
     * Loads an image from the supplied input stream. Supports formats supported by {@link ImageIO}
     * but not {@link FastImageIO}.
     */
    protected static BufferedImage loadImage (InputStream iis)
        throws IOException
    {
        BufferedImage image;

        if (iis instanceof ImageInputStream) {
            image = ImageIO.read(iis);

        } else {
            // if we don't already have an image input stream, create a memory cache image input
            // stream to avoid causing freakout if we're used in a sandbox because ImageIO
            // otherwise use FileCacheImageInputStream which tries to create a temp file
            MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(iis);
            image = ImageIO.read(mciis);
            try {
                // this doesn't close the underlying stream
                mciis.close();
            } catch (IOException ioe) {
                // ImageInputStreamImpl.close() throws an IOException if it's already closed;
                // there's no way to find out if it's already closed or not, so we have to check
                // the exception message to determine if this is actually warning worthy
                if (!"closed".equals(ioe.getMessage())) {
                    log.warning("Failure closing image input '" + iis + "'.", ioe);
                }
            }
        }

        // finally close our input stream
        StreamUtil.close(iis);

        return image;
    }

    /**
     * Converts the java version string to a more comparable numeric version number.
     */
    protected static int getNumericJavaVersion (String verstr)
    {
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(_\\d+)?.*").matcher(verstr);
        if (!m.matches()) {
            // if we can't parse the java version we're in weird land and should probably just try
            // our luck with what we've got rather than try to download a new jvm
            log.warning("Unable to parse VM version, hoping for the best [version=" + verstr + "]");
            return 0;
        }

        int one = Integer.parseInt(m.group(1)); // will there ever be a two?
        int major = Integer.parseInt(m.group(2));
        int minor = Integer.parseInt(m.group(3));
        int patch = m.group(4) == null ? 0 : Integer.parseInt(m.group(4).substring(1));
        return patch + 100 * (minor + 100 * (major + 100 * one));
    }

    /** Used to unpack bundles on a separate thread. */
    protected static class Unpacker extends Thread
    {
        public Unpacker (List<ResourceBundle> bundles, InitObserver obs) {
            _bundles = bundles;
            _obs = obs;
            _startTime = System.currentTimeMillis();
        }

        @Override
        public void run () {
            try {
                // Tell the observer were starting
                if (_obs != null) {
                    _obs.progress(0, -1);
                }

                int count = 0;
                for (ResourceBundle bundle : _bundles) {
                    if (bundle instanceof FileResourceBundle &&
                        !((FileResourceBundle)bundle).sourceIsReady()) {
                        log.warning("Bundle failed to initialize " + bundle + ".");
                    }
                    if (_obs != null) {
                        int pct = count*100/_bundles.size();

                        long remaining = 0;
                        if (pct > 0) {
                            // We should potentially do something that better understands the fact
                            // that the first couple percent are wacky, but this should is likely
                            // good enough, and is certainly better than before when we always
                            // claimed we only needed one second to finish.
                            remaining = Math.round((100 - pct) *
                                ((System.currentTimeMillis() - _startTime) / 1000.0) / pct);
                        }

                        if (pct < 100) {
                            _obs.progress(pct, remaining);
                        }
                    }
                    count++;
                }
                if (_obs != null) {
                    _obs.progress(100, 0);
                }

            } catch (Exception e) {
                if (_obs != null) {
                    _obs.initializationFailed(e);
                }
            }
        }

        protected List<ResourceBundle> _bundles;
        protected InitObserver _obs;
        protected long _startTime;
    }

    /** Contains the state of an observed file resource. */
    protected static class ObservedResource
    {
        /** The observers listening for modifications to this resource. */
        public WeakObserverList<ModificationObserver> observers = WeakObserverList.newFastUnsafe();

        public ObservedResource (File file) {
            _file = file;
            _lastModified = file.lastModified();
        }

        /**
         * Checks for a modification to the observed resource, notifying the observers if
         * one is detected.
         *
         * @param path the path of the resource (to forward to observers).
         * @return <code>true</code> if the list of observers is empty and the resource should be
         * removed from the observed list, <code>false</code> if it should remain in the list.
         */
        public boolean checkForModification (String path) {
            long newLastModified = _file.lastModified();
            if (newLastModified > _lastModified) {
                _resourceModifiedOp.init(path, _lastModified = newLastModified);
                observers.apply(_resourceModifiedOp);
            } else {
                // remove any observers that have been garbage-collected
                observers.prune();
            }
            return observers.isEmpty();
        }

        protected File _file;
        protected long _lastModified;
    }

    /** An observer op that calls {@link ModificationObserver#resourceModified}. */
    protected static class ResourceModifiedOp
        implements ObserverList.ObserverOp<ModificationObserver>
    {
        public void init (String path, long lastModified) {
            _path = path;
            _lastModified = lastModified;
        }

        // documentation inherited from interface ObserverOp
        public boolean apply (ModificationObserver obs) {
            obs.resourceModified(_path, _lastModified);
            return true;
        }

        protected String _path;
        protected long _lastModified;
    }

    /** The classloader we use for classpath-based resource loading. */
    protected ClassLoader _loader;

    /** The directory that contains our resource bundles. */
    protected File _rdir;

    /** The prefix we prepend to resource paths before attempting to load them from the
     * classpath. */
    protected String _rootPath;

    /** The root path we give to network bundles for all resources they're interested in. */
    protected String _networkRootPath;

    /** Whether or not to unpack our resource bundles. */
    protected boolean _unpack;

    /** Our default resource set. */
    protected ResourceBundle[] _default = new ResourceBundle[0];

    /** A table of our resource sets. */
    protected HashMap<String, ResourceBundle[]> _sets = Maps.newHashMap();

    /** Converts a path to a locale-specific path. */
    protected LocaleHandler _localeHandler;

    /** Maps resource paths to observed file resources. */
    protected HashMap<String, ObservedResource> _observed = Maps.newHashMap();

    /** A reusable instance of {@link ResourceModifiedOp}. */
    protected static ResourceModifiedOp _resourceModifiedOp = new ResourceModifiedOp();

    /** The prefix of configuration entries that describe a resource set. */
    protected static final String RESOURCE_SET_PREFIX = "resource.set.";

    /** The prefix of configuration entries that describe a resource set. */
    protected static final String RESOURCE_SET_TYPE_PREFIX = "resource.set_type.";

    /** The name of the default resource set. */
    protected static final String DEFAULT_RESOURCE_SET = "default";

    /** Resource set type indicating the resources should be loaded from local files. */
    protected static final String FILE_SET_TYPE = "file";

    /** Resource set type indicating the resources should be loaded over the network. */
    protected static final String NETWORK_SET_TYPE = "network";
}
