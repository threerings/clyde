//
// $Id$

package com.threerings.editor.swing.editors;

import com.threerings.config.DerivedConfig;

/**
 * An editor for the configuration references in a DerivedConfig.
 */
public class DerivedConfigReferenceEditor extends ConfigReferenceEditor
{
    @Override
    protected Class<?> getArgumentType ()
    {
        return ((DerivedConfig)_object).cclass;
    }
}
