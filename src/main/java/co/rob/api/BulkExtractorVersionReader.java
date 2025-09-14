package co.rob.api;

import co.rob.api.generated.HealthV1Api;
import co.rob.api.generated.invoker.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>BulkExtractorVersionChecker</code> class verifies the version of bulk_extractor
 * against the expected value.  The version must be in the format "#.#.#".
 */
public class BulkExtractorVersionReader {

    private static final Logger logger = LoggerFactory.getLogger(BulkExtractorVersionReader.class);

    private final HealthV1Api healthV1Api;

    public BulkExtractorVersionReader() {
        healthV1Api = new HealthV1Api(new ApiClientWrapper());
    }

    /**
     * Displays the bulk_extractor and BEViewer versions
     *
     * @return version details as a String
     */
    public String getVersion() throws ApiException {
        var response = healthV1Api.getHealthV1();
        logger.info("BulkExtractorVersionReader.readVersion: bulk_extractor version [{}]", response.getBulkExtractorVersion());
        return response.getBulkExtractorVersion();
    }
}

