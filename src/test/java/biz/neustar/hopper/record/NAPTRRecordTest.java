package biz.neustar.hopper.record;

import static org.junit.Assert.*;

import org.junit.Test;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;

public class NAPTRRecordTest {

    @Test
    public void test_ctor_5arg() throws TextParseException {

        NAPTRRecord naptrRecord = new biz.neustar.hopper.record.NAPTRRecord(
                Name.fromString("www.naptr-test.com."), DClass.IN, 100, 101, 10,
                String.valueOf(210), "SIP+D2U","", new Name("."));
        assertEquals(Name.fromString("www.naptr-test.com."), naptrRecord.getName());
        assertEquals(Type.NAPTR, naptrRecord.getType());
        assertEquals(DClass.IN, naptrRecord.getDClass());
        assertEquals(100, naptrRecord.getTTL());
        assertEquals(101, naptrRecord.getOrder());
        assertEquals(10, naptrRecord.getPreference());
        assertEquals("", naptrRecord.getRegexp());
        assertEquals(new Name("."), naptrRecord.getReplacement());
        assertEquals("210", naptrRecord.getFlags());
        assertEquals("SIP+D2U", naptrRecord.getService());

        // It should not throw NPE
        naptrRecord.toWireCanonical();

        // NAPTR record with null regexp, flags and replacement
        naptrRecord = new biz.neustar.hopper.record.NAPTRRecord(
                Name.fromString("www.naptr-test.com."), DClass.IN, 100, 101, 10,
                null, "SIP+D2U", null, null);
        assertEquals(Name.fromString("www.naptr-test.com."), naptrRecord.getName());
        assertEquals(Type.NAPTR, naptrRecord.getType());
        assertEquals(DClass.IN, naptrRecord.getDClass());
        assertEquals(100, naptrRecord.getTTL());
        assertEquals(101, naptrRecord.getOrder());
        assertEquals(10, naptrRecord.getPreference());
        assertEquals("", naptrRecord.getRegexp());
        assertEquals(new Name("."), naptrRecord.getReplacement());
        assertEquals("", naptrRecord.getFlags());
        assertEquals("SIP+D2U", naptrRecord.getService());

        // It should not throw NPE
        naptrRecord.toWireCanonical();

        // NAPTR with legal values
        naptrRecord = new biz.neustar.hopper.record.NAPTRRecord(
                Name.fromString("www.naptr-test.com."), DClass.IN, 100, 101, 10,
                "220", "SIP+D2U", "*", new Name("_udp.naptr-test.com."));
        assertEquals(Name.fromString("www.naptr-test.com."), naptrRecord.getName());
        assertEquals(Type.NAPTR, naptrRecord.getType());
        assertEquals(DClass.IN, naptrRecord.getDClass());
        assertEquals(100, naptrRecord.getTTL());
        assertEquals(101, naptrRecord.getOrder());
        assertEquals(10, naptrRecord.getPreference());
        assertEquals("*", naptrRecord.getRegexp());
        assertEquals(new Name("_udp.naptr-test.com."), naptrRecord.getReplacement());
        assertEquals("220", naptrRecord.getFlags());
        assertEquals("SIP+D2U", naptrRecord.getService());

        // It should not throw NPE
        naptrRecord.toWireCanonical();
    }

}
