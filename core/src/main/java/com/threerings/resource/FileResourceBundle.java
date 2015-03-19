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

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.awt.image.BufferedImage;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.FileUtil;
import com.samskivert.util.StringUtil;

import static com.threerings.resource.Log.log;

/**
 * A resource bundle provides access to the resources in a jar file.
 */
public class FileResourceBundle extends ResourceBundle
{
    /**
     * Constructs a resource bundle with the supplied jar file.
     *
     * @param source a file object that references our source jar file.
     */
    public FileResourceBundle (File source)
    {
        this(source, false, false);
    }

    /**
     * Constructs a resource bundle with the supplied jar file.
     *
     * @param source a file object that references our source jar file.
     * @param delay if true, the bundle will wait until someone calls {@link #sourceIsReady}
     * before allowing access to its resources.
     * @param unpack if true the bundle will unpack itself into a temporary directory
     */
    public FileResourceBundle (File source, boolean delay, boolean unpack)
    {
        _source = source;
        if (unpack) {
            String root = stripSuffix(source.getPath());
            _unpacked = new File(root + ".stamp");
            _cache = new File(root);
        }

        if (!delay) {
            sourceIsReady();
        }
    }

    @Override
    public String getIdent ()
    {
        return _source.getPath();
    }

    @Override
    public InputStream getResource (String path)
        throws IOException
    {
        // unpack our resources into a temp directory so that we can load
        // them quickly and the file system can cache them sensibly
        File rfile = getResourceFile(path);
        return (rfile == null) ? null : new FileInputStream(rfile);
    }

    @Override
    public BufferedImage getImageResource (String path, boolean useFastIO)
        throws IOException
    {
        return ResourceManager.loadImage(getResourceFile(path), useFastIO);
    }

    /**
     * Returns the {@link File} from which resources are fetched for this bundle.
     */
    public File getSource ()
    {
        return _source;
    }

    /**
     * @return true if the bundle is fully downloaded and successfully unpacked.
     */
    public boolean isUnpacked ()
    {
        return (_source.exists() && _unpacked != null &&
                _unpacked.lastModified() == _source.lastModified());
    }

    /**
     * Called by the resource manager once it has ensured that our resource jar file is up to date
     * and ready for reading.
     *
     * @return true if we successfully unpacked our resources, false if we encountered errors in
     * doing so.
     */
    public boolean sourceIsReady ()
    {
        // make a note of our source's last modification time
        _sourceLastMod = _source.lastModified();

        // if we are unpacking files, the time to do so is now
        if (_unpacked != null && _unpacked.lastModified() != _sourceLastMod) {
            try {
                resolveJarFile();
            } catch (IOException ioe) {
                log.warning("Failure resolving jar file", "source", _source, ioe);
                wipeBundle(true);
                return false;
            }

            log.info("Unpacking into " + _cache + "...");
            if (!_cache.exists()) {
                if (!_cache.mkdir()) {
                    log.warning("Failed to create bundle cache directory", "dir", _cache);
                    closeJar();
                    // we are hopelessly fucked
                    return false;
                }
            } else {
                FileUtil.recursiveClean(_cache);
            }

            // unpack the jar file (this will close the jar when it's done)
            if (!FileUtil.unpackJar(_jarSource, _cache)) {
                // if something went awry, delete everything in the hopes
                // that next time things will work
                wipeBundle(true);
                return false;
            }

            // if everything unpacked smoothly, create our unpack stamp
            try {
                _unpacked.createNewFile();
                if (!_unpacked.setLastModified(_sourceLastMod)) {
                    log.warning("Failed to set last mod on stamp file", "file", _unpacked);
                }
            } catch (IOException ioe) {
                log.warning("Failure creating stamp file", "file", _unpacked, ioe);
                // no need to stick a fork in things at this point
            }
        }

        return true;
    }

    /**
     * Clears out everything associated with this resource bundle in the hopes that we can
     * download it afresh and everything will work the next time around.
     */
    public void wipeBundle (boolean deleteJar)
    {
        // clear out our cache directory
        if (_cache != null) {
            FileUtil.recursiveClean(_cache);
        }

        // delete our unpack stamp file
        if (_unpacked != null) {
            _unpacked.delete();
        }

        // clear out any .jarv file that Getdown might be maintaining so
        // that we ensure that it is revalidated
        File vfile = new File(FileUtil.resuffix(_source, ".jar", ".jarv"));
        if (vfile.exists() && !vfile.delete()) {
            log.warning("Failed to delete vfile", "file", vfile);
        }

        // close and delete our source jar file
        if (deleteJar && _source != null) {
            closeJar();
            if (!_source.delete()) {
                log.warning("Failed to delete source",
                    "source", _source, "exists", _source.exists());
            }
        }
    }

