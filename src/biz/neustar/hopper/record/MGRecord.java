// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.SingleNameBase;

/**
 * Mail Group Record - specifies a mailbox which is a member of a mail group.
 * 
 * @author Brian Wellington
 */

public class MGRecord extends SingleNameBase {

    private static final long serialVersionUID = -3980055550863644582L;

    public MGRecord() {
    }

    protected Record getObject() {
        return new MGRecord();
    }

    /**
     * Creates a new MG Record with the given data
     * 
     * @param mailbox
     *            The mailbox that is a member of the group specified by the
     *            domain.
     */
    public MGRecord(Name name, DClass dclass, long ttl, Name mailbox) {
        super(name, Type.MG, dclass, ttl, mailbox, "mailbox");
    }

    /** Gets the mailbox in the mail group specified by the domain */
    public Name getMailbox() {
        return getSingleName();
    }

}
