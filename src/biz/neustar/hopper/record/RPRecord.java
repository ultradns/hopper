// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Tokenizer;

/**
 * Responsible Person Record - lists the mail address of a responsible person
 * and a domain where TXT records are available.
 * 
 * @author Tom Scola <tscola@research.att.com>
 * @author Brian Wellington
 */

public class RPRecord extends Record {

    private static final long serialVersionUID = 8124584364211337460L;

    private Name mailbox;
    private Name textDomain;

    public RPRecord() {
    }

    protected Record getObject() {
        return new RPRecord();
    }

    /**
     * Creates an RP Record from the given data
     * 
     * @param mailbox
     *            The responsible person
     * @param textDomain
     *            The address where TXT records can be found
     */
    public RPRecord(Name name, int dclass, long ttl, Name mailbox,
            Name textDomain) {
        super(name, Type.RP, dclass, ttl);

        this.mailbox = checkName("mailbox", mailbox);
        this.textDomain = checkName("textDomain", textDomain);
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        mailbox = new Name(in);
        textDomain = new Name(in);
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        mailbox = st.getName(origin);
        textDomain = st.getName(origin);
    }

    /** Converts the RP Record to a String */
    public String rrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(mailbox);
        sb.append(" ");
        sb.append(textDomain);
        return sb.toString();
    }

    /** Gets the mailbox address of the RP Record */
    public Name getMailbox() {
        return mailbox;
    }

    /** Gets the text domain info of the RP Record */
    public Name getTextDomain() {
        return textDomain;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        mailbox.toWire(out, null, canonical);
        textDomain.toWire(out, null, canonical);
    }

}
