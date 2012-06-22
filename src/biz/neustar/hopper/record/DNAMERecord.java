// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.SingleNameBase;

/**
 * DNAME Record - maps a nonterminal alias (subtree) to a different domain
 * 
 * @author Brian Wellington
 */

public class DNAMERecord extends SingleNameBase {

    private static final long serialVersionUID = 2670767677200844154L;

    public DNAMERecord() {
    }

    protected Record getObject() {
        return new DNAMERecord();
    }

    /**
     * Creates a new DNAMERecord with the given data
     * 
     * @param alias
     *            The name to which the DNAME alias points
     */
    public DNAMERecord(Name name, DClass in, long ttl, Name alias) {
        super(name, Type.DNAME, in, ttl, alias, "alias");
    }

    /**
     * Gets the target of the DNAME Record
     */
    public Name getTarget() {
        return getSingleName();
    }

    /** Gets the alias specified by the DNAME Record */
    public Name getAlias() {
        return getSingleName();
    }

}
