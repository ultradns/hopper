/*
 * User: jonbodner
 * Date: 10/27/2014
 * Time: 5:59PM
 *
 * Copyright 2000-2014 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */
package biz.neustar.hopper.record.impl;

import biz.neustar.hopper.exception.RelativeNameException;
import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.SPFRecord;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class SPFRecordTest extends TestCase {
    Name m_an, m_rn;
    long m_ttl;
    String firstString="I am the first string";
    byte[] firstStringBytes = new byte[] {
        21, 73, 32, 97, 109, 32, 116, 104, 101, 32, 102, 105, 114, 115, 116, 32, 115, 116, 114, 105, 110, 103};
    String secondString="I am the second string";
    List<String> bothStrings = Arrays.asList(firstString, secondString);

    protected void setUp() throws TextParseException, UnknownHostException {
        m_an = Name.fromString("My.Absolute.Name.");
        m_rn = Name.fromString("My.Relative.Name");
        m_ttl = 0x13579;
    }

    public void test_ctor_0arg() throws UnknownHostException {
        SPFRecord ar = new SPFRecord();
        Assert.assertNull(ar.getName());
        Assert.assertEquals(0, ar.getType());
        Assert.assertNull(ar.getDClass());
        Assert.assertEquals(0, ar.getTTL());
        List<String> strings = ar.getStrings();
        Assert.assertEquals(0, strings.size());
    }

    public void test_ctor_4arg() {
        SPFRecord ar = new SPFRecord(m_an, DClass.IN, m_ttl, bothStrings);
        Assert.assertEquals(m_an, ar.getName());
        Assert.assertEquals(Type.SPF, ar.getType());
        Assert.assertEquals(DClass.IN, ar.getDClass());
        Assert.assertEquals(m_ttl, ar.getTTL());
        Assert.assertEquals(bothStrings, ar.getStrings());

        // a relative name
        try {
            new SPFRecord(m_rn, DClass.IN, m_ttl, bothStrings);
            Assert.fail("RelativeNameException not thrown");
        } catch (RelativeNameException e) {
        }

    }

    public void test_rrFromWire() throws IOException {
        DNSInput di = new DNSInput(firstStringBytes);
        SPFRecord ar = new SPFRecord();

        ar.rrFromWire(di);
        assertEquals(firstString, ar.getStrings().get(0));
    }

    public void test_rrToString() {
        SPFRecord ar = new SPFRecord(m_an, DClass.IN, m_ttl, bothStrings);
        Assert.assertEquals("\"" + firstString + "\" \"" + secondString + "\"", ar.rrToString());
    }

    public void test_rrToStringNull() {
        SPFRecord ar = new SPFRecord(m_an, DClass.IN, m_ttl, (String)null);
        Assert.assertEquals("\"\"", ar.rrToString());
    }

    public void test_rrToStringEmptyString() {
        SPFRecord ar = new SPFRecord(m_an, DClass.IN, m_ttl, "");
        Assert.assertEquals("\"\"", ar.rrToString());
    }

    public void test_rrToWire() {
        SPFRecord ar = new SPFRecord(m_an, DClass.IN, m_ttl, firstString);
        DNSOutput dout = new DNSOutput();

        ar.rrToWire(dout, null, true);
        Assert.assertTrue(Arrays.equals(firstStringBytes, dout.toByteArray()));

        dout = new DNSOutput();
        ar.rrToWire(dout, null, false);
        Assert.assertTrue(Arrays.equals(firstStringBytes, dout.toByteArray()));
    }

    public void test_rrToWireNull() {
        SPFRecord ar = new SPFRecord(m_an, DClass.IN, m_ttl, (String)null);
        DNSOutput dout = new DNSOutput();

        ar.rrToWire(dout, null, true);
        Assert.assertTrue(Arrays.equals(new byte[]{0}, dout.toByteArray()));

        dout = new DNSOutput();
        ar.rrToWire(dout, null, false);
        Assert.assertTrue(Arrays.equals(new byte[]{0}, dout.toByteArray()));
    }

    public void test_rrToWireEmptyString() {
        SPFRecord ar = new SPFRecord(m_an, DClass.IN, m_ttl, "");
        DNSOutput dout = new DNSOutput();

        ar.rrToWire(dout, null, true);
        Assert.assertTrue(Arrays.equals(new byte[]{0}, dout.toByteArray()));

        dout = new DNSOutput();
        ar.rrToWire(dout, null, false);
        Assert.assertTrue(Arrays.equals(new byte[]{0}, dout.toByteArray()));
    }
}
