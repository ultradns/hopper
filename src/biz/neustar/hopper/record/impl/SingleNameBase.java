// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record.impl;

import static biz.neustar.hopper.record.Record.checkName;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.util.Tokenizer;

/**
 * Implements common functionality for the many record types whose format is a
 * single name.
 * 
 * @author Brian Wellington
 */

public abstract class SingleNameBase extends Record {

    private static final long serialVersionUID = -18595042501413L;

    protected Name singleName;

    protected SingleNameBase() {
    }

    protected SingleNameBase(Name name, int type, int dclass, long ttl) {
        super(name, type, dclass, ttl);
    }

    protected SingleNameBase(Name name, int type, int dclass, long ttl,
            Name singleName, String description) {
        super(name, type, dclass, ttl);
        this.singleName = checkName(description, singleName);
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        singleName = new Name(in);
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        singleName = st.getName(origin);
    }

    public String rrToString() {
        return singleName.toString();
    }

    protected Name getSingleName() {
        return singleName;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        singleName.toWire(out, null, canonical);
    }

}
