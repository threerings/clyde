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

package com.threerings.opengl.util;

import com.samskivert.util.RunAnywhere;

import org.lwjgl.system.JNI;
import org.lwjgl.system.macosx.ObjCRuntime;

import static com.threerings.opengl.Log.log;

/**
 * Queries the state of AppKit's main event loop, via LWJGL's ObjC runtime bindings.
 */
public final class MacEventLoop
{
  /**
   * Returns whether something is already running AppKit's main event loop ({@code [NSApp run]}),
   * as AWT does once its toolkit is initialized. Always false off macOS, and false (with a
   * warning) if the query itself fails.
   */
  public static boolean isRunning ()
  {
    if (!RunAnywhere.isMacOS()) return false;
    try {
      long msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
      if (msgSend == 0) {
        log.warning("objc_msgSend not found; assuming no AppKit event loop.");
        return false;
      }
      long app = JNI.invokePPJ(ObjCRuntime.objc_getClass("NSApplication"),
        ObjCRuntime.sel_getUid("sharedApplication"), msgSend);
      return app != 0 && JNI.invokePPZ(app, ObjCRuntime.sel_getUid("isRunning"), msgSend);
    } catch (Throwable t) {
      log.warning("Couldn't query the AppKit event loop; assuming it isn't running.", t);
      return false;
    }
  }
}
