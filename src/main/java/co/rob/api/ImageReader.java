package co.rob.api;

import co.rob.api.generated.ReadImageV1Api;
import co.rob.api.generated.invoker.ApiException;
import co.rob.ui.dialog.WError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The <code>BulkExtractorFileReader</code> class provides path reading services
 * using the bulk_extractor utility, which is capable of extracting path buffers.
 */
public class ImageReader {

    private static final Logger logger = LoggerFactory.getLogger(BulkExtractorVersionReader.class);

    private final ReadImageV1Api readImageV1Api;
    private final File imageFile;

    public record ImageReaderResponse(byte[] bytes, long totalSizeAtPath) {
    }

    public ImageReader(File newFile) {
        if (newFile == null) {
            throw new RuntimeException("imageReader");
        }
        imageFile = newFile;
        // this is the image file, used as `-http <image_file>` store for now, update BE code later
        readImageV1Api = new ReadImageV1Api(new ApiClientWrapper());
    }

    public ImageReaderResponse read(String forensicPath, long numBytes) throws ApiException {
        long rangeStopValue = ((numBytes > 0) ? numBytes - 1 : 0);
        var response = readImageV1Api.readImageV1(forensicPath, numBytes, rangeStopValue);
        return new ImageReaderResponse(response.getBytes(), response.getTotalSizeAtPath());
    }
}

