package com.threerings.opengl.lwjgl2;

import org.lwjgl.glfw.GLFW;

/**
 * Compatibility class providing LWJGL 2 keyboard constants.
 * These map LWJGL 2's DirectInput-based scancode constants to their original integer values.
 * The actual input polling is handled by the Display/Root classes via GLFW callbacks.
 */
public class Keyboard
{
    // LWJGL 2 key constants (original values preserved for serialization compatibility)
    public static final int KEY_NONE = 0x00;
    public static final int KEY_ESCAPE = 0x01;
    public static final int KEY_1 = 0x02;
    public static final int KEY_2 = 0x03;
    public static final int KEY_3 = 0x04;
    public static final int KEY_4 = 0x05;
    public static final int KEY_5 = 0x06;
    public static final int KEY_6 = 0x07;
    public static final int KEY_7 = 0x08;
    public static final int KEY_8 = 0x09;
    public static final int KEY_9 = 0x0A;
    public static final int KEY_0 = 0x0B;
    public static final int KEY_MINUS = 0x0C;
    public static final int KEY_EQUALS = 0x0D;
    public static final int KEY_BACK = 0x0E;
    public static final int KEY_TAB = 0x0F;
    public static final int KEY_Q = 0x10;
    public static final int KEY_W = 0x11;
    public static final int KEY_E = 0x12;
    public static final int KEY_R = 0x13;
    public static final int KEY_T = 0x14;
    public static final int KEY_Y = 0x15;
    public static final int KEY_U = 0x16;
    public static final int KEY_I = 0x17;
    public static final int KEY_O = 0x18;
    public static final int KEY_P = 0x19;
    public static final int KEY_LBRACKET = 0x1A;
    public static final int KEY_RBRACKET = 0x1B;
    public static final int KEY_RETURN = 0x1C;
    public static final int KEY_LCONTROL = 0x1D;
    public static final int KEY_A = 0x1E;
    public static final int KEY_S = 0x1F;
    public static final int KEY_D = 0x20;
    public static final int KEY_F = 0x21;
    public static final int KEY_G = 0x22;
    public static final int KEY_H = 0x23;
    public static final int KEY_J = 0x24;
    public static final int KEY_K = 0x25;
    public static final int KEY_L = 0x26;
    public static final int KEY_SEMICOLON = 0x27;
    public static final int KEY_APOSTROPHE = 0x28;
    public static final int KEY_GRAVE = 0x29;
    public static final int KEY_LSHIFT = 0x2A;
    public static final int KEY_BACKSLASH = 0x2B;
    public static final int KEY_Z = 0x2C;
    public static final int KEY_X = 0x2D;
    public static final int KEY_C = 0x2E;
    public static final int KEY_V = 0x2F;
    public static final int KEY_B = 0x30;
    public static final int KEY_N = 0x31;
    public static final int KEY_M = 0x32;
    public static final int KEY_COMMA = 0x33;
    public static final int KEY_PERIOD = 0x34;
    public static final int KEY_SLASH = 0x35;
    public static final int KEY_RSHIFT = 0x36;
    public static final int KEY_MULTIPLY = 0x37;
    public static final int KEY_LMENU = 0x38;
    public static final int KEY_SPACE = 0x39;
    public static final int KEY_CAPITAL = 0x3A;
    public static final int KEY_F1 = 0x3B;
    public static final int KEY_F2 = 0x3C;
    public static final int KEY_F3 = 0x3D;
    public static final int KEY_F4 = 0x3E;
    public static final int KEY_F5 = 0x3F;
    public static final int KEY_F6 = 0x40;
    public static final int KEY_F7 = 0x41;
    public static final int KEY_F8 = 0x42;
    public static final int KEY_F9 = 0x43;
    public static final int KEY_F10 = 0x44;
    public static final int KEY_NUMLOCK = 0x45;
    public static final int KEY_SCROLL = 0x46;
    public static final int KEY_NUMPAD7 = 0x47;
    public static final int KEY_NUMPAD8 = 0x48;
    public static final int KEY_NUMPAD9 = 0x49;
    public static final int KEY_SUBTRACT = 0x4A;
    public static final int KEY_NUMPAD4 = 0x4B;
    public static final int KEY_NUMPAD5 = 0x4C;
    public static final int KEY_NUMPAD6 = 0x4D;
    public static final int KEY_ADD = 0x4E;
    public static final int KEY_NUMPAD1 = 0x4F;
    public static final int KEY_NUMPAD2 = 0x50;
    public static final int KEY_NUMPAD3 = 0x51;
    public static final int KEY_NUMPAD0 = 0x52;
    public static final int KEY_DECIMAL = 0x53;
    public static final int KEY_F11 = 0x57;
    public static final int KEY_F12 = 0x58;
    public static final int KEY_F13 = 0x64;
    public static final int KEY_F14 = 0x65;
    public static final int KEY_F15 = 0x66;
    public static final int KEY_KANA = 0x70;
    public static final int KEY_CONVERT = 0x79;
    public static final int KEY_NOCONVERT = 0x7B;
    public static final int KEY_YEN = 0x7D;
    public static final int KEY_NUMPADEQUALS = 0x8D;
    public static final int KEY_CIRCUMFLEX = 0x90;
    public static final int KEY_AT = 0x91;
    public static final int KEY_COLON = 0x92;
    public static final int KEY_UNDERLINE = 0x93;
    public static final int KEY_KANJI = 0x94;
    public static final int KEY_STOP = 0x95;
    public static final int KEY_AX = 0x96;
    public static final int KEY_UNLABELED = 0x97;
    public static final int KEY_NUMPADENTER = 0x9C;
    public static final int KEY_RCONTROL = 0x9D;
    public static final int KEY_NUMPADCOMMA = 0xB3;
    public static final int KEY_DIVIDE = 0xB5;
    public static final int KEY_SYSRQ = 0xB7;
    public static final int KEY_RMENU = 0xB8;
    public static final int KEY_PAUSE = 0xC5;
    public static final int KEY_HOME = 0xC7;
    public static final int KEY_UP = 0xC8;
    public static final int KEY_PRIOR = 0xC9;
    public static final int KEY_LEFT = 0xCB;
    public static final int KEY_RIGHT = 0xCD;
    public static final int KEY_END = 0xCF;
    public static final int KEY_DOWN = 0xD0;
    public static final int KEY_NEXT = 0xD1;
    public static final int KEY_INSERT = 0xD2;
    public static final int KEY_DELETE = 0xD3;
    public static final int KEY_LMETA = 0xDB;
    public static final int KEY_LWIN = KEY_LMETA;
    public static final int KEY_RMETA = 0xDC;
    public static final int KEY_RWIN = KEY_RMETA;
    public static final int KEY_APPS = 0xDD;
    public static final int KEY_POWER = 0xDE;
    public static final int KEY_SLEEP = 0xDF;

