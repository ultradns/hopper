// Copyright (c) 2008 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Tokenizer;
import biz.neustar.hopper.util.base64;

/**
 * DHCID - Dynamic Host Configuration Protocol (DHCP) ID (RFC 4701)
 * 
 * @author Brian Wellington
 */

public class DHCIDRecord extends Record {

    private static final long serialVersionUID = -8214820200808997707L;

    private byte[] data;

    public DHCIDRecord() {
    }

    protected Record getObject() {
        return new DHCIDRecord();
    }

    /**
     * Creates an DHCID Record from the given data
     * 
     * @param data
     *            The binary data, which is opaque to DNS.
     */
    public DHCIDRecord(Name name, int dclass, long ttl, byte[] data) {
        super(name, Type.DHCID, dclass, ttl);
        this.data = data;
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        data = in.readByteArray();
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        data = st.getBase64();
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeByteArray(data);
    }

    public String rrToString() {
        return base64.toString(data);
    }

    /**
     * Returns the binary data.
     */
    public byte[] getData() {
        return data;
    }

}
