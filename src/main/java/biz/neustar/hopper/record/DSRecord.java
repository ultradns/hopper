// Copyright (c) 2002-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.DNSSEC;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Tokenizer;
import biz.neustar.hopper.util.base16;

/**
 * DS - contains a Delegation Signer record, which acts as a placeholder for KEY
 * records in the parent zone.
 * 
 * @see DNSSEC
 * 
 * @author David Blacka
 * @author Brian Wellington
 */

public class DSRecord extends Record {

    public static class Digest {
        private Digest() {
        }

        /** SHA-1 */
        public static final int SHA1 = 1;

        /** SHA-256 */
        public static final int SHA256 = 2;
    }

    public static final int SHA1_DIGEST_ID = Digest.SHA1;
    public static final int SHA256_DIGEST_ID = Digest.SHA256;

    private static final long serialVersionUID = -9001819329700081493L;

    private int footprint;
    private DNSSEC.Algorithm alg;
    private int digestid;
    private byte[] digest;

    public DSRecord() {
    }

    protected Record getObject() {
        return new DSRecord();
    }

    /**
     * Creates a DS Record from the given data
     * 
     * @param footprint
     *            The original KEY record's footprint (keyid).
     * @param alg
     *            The original key algorithm.
     * @param digestid
     *            The digest id code.
     * @param digest
     *            A hash of the original key.
     */
    public DSRecord(Name name, DClass in, long ttl, int footprint, DNSSEC.Algorithm alg,
            int digestid, byte[] digest) {
        super(name, Type.DS, in, ttl);
        this.footprint = checkU16("footprint", footprint);
        // TODO: move this check into the Algorithm
        this.alg = alg;
        this.digestid = checkU8("digestid", digestid);
        this.digest = digest;
    }

    /**
     * Creates a DS Record from the given data
     * 
     * @param digestid
     *            The digest id code.
     * @param key
     *            The key to digest
     */
    public DSRecord(Name name, DClass in, long ttl, int digestid,
            DNSKEYRecord key) {
        this(name, in, ttl, key.getFootprint(), key.getAlgorithm(),
                digestid, DNSSEC.generateDSDigest(key, digestid));
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        footprint = in.readU16();
        alg = DNSSEC.Algorithm.valueOf(in.readU8());
        digestid = in.readU8();
        digest = in.readByteArray();
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        footprint = st.getUInt16();
        alg = DNSSEC.Algorithm.valueOf(st.getUInt8());
        digestid = st.getUInt8();
        digest = st.getHex();
    }

    /**
     * Converts rdata to a String
     */
    public String rrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(footprint);
        sb.append(" ");
        sb.append(alg.getValue());
        sb.append(" ");
        sb.append(digestid);
        if (digest != null) {
            sb.append(" ");
            sb.append(base16.toString(digest));
        }

        return sb.toString();
    }

    /**
     * Returns the key's algorithm.
     */
    public DNSSEC.Algorithm getAlgorithm() {
        return alg;
    }

    /**
     * Returns the key's Digest ID.
     */
    public int getDigestID() {
        return digestid;
    }

    /**
     * Returns the binary hash of the key.
     */
    public byte[] getDigest() {
        return digest;
    }

    /**
     * Returns the key's footprint.
     */
    public int getFootprint() {
        return footprint;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU16(footprint);
        out.writeU8(alg.getValue());
        out.writeU8(digestid);
        if (digest != null) {
            out.writeByteArray(digest);
        }
    }

}