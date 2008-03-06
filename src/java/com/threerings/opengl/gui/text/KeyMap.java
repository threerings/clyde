//
// $Id$

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
