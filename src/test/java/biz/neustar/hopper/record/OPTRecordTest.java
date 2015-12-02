package biz.neustar.hopper.record;

import java.util.List;
import junit.framework.TestCase;
import org.junit.Assert;
import biz.neustar.hopper.message.EDNSOption;
import biz.neustar.hopper.message.EDNSOption.Code;
import biz.neustar.hopper.message.GenericEDNSOption;
import biz.neustar.hopper.message.Section;

import com.google.common.collect.Lists;

/**
 * Test @{OPTRecord}.
 */
public class OPTRecordTest extends TestCase {

    /**
     * Test for OPTRecord.
     * 
     * @throws Exception
     *             the exception
     */
    public void testOPTRecord() throws Exception {

        byte[] expectedWireFormat = { 0x00, 0x00, 0x29, 0x10, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00 };
        String expectedOPTRecordString = " ; payload 4096, xrcode 0, version 0, flags 0";

        // Create OPT record.
        OPTRecord optRecord = new OPTRecord(4096, 0, 0);
        assertEquals(0, optRecord.getExtendedRcode());
        assertEquals(4096, optRecord.getPayloadSize());
        assertEquals(0, optRecord.getVersion());

        // Convert OPT record to wired format
        byte[] wireFormatCanonical = optRecord.toWireCanonical();
        Assert.assertArrayEquals(expectedWireFormat, wireFormatCanonical);

        byte[] wireFormat = optRecord.toWire(Section.ADDITIONAL);
        Assert.assertArrayEquals(expectedWireFormat, wireFormat);

        // Convert OPT record from wired format
        Record rec = Record.fromWire(expectedWireFormat, Section.ADDITIONAL);
        assertEquals(rec, optRecord);

        // Convert OPT record to string.
        String optRecordString = optRecord.rrToString();
        assertEquals(expectedOPTRecordString, optRecordString);

        byte[] expectedWireFormatwithFalg = { 0x00, 0x00, 0x29, 0x10, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x00 };
        expectedOPTRecordString = " ; payload 4096, xrcode 0, version 0, flags 1";

        // Create OPT record with flag
        optRecord = new OPTRecord(4096, 0, 0, 1);
        assertEquals(1, optRecord.getFlags());
        assertTrue(optRecord.getOptions().isEmpty());
        assertTrue(optRecord.getOptions(3).isEmpty());

        // Convert OPT record to wired format
        wireFormatCanonical = optRecord.toWireCanonical();
        Assert.assertArrayEquals(expectedWireFormatwithFalg, wireFormatCanonical);

        wireFormat = optRecord.toWire(Section.ADDITIONAL);
        Assert.assertArrayEquals(expectedWireFormatwithFalg, wireFormat);

        // Convert OPT record from wired format
        rec = Record.fromWire(expectedWireFormatwithFalg, Section.ADDITIONAL);
        Assert.assertEquals(rec, optRecord);

        // Convert OPT record to string.
        optRecordString = optRecord.rrToString();
        assertEquals(expectedOPTRecordString, optRecordString);

        byte[] expectedWireFormatwithOptions = { 0x00, 0x00, 0x29, 0x10, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x04, 0x00, 0x03, 0x00, 0x00 };
        expectedOPTRecordString = "[{NSID: <>}]  ; payload 4096, xrcode 0, version 0, flags 1";

        // Create OPT record with options.
        List<EDNSOption> options = Lists.newArrayList();
        EDNSOption option = new GenericEDNSOption(Code.NSID, new byte[0]);
        options.add(option);
        optRecord = new OPTRecord(4096, 0, 0, 1, options);
        assertEquals(options, optRecord.getOptions());
        assertEquals(options, optRecord.getOptions(3));
        assertTrue(optRecord.getOptions(0).isEmpty());

        // Convert OPT record to wired format
        wireFormatCanonical = optRecord.toWireCanonical();
        Assert.assertArrayEquals(expectedWireFormatwithOptions, wireFormatCanonical);

        wireFormat = optRecord.toWire(Section.ADDITIONAL);
        Assert.assertArrayEquals(expectedWireFormatwithOptions, wireFormat);

        // Convert OPT record from wired format
        rec = Record.fromWire(expectedWireFormatwithOptions, Section.ADDITIONAL);
        Assert.assertEquals(rec, optRecord);

        // Convert OPT record to string.
        optRecordString = optRecord.rrToString();
        assertEquals(expectedOPTRecordString, optRecordString);
    }
}