    /** The total number of keyboard keys (matching LWJGL 2's value). */
    public static final int KEYBOARD_SIZE = 256;

    /**
     * Converts a GLFW key code to an LWJGL 2 key code.
     */
    public static int glfwToLwjgl2 (int glfwKey)
    {
        switch (glfwKey) {
            case GLFW.GLFW_KEY_SPACE: return KEY_SPACE;
            case GLFW.GLFW_KEY_APOSTROPHE: return KEY_APOSTROPHE;
            case GLFW.GLFW_KEY_COMMA: return KEY_COMMA;
            case GLFW.GLFW_KEY_MINUS: return KEY_MINUS;
            case GLFW.GLFW_KEY_PERIOD: return KEY_PERIOD;
            case GLFW.GLFW_KEY_SLASH: return KEY_SLASH;
            case GLFW.GLFW_KEY_0: return KEY_0;
            case GLFW.GLFW_KEY_1: return KEY_1;
            case GLFW.GLFW_KEY_2: return KEY_2;
            case GLFW.GLFW_KEY_3: return KEY_3;
            case GLFW.GLFW_KEY_4: return KEY_4;
            case GLFW.GLFW_KEY_5: return KEY_5;
            case GLFW.GLFW_KEY_6: return KEY_6;
            case GLFW.GLFW_KEY_7: return KEY_7;
            case GLFW.GLFW_KEY_8: return KEY_8;
            case GLFW.GLFW_KEY_9: return KEY_9;
            case GLFW.GLFW_KEY_SEMICOLON: return KEY_SEMICOLON;
            case GLFW.GLFW_KEY_EQUAL: return KEY_EQUALS;
            case GLFW.GLFW_KEY_A: return KEY_A;
            case GLFW.GLFW_KEY_B: return KEY_B;
            case GLFW.GLFW_KEY_C: return KEY_C;
            case GLFW.GLFW_KEY_D: return KEY_D;
            case GLFW.GLFW_KEY_E: return KEY_E;
            case GLFW.GLFW_KEY_F: return KEY_F;
            case GLFW.GLFW_KEY_G: return KEY_G;
            case GLFW.GLFW_KEY_H: return KEY_H;
            case GLFW.GLFW_KEY_I: return KEY_I;
            case GLFW.GLFW_KEY_J: return KEY_J;
            case GLFW.GLFW_KEY_K: return KEY_K;
            case GLFW.GLFW_KEY_L: return KEY_L;
            case GLFW.GLFW_KEY_M: return KEY_M;
            case GLFW.GLFW_KEY_N: return KEY_N;
            case GLFW.GLFW_KEY_O: return KEY_O;
            case GLFW.GLFW_KEY_P: return KEY_P;
            case GLFW.GLFW_KEY_Q: return KEY_Q;
            case GLFW.GLFW_KEY_R: return KEY_R;
            case GLFW.GLFW_KEY_S: return KEY_S;
            case GLFW.GLFW_KEY_T: return KEY_T;
            case GLFW.GLFW_KEY_U: return KEY_U;
            case GLFW.GLFW_KEY_V: return KEY_V;
            case GLFW.GLFW_KEY_W: return KEY_W;
            case GLFW.GLFW_KEY_X: return KEY_X;
            case GLFW.GLFW_KEY_Y: return KEY_Y;
            case GLFW.GLFW_KEY_Z: return KEY_Z;
            case GLFW.GLFW_KEY_LEFT_BRACKET: return KEY_LBRACKET;
            case GLFW.GLFW_KEY_BACKSLASH: return KEY_BACKSLASH;
            case GLFW.GLFW_KEY_RIGHT_BRACKET: return KEY_RBRACKET;
            case GLFW.GLFW_KEY_GRAVE_ACCENT: return KEY_GRAVE;
            case GLFW.GLFW_KEY_ESCAPE: return KEY_ESCAPE;
            case GLFW.GLFW_KEY_ENTER: return KEY_RETURN;
            case GLFW.GLFW_KEY_TAB: return KEY_TAB;
            case GLFW.GLFW_KEY_BACKSPACE: return KEY_BACK;
            case GLFW.GLFW_KEY_INSERT: return KEY_INSERT;
            case GLFW.GLFW_KEY_DELETE: return KEY_DELETE;
            case GLFW.GLFW_KEY_RIGHT: return KEY_RIGHT;
            case GLFW.GLFW_KEY_LEFT: return KEY_LEFT;
            case GLFW.GLFW_KEY_DOWN: return KEY_DOWN;
            case GLFW.GLFW_KEY_UP: return KEY_UP;
            case GLFW.GLFW_KEY_PAGE_UP: return KEY_PRIOR;
            case GLFW.GLFW_KEY_PAGE_DOWN: return KEY_NEXT;
            case GLFW.GLFW_KEY_HOME: return KEY_HOME;
            case GLFW.GLFW_KEY_END: return KEY_END;
            case GLFW.GLFW_KEY_CAPS_LOCK: return KEY_CAPITAL;
            case GLFW.GLFW_KEY_SCROLL_LOCK: return KEY_SCROLL;
            case GLFW.GLFW_KEY_NUM_LOCK: return KEY_NUMLOCK;
            case GLFW.GLFW_KEY_PRINT_SCREEN: return KEY_SYSRQ;
            case GLFW.GLFW_KEY_PAUSE: return KEY_PAUSE;
            case GLFW.GLFW_KEY_F1: return KEY_F1;
            case GLFW.GLFW_KEY_F2: return KEY_F2;
            case GLFW.GLFW_KEY_F3: return KEY_F3;
            case GLFW.GLFW_KEY_F4: return KEY_F4;
            case GLFW.GLFW_KEY_F5: return KEY_F5;
            case GLFW.GLFW_KEY_F6: return KEY_F6;
            case GLFW.GLFW_KEY_F7: return KEY_F7;
            case GLFW.GLFW_KEY_F8: return KEY_F8;
            case GLFW.GLFW_KEY_F9: return KEY_F9;
            case GLFW.GLFW_KEY_F10: return KEY_F10;
            case GLFW.GLFW_KEY_F11: return KEY_F11;
            case GLFW.GLFW_KEY_F12: return KEY_F12;
            case GLFW.GLFW_KEY_F13: return KEY_F13;
            case GLFW.GLFW_KEY_F14: return KEY_F14;
            case GLFW.GLFW_KEY_F15: return KEY_F15;
            case GLFW.GLFW_KEY_KP_0: return KEY_NUMPAD0;
            case GLFW.GLFW_KEY_KP_1: return KEY_NUMPAD1;
            case GLFW.GLFW_KEY_KP_2: return KEY_NUMPAD2;
            case GLFW.GLFW_KEY_KP_3: return KEY_NUMPAD3;
            case GLFW.GLFW_KEY_KP_4: return KEY_NUMPAD4;
            case GLFW.GLFW_KEY_KP_5: return KEY_NUMPAD5;
            case GLFW.GLFW_KEY_KP_6: return KEY_NUMPAD6;
            case GLFW.GLFW_KEY_KP_7: return KEY_NUMPAD7;
            case GLFW.GLFW_KEY_KP_8: return KEY_NUMPAD8;
            case GLFW.GLFW_KEY_KP_9: return KEY_NUMPAD9;
            case GLFW.GLFW_KEY_KP_DECIMAL: return KEY_DECIMAL;
            case GLFW.GLFW_KEY_KP_DIVIDE: return KEY_DIVIDE;
            case GLFW.GLFW_KEY_KP_MULTIPLY: return KEY_MULTIPLY;
            case GLFW.GLFW_KEY_KP_SUBTRACT: return KEY_SUBTRACT;
            case GLFW.GLFW_KEY_KP_ADD: return KEY_ADD;
            case GLFW.GLFW_KEY_KP_ENTER: return KEY_NUMPADENTER;
            case GLFW.GLFW_KEY_KP_EQUAL: return KEY_NUMPADEQUALS;
            case GLFW.GLFW_KEY_LEFT_SHIFT: return KEY_LSHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL: return KEY_LCONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT: return KEY_LMENU;
            case GLFW.GLFW_KEY_LEFT_SUPER: return KEY_LMETA;
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return KEY_RSHIFT;
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return KEY_RCONTROL;
            case GLFW.GLFW_KEY_RIGHT_ALT: return KEY_RMENU;
            case GLFW.GLFW_KEY_RIGHT_SUPER: return KEY_RMETA;
            default: return KEY_NONE;
        }
    }

