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
import java.lang.foreign.SymbolLookup;

import com.samskivert.util.RunAnywhere;
import com.samskivert.util.RunQueue;

import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.system.JNI;
import org.lwjgl.system.macosx.ObjCRuntime;

import static com.threerings.opengl.Log.log;

/**
 * Helpers for macOS Cocoa "native fullscreen" — the green-button fullscreen that creates a new
 * Space. Distinct from GLFW's exclusive-fullscreen-with-videomode switch, which the rest of
 * {@link GlDisplayApp} handles.
 *
 * <p>Uses existing LWJGL bridges (no new native code): {@code GLFWNativeCocoa} for the GLFW
 * window → NSWindow handle, {@code ObjCRuntime.sel_getUid} for selector lookup, {@code JNI.invokeX}
 * for calling {@code objc_msgSend}. The objc_msgSend function pointer is found via the
 * JDK's FFM {@code Linker.defaultLookup()} (Java 22+).
 *
 * <p>All public methods are safe to call on non-macOS; they become no-ops there.
 */
public final class MacFullscreen
{
  /**
   * Returns whether the given GLFW window is currently in Cocoa native fullscreen.
   * Always false when not on macOS or when {@code glfwWindow} is 0.
   */
  public static boolean isFullscreen (long glfwWindow)
  {
    if (!RunAnywhere.isMacOS() || glfwWindow == 0 || !init()) return false;
    long nsWindow = GLFWNativeCocoa.glfwGetCocoaWindow(glfwWindow);
    if (nsWindow == 0) return false;
    long mask = JNI.invokePPJ(nsWindow, _selStyleMask, _objcMsgSend);
    return (mask & NS_WINDOW_STYLE_MASK_FULLSCREEN) != 0;
  }

  /**
   * Toggles Cocoa native fullscreen on the given window to the given state. The transition is
   * animated (~500ms) and asynchronous. Once the window reports the desired state — or after
   * we've exhausted our polling budget — {@code onComplete} is run on {@code queue}.
   *
   * <p>On non-macOS (or any other early-out path) {@code onComplete} runs inline immediately.
   */
  public static void setFullscreen (long glfwWindow, boolean fullscreen,
                                    RunQueue queue, Runnable onComplete)
  {
    if (!RunAnywhere.isMacOS() || glfwWindow == 0 || !init() ||
        isFullscreen(glfwWindow) == fullscreen) {
      if (onComplete != null) onComplete.run();
      return;
    }
    long nsWindow = GLFWNativeCocoa.glfwGetCocoaWindow(glfwWindow);
    if (nsWindow == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }
    // -[NSWindow toggleFullScreen:] must run on the AppKit main thread. With the glfw_async
    // build we're calling this from the game main thread, which is NOT the AppKit main thread
    // — calling toggleFullScreen: directly crashes inside AppKit. We dispatch it via
    // -[NSObject performSelectorOnMainThread:withObject:waitUntilDone:] which is safe to
    // invoke from any thread; with waitUntilDone:NO it schedules toggleFullScreen: on main
    // and returns immediately.
    JNI.invokePPPPV(nsWindow, _selPerformOnMain, _selToggleFullScreen,
        0L /* nil withObject */, false /* waitUntilDone */, _objcMsgSend);
    pollUntilSettled(glfwWindow, fullscreen, queue, onComplete, MAX_POLL_ATTEMPTS);
  }

  /**
   * Re-posts itself onto {@code queue} until the window's fullscreen state matches
   * {@code desired}, then runs {@code onComplete}. Gives up (but still runs the continuation)
   * after {@code remaining} attempts to avoid looping forever if the animation never fires.
   */
  private static void pollUntilSettled (long glfwWindow, boolean desired,
                                        RunQueue queue, Runnable onComplete, int remaining)
  {
    if (isFullscreen(glfwWindow) == desired) {
      if (onComplete != null) onComplete.run();
      return;
    }
    if (remaining <= 0) {
      log.warning("MacFullscreen toggle didn't settle; continuing anyway.",
          "desired", desired);
      if (onComplete != null) onComplete.run();
      return;
    }
    queue.postRunnable(() ->
      pollUntilSettled(glfwWindow, desired, queue, onComplete, remaining - 1));
  }

  /**
   * Lazily resolves the ObjC selectors and the {@code objc_msgSend} function pointer on
   * first use. Runs once; subsequent calls are a no-op. Returns false if we couldn't set
   * things up (e.g., running on a non-Mac JVM that still somehow loaded this class).
   */
  private static synchronized boolean init ()
  {
    if (_initialized) return _objcMsgSend != 0;
    _initialized = true;
    if (!RunAnywhere.isMacOS()) return false;
    try {
      // defaultLookup() on macOS only covers libSystem-level symbols; libobjc isn't in it,
      // so we have to load it explicitly. libobjc.A.dylib is already loaded into the
      // process (GLFW/AWT pull it in); libraryLookup just finds the existing image.
      @SuppressWarnings("restricted") // yeah, yeah, we know, just trying it here...
      SymbolLookup libobjc = SymbolLookup.libraryLookup("libobjc.A.dylib", Arena.global());
      _objcMsgSend = libobjc.find("objc_msgSend")
          .orElseThrow(() -> new IllegalStateException("objc_msgSend not found"))
          .address();
      _selStyleMask = ObjCRuntime.sel_getUid("styleMask");
      _selToggleFullScreen = ObjCRuntime.sel_getUid("toggleFullScreen:");
      _selPerformOnMain =
          ObjCRuntime.sel_getUid("performSelectorOnMainThread:withObject:waitUntilDone:");
    } catch (Throwable t) {
      log.warning("MacFullscreen init failed; Cocoa fullscreen control disabled.", t);
      _objcMsgSend = 0;
      return false;
    }
    return true;
  }

  /** NSWindowStyleMaskFullScreen. */
  private static final long NS_WINDOW_STYLE_MASK_FULLSCREEN = 1L << 14;

  /** Upper bound on how many run-queue iterations we'll wait for the animated toggle to
   *  complete. At 60Hz (one check per frame at worst) this is ~16s of wall clock, well past
   *  macOS's sub-second fullscreen transition. */
  private static final int MAX_POLL_ATTEMPTS = 1000;

  private static boolean _initialized;
  private static long _objcMsgSend;
  private static long _selStyleMask;
  private static long _selToggleFullScreen;
  private static long _selPerformOnMain;
}
