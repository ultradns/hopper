// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.SingleNameBase;

/**
 * NSAP Pointer Record - maps a domain name representing an NSAP Address to a
 * hostname.
 * 
 * @author Brian Wellington
 */

public class NSAP_PTRRecord extends SingleNameBase {

    private static final long serialVersionUID = 2386284746382064904L;

    public NSAP_PTRRecord() {
    }

    protected Record getObject() {
        return new NSAP_PTRRecord();
    }

    /**
     * Creates a new NSAP_PTR Record with the given data
     * 
     * @param target
     *            The name of the host with this address
     */
    public NSAP_PTRRecord(Name name, DClass dclass, long ttl, Name target) {
        super(name, Type.NSAP_PTR, dclass, ttl, target, "target");
    }

    /** Gets the target of the NSAP_PTR Record */
    public Name getTarget() {
        return getSingleName();
    }

}
