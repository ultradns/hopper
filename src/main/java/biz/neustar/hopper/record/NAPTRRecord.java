// Copyright (c) 2000-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Tokenizer;

/**
 * Name Authority Pointer Record - specifies rewrite rule, that when applied to
 * an existing string will produce a new domain.
 * 
 * @author Chuck Santos
 */

public class NAPTRRecord extends Record {

    private static final long serialVersionUID = 5191232392044947002L;

    private int order, preference;
    private byte[] flags, service, regexp;
    private Name replacement;

    static byte [] EMPTY;
    static final Name DOT;
    static {
        try {
            EMPTY = byteArrayFromString("");
            DOT = new Name(".");
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }
    


    public NAPTRRecord() {
    }

    protected Record getObject() {
        return new NAPTRRecord();
    }

    /**
     * Creates an NAPTR Record from the given data
     * 
     * @param order
     *            The order of this NAPTR. Records with lower order are
     *            preferred.
     * @param preference
     *            The preference, used to select between records at the same
     *            order.
     * @param flags
     *            The control aspects of the NAPTRRecord.
     * @param service
     *            The service or protocol available down the rewrite path.
     * @param regexp
     *            The regular/substitution expression.
     * @param replacement
     *            The domain-name to query for the next DNS resource record,
     *            depending on the value of the flags field.
     * @throws IllegalArgumentException
     *             One of the strings has invalid escapes
     */
    public NAPTRRecord(Name name, DClass dclass, long ttl, int order,
            int preference, String flags, String service, String regexp,
            Name replacement) {
        super(name, Type.NAPTR, dclass, ttl);
        this.order = checkU16("order", order);
        this.preference = checkU16("preference", preference);
        try {
            this.flags = (null == flags) ? EMPTY : byteArrayFromString(flags);
            this.service = (null == service) ? EMPTY : byteArrayFromString(service);
            this.regexp = (null == regexp) ? EMPTY : byteArrayFromString(regexp);
        } catch (TextParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        this.replacement = (null == replacement) ? DOT : checkName("replacement", replacement);
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        order = in.readU16();
        preference = in.readU16();
        flags = in.readCountedString();
        service = in.readCountedString();
        regexp = in.readCountedString();
        replacement = new Name(in);
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        order = st.getUInt16();
        preference = st.getUInt16();
        try {
            flags = byteArrayFromString(st.getString());
            service = byteArrayFromString(st.getString());
            regexp = byteArrayFromString(st.getString());
        } catch (TextParseException e) {
            throw st.exception(e.getMessage());
        }
        replacement = st.getName(origin);
    }

    /** Converts rdata to a String */
    public String rrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(order);
        sb.append(" ");
        sb.append(preference);
        sb.append(" ");
        sb.append(byteArrayToString(flags, true));
        sb.append(" ");
        sb.append(byteArrayToString(service, true));
        sb.append(" ");
        sb.append(byteArrayToString(regexp, true));
        sb.append(" ");
        sb.append(replacement);
        return sb.toString();
    }

    /** Returns the order */
    public int getOrder() {
        return order;
    }

    /** Returns the preference */
    public int getPreference() {
        return preference;
    }

    /** Returns flags */
    public String getFlags() {
        return byteArrayToString(flags, false);
    }

    /** Returns service */
    public String getService() {
        return byteArrayToString(service, false);
    }

    /** Returns regexp */
    public String getRegexp() {
        return byteArrayToString(regexp, false);
    }

    /** Returns the replacement domain-name */
    public Name getReplacement() {
        return replacement;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU16(order);
        out.writeU16(preference);

        // Handle NULL flags
        if (flags == null) {
            out.writeCountedString(EMPTY);
        } else {
            out.writeCountedString(flags);
        }

        if (service == null) {
            out.writeCountedString(EMPTY);
        } else {
            out.writeCountedString(service);
        }

        // Handle null regex
        if (regexp == null)  {
            out.writeCountedString(EMPTY);
        } else {
            out.writeCountedString(regexp);
        }

        // Handle null replacement
        if (replacement == null) {
            DOT.toWire(out, null, canonical);
        } else {
            replacement.toWire(out, null, canonical);
        }
    }

    public Name getAdditionalName() {
        return replacement;
    }

}
