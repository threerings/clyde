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
        _property.set(_object, (file == null) ?
            null : _ctx.getResourceManager().getResourcePath(file));
    }
}