    /**
     * Returns a file from which the specified resource can be loaded. This method will unpack the
     * resource into a temporary directory and return a reference to that file.
     *
     * @param path the path to the resource in this jar file.
     *
     * @return a file from which the resource can be loaded or null if no such resource exists.
     */
    public File getResourceFile (String path)
        throws IOException
    {
        if (resolveJarFile()) {
            return null;
        }

        // if we have been unpacked, return our unpacked file
        if (_cache != null) {
            File cfile = new File(_cache, path);
            if (cfile.exists()) {
                return cfile;
            } else {
                return null;
            }
        }

        // otherwise, we unpack resources as needed into a temp directory
        String tpath = StringUtil.md5hex(_source.getPath() + "%" + path);
        File tfile = new File(getCacheDir(), tpath);
        if (tfile.exists() && (tfile.lastModified() > _sourceLastMod)) {
            return tfile;
        }

        JarEntry entry = _jarSource.getJarEntry(path);
        if (entry == null) {
//             log.info("Couldn't locate path in jar", "path", path, "jar", _jarSource);
            return null;
        }

        // copy the resource into the temporary file
        BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(tfile));
        InputStream jin = _jarSource.getInputStream(entry);
        StreamUtil.copy(jin, fout);
        jin.close();
        fout.close();

        return tfile;
    }

    /**
     * Returns true if this resource bundle contains the resource with the specified path. This
     * avoids actually loading the resource, in the event that the caller only cares to know that
     * the resource exists.
     */
    public boolean containsResource (String path)
    {
        try {
            if (resolveJarFile()) {
                return false;
            }
            return (_jarSource.getJarEntry(path) != null);
        } catch (IOException ioe) {
            return false;
        }
    }

    @Override
    public String toString ()
    {
        try {
            resolveJarFile();
            return (_jarSource == null) ? "[file=" + _source + "]" :
                "[path=" + _jarSource.getName() + "]";

        } catch (IOException ioe) {
            return "[file=" + _source + ", ioe=" + ioe + "]";
        }
    }

    /**
     * Creates the internal jar file reference if we've not already got it; we do this lazily so
     * as to avoid any jar- or zip-file-related antics until and unless doing so is required, and
     * because the resource manager would like to be able to create bundles before the associated
     * files have been fully downloaded.
     *
     * @return true if the jar file could not yet be resolved because we haven't yet heard from
     * the resource manager that it is ready for us to access, false if all is cool.
     */
    protected boolean resolveJarFile ()
        throws IOException
    {
        // if we don't yet have our resource bundle's last mod time, we
        // have not yet been notified that it is ready
        if (_sourceLastMod == -1) {
            return true;
        }

        if (!_source.exists()) {
            throw new IOException("Missing jar file for resource bundle: " + _source + ".");
        }

        try {
            if (_jarSource == null) {
                _jarSource = new JarFile(_source);
            }
            return false;

        } catch (IOException ioe) {
            String msg = "Failed to resolve resource bundle jar file '" + _source + "'";
            log.warning(msg + ".", ioe);
            throw (IOException) new IOException(msg).initCause(ioe);
        }
    }

    /**
     * Closes our (possibly opened) jar file.
     */
    protected void closeJar ()
    {
        try {
            if (_jarSource != null) {
                _jarSource.close();
            }
        } catch (Exception ioe) {
            log.warning("Failed to close jar file", "path", _source, "error", ioe);
        }
    }

    /**
     * Returns the cache directory used for unpacked resources.
     */
    public static File getCacheDir ()
    {
        if (_tmpdir == null) {
            String tmpdir = System.getProperty("java.io.tmpdir");
            if (tmpdir == null) {
                log.info("No system defined temp directory. Faking it.");
                tmpdir = System.getProperty("user.home");
            }
            setCacheDir(new File(tmpdir));
        }
        return _tmpdir;
    }

    /**
     * Specifies the directory in which our temporary resource files should be stored.
     */
    public static void setCacheDir (File tmpdir)
    {
        String rando = Long.toHexString((long)(Math.random() * Long.MAX_VALUE));
        _tmpdir = new File(tmpdir, "narcache_" + rando);
        if (!_tmpdir.exists()) {
            if (_tmpdir.mkdirs()) {
                log.debug("Created narya temp cache directory '" + _tmpdir + "'.");
            } else {
                log.warning("Failed to create temp cache directory '" + _tmpdir + "'.");
            }
        }

        // add a hook to blow away the temp directory when we exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run () {
                log.info("Clearing narya temp cache '" + _tmpdir + "'.");
                FileUtil.recursiveDelete(_tmpdir);
            }
        });
    }

    /** Strips the .jar off of jar file paths. */
    protected static String stripSuffix (String path)
    {
        if (path.endsWith(".jar")) {
            return path.substring(0, path.length()-4);
        } else {
            // we have to change the path somehow
            return path + "-cache";
        }
    }

    /** The file from which we construct our jar file. */
    protected File _source;

    /** The last modified time of our source jar file. */
    protected long _sourceLastMod = -1;

    /** A file whose timestamp indicates whether or not our existing jar file has been unpacked. */
    protected File _unpacked;

    /** A directory into which we unpack files from our bundle. */
    protected File _cache;

    /** The jar file from which we load resources. */
    protected JarFile _jarSource;

    /** A directory in which we temporarily unpack our resource files. */
    protected static File _tmpdir;
}
