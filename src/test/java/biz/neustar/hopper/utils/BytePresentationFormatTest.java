/**
 * Copyright 2000-2015 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.utils;

import static org.junit.Assert.*;
import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.util.BytePresentationFormat;
import junit.framework.TestCase;

public class BytePresentationFormatTest extends TestCase {

    public void test_0() throws TextParseException {

        byte[] expectedOctets = { 99, 97, 46, 101, 120, 97, 109, 112, 108, 101, 46, 110, 101,
                116, 59, 32, 112, 111, 108, 105, 99, 121, 61, 101, 118 };

        String pf = "ca.example.net; policy=ev";
        byte[] octets = BytePresentationFormat.toByteArray(pf);
        assertArrayEquals(expectedOctets, octets);
        assertEquals(BytePresentationFormat.toPresentationString(octets), pf);
    }

    public void test_1() throws TextParseException {

        String pf = "ca.example.net;\\031policy=ev";
        byte[] expectedOctets = { 99, 97, 46, 101, 120, 97, 109, 112, 108, 101, 46, 110, 101,
                116, 59, 31, 112, 111, 108, 105, 99, 121, 61, 101, 118 };

        byte[] octets = BytePresentationFormat.toByteArray(pf);
        assertArrayEquals(expectedOctets, octets);
        assertEquals(BytePresentationFormat.toPresentationString(octets), pf);
    }

    public void test_2() throws TextParseException {

        String pf = "ca.\\127example.net;\\031policy=ev";
        byte[] expectedOctets = { 99, 97, 46, 127, 101, 120, 97, 109, 112, 108, 101, 46, 110, 101,
                116, 59, 31, 112, 111, 108, 105, 99, 121, 61, 101, 118 };

        byte[] octets = BytePresentationFormat.toByteArray(pf);
        assertArrayEquals(expectedOctets, octets);
        assertEquals(BytePresentationFormat.toPresentationString(octets), pf);
    }

}
