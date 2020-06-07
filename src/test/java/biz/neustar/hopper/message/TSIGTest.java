package biz.neustar.hopper.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import biz.neustar.hopper.record.OPTRecord;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.resolver.SimpleResolver;

public class TSIGTest {
    @Test
    public void TSIG_query() throws IOException {
        TSIG key = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

        Name qname = Name.fromString("www.example.");
        Record rec = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(rec);
        msg.setTSIG(key, Rcode.NOERROR, null);
        byte[] bytes = msg.toWire(512);
        assertEquals(bytes[11], 1);

        Message parsed = new Message(bytes);
        int result = key.verify(parsed, bytes, null);
        assertEquals(result, Rcode.NOERROR);
        assertTrue(parsed.isSigned());
    }

    @Test
    public void TSIG_queryIsLastAddMessageRecord() throws IOException {
        TSIG key = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

        Name qname = Name.fromString("www.example.");
        Record rec = Record.newRecord(qname, Type.A, DClass.IN);
        OPTRecord opt = new OPTRecord(SimpleResolver.DEFAULT_EDNS_PAYLOADSIZE, 0, 0, 0);
        Message msg = Message.newQuery(rec);
        msg.setTSIG(key, Rcode.NOERROR, null);
        msg.addRecord(opt, Section.ADDITIONAL);
        byte[] bytes = msg.toWire(512);
        assertEquals(bytes[11], 2); // additional RR count, lower byte

        Message parsed = new Message(bytes);
        Record[] additionalSection = parsed.getSectionArray(Section.ADDITIONAL);
        assertEquals(Type.string(Type.OPT), Type.string(additionalSection[0].getType()));
        assertEquals(Type.string(Type.TSIG), Type.string(additionalSection[1].getType()));
        int result = key.verify(parsed, bytes, null);
        assertEquals(result, Rcode.NOERROR);
        assertTrue(parsed.isSigned());
    }

    @Test
    public void TSIG_response() throws IOException {
        TSIG key = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

        Name qname = Name.fromString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(key, Rcode.NOERROR, null);
        byte[] qbytes = query.toWire();
        Message qparsed = new Message(qbytes);

        Message response = new Message(query.getHeader().getID());
        response.setTSIG(key, Rcode.NOERROR, qparsed.getTSIG());
        response.getHeader().setFlag(Flag.QR);
        response.addRecord(question, Section.QUESTION);
        Record answer = Record.fromString(qname, Type.A, DClass.IN, 300, "1.2.3.4", null);
        response.addRecord(answer, Section.ANSWER);
        byte[] bytes = response.toWire(512);

        Message parsed = new Message(bytes);
        int result = key.verify(parsed, bytes, qparsed.getTSIG());
        assertEquals(result, Rcode.NOERROR);
        assertTrue(parsed.isSigned());
    }

    @Test
    public void TSIG_truncated() throws IOException {
        TSIG key = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

        Name qname = Name.fromString("www.example.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);
        query.setTSIG(key, Rcode.NOERROR, null);
        byte[] qbytes = query.toWire();
        Message qparsed = new Message(qbytes);

        Message response = new Message(query.getHeader().getID());
        response.setTSIG(key, Rcode.NOERROR, qparsed.getTSIG());
        response.getHeader().setFlag(Flag.QR);
        response.addRecord(question, Section.QUESTION);
        for (int i = 0; i < 40; i++) {
            Record answer = Record.fromString(qname, Type.TXT, DClass.IN, 300, "foo" + i, null);
            response.addRecord(answer, Section.ANSWER);
        }
        byte[] bytes = response.toWire(512);

        Message parsed = new Message(bytes);
        assertTrue(parsed.getHeader().getFlag(Flag.TC.getValue()));
        int result = key.verify(parsed, bytes, qparsed.getTSIG());
        assertEquals(result, Rcode.NOERROR);
        assertTrue(parsed.isSigned());
    }

}
