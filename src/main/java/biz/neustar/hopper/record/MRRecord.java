// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.SingleNameBase;

/**
 * Mailbox Rename Record - specifies a rename of a mailbox.
 * 
 * @author Brian Wellington
 */

public class MRRecord extends SingleNameBase {

    private static final long serialVersionUID = -5617939094209927533L;

    public MRRecord() {
    }

    protected Record getObject() {
        return new MRRecord();
    }

    /**
     * Creates a new MR Record with the given data
     * 
     * @param newName
     *            The new name of the mailbox specified by the domain. domain.
     */
    public MRRecord(Name name, DClass in, long ttl, Name newName) {
        super(name, Type.MR, in, ttl, newName, "new name");
    }

    /** Gets the new name of the mailbox specified by the domain */
    public Name getNewName() {
        return getSingleName();
    }

}
