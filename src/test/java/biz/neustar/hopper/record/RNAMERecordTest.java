/**
 * Copyright 2000-2015 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.record;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import junit.framework.TestCase;

public class RNAMERecordTest extends TestCase {
    public void test_ctor_0arg() {
        RNAMERecord d = new RNAMERecord();
        assertNull(d.getName());
        assertNull(d.getTarget());
    }

    public void test_ctor_4arg() throws TextParseException {
        Name n = Name.fromString("my.name.");
        Name t = Name.fromString("my.target.");

        RNAMERecord d = new RNAMERecord(n, DClass.IN, 0xABCDEL, t);
        assertEquals(n, d.getName());
        assertEquals(Type.RNAME, d.getType());
        assertEquals(DClass.IN, d.getDClass());
        assertEquals(0xABCDEL, d.getTTL());
        assertEquals(t, d.getTarget());
    }

    public void test_getObject() {
        RNAMERecord d = new RNAMERecord();
        Record r = d.getObject();
        assertTrue(r instanceof RNAMERecord);
    }
}
