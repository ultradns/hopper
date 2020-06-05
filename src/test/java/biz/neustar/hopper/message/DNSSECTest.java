package biz.neustar.hopper.message;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import biz.neustar.hopper.message.DNSSEC.Algorithm;
import biz.neustar.hopper.message.DNSSEC.SignatureExpiredException;
import biz.neustar.hopper.record.DNSKEYRecord;
import biz.neustar.hopper.record.KEYRecord;
import biz.neustar.hopper.record.RRSIGRecord;
import biz.neustar.hopper.record.RRSet;
import biz.neustar.hopper.record.Record;

public class DNSSECTest {

    @Test
    public void testECDSAP256SHA256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair pair = generator.genKeyPair();

        RRSet rrset = new RRSet();
        rrset.addRR(Record.fromString(new Name("www.yahoo.com."), Type.A, DClass.IN, 100, "2.2.2.2", Name.root));
        DNSKEYRecord zsk = new DNSKEYRecord(new Name("yahoo.com."), DClass.IN, 100,
                256, KEYRecord.PROTOCOL_DNSSEC, Algorithm.ECDSAP256SHA256, pair.getPublic());

        try {
            Date inception = new Date(0);
            Date expiration = new Date(100000000);
            RRSIGRecord rrsig = DNSSEC.sign(rrset, zsk, pair.getPrivate(), inception, expiration);
            DNSSEC.verify(rrset, rrsig, zsk);
            Assert.fail("SignatureExpiredException should have been thrown");
        } catch (SignatureExpiredException e) {

        }

        for (int i = 0; i < 1000; i++) {
            Date inception = new Date(System.currentTimeMillis());
            Date expiration = new Date(System.currentTimeMillis() + 86400000);
            RRSIGRecord rrsig = DNSSEC.sign(rrset, zsk, pair.getPrivate(), inception, expiration);
            DNSSEC.verify(rrset, rrsig, zsk);
        }
        PublicKey key = DNSSEC.toPublicKey(zsk);
        Assert.assertEquals(pair.getPublic(), key);
    }

    @Test
    public void testRSASHA256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(512);
        KeyPair pair = generator.genKeyPair();

        RRSet rrset = new RRSet();
        rrset.addRR(Record.fromString(new Name("www.yahoo.com."), Type.A, DClass.IN, 100, "2.2.2.2", Name.root));
        DNSKEYRecord zsk = new DNSKEYRecord(new Name("yahoo.com."), DClass.IN, 100,
                256, KEYRecord.PROTOCOL_DNSSEC, Algorithm.RSASHA256, pair.getPublic());

        try {
            Date inception = new Date(0);
            Date expiration = new Date(100000000);
            RRSIGRecord rrsig = DNSSEC.sign(rrset, zsk, pair.getPrivate(), inception, expiration);
            DNSSEC.verify(rrset, rrsig, zsk);
            Assert.fail("SignatureExpiredException should have been thrown");
        } catch (SignatureExpiredException e) {

        }

        for (int i = 0; i < 1000; i++) {
            Date inception = new Date(System.currentTimeMillis());
            Date expiration = new Date(System.currentTimeMillis() + 86400000);
            RRSIGRecord rrsig = DNSSEC.sign(rrset, zsk, pair.getPrivate(), inception, expiration);
            DNSSEC.verify(rrset, rrsig, zsk);
        }
        PublicKey key = DNSSEC.toPublicKey(zsk);
        Assert.assertEquals(pair.getPublic(), key);
    }

}
