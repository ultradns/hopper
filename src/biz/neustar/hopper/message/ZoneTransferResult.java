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

/**
 * The result of a zone transfer.
 * @author mkube
 *
 */
public class ZoneTransferResult {

	/** True if an IXFR results is up to date */
	private boolean upToDate = false;
	
	/** The type of the received transfer */
	private ZoneTransferType type;
	
	/** The AXFR response */
	private List<Record> axfr = new ArrayList<Record>();
	
	/** The IXFR response */
	private List<Delta> ixfr = new ArrayList<Delta>();

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
}
