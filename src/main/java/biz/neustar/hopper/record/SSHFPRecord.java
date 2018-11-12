// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Tokenizer;
import biz.neustar.hopper.util.base16;

/**
 * SSH Fingerprint - stores the fingerprint of an SSH host key.
 * 
 * @author Brian Wellington
 */

public class SSHFPRecord extends Record {

    private static final long serialVersionUID = -8104701402654687025L;

    public static class Algorithm {
        private Algorithm() {
        }

        public static final int RSA = 1;
        public static final int DSS = 2;
        public static final int ECDSA = 3;
        public static final int ED22519 = 4;
    }

    public static class Digest {
        private Digest() {
        }

        public static final int SHA1 = 1;
        public static final int SHA256 = 2;
    }

    private int alg;
    private int digestType;
    private byte[] fingerprint;

    public SSHFPRecord() {
    }

    protected Record getObject() {
        return new SSHFPRecord();
    }

    /**
     * Creates an SSHFP Record from the given data.
     * 
     * @param alg
     *            The public key's algorithm.
     * @param digestType
     *            The public key's digest type.
     * @param fingerprint
     *            The public key's fingerprint.
     */
    public SSHFPRecord(Name name, DClass dclass, long ttl, int alg,
            int digestType, byte[] fingerprint) {
        super(name, Type.SSHFP, dclass, ttl);
        this.alg = checkU8("alg", alg);
        this.digestType = checkU8("digestType", digestType);
        this.fingerprint = fingerprint;
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        alg = in.readU8();
        digestType = in.readU8();
        fingerprint = in.readByteArray();
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        alg = st.getUInt8();
        digestType = st.getUInt8();
        fingerprint = st.getHex(true);
    }

    public String rrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(alg);
        sb.append(" ");
        sb.append(digestType);
        sb.append(" ");
        sb.append(base16.toString(fingerprint));
        return sb.toString();
    }

    /** Returns the public key's algorithm. */
    public int getAlgorithm() {
        return alg;
    }

    /** Returns the public key's digest type. */
    public int getDigestType() {
        return digestType;
    }

    /** Returns the fingerprint */
    public byte[] getFingerPrint() {
        return fingerprint;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU8(alg);
        out.writeU8(digestType);
        out.writeByteArray(fingerprint);
    }

}
