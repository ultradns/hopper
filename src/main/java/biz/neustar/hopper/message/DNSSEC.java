// Copyright (c) 1999-2010 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.message;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import biz.neustar.hopper.message.impl.TrackedType;
import biz.neustar.hopper.message.impl.TrackedTypeRegistrar;
import biz.neustar.hopper.record.DNSKEYRecord;
import biz.neustar.hopper.record.DSRecord;
import biz.neustar.hopper.record.KEYRecord;
import biz.neustar.hopper.record.RRSIGRecord;
import biz.neustar.hopper.record.RRSet;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.record.SIGRecord;
import biz.neustar.hopper.record.impl.KEYBase;
import biz.neustar.hopper.record.impl.SIGBase;

/**
 * Constants and methods relating to DNSSEC.
 * 
 * DNSSEC provides authentication for DNS information.
 * 
 * @see RRSIGRecord
 * @see DNSKEYRecord
 * @see RRSet
 * 
 * @author Brian Wellington
 */

public class DNSSEC {

    public static class Algorithm  extends TrackedType {
        private static final TrackedTypeRegistrar REGISTRAR = 
                registrarBuilder(Algorithm.class)
                .maxValue(0xFF).allowNumericName(true).build();
        
        /** RSA/MD5 public key (deprecated) */
        public static final Algorithm RSAMD5 = REGISTRAR.add(new Algorithm(1, "RSAMD5"));
                
        /** Diffie Hellman key */
        public static final Algorithm DH = REGISTRAR.add(new Algorithm(2, "DH"));

        /** DSA public key */
        public static final Algorithm DSA = REGISTRAR.add(new Algorithm(3, "DSA"));

        /** Elliptic Curve key */
        public static final Algorithm ECC = REGISTRAR.add(new Algorithm(4, "ECC"));

        /** RSA/SHA1 public key */
        public static final Algorithm RSASHA1 = REGISTRAR.add(new Algorithm(5, "RSASHA1"));

        /** DSA/SHA1, NSEC3-aware public key */
        public static final Algorithm DSA_NSEC3_SHA1 = REGISTRAR.add(new Algorithm(6, "DSA-NSEC3-SHA1"));
        
        /** RSA/SHA1, NSEC3-aware public key */
        public static final Algorithm RSA_NSEC3_SHA1 = REGISTRAR.add(new Algorithm(7, "RSA-NSEC3-SHA1"));

        /** RSA/SHA256 public key */
        public static final Algorithm RSASHA256 = REGISTRAR.add(new Algorithm(8, "RSASHA256"));

        /** RSA/SHA512 public key */
        public static final Algorithm RSASHA512 = REGISTRAR.add(new Algorithm(10, "RSASHA512"));

        /** Indirect keys; the actual key is elsewhere. */
        public static final Algorithm INDIRECT = REGISTRAR.add(new Algorithm(252, "INDIRECT"));

        /** Private algorithm, specified by domain name */
        public static final Algorithm PRIVATEDNS = REGISTRAR.add(new Algorithm(253, "PRIVATEDNS"));

        /** Private algorithm, specified by OID */
        public static final Algorithm PRIVATEOID = REGISTRAR.add(new Algorithm(254, "PRIVATEOID"));

        
        public Algorithm(int value, String name, String... altNames) {
            super(value, name, altNames); // "DNSSEC algorithm"
        }

        
        public static Algorithm valueOf(int value) {
            return REGISTRAR.getOrCreateType(value);
        }
        
        public static Algorithm valueOf(String name) {
            return REGISTRAR.getOrCreateType(name);
        }

        public String toString() {
            return String.valueOf(getValue());
        }
    }

    private DNSSEC() {
    }

    private static void digestSIG(DNSOutput out, SIGBase sig) {
        out.writeU16(sig.getTypeCovered());
        out.writeU8(sig.getAlgorithm().getValue());
        out.writeU8(sig.getLabels());
        out.writeU32(sig.getOrigTTL());
        out.writeU32(sig.getExpire().getTime() / 1000);
        out.writeU32(sig.getTimeSigned().getTime() / 1000);
        out.writeU16(sig.getFootprint());
        sig.getSigner().toWireCanonical(out);
    }

