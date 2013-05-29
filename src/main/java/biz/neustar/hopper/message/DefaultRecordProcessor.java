package biz.neustar.hopper.message;

import biz.neustar.hopper.record.Record;

/**
 * Default processor for individual record for an incoming DNS Zone Transfer.
 */
public class DefaultRecordProcessor implements RecordProcessor {

    /**
     * This method process a record of given section of an incoming transfer.
     *
     * @param section The section to which record belongs.
     * @param message The transfer message to be filled up.
     * @param record The record to process.
     */
    @Override
    public void process(final int section,
            final Message message, final Record record) {
        message.addRecordWithoutHeaderUpdate(record, section);
    }
}
