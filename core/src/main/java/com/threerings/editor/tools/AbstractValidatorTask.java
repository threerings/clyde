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

import com.threerings.tools.FileSetTask;

import com.threerings.resource.ResourceManager;
import com.threerings.util.ResourceContext;
import com.threerings.util.MessageManager;

import com.threerings.editor.util.Validator;

import com.threerings.config.ConfigManager;

/**
 * Abstract class for performing validation.
 */
public abstract class AbstractValidatorTask extends FileSetTask
{
    @Override
    public void execute ()
        throws BuildException
    {
        ValidatorContext context = createContext();

        Validator validator = createValidator();
        Iterable<File> files = getFiles();

        if (!validate(context.getConfigManager(), files, validator)) {
            throw new BuildException();
        }
    }

    /**
     * Do the actual validation.
     */
    protected abstract boolean validate (
            ConfigManager cfgmgr, Iterable<File> files, Validator validator);

    /**
     * Create the validator to use for this task.
     */
    protected Validator createValidator ()
    {
        return new Validator(System.err);
    }

    /**
     * Create the context for this task.
     */
    protected ValidatorContext createContext ()
    {
        return new ValidatorContext();
    }

    /**
     * The context for this validator.
     */
    protected static class ValidatorContext
        implements ResourceContext
    {
        /** Default constructor. */
        public ValidatorContext ()
        {
            _rsrcmgr = createResourceManager();
            _rsrcmgr.initResourceDir("rsrc/");
            _msgmgr = createMessageManager();
            _cfgmgr = createConfigManager(_rsrcmgr, _msgmgr);
            _cfgmgr.init();
        }

        // from ResourceContext
        public ResourceManager getResourceManager ()
        {
            return _rsrcmgr;
        }

        // from ResourceContext
        public MessageManager getMessageManager ()
        {
            return _msgmgr;
        }

        // from ResourceContext
        public ConfigManager getConfigManager ()
        {
            return _cfgmgr;
        }

        /**
         * Create and return a resource manager.
         */
        protected ResourceManager createResourceManager ()
        {
            return new ResourceManager("rsrc/");
        }

        /**
         * Create and return a message manager.
         */
        public MessageManager createMessageManager ()
        {
            return new MessageManager("rsrc.i18n");
        }

        /**
         * Create an return a config manager based on the passed in message and resource manager.
         */
        public ConfigManager createConfigManager (ResourceManager rsrcmgr, MessageManager msgmgr)
        {
            return new ConfigManager(rsrcmgr, msgmgr, "config/");
        }

        /** The resource manager. */
        protected ResourceManager _rsrcmgr;

        /** The message manager. */
        protected MessageManager _msgmgr;

        /** The config manager. */
        protected ConfigManager _cfgmgr;
    }
}