    /**
     * Converts an LWJGL 2 key code to a GLFW key code.
     */
    public static int lwjgl2ToGlfw (int lwjgl2Key)
    {
        switch (lwjgl2Key) {
            case KEY_SPACE: return GLFW.GLFW_KEY_SPACE;
            case KEY_APOSTROPHE: return GLFW.GLFW_KEY_APOSTROPHE;
            case KEY_COMMA: return GLFW.GLFW_KEY_COMMA;
            case KEY_MINUS: return GLFW.GLFW_KEY_MINUS;
            case KEY_PERIOD: return GLFW.GLFW_KEY_PERIOD;
            case KEY_SLASH: return GLFW.GLFW_KEY_SLASH;
            case KEY_0: return GLFW.GLFW_KEY_0;
            case KEY_1: return GLFW.GLFW_KEY_1;
            case KEY_2: return GLFW.GLFW_KEY_2;
            case KEY_3: return GLFW.GLFW_KEY_3;
            case KEY_4: return GLFW.GLFW_KEY_4;
            case KEY_5: return GLFW.GLFW_KEY_5;
            case KEY_6: return GLFW.GLFW_KEY_6;
            case KEY_7: return GLFW.GLFW_KEY_7;
            case KEY_8: return GLFW.GLFW_KEY_8;
            case KEY_9: return GLFW.GLFW_KEY_9;
            case KEY_SEMICOLON: return GLFW.GLFW_KEY_SEMICOLON;
            case KEY_EQUALS: return GLFW.GLFW_KEY_EQUAL;
            case KEY_A: return GLFW.GLFW_KEY_A;
            case KEY_B: return GLFW.GLFW_KEY_B;
            case KEY_C: return GLFW.GLFW_KEY_C;
            case KEY_D: return GLFW.GLFW_KEY_D;
            case KEY_E: return GLFW.GLFW_KEY_E;
            case KEY_F: return GLFW.GLFW_KEY_F;
            case KEY_G: return GLFW.GLFW_KEY_G;
            case KEY_H: return GLFW.GLFW_KEY_H;
            case KEY_I: return GLFW.GLFW_KEY_I;
            case KEY_J: return GLFW.GLFW_KEY_J;
            case KEY_K: return GLFW.GLFW_KEY_K;
            case KEY_L: return GLFW.GLFW_KEY_L;
            case KEY_M: return GLFW.GLFW_KEY_M;
            case KEY_N: return GLFW.GLFW_KEY_N;
            case KEY_O: return GLFW.GLFW_KEY_O;
            case KEY_P: return GLFW.GLFW_KEY_P;
            case KEY_Q: return GLFW.GLFW_KEY_Q;
            case KEY_R: return GLFW.GLFW_KEY_R;
            case KEY_S: return GLFW.GLFW_KEY_S;
            case KEY_T: return GLFW.GLFW_KEY_T;
            case KEY_U: return GLFW.GLFW_KEY_U;
            case KEY_V: return GLFW.GLFW_KEY_V;
            case KEY_W: return GLFW.GLFW_KEY_W;
            case KEY_X: return GLFW.GLFW_KEY_X;
            case KEY_Y: return GLFW.GLFW_KEY_Y;
            case KEY_Z: return GLFW.GLFW_KEY_Z;
            case KEY_LBRACKET: return GLFW.GLFW_KEY_LEFT_BRACKET;
            case KEY_BACKSLASH: return GLFW.GLFW_KEY_BACKSLASH;
            case KEY_RBRACKET: return GLFW.GLFW_KEY_RIGHT_BRACKET;
            case KEY_GRAVE: return GLFW.GLFW_KEY_GRAVE_ACCENT;
            case KEY_ESCAPE: return GLFW.GLFW_KEY_ESCAPE;
            case KEY_RETURN: return GLFW.GLFW_KEY_ENTER;
            case KEY_TAB: return GLFW.GLFW_KEY_TAB;
            case KEY_BACK: return GLFW.GLFW_KEY_BACKSPACE;
            case KEY_INSERT: return GLFW.GLFW_KEY_INSERT;
            case KEY_DELETE: return GLFW.GLFW_KEY_DELETE;
            case KEY_RIGHT: return GLFW.GLFW_KEY_RIGHT;
            case KEY_LEFT: return GLFW.GLFW_KEY_LEFT;
            case KEY_DOWN: return GLFW.GLFW_KEY_DOWN;
            case KEY_UP: return GLFW.GLFW_KEY_UP;
            case KEY_PRIOR: return GLFW.GLFW_KEY_PAGE_UP;
            case KEY_NEXT: return GLFW.GLFW_KEY_PAGE_DOWN;
            case KEY_HOME: return GLFW.GLFW_KEY_HOME;
            case KEY_END: return GLFW.GLFW_KEY_END;
            case KEY_CAPITAL: return GLFW.GLFW_KEY_CAPS_LOCK;
            case KEY_SCROLL: return GLFW.GLFW_KEY_SCROLL_LOCK;
            case KEY_NUMLOCK: return GLFW.GLFW_KEY_NUM_LOCK;
            case KEY_SYSRQ: return GLFW.GLFW_KEY_PRINT_SCREEN;
            case KEY_PAUSE: return GLFW.GLFW_KEY_PAUSE;
            case KEY_F1: return GLFW.GLFW_KEY_F1;
            case KEY_F2: return GLFW.GLFW_KEY_F2;
            case KEY_F3: return GLFW.GLFW_KEY_F3;
            case KEY_F4: return GLFW.GLFW_KEY_F4;
            case KEY_F5: return GLFW.GLFW_KEY_F5;
            case KEY_F6: return GLFW.GLFW_KEY_F6;
            case KEY_F7: return GLFW.GLFW_KEY_F7;
            case KEY_F8: return GLFW.GLFW_KEY_F8;
            case KEY_F9: return GLFW.GLFW_KEY_F9;
            case KEY_F10: return GLFW.GLFW_KEY_F10;
            case KEY_F11: return GLFW.GLFW_KEY_F11;
            case KEY_F12: return GLFW.GLFW_KEY_F12;
            case KEY_F13: return GLFW.GLFW_KEY_F13;
            case KEY_F14: return GLFW.GLFW_KEY_F14;
            case KEY_F15: return GLFW.GLFW_KEY_F15;
            case KEY_NUMPAD0: return GLFW.GLFW_KEY_KP_0;
            case KEY_NUMPAD1: return GLFW.GLFW_KEY_KP_1;
            case KEY_NUMPAD2: return GLFW.GLFW_KEY_KP_2;
            case KEY_NUMPAD3: return GLFW.GLFW_KEY_KP_3;
            case KEY_NUMPAD4: return GLFW.GLFW_KEY_KP_4;
            case KEY_NUMPAD5: return GLFW.GLFW_KEY_KP_5;
            case KEY_NUMPAD6: return GLFW.GLFW_KEY_KP_6;
            case KEY_NUMPAD7: return GLFW.GLFW_KEY_KP_7;
            case KEY_NUMPAD8: return GLFW.GLFW_KEY_KP_8;
            case KEY_NUMPAD9: return GLFW.GLFW_KEY_KP_9;
            case KEY_DECIMAL: return GLFW.GLFW_KEY_KP_DECIMAL;
            case KEY_DIVIDE: return GLFW.GLFW_KEY_KP_DIVIDE;
            case KEY_MULTIPLY: return GLFW.GLFW_KEY_KP_MULTIPLY;
            case KEY_SUBTRACT: return GLFW.GLFW_KEY_KP_SUBTRACT;
            case KEY_ADD: return GLFW.GLFW_KEY_KP_ADD;
            case KEY_NUMPADENTER: return GLFW.GLFW_KEY_KP_ENTER;
            case KEY_NUMPADEQUALS: return GLFW.GLFW_KEY_KP_EQUAL;
            case KEY_LSHIFT: return GLFW.GLFW_KEY_LEFT_SHIFT;
            case KEY_LCONTROL: return GLFW.GLFW_KEY_LEFT_CONTROL;
            case KEY_LMENU: return GLFW.GLFW_KEY_LEFT_ALT;
            case KEY_LMETA: return GLFW.GLFW_KEY_LEFT_SUPER;
            case KEY_RSHIFT: return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case KEY_RCONTROL: return GLFW.GLFW_KEY_RIGHT_CONTROL;
            case KEY_RMENU: return GLFW.GLFW_KEY_RIGHT_ALT;
            case KEY_RMETA: return GLFW.GLFW_KEY_RIGHT_SUPER;
            default: return GLFW.GLFW_KEY_UNKNOWN;
        }
    }

    /**
     * Returns the name for a key code.
     */
    public static String getKeyName (int key)
    {
        int glfwKey = lwjgl2ToGlfw(key);
        if (glfwKey != GLFW.GLFW_KEY_UNKNOWN) {
            String name = GLFW.glfwGetKeyName(glfwKey, 0);
            if (name != null) {
                return name;
            }
        }
        return "Unknown(" + key + ")";
    }
}
