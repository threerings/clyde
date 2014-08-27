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

package com.threerings.editor.tools;

import java.io.File;

import org.apache.tools.ant.BuildException;

import com.threerings.resource.ResourceManager;

import com.threerings.editor.util.Validator;

import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;

/**
 * Validates the references in a set of configs.
 */
public class ConfigValidatorTask extends AbstractValidatorTask
{
    @Override
    protected boolean validate (ConfigManager cfgmgr, Iterable<File> files, Validator validator)
    {
        // validate the base configs
        boolean valid = cfgmgr.validateReferences(validator);

        // validate the resource configs
        ResourceManager rsrcmgr = cfgmgr.getResourceManager();
        for (File source : files) {
            String path = rsrcmgr.getResourcePath(source);
            if (path == null) continue;
            ManagedConfig config = cfgmgr.getResourceConfig(path);
            if (config == null) continue;
            validator.pushWhere(path);
            try {
                valid &= config.validateReferences(validator);
            } catch (Exception e) {
                throw new BuildException("Error validating '" + path + "': " + e);
            } finally {
                validator.popWhere();
            }
        }

        return valid;
    }
}
