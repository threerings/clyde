//
// $Id$

package com.threerings.io;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;

import com.samskivert.util.StringUtil;

import static com.threerings.ClydeLog.*;

/**
 * The counterpart of {@link UnreliableObjectOutputStream}.
 */
public class UnreliableObjectInputStream extends ObjectInputStream
{
    /**
     * Constructs an object input stream which will read its data from the supplied source stream.
     */
    public UnreliableObjectInputStream (InputStream source)
    {
        super(source);
    }

    @Override // documentation inherited
    public Object readObject ()
        throws IOException, ClassNotFoundException
    {
        ClassMapping cmap;

        // create our classmap if necessary
        if (_classmap == null) {
            _classmap = new ArrayList<ClassMapping>();
            // insert a zeroth element
            _classmap.add(null);
        }

        try {
            // read in the class code for this instance
            short code = readShort();

            // a zero code indicates a null value
            if (code == 0) {
                if (STREAM_DEBUG) {
                    log.info(hashCode() + ": Read null.");
                }
                return null;
            }

            // if the code is negative, that means that class metadata follows
            if (code < 0) {
                // first swap the code into positive-land
                code *= -1;

                // read in the class metadata
                String cname = readUTF();

                // if we have a translation (used to cope when serialized classes are renamed)
                // use it
                if (_translations != null) {
                    String tname = _translations.get(cname);
                    if (tname != null) {
                        cname = tname;
                    }
                }

                // see if we already have a mapping
                cmap = (code < _classmap.size()) ? _classmap.get(code) : null;
                if (cmap != null) {
                    // sanity check
                    if (!cmap.sclass.getName().equals(cname)) {
                        throw new RuntimeException(
                            "Received metadata for class that conflicts with existing mapping " +
                            "[code=" + code + ", oclass=" + cmap.sclass.getName() + ", nclass=" +
                            cname + "]");
                    }

                } else {
                    // create a class mapping record, and cache it
                    Class sclass = Class.forName(cname, true, _loader);
                    Streamer streamer = Streamer.getStreamer(sclass);
                    if (STREAM_DEBUG) {
                        log.info(hashCode() + ": New class '" + cname + "' [code=" + code + "].");
                    }

                    // sanity check
                    if (streamer == null) {
                        String errmsg = "Aiya! Unable to create streamer for newly seen class " +
                            "[code=" + code + ", class=" + cname + "]";
                        throw new RuntimeException(errmsg);
                    }

                    // insert null entries for missing mappings
                    cmap = new ClassMapping(code, sclass, streamer);
                    for (int ii = 0, nn = (code + 1) - _classmap.size(); ii < nn; ii++) {
                        _classmap.add(null);
                    }
                    _classmap.set(code, cmap);
                }

            } else {
                cmap = _classmap.get(code);

                // sanity check
                if (cmap == null) {
                    // this will help with debugging
                    log.warning("Internal stream error, no class metadata [code=" + code +
                                ", ois=" + this + "].");
                    Thread.dumpStack();
                    log.warning(
                        "ObjectInputStream mappings " + StringUtil.toString(_classmap) + ".");
                    String errmsg = "Read object code for which we have no registered class " +
                        "metadata [code=" + code + "]";
                    throw new RuntimeException(errmsg);
                }
            }

            if (STREAM_DEBUG) {
                log.info(hashCode() + ": Reading with " + cmap.streamer + ".");
            }

            // create an instance of the appropriate object
            Object target = cmap.streamer.createObject(this);
            readBareObject(target, cmap.streamer, true);
            return target;

        } catch (OutOfMemoryError oome) {
            throw (IOException)new IOException("Malformed object data").initCause(oome);
        }
    }
}
