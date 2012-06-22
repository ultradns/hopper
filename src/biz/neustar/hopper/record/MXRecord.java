// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.U16NameBase;

/**
 * Mail Exchange - specifies where mail to a domain is sent
 * 
 * @author Brian Wellington
 */

public class MXRecord extends U16NameBase {

    private static final long serialVersionUID = 2914841027584208546L;

    public MXRecord() {
    }

    protected Record getObject() {
        return new MXRecord();
    }

    /**
     * Creates an MX Record from the given data
     * 
     * @param priority
     *            The priority of this MX. Records with lower priority are
     *            preferred.
     * @param target
     *            The host that mail is sent to
     */
    public MXRecord(Name name, DClass in, long ttl, int priority, Name target) {
        super(name, Type.MX, in, ttl, priority, "priority", target,
                "target");
    }

    /** Returns the target of the MX record */
    public Name getTarget() {
        return getNameField();
    }

    /** Returns the priority of this MX record */
    public int getPriority() {
        return getU16Field();
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeU16(u16Field);
        nameField.toWire(out, c, canonical);
    }

    public Name getAdditionalName() {
        return getNameField();
    }

}
