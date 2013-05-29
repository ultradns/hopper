package biz.neustar.hopper.message;

/**
 * Interface to handle wired format AXFR & IXFR.
 */
public interface WiredDataProcessor {
    /**
     * The method to handle wired format AXFR/IXFR as needed.
     *
     * @param handler The data handler.
     * @param wiredData The wired AXFR/IXFR.
     */
    void process(ZoneTransferHandler handler, byte[] wiredData);
}
