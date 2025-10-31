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

package com.threerings.tudey.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.google.common.io.CountingInputStream;

import com.threerings.util.ReflectionUtil;

import com.threerings.config.ConfigManager;
import com.threerings.editor.tools.AbstractValidatorTask;
import com.threerings.editor.util.Validator;
import com.threerings.export.BinaryImporter;

import com.threerings.tudey.data.TudeySceneModel;

import static com.threerings.tudey.Log.log;

/**
 * Validates the references in a set of scenes.
 */
public class SceneValidatorTask extends AbstractValidatorTask
{
  @Override
  protected boolean validate (ConfigManager cfgmgr, Iterable<File> files, Validator validator)
  {
    boolean valid = true;

    final CountingInputStream[] counter = new CountingInputStream[1];
    for (File source : files) {
      try {
        final long maxSize = getMaxUncompressedSize(source);
        TudeySceneModel model = (TudeySceneModel)new BinaryImporter(new FileInputStream(source)) {
          @Override protected Object newInstance (Class<?> clazz, Object outer) {
            return SceneValidatorTask.this.newInstance(clazz, outer);
          }
          @Override protected InputStream createInflaterStream () {
            InputStream base = super.createInflaterStream();
            return maxSize == Long.MAX_VALUE ? base : (counter[0] = new CountingInputStream(base));
          }
        }.readObject();
        try {
          if (counter[0] != null && counter[0].getCount() > maxSize) {
            valid = false;
            validator.output("Uncompressed size too large " + source + ": " +
              "(" + counter[0].getCount() + " > " + maxSize + ")");
            continue;
          }
        } finally {
          counter[0] = null;
        }
        validator.pushWhere("Scene: " + source);
        try {
          valid &= checkModel(cfgmgr, validator, model);
        } finally {
          validator.popWhere();
        }

      } catch (Exception e) { // IOException, ClassCastException
        log.warning("Failed to read scene.", "file", source, e);
        valid = false;
      }
    }

    return valid;
  }

  /**
   * Get the maximum uncompressed size of a scene data file.
   */
  protected long getMaxUncompressedSize (File file) {
    return Long.MAX_VALUE;
  }

  /**
   * Do any further model checking.
   */
  protected boolean checkModel (ConfigManager cfgmgr, Validator validator, TudeySceneModel model)
  {
    model.getConfigManager().init("scene", cfgmgr);
    return model.validateReferences(validator);
  }

  /**
   * We defer importer newInstance to here.
   */
  protected Object newInstance (Class<?> clazz, Object outer)
  {
    return ReflectionUtil.newInstance(clazz, outer);
  }
}
