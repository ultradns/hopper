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

/**
 * X25 - identifies the PSDN (Public Switched Data Network) address in the X.121
 * numbering plan associated with a name.
 * 
 * @author Brian Wellington
 */

public class X25Record extends Record {

    private static final long serialVersionUID = 4267576252335579764L;

    private byte[] address;

    public X25Record() {
    }

    protected Record getObject() {
        return new X25Record();
    }

    private static final byte[] checkAndConvertAddress(String address) {
        int length = address.length();
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            char c = address.charAt(i);
            if (!Character.isDigit(c)) {
                return null;
            }
            out[i] = (byte) c;
        }
        return out;
    }

    /**
     * Creates an X25 Record from the given data
     * 
     * @param address
     *            The X.25 PSDN address.
     * @throws IllegalArgumentException
     *             The address is not a valid PSDN address.
     */
    public X25Record(Name name, DClass dclass, long ttl, String address) {
        super(name, Type.X25, dclass, ttl);
        this.address = checkAndConvertAddress(address);
        if (this.address == null) {
            throw new IllegalArgumentException("invalid PSDN address "
                    + address);
        }
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        address = in.readCountedString();
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        String addr = st.getString();
        this.address = checkAndConvertAddress(addr);
        if (this.address == null) {
            throw st.exception("invalid PSDN address " + addr);
        }
    }

    /**
     * Returns the X.25 PSDN address.
     */
    public String getAddress() {
        return byteArrayToString(address, false);
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeCountedString(address);
    }

    public String rrToString() {
        return byteArrayToString(address, true);
    }

}
