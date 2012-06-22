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
 * The NULL Record. This has no defined purpose, but can be used to hold
 * arbitrary data.
 * 
 * @author Brian Wellington
 */

public class NULLRecord extends Record {

    private static final long serialVersionUID = -5796493183235216538L;

    private byte[] data;

    public NULLRecord() {
    }

    protected Record getObject() {
        return new NULLRecord();
    }

    /**
     * Creates a NULL record from the given data.
     * 
     * @param data
     *            The contents of the record.
     */
    public NULLRecord(Name name, DClass dclass, long ttl, byte[] data) {
        super(name, Type.NULL, dclass, ttl);

        if (data.length > 0xFFFF) {
            throw new IllegalArgumentException("data must be <65536 bytes");
        }
        this.data = data;
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        data = in.readByteArray();
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        throw st.exception("no defined text format for NULL records");
    }

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
