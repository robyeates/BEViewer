package co.rob.api;

import co.rob.api.generated.ScannersV1Api;
import co.rob.api.generated.invoker.ApiException;
import co.rob.api.generated.model.ScannerListResponseV1;
import co.rob.api.generated.model.ScannerV1;
import co.rob.pojo.scan.Scanner;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The <code>BulkExtractorScanListReader</code> class sets an array of scanners
 * indicating their name and whether they are enabled by default.
 */
public class BulkExtractorScanListReader {

    private final ScannersV1Api scannersV1Api;

    public BulkExtractorScanListReader() {
        this.scannersV1Api = new ScannersV1Api(new ApiClientWrapper());
    }

    /**
     * Read and set the scan list.
     */
    public List<Scanner> readScanList(boolean usePluginDirectories, String pluginDirectories) throws ApiException {
        String dirs = usePluginDirectories ? pluginDirectories : null;
        return ScannerMapper.toDomain(scannersV1Api.getScannersV1(dirs));
    }

    /**
     * Mapper for converting OpenAPI-generated scanner DTOs into
     * internal UI-facing Scanner objects.
     */
    private static final class ScannerMapper {

        private ScannerMapper() {
            // Utility class
        }

        public static List<Scanner> toDomain(ScannerListResponseV1 response) {
            if (response == null) {
                return List.of();
            }
            return response.getScanners().stream()
                    .map(ScannerMapper::toDomain)
                    .collect(Collectors.toList());
        }

        public static Scanner toDomain(ScannerV1 dto) {
            if (dto == null) {
                return null;
            }
            return new Scanner(
                    dto.getName(),
                    dto.getDefaultUse(),
                    dto.getEnabled()
            );
        }
    }
}

