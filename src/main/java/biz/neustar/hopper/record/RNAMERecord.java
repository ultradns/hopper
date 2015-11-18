/**
 * Copyright 2000-2015 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.SingleCompressedNameBase;

/**
 * RNAME is CNAME@Apex internal record type
 * 
 * @author Vitaliy Pavlyuk
 */

public class RNAMERecord extends SingleCompressedNameBase {

    private static final long serialVersionUID = -1242373886892538580L;

    public RNAMERecord() {
    }

    protected Record getObject() {
        return new RNAMERecord();
    }

    /**
     * Creates a new RNAMERecord with the given data
     *
     * @param target
     *            The name to which the RNAME points
     */
    public RNAMERecord(Name name, DClass dclass, long ttl, Name target) {
        super(name, Type.RNAME, dclass, ttl, target, "target");
    }

    /**
     * Gets the target of the RNAME Record
     */
    public Name getTarget() {
        return getSingleName();
    }

}
