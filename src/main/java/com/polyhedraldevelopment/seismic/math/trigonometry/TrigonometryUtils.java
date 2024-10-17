package com.polyhedraldevelopment.seismic.math.trigonometry;

import com.polyhedraldevelopment.seismic.math.floatingpoint.FloatingPointFunctions;

import java.util.random.RandomGenerator;


class TrigonometryUtils {
    private static final int lookupBits = 14;

    static final int lookupTableSize = 1 << TrigonometryUtils.lookupBits;

    private static final int lookupTableSizeWithMargin = TrigonometryUtils.lookupTableSize + 1;
    private static final float tauOverLookupSize = (float) (TrigonometryConstants.TAU / TrigonometryUtils.lookupTableSize);
    static final double radianToIndex = (~(-1 << TrigonometryUtils.lookupBits) + 1) / TrigonometryConstants.TAU;
    private static final int[] sinTable;

    static {
        sinTable = new int[TrigonometryUtils.lookupTableSizeWithMargin];
        for(int i = 0; i < TrigonometryUtils.lookupTableSizeWithMargin; i++) {
            double d = i * TrigonometryUtils.tauOverLookupSize;
            TrigonometryUtils.sinTable[i] = Float.floatToRawIntBits((float) StrictMath.sin(d));
        }

        // Four cardinal directions (credits: Nate)
        for(int i = 0; i < 360; i += 90) {
            double rad = Math.toRadians(i);
            TrigonometryUtils.sinTable[(int) (rad * TrigonometryUtils.radianToIndex) & 0xFFFF] = Float.floatToRawIntBits((float) StrictMath.sin(rad));
        }

        // Test that the lookup table is correct during runtime
        RandomGenerator random = RandomGenerator.getDefault();
        for(int i = 0; i < TrigonometryUtils.lookupTableSizeWithMargin; i++) {
            double d = -1 + 2.0 * random.nextDouble(); // Generate a random value between -1 and 1
            double expected = TrigonometryFunctions.sin(d);
            double value = StrictMath.sin(d);

            if(!FloatingPointFunctions.equals(expected, value, 0.001)) {
                throw new IllegalArgumentException(String.format("LUT error at value %f (expected: %s, found: %s)", Double.valueOf(d),
                    Double.valueOf(expected), Double.valueOf(value)));
            }
        }

        for(int i = 0; i < 360; i += 90) {
            double rad = Math.toRadians(i);
            double expected = TrigonometryFunctions.sin(rad);
            double value = StrictMath.sin(rad);

            if(!FloatingPointFunctions.equals(expected, value)) {
                throw new IllegalArgumentException(
                    String.format("LUT error at cardinal direction %s (expected: %s, found: %s)", Integer.valueOf(i), Double.valueOf(expected), Double.valueOf(value)));
            }
        }
    }

    // Seismic is a double precision library, however, the sin table is a lut, which needs to be compact
    // for the best chance of fitting in the CPU cache. For this reason the sin table is stored as float.
    static double sinLookup(int index) {
        // Trigonometric identity: sin(-x) = -sin(x)
        // Given a domain of 0 <= x <= 2*pi, just negate the value if x > pi.
        // This allows the sin table size to be halved.
        int neg = (index & 0x8000) << 16;

        // All bits set if (pi/2 <= x), none set otherwise
        // Extracts the 15th bit from 'half'
        int mask = (index << 17) >> 31;

        // Trigonometric identity: sin(x) = sin(pi/2 - x)
        int pos = (0x8001 & mask) + (index ^ mask);

        // Wrap the position in the table. Moving this down to immediately before the array access
        // seems to help the Hotspot compiler optimize the bit math better.
        pos &= 0x7fff;

        // Fetch the corresponding value from the LUT and invert the sign bit as needed
        // This directly manipulate the sign bit on the float bits to simplify logic
        return Float.intBitsToFloat(TrigonometryUtils.sinTable[pos] ^ neg);
    }
}
