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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.samskivert.util.RunAnywhere;

import org.lwjgl.glfw.GLFWNativeNSGL;
import org.lwjgl.system.JNI;
import org.lwjgl.system.macosx.ObjCRuntime;

import static com.threerings.opengl.Log.log;

/**
 * Defuses a crash in Apple's OpenGL-on-Metal layer. When screen parameters change (the window
 * dragged to another display, a fullscreen Space entered or exited), GLFW reacts by calling
 * {@code -[NSOpenGLContext update]} inline on the AppKit main thread — while the game thread
 * is mid-frame with a Metal command encoder open on the same command buffer. Metal allows one
 * active encoder per command buffer, so the process aborts with "A command encoder is already
 * encoding to this command buffer". GLFW (through at least LWJGL 3.4.1's build) neither locks
 * the context nor defers the update, so we defer it ourselves; if GLFW ever fixes this
 * upstream, this class can be deleted.
 *
 * <p>{@link #install} swaps the window's NSOpenGLContext into a runtime-registered subclass
 * whose {@code update} override merely raises a flag. {@link #processPending}, called between
 * frames by {@link com.threerings.opengl.GlDisplayApp}'s main loop, then replays the real
 * update <em>on the AppKit main thread</em> while the game thread parks waiting for it — so
 * the update never overlaps frame encoding, and no locking is needed. The update must stay on
 * the main thread: modern AppKit traps if update runs anywhere else, so we control
 * <em>when</em> it runs, not <em>where</em>. The replay reaches the real update through a
 * second selector ({@code clydeRealUpdate}) bound to NSOpenGLContext's original IMP, since
 * messaging {@code update} would just re-enter our override. Worst case is one frame drawn at
 * a stale size mid-transition — previously it was a crash.
 *
 * <p>Same no-new-native-code approach as {@link MacFullscreen}: LWJGL's ObjC runtime bindings,
 * plus one FFM upcall stub for the override IMP. All methods are no-ops off macOS.
 */
public final class MacContextUpdate
{
  /**
   * Reroutes the given GLFW window's NSOpenGLContext updates through {@link #processPending}.
   * Call once, after the GL context has been created and before rendering begins.
   */
  @SuppressWarnings("restricted") // libraryLookup, upcallStub
  public static void install (long glfwWindow)
  {
    if (!RunAnywhere.isMacOS() || glfwWindow == 0 || _context != 0) return;
    try {
      long ctx = GLFWNativeNSGL.glfwGetNSGLContext(glfwWindow);
      if (ctx == 0) {
        log.warning("No NSGL context found; context update deferral disabled.");
        return;
      }
      long superclass = ObjCRuntime.objc_getClass("NSOpenGLContext");
      long selUpdate = ObjCRuntime.sel_getUid("update");
      long superUpdate = ObjCRuntime.class_getMethodImplementation(superclass, selUpdate);

      // objc_msgSend, for performSelectorOnMainThread. libobjc is already loaded into the
      // process; libraryLookup just finds the existing image (as in MacFullscreen).
      long msgSend = SymbolLookup.libraryLookup("libobjc.A.dylib", Arena.global())
          .find("objc_msgSend")
          .orElseThrow(() -> new IllegalStateException("objc_msgSend not found"))
          .address();

      // An IMP with signature void(id, SEL) that raises the pending flag. Global arena: the
      // subclass is registered for the life of the process, so its IMP must live as long.
      MemorySegment stub = Linker.nativeLinker().upcallStub(
        MethodHandles.lookup().findStatic(MacContextUpdate.class, "updateRequested",
          MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class)),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
        Arena.global());

      long subclass = ObjCRuntime.objc_allocateClassPair(superclass, SUBCLASS_NAME, 0);
      if (subclass == 0) {
        log.warning("Couldn't allocate context subclass; context update deferral disabled.");
        return;
      }
      if (!ObjCRuntime.class_addMethod(subclass, selUpdate, stub.address(), "v@:")) {
        log.warning("Couldn't add update override; context update deferral disabled.");
        return;
      }
      // Expose the original update IMP under a second selector so the main-thread replay
      // can invoke the real thing without dispatching back into our override.
      long selRealUpdate = ObjCRuntime.sel_getUid("clydeRealUpdate");
      if (!ObjCRuntime.class_addMethod(subclass, selRealUpdate, superUpdate, "v@:")) {
        log.warning("Couldn't add replay selector; context update deferral disabled.");
        return;
      }
      ObjCRuntime.objc_registerClassPair(subclass);
      ObjCRuntime.object_setClass(ctx, subclass);

      _selRealUpdate = selRealUpdate;
      _selPerformOnMain =
          ObjCRuntime.sel_getUid("performSelectorOnMainThread:withObject:waitUntilDone:");
      _objcMsgSend = msgSend;
      _context = ctx; // publishes installation; assign last
    } catch (Throwable t) {
      log.warning("MacContextUpdate install failed; context update deferral disabled.", t);
    }
  }

  /**
   * Replays any context update requested since the last call. Call between frames on the
   * game thread: we park here while the AppKit main thread runs the real update, which is
   * what makes it safe — nothing is encoding while we wait. Cheap no-op when nothing is
   * pending or {@link #install} never ran.
   */
  public static void processPending ()
  {
    if (_context == 0 || !_pending) return;
    _pending = false; // clear first: a request arriving mid-replay is honored next frame
    // waitUntilDone:YES; performSelectorOnMainThread queues in the common run loop modes,
    // so this is serviced even during window drags and fullscreen transitions.
    JNI.invokePPPPV(_context, _selPerformOnMain, _selRealUpdate,
        0L /* nil withObject */, true /* waitUntilDone */, _objcMsgSend);
  }

  /**
   * Upcall target for the overridden {@code update}. Runs on whatever thread AppKit calls
   * update from (normally its main thread), so it must only raise the flag — no allocation,
   * no logging, no GL.
   */
  @SuppressWarnings("unused") // invoked via the method handle in install()
  private static void updateRequested (MemorySegment self, MemorySegment sel)
  {
    _pending = true;
  }

  /** ObjC name for our NSOpenGLContext subclass. */
  private static final String SUBCLASS_NAME = "ClydeDeferredUpdateGLContext";

  /** The NSOpenGLContext whose updates we defer, or 0 if not installed. */
  private static volatile long _context;

  /** The selector bound to NSOpenGLContext's original update IMP. */
  private static long _selRealUpdate;

  /** performSelectorOnMainThread:withObject:waitUntilDone:. */
  private static long _selPerformOnMain;

  /** The objc_msgSend function pointer. */
  private static long _objcMsgSend;

  /** Set from the AppKit thread; cleared by processPending. */
  private static volatile boolean _pending;
}
