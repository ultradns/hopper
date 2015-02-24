// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.Address;
import biz.neustar.hopper.util.Tokenizer;

/**
 * IPv6 Address Record - maps a domain name to an IPv6 address
 * 
 * @author Brian Wellington
 */

public class AAAARecord extends Record {

    private static final long serialVersionUID = -7749806497566704077L;

    /**
     * Has to be an IPV6 Address, see http://www.ietf.org/rfc/rfc3596.txt
     */
    private Inet6Address address;

    public AAAARecord() {
    }

    protected Record getObject() {
        return new AAAARecord();
    }

    /**
     * Creates an AAAA Record from the given data
     * 
     * @param address
     *            The address suffix
     */
    public AAAARecord(Name name, DClass in, long ttl, InetAddress address) {
        super(name, Type.AAAA, in, ttl);
        if (Address.familyOf(address) != Address.IPv6) {
            throw new IllegalArgumentException("invalid IPv6 address");
        }
        if (!(address instanceof Inet6Address)) {
            throw new IllegalArgumentException("invalid IPv6 address");
        }
        this.address = (Inet6Address) address;
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        address = Inet6Address.getByAddress(null, in.readByteArray(16), null);
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
    	String token = st.getString();
    	InetAddress shouldBeIPV6 = Address.getByAddress(token, Address.IPv6);
        if (!(shouldBeIPV6 instanceof Inet6Address)) {
            throw new IllegalArgumentException("invalid IPv6 address");
        }
        address = (Inet6Address) shouldBeIPV6;
    }

    /** Converts rdata to a String */
    public String rrToString() {
        return address.getHostAddress();
    }

    /** Returns the address */
    public InetAddress getAddress() {
        return address;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeByteArray(address.getAddress());
    }

}
