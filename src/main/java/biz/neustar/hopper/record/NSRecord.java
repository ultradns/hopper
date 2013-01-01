// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.SingleCompressedNameBase;

/**
 * Name Server Record - contains the name server serving the named zone
 * 
 * @author Brian Wellington
 */

public class NSRecord extends SingleCompressedNameBase {

    private static final long serialVersionUID = 487170758138268838L;

    public NSRecord() {
    }

    protected Record getObject() {
        return new NSRecord();
    }

    /**
     * Creates a new NS Record with the given data
     * 
     * @param target
     *            The name server for the given domain
     */
    public NSRecord(Name name, DClass dclass, long ttl, Name target) {
        super(name, Type.NS, dclass, ttl, target, "target");
    }

    /** Gets the target of the NS Record */
    public Name getTarget() {
        return getSingleName();
    }

    public Name getAdditionalName() {
        return getSingleName();
    }

}
