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


    @Test
    public void encode() {
        String msg = "12567" + "," + "game";

        int packLength = Integer.MAX_VALUE;
        byte[] message = new byte[4 + 2 + 2 + 4 + 4];

        int offset = BruteForceCoding.encodeIntBigEndian(message, packLength, 0, 4 * BruteForceCoding.BSIZE);

    }
}
