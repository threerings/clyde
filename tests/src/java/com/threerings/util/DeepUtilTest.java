//
// $Id$

package com.threerings.util;

import java.util.Arrays;

import junit.framework.TestCase;

import com.samskivert.util.RandomUtil;

/**
 * Tests the {@link DeepUtil} methods.
 */
public class DeepUtilTest extends TestCase
{
    public DeepUtilTest (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    public void setUp ()
    {
        // use a fixed seed so that our results are reproducible
        RandomUtil.rand.setSeed(1199325877849L);
    }

    public void testCopy ()
    {
        // make sure it works for some wrappers and arrays.  the bounds on the lengths of the
        // arrays (here, in testEquals, and in Child.randomize) are totally arbitrary.  i chose
        // 5-10 and 10-20 for roundness, but used RandomUtil.getInt wrong and ended up with
        // 6-9 and 11-19.  now i have fixed the lengths that were, again, completely arbitrary
        Integer v1 = RandomUtil.rand.nextInt();
        assertEquals(DeepUtil.copy(v1), v1);
        float[][] v2 = new float[RandomUtil.getInRange(5, 11)][RandomUtil.getInRange(5, 11)];
        for (int ii = 0; ii < v2.length; ii++) {
            for (int jj = 0; jj < v2[ii].length; jj++) {
                v2[ii][jj] = RandomUtil.rand.nextFloat();
            }
        }
        assertEquals(true, Arrays.deepEquals(v2, DeepUtil.copy(v2)));
        float[][] v3 = new float[v2.length][v2[0].length];
        assertSame(v3, DeepUtil.copy(v2, v3));
        assertEquals(true, Arrays.deepEquals(v2, v3));

        // try it with some objects
        Child c1 = new Child(), c2 = new Child();
        c1.randomize();
        assertSame(c2, DeepUtil.copy(c1, c2));
        assertEquals(c1, c2);
        c2.randomize();
        assertEquals(DeepUtil.copy(c2), c2);
    }

    public void testEquals ()
    {
        // make sure it works for some wrappers and arrays
        Integer v1 = RandomUtil.rand.nextInt(), v2 = RandomUtil.rand.nextInt();
        assertEquals(false, DeepUtil.equals(null, v1));
        assertEquals(false, DeepUtil.equals(v1, null));
        assertEquals(true, DeepUtil.equals(v1, v1));
        assertEquals(DeepUtil.equals(v1, v2), v1.equals(v2));
        Float v3 = RandomUtil.rand.nextFloat();
        assertEquals(false, DeepUtil.equals(v1, v3));
        byte[] v4 = new byte[RandomUtil.getInRange(5, 11)];
        for (int ii = 0; ii < v4.length; ii++) {
            v4[ii] = (byte)RandomUtil.rand.nextInt();
        }
        assertEquals(true, DeepUtil.equals(v4, v4));
        assertEquals(true, DeepUtil.equals(v4, v4.clone()));
        assertEquals(false, DeepUtil.equals(v4, new byte[5]));

        // try it with some objects
        Child c1 = new Child(), c2 = new Child();
        assertEquals(true, DeepUtil.equals(c1, c2));
        c1.randomize();
        c2.set(c1);
        assertEquals(true, DeepUtil.equals(c1, c2));
        c2.randomize();
        assertEquals(false, DeepUtil.equals(c1, c2));
    }

    public void testHashCode ()
    {
        // make sure it works for some wrappers and arrays
        Integer v1 = RandomUtil.rand.nextInt();
        assertEquals(DeepUtil.hashCode(v1), v1.hashCode());
        Float v2 = RandomUtil.rand.nextFloat();
        assertEquals(DeepUtil.hashCode(v2), v2.hashCode());
        double[][] v3 = new double[10][10];
        for (int ii = 0; ii < v3.length; ii++) {
            for (int jj = 0; jj < v3[ii].length; jj++) {
                v3[ii][jj] = RandomUtil.rand.nextDouble();
            }
        }
        assertEquals(DeepUtil.hashCode(v3), Arrays.deepHashCode(v3));

        // make sure the hash values for equal objects are equal
        Child c1 = new Child(), c2 = new Child();
        assertEquals(DeepUtil.hashCode(c1), DeepUtil.hashCode(c2));
        c1.randomize();
        c2.set(c1);
        assertEquals(DeepUtil.hashCode(c1), DeepUtil.hashCode(c2));

        // make sure the hash values for different objects are different
        Child c3 = new Child();
        c3.randomize();
        assertEquals(false, DeepUtil.hashCode(c1) == DeepUtil.hashCode(c3));
        c2.v2 = !c2.v2;
        assertEquals(false, DeepUtil.hashCode(c1) == DeepUtil.hashCode(c2));
    }

    protected abstract class Parent
    {
        public byte v1;
    }

    protected class Child extends Parent
    {
        public boolean v2;
        public Object[][] v3;

        public void randomize ()
        {
            v1 = (byte)RandomUtil.rand.nextInt();
            v2 = RandomUtil.rand.nextBoolean();
            v3 = new Object[RandomUtil.getInRange(10, 21)][];
            for (int ii = 0; ii < v3.length; ii++) {
                if (RandomUtil.rand.nextBoolean()) {
                    continue;
                }
                v3[ii] = new Object[RandomUtil.getInRange(10, 21)];
                for (int jj = 0; jj < v3[ii].length; jj++) {
                    if (RandomUtil.rand.nextBoolean()) {
                        continue;
                    }
                    switch (RandomUtil.getInt(3)) {
                        case 0:
                            v3[ii][jj] = RandomUtil.rand.nextDouble();
                            break;
                        case 1:
                            v3[ii][jj] = (short)RandomUtil.rand.nextInt();
                            break;
                        case 2:
                            Other other = new Other();
                            other.randomize();
                            v3[ii][jj] = other;
                            break;
                    }
                }
            }
        }

        public void set (Child other)
        {
            v1 = other.v1;
            v2 = other.v2;
            v3 = other.v3;
        }

        public boolean equals (Object other)
        {
            if (!(other instanceof Child)) {
                return false;
            }
            Child ochild = (Child)other;
            return v1 == ochild.v1 && v2 == ochild.v2 && Arrays.deepEquals(v3, ochild.v3);
        }
    }

    protected static class Other
    {
        public float v1;
        public long v2;

        public void randomize ()
        {
            v1 = RandomUtil.rand.nextFloat();
            v2 = RandomUtil.rand.nextLong();
        }

        public void set (Other other)
        {
            v1 = other.v1;
            v2 = other.v2;
        }

        public boolean equals (Object other)
        {
            if (!(other instanceof Other)) {
                return false;
            }
            Other oother = (Other)other;
            return v1 == oother.v1 && v2 == oother.v2;
        }
    }
}
