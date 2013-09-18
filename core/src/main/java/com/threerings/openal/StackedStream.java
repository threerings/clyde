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

package com.threerings.openal;

import java.io.IOException;

import static com.threerings.openal.Log.log;

/**
 * A ResourceStream that can pause other streams on the same stack, and resume them
 * once this stream is popped.
 */
public class StackedStream extends ResourceStream
{
    /**
     * Creates a new stacked stream.
     */
    public StackedStream (SoundManager soundmgr, String file, boolean loop, String stack)
        throws IOException
    {
        super(soundmgr, file, loop);
        _stack = stack;
    }

    /**
     * Returns the name of the stack.
     */
    public String getStack ()
    {
        return _stack;
    }

    /**
     * Returns the level of the stack.
     */
    public int getLevel ()
    {
        return _level;
    }

    /**
     * Pushes the stream onto the stack, stopping any streams on top of the stack.
     */
    public void push (float interval, boolean current)
    {
        if (_level > -1) {
            log.warning("Can't push stream already on the stack", "level", _level);
            return;
        }
        int top = getTop();
        if (top > -1 && !current) {
            for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
                if (sameStack(stream) && ((StackedStream)stream).getLevel() == top) {
                    if (interval > 0f) {
                        stream.fadeOut(interval, false);
                    } else {
                        stream.stop();
                    }
                }
            }
        }
        _level = top;
        if (top == -1 || !current) {
            _level++;
        }
        if (interval > 0f) {
            fadeIn(interval);
        } else {
            play();
        }
    }

    /**
     * Pops the stream off the stack, resuming any streams that were below it.
     */
    public void pop (float interval)
    {
        if (_level == -1) {
            log.warning("Can't pop stream not on the stack");
            return;
        }
        if (interval > 0f) {
            fadeOut(interval, true);
        } else {
            dispose();
        }
        int oldLevel = _level;
        _level = -1;
        int top = getTop();
        if (top > -1 && top < oldLevel) {
            for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
                if (sameStack(stream) && ((StackedStream)stream).getLevel() == top &&
                        stream._fadeMode != FadeMode.OUT_DISPOSE) {
                    if (interval > 0f) {
                        stream.fadeIn(interval);
                    } else {
                        stream.play();
                    }
                }
            }
        }
    }

    /**
     * Returns true if this stream is on the same stack.
     */
    protected boolean sameStack (com.threerings.openal.Stream other)
    {
        if (other instanceof StackedStream) {
            String stack = ((StackedStream)other).getStack();
            return _stack == null ? stack == null : _stack.equals(stack);
        }
        return false;
    }

    /**
     * Returns the top level of the stack.
     */
    protected int getTop ()
    {
        int top = -1;
        for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
            if (sameStack(stream)) {
                top = Math.max(top, ((StackedStream)stream).getLevel());
            }
        }
        return top;
    }

    /** The stack this stream is on. */
    protected String _stack;

    /** The level of the stack this stream is on. */
    protected int _level = -1;
}
