package biz.neustar.hopper.message;

import java.util.LinkedList;
import java.util.List;

import biz.neustar.hopper.record.Record;

/**
 * All changes between two versions of a zone in an IXFR response.
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public class Delta {

    /** A list of records added between the start and end versions */
    private final List<Record> adds = new LinkedList<Record>();

    /** A list of records deleted between the start and end versions */
    private final List<Record> deletes = new LinkedList<Record>();
    
    /** The starting SOA serial number */
    private long start;

	/** The ending SOA serial number */
    private long end;

    public Delta() {
    }

	protected List<Record> getAdds() {
		return adds;
	}

	protected List<Record> getDeletes() {
		return deletes;
	}

    public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

}
