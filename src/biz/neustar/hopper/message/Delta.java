package biz.neustar.hopper.message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import biz.neustar.hopper.record.Record;

/**
 * All changes between two versions of a zone in an IXFR response.
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public class Delta {

    /** A list of records added between the start and end versions */
    private List<Record> adds = new LinkedList<Record>();

    /** A list of records deleted between the start and end versions */
    private List<Record> deletes = new LinkedList<Record>();

    public Delta() {
        adds = new ArrayList<Record>();
        deletes = new ArrayList<Record>();
    }

	protected List<Record> getAdds() {
		return adds;
	}

	protected List<Record> getDeletes() {
		return deletes;
	}

}
