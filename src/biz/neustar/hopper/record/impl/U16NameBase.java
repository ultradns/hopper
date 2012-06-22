// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record.impl;

import java.io.IOException;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.util.Tokenizer;

/**
 * Implements common functionality for the many record types whose format is an
 * unsigned 16 bit integer followed by a name.
 * 
 * @author Brian Wellington
 */

public abstract class U16NameBase extends Record {

    private static final long serialVersionUID = -8315884183112502995L;

    protected int u16Field;
    protected Name nameField;

    protected U16NameBase() {
    }

    protected U16NameBase(Name name, int type, DClass dclass, long ttl) {
        super(name, type, dclass, ttl);
    }

    protected U16NameBase(Name name, int type, DClass dclass, long ttl,
            int u16Field, String u16Description, Name nameField,
            String nameDescription) {
        super(name, type, dclass, ttl);
        this.u16Field = checkU16(u16Description, u16Field);
        this.nameField = checkName(nameDescription, nameField);
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        u16Field = in.readU16();
        nameField = new Name(in);
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        u16Field = st.getUInt16();
        nameField = st.getName(origin);
    }

    public String rrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(u16Field);
        sb.append(" ");
        sb.append(nameField);
        return sb.toString();
    }

    protected int getU16Field() {
        return u16Field;
    }

    protected Name getNameField() {
        return nameField;
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU16(u16Field);
        nameField.toWire(out, null, canonical);
    }

}
