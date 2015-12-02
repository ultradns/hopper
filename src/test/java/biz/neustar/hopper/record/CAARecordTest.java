/**
 * Copyright 2000-2015 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.record;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.util.Tokenizer;
import junit.framework.TestCase;

public class CAARecordTest extends TestCase {


    public void test_constructor() throws TextParseException {

        byte[] expectedWireFormat = {0, 5, 105, 115, 115, 117, 101, 99, 97, 46, 101,
                120, 97, 109, 112, 108, 101, 46, 110, 101, 116, 59, 32, 112,
                111, 108, 105, 99, 121, 61, 101, 118};


        byte flags = 0;
        String tag = "issue";
        String value = "\"ca.example.net; policy=ev\"";
        CAARecord caaRecord = new CAARecord(new Name("example.com."), DClass.IN, 120L, flags, tag, value);
        assertEquals(0, caaRecord.getFlags());
        assertEquals(tag, caaRecord.getTag());
        assertEquals(value, caaRecord.getValuePresentationFormat());

        byte[] wireFormat = caaRecord.rdataToWireCanonical();
        Assert.assertArrayEquals(expectedWireFormat, wireFormat);
    }

    public void test_tokenizer() throws IOException {

        byte[] expectedWireFormat = { (byte) 128, 3, 116, 98, 115, 85, 110, 107, 110, 111, 119, 110};
        String rdata = "128 tbs \"Unknown\"";
        CAARecord caaRecord = new CAARecord();
        caaRecord.rdataFromString(new Tokenizer(rdata), null);
        byte[] wireFormat = caaRecord.rdataToWireCanonical();
        Assert.assertArrayEquals(expectedWireFormat, wireFormat);
    }

    public void test_rrwirein() throws IOException {

        byte[] inWireBytes = {(byte) 129,
                5, 105, 111, 100, 101, 102,       // iodef
                109, 97, 105, 108, 116, 111, 58, 115, 101, 99, 117, 114, 105, 116, 121,
                64, 101, 120, 97, 109, 112, 108, 101, 46, 99, 111, 109};
        int flags = 129;
        String tag = "iodef";
        String value = "\"mailto:security@example.com\"";
        CAARecord caaRecord = new CAARecord();
        caaRecord.rrFromWire(new DNSInput(inWireBytes));
        Assert.assertEquals(flags, caaRecord.getFlags());
        assertEquals(tag, caaRecord.getTag());
        assertEquals(value, caaRecord.getValuePresentationFormat());
        byte[] wireFormat = caaRecord.rdataToWireCanonical();
        Assert.assertArrayEquals(inWireBytes, wireFormat);
    }

}
