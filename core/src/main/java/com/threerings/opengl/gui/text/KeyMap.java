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

package com.threerings.opengl.gui.text;

/**
 * Maps key presses with specific modifier combinations to editor
 * commands. These are used by the text-entry components.
 */
public class KeyMap
{
    /** A command constant indicating no mapping exists for a particular
     * modifier and key code combination. */
    public static final int NO_MAPPING = -1;

    /** A modifiers code that if specified, will default any keyCode to
     * the specified command unless a specific modifier mapping is set. */
    public static final int ANY_MODIFIER = -1;

    /**
     * Adds a mapping for the specified modifier and key code combination
     * to the specified command.
     */
    public void addMapping (int modifiers, int keyCode, int command)
    {
        int kidx = keyCode % BUCKETS;

        // override any preexisting mapping
        for (Mapping map = _mappings[kidx]; map != null; map = map.next) {
            if (map.matches(modifiers, keyCode)) {
                map.command = command;
                return;
            }
        }

        // create a new mapping
        Mapping map = new Mapping(modifiers, keyCode, command);
        map.next = _mappings[kidx];
        _mappings[kidx] = map;
    }

    /**
     * Looks up and returns the command associated with the specified set
     * of modifiers and key code. Returns {@link #NO_MAPPING} if no
     * matching mapping can be found.
     */
    public int lookupMapping (int modifiers, int keyCode)
    {
        int kidx = keyCode % BUCKETS;
        int defaultCommand = NO_MAPPING;
        for (Mapping map = _mappings[kidx]; map != null; map = map.next) {
            if (map.matches(modifiers, keyCode)) {
                return map.command;
            } else if (map.matches(ANY_MODIFIER, keyCode)) {
                defaultCommand = map.command;
            }
        }
        return defaultCommand;
    }

    /** Contains information about a single key mapping. */
    protected static class Mapping
    {
        public int modifiers;
        public int keyCode;
        public int command;
        public Mapping next;

        public Mapping (int modifiers, int keyCode, int command) {
            this.modifiers = modifiers;
            this.keyCode = keyCode;
            this.command = command;
        }

        public boolean matches (int modifiers, int keyCode) {
            return (modifiers == this.modifiers && keyCode == this.keyCode);
        }
    }

    /** Contains a primitive hashmap of mappings. */
    protected Mapping[] _mappings = new Mapping[BUCKETS];

    /** The number of mapping buckets we maintain. */
    protected static final int BUCKETS = 64;
}
