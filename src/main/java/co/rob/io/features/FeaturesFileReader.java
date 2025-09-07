package co.rob.io.features;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Decomposed from FeaturesParserThread
 */
public class FeaturesFileReader implements AutoCloseable {
    private final FileInputStream fis;
    private final FileChannel channel;
    private final long size;
    private long position = 0;

    public FeaturesFileReader(File file) throws IOException {
        this.fis = new FileInputStream(file);
        this.channel = fis.getChannel();
        this.size = channel.size();
        skipUtf8BomIfPresent();
    }

    private void skipUtf8BomIfPresent() throws IOException {
        final byte[] utf8BOM = {(byte)0xef, (byte)0xbb, (byte)0xbf};
        byte[] bom = new byte[3];
        fis.read(bom);
        if (!(bom[0] == utf8BOM[0] && bom[1] == utf8BOM[1] && bom[2] == utf8BOM[2])) {
            channel.position(0); // rewind if no BOM
        } else {
            position = 3;
        }
    }

    public long size() { return size; }
    public long position() { return position; }

    public int readChunk(byte[] buffer) throws IOException {
        long remaining = size - position;
        int toRead = (int)Math.min(buffer.length, remaining);
        if (toRead <= 0) return -1;
        MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_ONLY, position, toRead);
        mbb.load();
        mbb.get(buffer, 0, toRead);
        position += toRead;
        return toRead;
    }

    @Override
    public void close() throws IOException {
        fis.close();
        channel.close();
    }
}