    /**
     * Creates a byte array containing the concatenation of the fields of the
     * SIG record and the RRsets to be signed/verified. This does not perform a
     * cryptographic digest.
     * 
     * @param rrsig
     *            The RRSIG record used to sign/verify the rrset.
     * @param rrset
     *            The data to be signed/verified.
     * @return The data to be cryptographically signed or verified.
     */
    public static byte[] digestRRset(RRSIGRecord rrsig, RRSet rrset) {
        DNSOutput out = new DNSOutput();
        digestSIG(out, rrsig);

        int size = rrset.size();
        Record[] records = new Record[size];

        @SuppressWarnings("rawtypes")
        Iterator it = rrset.rrs();
        Name name = rrset.getName();
        Name wild = null;
        int sigLabels = rrsig.getLabels() + 1; // Add the root label back.
        if (name.labels() > sigLabels) {
            wild = name.wild(name.labels() - sigLabels);
        }
        while (it.hasNext()) {
            records[--size] = (Record) it.next();
        }
        Arrays.sort(records);

        DNSOutput header = new DNSOutput();
        if (wild != null) {
            wild.toWireCanonical(header);
        } else {
            name.toWireCanonical(header);
        }
        header.writeU16(rrset.getType());
        header.writeU16(rrset.getDClass().getValue());
        header.writeU32(rrsig.getOrigTTL());
        for (int i = 0; i < records.length; i++) {
            out.writeByteArray(header.toByteArray());
            int lengthPosition = out.current();
            out.writeU16(0);
            out.writeByteArray(records[i].rdataToWireCanonical());
            int rrlength = out.current() - lengthPosition - 2;
            out.save();
            out.jump(lengthPosition);
            out.writeU16(rrlength);
            out.restore();
        }
        return out.toByteArray();
    }

    /**
     * Creates a byte array containing the concatenation of the fields of the
     * SIG(0) record and the message to be signed. This does not perform a
     * cryptographic digest.
     * 
     * @param sig
     *            The SIG record used to sign the rrset.
     * @param msg
     *            The message to be signed.
     * @param previous
     *            If this is a response, the signature from the query.
     * @return The data to be cryptographically signed.
     */
    public static byte[] digestMessage(SIGRecord sig, Message msg,
            byte[] previous) {
        DNSOutput out = new DNSOutput();
        digestSIG(out, sig);

        if (previous != null) {
            out.writeByteArray(previous);
        }

        msg.toWire(out);
        return out.toByteArray();
    }

    /**
     * A DNSSEC exception.
     */
    public static class DNSSECException extends Exception {
        private static final long serialVersionUID = 8213345600121330380L;

        DNSSECException(String s) {
            super(s);
        }
    }

    /**
     * An algorithm is unsupported by this DNSSEC implementation.
     */
    public static class UnsupportedAlgorithmException extends DNSSECException {
        private static final long serialVersionUID = 2917488650100837008L;

        UnsupportedAlgorithmException(int alg) {
            super("Unsupported algorithm: " + alg);
        }
        
        UnsupportedAlgorithmException(DNSSEC.Algorithm alg) {
            super("Unsupported algorithm: " + alg);
        }
    }

    /**
     * The cryptographic data in a DNSSEC key is malformed.
     */
    public static class MalformedKeyException extends DNSSECException {
        private static final long serialVersionUID = -921363279387420934L;

        MalformedKeyException(KEYBase rec) {
            super("Invalid key data: " + rec.rdataToString());
        }
    }

    /**
     * A DNSSEC verification failed because fields in the DNSKEY and RRSIG
     * records do not match.
     */
    public static class KeyMismatchException extends DNSSECException {
        private static final long serialVersionUID = -4465275064584370965L;

        KeyMismatchException(KEYBase key, SIGBase sig) {
            super("key " + key.getName() + "/"
                    + key.getAlgorithm().getName() + "/"
                    + key.getFootprint() + " " + "does not match signature "
                    + sig.getSigner() + "/"
                    + sig.getAlgorithm().getName() + "/"
                    + sig.getFootprint());
        }
    }

