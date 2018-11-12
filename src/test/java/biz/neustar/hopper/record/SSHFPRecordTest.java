package biz.neustar.hopper.record;

import static org.junit.Assert.*;

import org.junit.Test;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;

public class SSHFPRecordTest {

    @Test
    public void testSshfp() throws TextParseException {

        String fp = "a87f1b687ac0e57d2a081a2f282672334d90ed316d2b818ca9580ea384d92401";
        SSHFPRecord sshfpRecord = new SSHFPRecord(
                Name.fromString("www.sshfp-test.com."), DClass.IN, 100L,
                SSHFPRecord.Algorithm.ECDSA, SSHFPRecord.Digest.SHA256,
                fp.getBytes());
        assertEquals(Name.fromString("www.sshfp-test.com."), sshfpRecord.getName());
        assertEquals(Type.SSHFP, sshfpRecord.getType());
        assertEquals(DClass.IN, sshfpRecord.getDClass());
        assertEquals(100, sshfpRecord.getTTL());
        assertEquals(SSHFPRecord.Algorithm.ECDSA, sshfpRecord.getAlgorithm());
        assertEquals(SSHFPRecord.Digest.SHA256, sshfpRecord.getDigestType());
        assertArrayEquals(fp.getBytes(), sshfpRecord.getFingerPrint());

        assertTrue(sshfpRecord.rrToString().startsWith("3 2 "));
        DNSOutput dout = new DNSOutput();
        sshfpRecord.rrToWire(dout, null, true);
    }

}
