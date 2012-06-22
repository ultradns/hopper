// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record.impl;

import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;

/**
 * Implements common functionality for the many record types whose format is a
 * single compressed name.
 * 
 * @author Brian Wellington
 */

public abstract class SingleCompressedNameBase extends SingleNameBase {

    private static final long serialVersionUID = -236435396815460677L;

    protected SingleCompressedNameBase() {
    }

    protected SingleCompressedNameBase(Name name, int type, DClass dclass,
            long ttl, Name singleName, String description) {
        super(name, type, dclass, ttl, singleName, description);
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        singleName.toWire(out, c, canonical);
    }

}