    /**
     * A DNSSEC verification failed because the signature has expired.
     */
    public static class SignatureExpiredException extends DNSSECException {
        private static final long serialVersionUID = -1254741812830779109L;
        private Date when, now;

        SignatureExpiredException(Date when, Date now) {
            super("signature expired");
            this.when = when;
            this.now = now;
        }

        /**
         * @return When the signature expired
         */
        public Date getExpiration() {
            return when;
        }

        /**
         * @return When the verification was attempted
         */
        public Date getVerifyTime() {
            return now;
        }
    }

    /**
     * A DNSSEC verification failed because the signature has not yet become
     * valid.
     */
    public static class SignatureNotYetValidException extends DNSSECException {
        private static final long serialVersionUID = -317747998761825856L;
        private Date when, now;

        SignatureNotYetValidException(Date when, Date now) {
            super("signature is not yet valid");
            this.when = when;
            this.now = now;
        }

        /**
         * @return When the signature will become valid
         */
        public Date getExpiration() {
            return when;
        }

        /**
         * @return When the verification was attempted
         */
        public Date getVerifyTime() {
            return now;
        }
    }

    /**
     * A DNSSEC verification failed because the cryptographic signature
     * verification failed.
     */
    public static class SignatureVerificationException extends DNSSECException {
        private static final long serialVersionUID = -3667841929088389539L;

        SignatureVerificationException() {
            super("signature verification failed");
        }
    }

    /**
     * The key data provided is inconsistent.
     */
    public static class IncompatibleKeyException extends
            IllegalArgumentException {
        private static final long serialVersionUID = 7438083661136317598L;

        IncompatibleKeyException() {
            super("incompatible keys");
        }
    }

    private static int BigIntegerLength(BigInteger i) {
        return (i.bitLength() + 7) / 8;
    }

    private static BigInteger readBigInteger(DNSInput in, int len)
            throws IOException {
        byte[] b = in.readByteArray(len);
        return new BigInteger(1, b);
    }

    private static BigInteger readBigInteger(DNSInput in) {
        byte[] b = in.readByteArray();
        return new BigInteger(1, b);
    }

    private static void writeBigInteger(DNSOutput out, BigInteger val) {
        byte[] b = val.toByteArray();
        if (b[0] == 0) {
            out.writeByteArray(b, 1, b.length - 1);
        } else {
            out.writeByteArray(b);
        }
    }

