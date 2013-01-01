// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.util.Tokenizer;

/**
 * A class implementing Records with no data; that is, records used in the
 * question section of messages and meta-records in dynamic update.
 * 
 * @author Brian Wellington
 */

class EmptyRecord extends Record {

    private static final long serialVersionUID = 3601852050646429582L;

    EmptyRecord() {
    }

    protected Record getObject() {
        return new EmptyRecord();
    }

    protected void rrFromWire(DNSInput in) throws IOException {
    }

    protected void rdataFromString(Tokenizer st, Name origin)
            throws IOException {
    }

    public String rrToString() {
        return "";
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
    }

}
