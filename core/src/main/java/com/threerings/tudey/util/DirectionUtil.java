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

package com.threerings.tudey.util;

import com.threerings.math.Vector2f;

/**
 * Utilities related to direction restrictions for movement translations.
 */
public class DirectionUtil
{
    /**
     * Alters a step vector based on direction restrictions.
     *
     * @return true if the step is non-zero
     */
    public static boolean alterStep (Vector2f step, int directions)
    {
        if (directions == 0) {
            return true;
        }
        for (int ii = 0; ii < 4; ii++) {
            if ((directions & (1 << ii)) != 0) {
                if (step.dot(Direction.CARDINAL_VALUES[ii].getVector2f()) < 0) {
                    Vector2f vec = Direction.CARDINAL_VALUES[(ii + 3) % 4].getVector2f();
                    float dot = step.dot(vec);
                    if (dot > 0) {
                        step.set(vec.mult(dot));
                        continue;
                    }
                    vec = Direction.CARDINAL_VALUES[(ii + 1) % 4].getVector2f();
                    dot = step.dot(vec);
                    if (dot > 0) {
                        step.set(vec.mult(dot));
                        continue;
                    }
                    step.set(0f, 0f);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Rotates the direction flags by the angle (snapping to the nearest cardinal direction).
     */
    public static int rotateDirections (int directions, float angle)
    {
        if (directions == 0) {
            return 0;
        }
        Vector2f vec = Vector2f.UNIT_Y.rotate(angle);
        int offset = 0;
        float minDist = Float.POSITIVE_INFINITY;
        for (int ii = 0; ii < 4; ii++) {
            float dist = Direction.CARDINAL_VALUES[ii].getVector2f().distanceSquared(vec);
            if (dist < minDist) {
                minDist = dist;
                offset = ii;
            }
        }
        if (offset != 0) {
            return rotateCardinal(directions, offset);
        }
        return directions;
    }

    /**
     * Rotates the direction flags by the cardinal direction.
     */
    public static int rotateCardinal (int directions, int rotation)
    {
        int cardinal = directions & 0xf;
        int rotCardinal = ((cardinal << rotation) | (cardinal >> (4 - rotation))) & 0xf;
        directions = directions & ~0xf | rotCardinal;
        return directions;
    }
}
