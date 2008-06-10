//
// $Id$

package com.threerings.editor.swing.editors;

import java.io.File;

/**
 * Editor for resource references, which are set as files but stored as string paths relative
 * to the resource directory.
 */
public class ResourceEditor extends FileEditor
{
    @Override // documentation inherited
    protected String getDefaultDirectory ()
    {
        return _ctx.getResourceManager().getResourceFile("").toString();
    }

    @Override // documentation inherited
    protected File getPropertyFile ()
    {
        String path = (String)_property.get(_object);
        return (path == null) ? null : _ctx.getResourceManager().getResourceFile(path);
    }

    @Override // documentation inherited
    protected void setPropertyFile (File file)
    {
        String path = null;
        if (file != null) {
            String parent = _ctx.getResourceManager().getResourceFile("").toString();
            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String child = file.toString();
            if (child.startsWith(parent)) {
                path = child.substring(parent.length()).replace(File.separatorChar, '/');
            }
        }
        _property.set(_object, path);
    }
}
