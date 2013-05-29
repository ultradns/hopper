package biz.neustar.hopper.message;

/**
 * Processor for individual record for an incoming DNS Zone Transfer.
 *
 */import biz.neustar.hopper.record.Record;

public interface RecordProcessor {
    /**
     * This method process a record of given section of an incoming transfer.
     *
     * @param section The section to which record belongs.
     * @param message The transfer message to be filled up.
     * @param record The record to process.
     */
    void process(int section, final Message message, final Record record);
}
