// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.util.Tokenizer;

/**
 * A class implementing Records of unknown and/or unimplemented types. This
 * class can only be initialized using static Record initializers.
 * 
 * @author Brian Wellington
 */

public class UNKRecord extends Record {

    private static final long serialVersionUID = -4193583311594626915L;

    private byte[] data;

    UNKRecord() {
    }

    protected Record getObject() {
        return new UNKRecord();
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        data = in.readByteArray();
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        throw st.exception("invalid unknown RR encoding");
    }

    /** Converts this Record to the String "unknown format" */
    public String rrToString() {
        return unknownToString(data);
    }

    /** Returns the contents of this record. */
    public byte[] getData() {
        return data;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeByteArray(data);
    }

}
