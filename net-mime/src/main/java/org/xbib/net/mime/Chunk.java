package org.xbib.net.mime;

import java.io.IOException;
import java.nio.ByteBuffer;

final class Chunk {
    volatile Chunk next;
    volatile Data data;

    public Chunk(Data data) {
        this.data = data;
    }

    /**
     * Creates a new chunk and adds to linked list.
     *
     * @param dataHead of the linked list
     * @param buf MIME part partial data
     * @return created chunk
     */
    public Chunk createNext(DataHead dataHead, ByteBuffer buf) throws IOException {
        return next = new Chunk(data.createNext(dataHead, buf));
    }
}
