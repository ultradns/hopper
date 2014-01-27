package biz.neustar.hopper.nio;

/**
 * This method provides the chunked stream to consume data in chunks.
 *
 * @param <V> Type of chunk.
 */
public interface ChunkedStream <V> {
    /**
     * Returns {@code true} if and only if there is any data left in the
     * stream.
     *
     * @return A boolean value.
     *
     * @throws Exception In case of any error during iteration.
     */
    boolean hasNextChunk() throws Exception;

    /**
     * Fetches a chunked data from the stream.
     * @return the fetched chunk. {@code null} if there is no data left in the
     * stream.
     * @throws Exception In case of any error during iteration.
     */
    V nextChunk() throws Exception;

}
