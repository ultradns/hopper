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

public class TLSARecordTest extends TestCase {

    public void test_constructor() throws TextParseException {

	byte[] expectedWireFormat = {3, 1, 2, 170, 187, 204, 221, 238, 255};
	
	byte certUsage = 3;
	byte selector = 1;
	byte matchingType = 2;
	String certAssocData = "aabbccddeeff";
	TLSARecord tlsaRecord = new TLSARecord(
	    new Name("tlsa.com."), DClass.IN, 120L, 
	    certUsage, selector, matchingType, certUsageData);
	assertEquals(3, tlsaRecord.getCertUsage());
	assertEquals(1, tlsaRecord.getSelector());
	assertEquals(2, tlsaRecord.getMatchingType());
	assertEquals(certAssocData, tlsaRecord.getCertAssocDataPresentationFormat());
	
	byte[] wireFormat = tlsaRecord.rdataToWireCanonical();
	Assert.assertArrayEquals(expectedWireFormat, wireFormat);
    }

    public void test_tokenizer() throws IOException {
    
        byte[] expectedWireFormat = { (byte) 3, 1, 2, 170, 187, 204, 221, 238, 255};
        String rdata = "3 1 2 aabbccddeeff";
        TLSARecord tlsaRecord = new TLSARecord();
        tlsaRecord.rdataFromString(new Tokenizer(rdata), null);
        byte[] wireFormat = tlsaRecord.rdataToWireCanonical();
        Assert.assertArrayEquals(expectedWireFormat, wireFormat);
    }
    
    public void test_rrwirein() throws IOException {
    
    	byte[] inWireBytes = { (byte) 3, 1, 2, 170, 187, 204, 221, 238, 255};
    	int certUsage = 3;
    	int selector = 1;
    	int matchingType = 2;
    	String certAssocData = "aabbccddeeff";
    	TLSARecord tlsaRecord = new TLSARecord();
    	tlsaRecord.rrFromWire(new DNSInput(inWireBytes));
    	Assert.assertEquals(certUsage, tlsaRecord.getCertUsage());
    	Assert.assertEquals(selector, tlsaRecord.getSelector());
    	Assert.assertEquals(matchingType, tlsaRecord.getMatchingType());
    	assertEquals(certAssocData, tlsaRecord.getCertAssocDataPresentationFormat());
    	byte[] wireFormat = tlsaRecord.rdataToWireCanonical();
    	Assert.assertArrayEquals(inWireBytes, wireFormat);
    }
}
