/**
 * Copyright 2000-2015 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.record;

import java.io.IOException;
import java.util.Locale;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Hex;
import biz.neustar.hopper.util.Tokenizer;

public class TLSARecord extends Record {

    private static final long serialVersionUID = -171845215943233351L;

    /**
     * The Certificate Usage field of the TLSA record, range is [0,255].
     */
    private int certUsage;
    
    /**
     * The Selector field of the TLSA record, range is [0,255].
     */
    private int selector;
    
    /**
     * The Matching Type field of the TLSA record, range is [0,255].
     */
    private int matchingType;
    
    /**
     * The Certificate Association Data field of the TLSA record; binary data.
     */
    private byte[] certAssocData;

    public TLSARecord() {
    }

    public TLSARecord(Name name, DClass dclass, long ttl, int certUsage,
            int selector, int matchingType, String certAssocData) 
            throws TextParseException {
        super(name, Type.TLSA, dclass, ttl);
        this.certUsage = certUsage;
        this.selector = selector;
        this.matchingType = matchingType;
        setCertAssocData(certAssocData);
    }

    @Override
    protected Record getObject() {
        return new TLSARecord();
    }

    @Override
    protected void rrFromWire(DNSInput in) throws IOException {
    	certUsage = in.readU8();
    	selector = in.readU8();
    	matchingType = in.readU8();
    	setCertAssocData(in.readByteArray());
    }

    public void setCertAssocData(byte[] input) {
        this.certAssocData = input;
    }
    
    public void setCertAssocData(String value) {
        this.certAssocData = Hex.decode(value);
    }

    public int getCertUsage() {
        return certUsage;
    }
    
    public int getSelector() {
        return selector;
    }
    
    public int getMatchingType() {
        return matchingType;
    }

    public String getCertAssocDataPresentationFormat() {
        return Hex.encode(certAssocData).toLowerCase(Locale.US);
    }

    public byte[] getCertAssocData() {
        return certAssocData;
    }

    @Override
    public String rrToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(certUsage);
        sb.append(' ');
        sb.append(selector);
        sb.append(' ');
        sb.append(matchingType);
        sb.append(' ');
        sb.append(getCertAssocDataPresentationFormat());
        return sb.toString();
    }

    @Override
    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        certUsage = st.getUInt8();
        selector = st.getUInt8();
        matchingType = st.getUInt8();
        setCertAssocData(st.getString());
    }
    
    @Override
    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU8(certUsage);
        out.writeU8(selector);
        out.writeU8(matchingType);
        out.writeByteArray(certAssocData);
    }
}