    private static PublicKey toRSAPublicKey(KEYBase r) throws IOException,
            GeneralSecurityException {
        DNSInput in = new DNSInput(r.getKey());
        int exponentLength = in.readU8();
        if (exponentLength == 0) {
            exponentLength = in.readU16();
        }
        BigInteger exponent = readBigInteger(in, exponentLength);
        BigInteger modulus = readBigInteger(in);

        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private static PublicKey toDSAPublicKey(KEYBase r) throws IOException,
            GeneralSecurityException, MalformedKeyException {
        DNSInput in = new DNSInput(r.getKey());

        int t = in.readU8();
        if (t > 8) {
            throw new MalformedKeyException(r);
        }

        BigInteger q = readBigInteger(in, 20);
        BigInteger p = readBigInteger(in, 64 + t * 8);
        BigInteger g = readBigInteger(in, 64 + t * 8);
        BigInteger y = readBigInteger(in, 64 + t * 8);

        KeyFactory factory = KeyFactory.getInstance("DSA");
        return factory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
    }

    /** Converts a KEY/DNSKEY record into a PublicKey */
    public static PublicKey toPublicKey(KEYBase r) throws DNSSECException {
        DNSSEC.Algorithm alg = r.getAlgorithm();
        try {
            if (alg.equals(Algorithm.RSAMD5) ||
                    alg.equals(Algorithm.RSASHA1) ||
                    alg.equals(Algorithm.RSA_NSEC3_SHA1) ||
                    alg.equals(Algorithm.RSASHA256) ||
                    alg.equals(Algorithm.RSASHA512)) {
                
                return toRSAPublicKey(r);
            } else if (alg.equals(Algorithm.DSA) ||
                    alg.equals(Algorithm.DSA_NSEC3_SHA1)) {
                
                return toDSAPublicKey(r);
            } else {
                throw new UnsupportedAlgorithmException(alg);
            }
            
        } catch (IOException e) {
            throw new MalformedKeyException(r);
        } catch (GeneralSecurityException e) {
            throw new DNSSECException(e.toString());
        }
    }

    private static byte[] fromRSAPublicKey(RSAPublicKey key) {
        DNSOutput out = new DNSOutput();
        BigInteger exponent = key.getPublicExponent();
        BigInteger modulus = key.getModulus();
        int exponentLength = BigIntegerLength(exponent);

        if (exponentLength < 256) {
            out.writeU8(exponentLength);
        } else {
            out.writeU8(0);
            out.writeU16(exponentLength);
        }
        writeBigInteger(out, exponent);
        writeBigInteger(out, modulus);

        return out.toByteArray();
    }

    private static byte[] fromDSAPublicKey(DSAPublicKey key) {
        DNSOutput out = new DNSOutput();
        BigInteger q = key.getParams().getQ();
        BigInteger p = key.getParams().getP();
        BigInteger g = key.getParams().getG();
        BigInteger y = key.getY();
        int t = (p.toByteArray().length - 64) / 8;

        out.writeU8(t);
        writeBigInteger(out, q);
        writeBigInteger(out, p);
        writeBigInteger(out, g);
        writeBigInteger(out, y);

        return out.toByteArray();
    }

    /** Builds a DNSKEY record from a PublicKey */
    public static byte[] fromPublicKey(PublicKey key, Algorithm alg) throws DNSSECException {

        if (alg.equals(Algorithm.RSAMD5) ||
                alg.equals(Algorithm.RSASHA1) ||
                alg.equals(Algorithm.RSA_NSEC3_SHA1) ||
                alg.equals(Algorithm.RSASHA256) ||
                alg.equals(Algorithm.RSASHA512)) {
            if (!(key instanceof RSAPublicKey)) {
                throw new IncompatibleKeyException();
            }
            return fromRSAPublicKey((RSAPublicKey) key);
        } else if (alg.equals(Algorithm.DSA) ||
                alg.equals(Algorithm.DSA_NSEC3_SHA1)) {
        
            if (!(key instanceof DSAPublicKey)) {
                throw new IncompatibleKeyException();
            }
            return fromDSAPublicKey((DSAPublicKey) key);
        } else {
            throw new UnsupportedAlgorithmException(alg);
        }
    }

    /**
     * Convert an algorithm number to the corresponding JCA string.
     * 
     * @param alg
     *            The algorithm number.
     * @throws UnsupportedAlgorithmException
     *             The algorithm is unknown.
     */
    public static String algString(DNSSEC.Algorithm alg)
            throws UnsupportedAlgorithmException {
        
        if (alg.equals(Algorithm.RSAMD5)) {
            return "MD5withRSA";
        } else if (alg.equals(Algorithm.DSA) ||
                alg.equals(Algorithm.DSA_NSEC3_SHA1)) {
            return "SHA1withDSA";
        } else if (alg.equals(Algorithm.RSASHA1) ||
                alg.equals(Algorithm.RSA_NSEC3_SHA1)) {
            return "SHA1withRSA";
        } else if (alg.equals(Algorithm.RSASHA256)) {
            return "SHA256withRSA";
        } else if (alg.equals(Algorithm.RSASHA512)) {
            return "SHA512withRSA";
        } else {
            throw new UnsupportedAlgorithmException(alg);
        }
    }

    private static final int ASN1_SEQ = 0x30;
    private static final int ASN1_INT = 0x2;

    private static final int DSA_LEN = 20;

    private static byte[] DSASignaturefromDNS(byte[] dns)
            throws DNSSECException, IOException {
        if (dns.length != 1 + DSA_LEN * 2) {
            throw new SignatureVerificationException();
        }

        DNSInput in = new DNSInput(dns);
        DNSOutput out = new DNSOutput();

        in.readU8();

        byte[] r = in.readByteArray(DSA_LEN);
        int rlen = DSA_LEN;
        if (r[0] < 0) {
            rlen++;
        }

        byte[] s = in.readByteArray(DSA_LEN);
        int slen = DSA_LEN;
        if (s[0] < 0) {
            slen++;
        }

        out.writeU8(ASN1_SEQ);
        out.writeU8(rlen + slen + 4);

        out.writeU8(ASN1_INT);
        out.writeU8(rlen);
        if (rlen > DSA_LEN) {
            out.writeU8(0);
        }
        out.writeByteArray(r);

        out.writeU8(ASN1_INT);
        out.writeU8(slen);
        if (slen > DSA_LEN) {
            out.writeU8(0);
        }
        out.writeByteArray(s);

        return out.toByteArray();
    }

    private static byte[] DSASignaturetoDNS(byte[] key, int t)
            throws IOException {
        DNSInput in = new DNSInput(key);
        DNSOutput out = new DNSOutput();

        out.writeU8(t);

        int tmp = in.readU8();
        if (tmp != ASN1_SEQ) {
            throw new IOException();
        }
        in.readU8();

        tmp = in.readU8();
        if (tmp != ASN1_INT) {
            throw new IOException();
        }
        int rlen = in.readU8();
        if (rlen == DSA_LEN + 1) {
            if (in.readU8() != 0) {
                throw new IOException();
            }
        } else if (rlen != DSA_LEN) {
            throw new IOException();
        }
        byte[] bytes = in.readByteArray(DSA_LEN);
        out.writeByteArray(bytes);

        tmp = in.readU8();
        if (tmp != ASN1_INT) {
            throw new IOException();
        }
        int slen = in.readU8();
        if (slen == DSA_LEN + 1) {
            if (in.readU8() != 0) {
                throw new IOException();
            }
        } else if (slen != DSA_LEN) {
            throw new IOException();
        }
        bytes = in.readByteArray(DSA_LEN);
        out.writeByteArray(bytes);

        return out.toByteArray();
    }

    private static void verify(PublicKey key, DNSSEC.Algorithm alg, byte[] data,
            byte[] signature) throws DNSSECException {
        if (key instanceof DSAPublicKey) {
            try {
                signature = DSASignaturefromDNS(signature);
            } catch (IOException e) {
                throw new IllegalStateException();
            }
        }

        try {
            Signature s = Signature.getInstance(algString(alg));
            s.initVerify(key);
            s.update(data);
            if (!s.verify(signature)) {
                throw new SignatureVerificationException();
            }
        } catch (GeneralSecurityException e) {
            throw new DNSSECException(e.toString());
        }
    }

    private static boolean matches(SIGBase sig, KEYBase key) {
        return (key.getAlgorithm() == sig.getAlgorithm()
                && key.getFootprint() == sig.getFootprint() && key.getName()
                .equals(sig.getSigner()));
    }

    /**
     * Verify a DNSSEC signature.
     * 
     * @param rrset
     *            The data to be verified.
     * @param rrsig
     *            The RRSIG record containing the signature.
     * @param key
     *            The DNSKEY record to verify the signature with.
     * @throws UnsupportedAlgorithmException
     *             The algorithm is unknown
     * @throws MalformedKeyException
     *             The key is malformed
     * @throws KeyMismatchException
     *             The key and signature do not match
     * @throws SignatureExpiredException
     *             The signature has expired
     * @throws SignatureNotYetValidException
     *             The signature is not yet valid
     * @throws SignatureVerificationException
     *             The signature does not verify.
     * @throws DNSSECException
     *             Some other error occurred.
     */
    public static void verify(RRSet rrset, RRSIGRecord rrsig, DNSKEYRecord key)
            throws DNSSECException {
        if (!matches(rrsig, key)) {
            throw new KeyMismatchException(key, rrsig);
        }

        Date now = new Date();
        if (now.compareTo(rrsig.getExpire()) > 0) {
            throw new SignatureExpiredException(rrsig.getExpire(), now);
        }
        if (now.compareTo(rrsig.getTimeSigned()) < 0)
            throw new SignatureNotYetValidException(rrsig.getTimeSigned(), now);

        verify(key.getPublicKey(), rrsig.getAlgorithm(),
                digestRRset(rrsig, rrset), rrsig.getSignature());
    }

    /**
     * Sign some data
     * 
     * @param privkey
     *            Sign data with this key
     * @param pubkey
     *            Sign data with this key
     * @param alg
     *            Which signing algorithm to use
     * @param data
     *            To be signed
     * @return The signature
     * @throws DNSSECException
     *             If things go poorly
     */
    public static byte[] sign(PrivateKey privkey, PublicKey pubkey, Algorithm alg,
            byte[] data) throws DNSSECException {
        return sign(privkey, pubkey, alg, data, null);
    }

    /**
     * Sign some data with a specified JCE provider
     * 
     * @param privkey
     *            Sign data with this key
     * @param pubkey
     *            Sign data with this key
     * @param alg
     *            Which signing algorithm to use
     * @param data
     *            To be signed
     * @param provider
     *            JCE provider
     * @return The signature
     * @throws DNSSECException
     *             If things go poorly
     */
    public static byte[] sign(PrivateKey privkey, PublicKey pubkey, Algorithm alg,
            byte[] data, String provider) throws DNSSECException {
        byte[] signature;
        try {
            Signature s;
            if (provider != null) {
                s = Signature.getInstance(algString(alg), provider);
            } else {
                s = Signature.getInstance(algString(alg));
            }
            s.initSign(privkey);
            s.update(data);
            signature = s.sign();
        } catch (GeneralSecurityException e) {
            throw new DNSSECException(e.toString());
        }

        if (pubkey instanceof DSAPublicKey) {
            try {
                DSAPublicKey dsa = (DSAPublicKey) pubkey;
                BigInteger P = dsa.getParams().getP();
                int t = (BigIntegerLength(P) - 64) / 8;
                signature = DSASignaturetoDNS(signature, t);
            } catch (IOException e) {
                throw new IllegalStateException();
            }
        }

        return signature;
    }

    static void checkAlgorithm(PrivateKey key, DNSSEC.Algorithm alg)
            throws UnsupportedAlgorithmException {
        
        if (alg.equals(Algorithm.RSAMD5) ||
                alg.equals(Algorithm.RSASHA1) ||
                alg.equals(Algorithm.RSA_NSEC3_SHA1) ||
                alg.equals(Algorithm.RSASHA256) ||
                alg.equals(Algorithm.RSASHA512)) {
        
            if (!(key instanceof RSAPrivateKey)) {
                throw new IncompatibleKeyException();
            }
        } else if (alg.equals(Algorithm.DSA) ||
                alg.equals(Algorithm.DSA_NSEC3_SHA1)) {
            
            if (!(key instanceof DSAPrivateKey)) {
                throw new IncompatibleKeyException();
            }
        } else {
            throw new UnsupportedAlgorithmException(alg);
        }
    }

    /**
     * Generate a DNSSEC signature. key and privateKey must refer to the same
     * underlying cryptographic key.
     * 
     * @param rrset
     *            The data to be signed
     * @param key
     *            The DNSKEY record to use as part of signing
     * @param privkey
     *            The PrivateKey to use when signing
     * @param inception
     *            The time at which the signatures should become valid
     * @param expiration
     *            The time at which the signatures should expire
     * @throws UnsupportedAlgorithmException
     *             The algorithm is unknown
     * @throws MalformedKeyException
     *             The key is malformed
     * @throws DNSSECException
     *             Some other error occurred.
     * @return The generated signature
     */
    public static RRSIGRecord sign(RRSet rrset, DNSKEYRecord key,
            PrivateKey privkey, Date inception, Date expiration)
            throws DNSSECException {
        return sign(rrset, key, privkey, inception, expiration, null);
    }

    /**
     * Generate a DNSSEC signature. key and privateKey must refer to the same
     * underlying cryptographic key.
     * 
     * @param rrset
     *            The data to be signed
     * @param key
     *            The DNSKEY record to use as part of signing
     * @param privkey
     *            The PrivateKey to use when signing
     * @param inception
     *            The time at which the signatures should become valid
     * @param expiration
     *            The time at which the signatures should expire
     * @param provider
     *            The name of the JCA provider. If non-null, it will be passed
     *            to JCA getInstance() methods.
     * @throws UnsupportedAlgorithmException
     *             The algorithm is unknown
     * @throws MalformedKeyException
     *             The key is malformed
     * @throws DNSSECException
     *             Some other error occurred.
     * @return The generated signature
     */
    public static RRSIGRecord sign(RRSet rrset, DNSKEYRecord key,
            PrivateKey privkey, Date inception, Date expiration, String provider)
            throws DNSSECException {
        DNSSEC.Algorithm alg = key.getAlgorithm();
        checkAlgorithm(privkey, alg);

        RRSIGRecord rrsig = new RRSIGRecord(rrset.getName(), rrset.getDClass(),
                rrset.getTTL(), rrset.getType(), alg, rrset.getTTL(),
                expiration, inception, key.getFootprint(), key.getName(), null);
/// TODO: this really should be done as part of the RRSIG creation
        rrsig.setSignature(sign(privkey, key.getPublicKey(), alg,
                digestRRset(rrsig, rrset), provider));
        return rrsig;
    }

    public static SIGRecord signMessage(Message message, SIGRecord previous,
            KEYRecord key, PrivateKey privkey, Date inception, Date expiration)
            throws DNSSECException {
        DNSSEC.Algorithm alg = key.getAlgorithm();
        checkAlgorithm(privkey, alg);

        SIGRecord sig = new SIGRecord(Name.root, DClass.ANY, 0, 0, alg, 0,
                expiration, inception, key.getFootprint(), key.getName(), null);
        DNSOutput out = new DNSOutput();
        digestSIG(out, sig);
        if (previous != null) {
            out.writeByteArray(previous.getSignature());
        }
        message.toWire(out);
// TODO: this really should be done as part of the RRSIG creation
        sig.setSignature(sign(privkey, key.getPublicKey(), alg,
                out.toByteArray(), null));
        return sig;
    }

    public static void verifyMessage(Message message, byte[] bytes,
            SIGRecord sig, SIGRecord previous, KEYRecord key)
            throws DNSSECException {
        if (!matches(sig, key)) {
            throw new KeyMismatchException(key, sig);
        }

        Date now = new Date();
        if (now.compareTo(sig.getExpire()) > 0) {
            throw new SignatureExpiredException(sig.getExpire(), now);
        }
        if (now.compareTo(sig.getTimeSigned()) < 0)
            throw new SignatureNotYetValidException(sig.getTimeSigned(), now);

        DNSOutput out = new DNSOutput();
        digestSIG(out, sig);
        if (previous != null) {
            out.writeByteArray(previous.getSignature());
        }

        Header header = (Header) message.getHeader().clone();
        header.decCount(Section.ADDITIONAL);
        out.writeByteArray(header.toWire());

        out.writeByteArray(bytes, Header.LENGTH, message.sig0start
                - Header.LENGTH);

        verify(key.getPublicKey(), sig.getAlgorithm(), out.toByteArray(),
                sig.getSignature());
    }

    /**
     * Generate the digest value for a DS key
     * 
     * @param key
     *            Which is covered by the DS record
     * @param digestid
     *            The type of digest
     * @return The digest value as an array of bytes
     */
    public static byte[] generateDSDigest(DNSKEYRecord key, int digestid) {
        MessageDigest digest;
        try {
            switch (digestid) {
            case DSRecord.Digest.SHA1:
                digest = MessageDigest.getInstance("sha-1");
                break;
            case DSRecord.Digest.SHA256:
                digest = MessageDigest.getInstance("sha-256");
                break;
            default:
                throw new IllegalArgumentException("unknown DS digest type "
                        + digestid);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("no message digest support");
        }
        digest.update(key.getName().toWire());
        digest.update(key.rdataToWireCanonical());
        return digest.digest();
    }

}
