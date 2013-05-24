/*
 * *
 *  * Copyright 2000-2011 NeuStar, Inc. All rights reserved.
 *  * NeuStar, the Neustar logo and related names and logos are registered
 *  * trademarks, service marks or tradenames of NeuStar, Inc. All other
 *  * product names, company names, marks, logos and symbols may be trademarks
 *  * of their respective owners.
 *
 */
package biz.neustar.hopper.message;

import java.util.ArrayList;
import java.util.List;

import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.record.SOARecord;

/**
 * The result of a zone transfer.
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class ZoneTransferResult implements ZoneTransferHandler {

	/** True if an IXFR results is up to date */
	private boolean upToDate = true;

	/** The type of the received transfer */
	private ZoneTransferType type;

	/** The AXFR response */
	private List<Record> axfr = new ArrayList<Record>();

	/** The IXFR response */
	private List<Delta> ixfr = new ArrayList<Delta>();

	public void startAXFR() {
		upToDate = false;
		axfr = new ArrayList<Record>();
	}

	public void startIXFR() {
		upToDate = false;
		ixfr = new ArrayList<Delta>();
	}

	public void startIXFRDeletes(Record soa) {
		Delta delta = new Delta();
		delta.getDeletes().add(soa);
		delta.setStart(getSOASerial(soa));
		ixfr.add(delta);
	}

	public void startIXFRAdds(Record soa) {
		Delta delta = (Delta) ixfr.get(ixfr.size() - 1);
		delta.getAdds().add(soa);
		delta.setEnd(getSOASerial(soa));
	}

	public void handleRecord(Record r) {
		List<Record> list;
		if (!ixfr.isEmpty()) {
			Delta delta = (Delta) ixfr.get(ixfr.size() - 1);
			if (delta.getAdds().size() > 0)
				list = delta.getAdds();
			else
				list = delta.getDeletes();
		} else
			list = axfr;
		list.add(r);
	}

	public boolean isUpToDate() {
		return upToDate;
	}

	public void setUpToDate(boolean upToDate) {
		this.upToDate = upToDate;
	}

	public ZoneTransferType getType() {
		return type;
	}

	public void setType(ZoneTransferType type) {
		this.type = type;
	}

	public List<Record> getAxfr() {
		return axfr;
	}

	public void setAxfr(List<Record> axfr) {
		this.axfr = axfr;
	}

	public List<Delta> getIxfr() {
		return ixfr;
	}

	public void setIxfr(List<Delta> ixfr) {
		this.ixfr = ixfr;
	}

	protected static long getSOASerial(Record rec) {
		SOARecord soa = (SOARecord) rec;
		return soa.getSerial();
	}

}
