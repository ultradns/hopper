// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.resolver;

import java.util.ArrayList;
import java.util.List;

import biz.neustar.hopper.message.Zone;
import biz.neustar.hopper.record.CNAMERecord;
import biz.neustar.hopper.record.DNAMERecord;
import biz.neustar.hopper.record.RRSet;

/**
 * The Response from a query to Cache.lookupRecords() or Zone.findRecords()
 * 
 * @see Cache
 * @see Zone
 * [not thread-safe]
 * @author Brian Wellington
 */

public class SetResponse {

    /**
     * The Cache contains no information about the requested name/type
     */
    public static final int UNKNOWN = 0;

    /**
     * The Zone does not contain the requested name, or the Cache has determined
     * that the name does not exist.
     */
    public static final int NXDOMAIN = 1;

    /**
     * The Zone contains the name, but no data of the requested type, or the
     * Cache has determined that the name exists and has no data of the
     * requested type.
     */
    public static final int NXRRSET = 2;

    /**
     * A delegation enclosing the requested name was found.
     */
    public static final int DELEGATION = 3;

    /**
     * The Cache/Zone found a CNAME when looking for the name.
     * 
     * @see CNAMERecord
     */
    public static final int CNAME = 4;

    /**
     * The Cache/Zone found a DNAME when looking for the name.
     * 
     * @see DNAMERecord
     */
    public static final int DNAME = 5;

    /**
     * The Cache/Zone has successfully answered the question for the requested
     * name/type/class.
     */
    public static final int SUCCESSFUL = 6;

    private static final SetResponse unknown = new SetResponse(UNKNOWN);
    private static final SetResponse nxdomain = new SetResponse(NXDOMAIN);
    private static final SetResponse nxrrset = new SetResponse(NXRRSET);

    private int type;
    private Object data;

    private SetResponse() {
    }

    public SetResponse(int type, RRSet rrset) {
        if (type < 0 || type > 6) {
            throw new IllegalArgumentException("invalid type");
        }
        this.type = type;
        this.data = rrset;
    }

    public SetResponse(int type) {
        if (type < 0 || type > 6) {
            throw new IllegalArgumentException("invalid type");
        }
        this.type = type;
        this.data = null;
    }

    public static SetResponse ofType(int type) {
        switch (type) {
        case UNKNOWN:
            return unknown;
        case NXDOMAIN:
            return nxdomain;
        case NXRRSET:
            return nxrrset;
        case DELEGATION:
        case CNAME:
        case DNAME:
        case SUCCESSFUL:
            SetResponse sr = new SetResponse();
            sr.type = type;
            sr.data = null;
            return sr;
        default:
            throw new IllegalArgumentException("invalid type");
        }
    }

    // mutator making this not thread-safe
    public void addRRset(RRSet rrset) {
        if (data == null) {
            data = new ArrayList<RRSet>();
        }
        List<RRSet> l = (List<RRSet>) data;
        l.add(rrset);
    }

    /** Is the answer to the query unknown? */
    public boolean isUnknown() {
        return (type == UNKNOWN);
    }

    /** Is the answer to the query that the name does not exist? */
    public boolean isNXDOMAIN() {
        return (type == NXDOMAIN);
    }

    /** Is the answer to the query that the name exists, but the type does not? */
    public boolean isNXRRSET() {
        return (type == NXRRSET);
    }

    /** Is the result of the lookup that the name is below a delegation? */
    public boolean isDelegation() {
        return (type == DELEGATION);
    }

    /** Is the result of the lookup a CNAME? */
    public boolean isCNAME() {
        return (type == CNAME);
    }

    /** Is the result of the lookup a DNAME? */
    public boolean isDNAME() {
        return (type == DNAME);
    }

    /** Was the query successful? */
    public boolean isSuccessful() {
        return (type == SUCCESSFUL);
    }

    /** If the query was successful, return the answers */
    public RRSet[] answers() {
        if (type != SUCCESSFUL) {
            return null;
        }
        List<RRSet> l = (List<RRSet>) data;
        return l.toArray(new RRSet[0]);
    }

    /**
     * If the query encountered a CNAME, return it.
     */
    public CNAMERecord getCNAME() {
        return (CNAMERecord) ((RRSet) data).first();
    }

    /**
     * If the query encountered a DNAME, return it.
     */
    public DNAMERecord getDNAME() {
        return (DNAMERecord) ((RRSet) data).first();
    }

    /**
     * If the query hit a delegation point, return the NS set.
     */
    public RRSet getNS() {
        return (RRSet) data;
    }

    /** Prints the value of the SetResponse */
    public String toString() {
        switch (type) {
        case UNKNOWN:
            return "unknown";
        case NXDOMAIN:
            return "NXDOMAIN";
        case NXRRSET:
            return "NXRRSET";
        case DELEGATION:
            return "delegation: " + data;
        case CNAME:
            return "CNAME: " + data;
        case DNAME:
            return "DNAME: " + data;
        case SUCCESSFUL:
            return "successful";
        default:
            throw new IllegalStateException();
        }
    }

}
