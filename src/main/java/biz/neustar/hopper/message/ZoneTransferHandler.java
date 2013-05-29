package biz.neustar.hopper.message;

import biz.neustar.hopper.exception.ZoneTransferException;
import biz.neustar.hopper.record.Record;

/**
 * Callback interface for streaming XFRers
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public interface ZoneTransferHandler {
	/**
	 * Handles a Zone Transfer.
	 */

	/**
	 * Called when an AXFR transfer begins.
	 */
	void startAXFR() throws ZoneTransferException;

	/**
	 * Called when an IXFR transfer begins.
	 */
	void startIXFR() throws ZoneTransferException;

	/**
	 * Called when a series of IXFR deletions begins.
	 * 
	 * @param soa
	 *            The starting SOA.
	 */
	void startIXFRDeletes(Record soa) throws ZoneTransferException;

	/**
	 * Called when a series of IXFR adds begins.
	 * 
	 * @param soa
	 *            The starting SOA.
	 */
	void startIXFRAdds(Record soa) throws ZoneTransferException;

	/**
	 * Called for each content record in an AXFR.
	 * 
	 * @param r
	 *            The DNS record.
	 */
	void handleRecord(Record r) throws ZoneTransferException;

	/**
	 * Called when wired format AXFR/IXFR is available to be processed.
	 *
	 * @param wiredData The wired format AXFR/IXFR data.
	 */
	void handleWiredData(byte [] wiredData);

	/**
	 * Returns record processor for the handler.
	 *
	 * @return The record processor.
	 */
	RecordProcessor getRecordProcessor();
};
