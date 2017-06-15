package com.youga.goim;

import org.junit.Test;

import static com.youga.goim.BruteForceCoding.BSIZE;
import static com.youga.goim.BruteForceCoding.SSIZE;
import static com.youga.goim.BruteForceCoding.ISIZE;
import static com.youga.goim.BruteForceCoding.LSIZE;

/**
 * Created by Youga on 2017/6/15.
 */

public class BruteForceCodingTest {

    private static byte byteVal = 101; // one hundred and one
    private static short shortVal = 10001; // ten thousand and one
    private static int intVal = 100000001; // one hundred million and one
    private static long longVal = 1000000000001L;// one trillion and one

    @Test
    public void encode(){

        byte[] message = new byte[BSIZE + SSIZE + ISIZE + LSIZE];
        // Encode the fields in the target byte array
        int offset = BruteForceCoding.encodeIntBigEndian(message, byteVal, 0, BSIZE);
        offset = BruteForceCoding.encodeIntBigEndian(message, shortVal, offset, SSIZE);
        offset = BruteForceCoding.encodeIntBigEndian(message, intVal, offset, ISIZE);
        BruteForceCoding.encodeIntBigEndian(message, longVal, offset, LSIZE);
        System.out.println("Encoded message: " + BruteForceCoding.byteArrayToDecimalString(message));

        // Decode several fields
        long value = BruteForceCoding.decodeIntBigEndian(message, BSIZE, SSIZE);
        System.out.println("Decoded short = " + value);
        value = BruteForceCoding.decodeIntBigEndian(message, BSIZE + SSIZE + ISIZE, LSIZE);
        System.out.println("Decoded long = " + value);

        // Demonstrate dangers of conversion
        offset = 4;
        value = BruteForceCoding.decodeIntBigEndian(message, offset, BSIZE);
        System.out.println("Decoded value (offset " + offset + ", size " + BSIZE + ") = " + value);
        byte bVal = (byte) BruteForceCoding.decodeIntBigEndian(message, offset, BSIZE);
        System.out.println("Same value as byte = " + bVal);
    }
}
