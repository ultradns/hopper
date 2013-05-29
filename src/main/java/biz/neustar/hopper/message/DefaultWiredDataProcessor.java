package biz.neustar.hopper.message;

/**
 * The default wired data processor. Nothing is done on wired AXFR/IXFR
 * by default data processor.
 */
public class DefaultWiredDataProcessor implements WiredDataProcessor {

    @Override
    public void process(ZoneTransferHandler handler, byte[] wiredData) {
        // Nothing is processed by default.
    }

}
