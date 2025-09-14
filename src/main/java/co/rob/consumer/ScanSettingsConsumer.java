package co.rob.consumer;

import co.rob.DaggerContext;
import co.rob.api.ApiClientWrapper;
import co.rob.api.generated.ScansV1Api;
import co.rob.api.generated.invoker.ApiClient;
import co.rob.api.generated.invoker.ApiException;
import co.rob.api.generated.model.ScanAcceptedResponseV1;
import co.rob.api.generated.model.ScanSettingsV1;
import co.rob.api.generated.model.ScannerV1;
import co.rob.pojo.ImageSourceType;
import co.rob.pojo.scan.ScanSettings;
import co.rob.pojo.scan.Scanner;
import co.rob.state.ScanSettingsListModel;
import co.rob.ui.dialog.WError;
import co.rob.ui.dialog.WScanProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * The <code>ScanSettingsConsumer</code> consumes Scan Settings jobs
 * one at a time as they become available in the scan settings run queue.
 * <p>
 * The consumer loops, consuming jobs, until it parks.
 * The provider provides unpark permits after enqueueing jobs.
 * This policy keeps the consumer going.
 * <p>
 * This object must be initialized after ScanSettingsListModel.
 */
public class ScanSettingsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ScanSettingsConsumer.class);

    private static final BlockingQueue<ScanSettings> queue = new LinkedBlockingQueue<>();

    private static final ReentrantLock pauseLock = new ReentrantLock();
    private static final Condition unpaused = pauseLock.newCondition();
    private static volatile boolean isPaused = false;

    private final ScanSettingsListModel scanSettingsListModel;
    private final ApiClient apiClient;

    /**
     * Just loading the constructor starts the consumer.
     * //TODO invoke this somewhere after JFrame creation
     */
    public ScanSettingsConsumer() {
        scanSettingsListModel = DaggerContext.get().scanSettingsListModel();
        apiClient = new ApiClientWrapper();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Hook into list model: enqueue jobs on intervalAdded
        scanSettingsListModel.addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {}
            public void intervalRemoved(ListDataEvent e) {}

            public void intervalAdded(ListDataEvent e) {
                ScanSettings s = scanSettingsListModel.remove();
                if (s != null) {
                    queue.offer(s);
                }
            }
        });

        executor.submit(this::consumeLoop);
    }

    /**
     * Pause the consumer so that it does not start another buk_extractor run
     * or restart the consumer.
     */
    public static void pauseConsumer(boolean doPause) {
        pauseLock.lock();
        try {
            isPaused = doPause;
            if (!isPaused) {
                unpaused.signalAll(); // wake up worker immediately
            }
        } finally {
            pauseLock.unlock();
        }
    }

    private void consumeLoop() {
        var scansV1Api = new ScansV1Api(apiClient);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                // block until a job is available
                ScanSettings scanSettings = queue.take();

                // respect pause flag
                pauseLock.lock();
                try {
                    while (isPaused) {
                        unpaused.await(); // block until unpaused
                    }
                } finally {
                    pauseLock.unlock();
                }

                logger.info("Starting bulk_extractor run: '{}'", scanSettings.getCommandString());

                ScanAcceptedResponseV1 scanId;
                try {
                    scanId = scansV1Api.startScanV1(ScanSettingsMapper.toDto(scanSettings));
                } catch (ApiException e) {
                    logger.error("Failed to POST scan settings '{}'", scanSettings, e);
                    WError.showErrorLater("bulk_extractor Scanner failed to start command\n'" +
                            scanSettings.getCommandString() + "'", "bulk_extractor failure", e);
                    continue;
                }

                WScanProgress.openWindow(scanSettings, scanId.getScanId(), apiClient);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static final class ScanSettingsMapper {

        private ScanSettingsMapper() {
            // Utility class
        }

        /**
         * Convert domain ScanSettings -> API DTO (ScanSettingsV1).
         */
        public static ScanSettingsV1 toDto(ScanSettings settings) {
            if (settings == null) {
                return null;
            }
            ScanSettingsV1 dto = new ScanSettingsV1();

            // required
            dto.setImageSourceType(ScanSettingsV1.ImageSourceTypeEnum.fromValue(settings.imageSourceType.name()));
            dto.setInputImage(settings.inputImage);
            dto.setOutdir(settings.outdir);

            // general options
            dto.setUseBannerFile(settings.useBannerFile);
            dto.setBannerFile(settings.bannerFile);
            dto.setUseAlertlistFile(settings.useAlertlistFile);
            dto.setAlertlistFile(settings.alertlistFile);
            dto.setUseStoplistFile(settings.useStoplistFile);
            dto.setStoplistFile(settings.stoplistFile);
            dto.setUseFindRegexTextFile(settings.useFindRegexTextFile);
            dto.setFindRegexTextFile(settings.findRegexTextFile);
            dto.setUseFindRegexText(settings.useFindRegexText);
            dto.setFindRegexText(settings.findRegexText);
            dto.setUseRandomSampling(settings.useRandomSampling);
            dto.setRandomSampling(settings.randomSampling);

            // tuning
            dto.setUseContextWindowSize(settings.useContextWindowSize);
            dto.setContextWindowSize(settings.contextWindowSize);
            dto.setUsePageSize(settings.usePageSize);
            dto.setPageSize(settings.pageSize);
            dto.setUseMarginSize(settings.useMarginSize);
            dto.setMarginSize(settings.marginSize);
            dto.setUseBlockSize(settings.useBlockSize);
            dto.setBlockSize(settings.blockSize);
            dto.setUseNumThreads(settings.useNumThreads);
            dto.setNumThreads(settings.numThreads);
            dto.setUseMaxRecursionDepth(settings.useMaxRecursionDepth);
            dto.setMaxRecursionDepth(settings.maxRecursionDepth);
            dto.setUseMaxWait(settings.useMaxWait);
            dto.setMaxWait(settings.maxWait);

            // parallelizing
            dto.setUseStartProcessingAt(settings.useStartProcessingAt);
            dto.setStartProcessingAt(settings.startProcessingAt);
            dto.setUseProcessRange(settings.useProcessRange);
            dto.setProcessRange(settings.processRange);
            dto.setUseAddOffset(settings.useAddOffset);
            dto.setAddOffset(settings.addOffset);

            // debugging
            dto.setUseStartOnPageNumber(settings.useStartOnPageNumber);
            dto.setStartOnPageNumber(settings.startOnPageNumber);
            dto.setUseDebugNumber(settings.useDebugNumber);
            dto.setDebugNumber(settings.debugNumber);
            dto.setUseEraseOutputDirectory(settings.useEraseOutputDirectory);

            // scanner controls
            dto.setUsePluginDirectories(settings.usePluginDirectories);
            dto.setPluginDirectories(settings.pluginDirectories);
            dto.setUseSettableOptions(settings.useSettableOptions);
            dto.setSettableOptions(settings.settableOptions);

            // scanners
            dto.setScanners(settings.getScanners().stream()
                    .map(ScanSettingsMapper::scannerToDto)
                    .collect(Collectors.toList()));

            return dto;
        }

        /**
         * Convert API DTO (ScanSettingsV1) -> domain ScanSettings.
         */
        public static ScanSettings toDomain(ScanSettingsV1 dto) {
            if (dto == null) {
                return null;
            }
            ScanSettings settings = new ScanSettings();

            // required
            settings.imageSourceType = ImageSourceType.valueOf(dto.getImageSourceType().name());
            settings.inputImage = dto.getInputImage();
            settings.outdir = dto.getOutdir();

            // general options
            settings.useBannerFile = safe(dto.getUseBannerFile());
            settings.bannerFile = dto.getBannerFile();
            settings.useAlertlistFile = safe(dto.getUseAlertlistFile());
            settings.alertlistFile = dto.getAlertlistFile();
            settings.useStoplistFile = safe(dto.getUseStoplistFile());
            settings.stoplistFile = dto.getStoplistFile();
            settings.useFindRegexTextFile = safe(dto.getUseFindRegexTextFile());
            settings.findRegexTextFile = dto.getFindRegexTextFile();
            settings.useFindRegexText = safe(dto.getUseFindRegexText());
            settings.findRegexText = dto.getFindRegexText();
            settings.useRandomSampling = safe(dto.getUseRandomSampling());
            settings.randomSampling = dto.getRandomSampling();

            // tuning
            settings.useContextWindowSize = safe(dto.getUseContextWindowSize());
            settings.contextWindowSize = dto.getContextWindowSize();
            settings.usePageSize = safe(dto.getUsePageSize());
            settings.pageSize = dto.getPageSize();
            settings.useMarginSize = safe(dto.getUseMarginSize());
            settings.marginSize = dto.getMarginSize();
            settings.useBlockSize = safe(dto.getUseBlockSize());
            settings.blockSize = dto.getBlockSize();
            settings.useNumThreads = safe(dto.getUseNumThreads());
            settings.numThreads = dto.getNumThreads();
            settings.useMaxRecursionDepth = safe(dto.getUseMaxRecursionDepth());
            settings.maxRecursionDepth = dto.getMaxRecursionDepth();
            settings.useMaxWait = safe(dto.getUseMaxWait());
            settings.maxWait = dto.getMaxWait();

            // parallelizing
            settings.useStartProcessingAt = safe(dto.getUseStartProcessingAt());
            settings.startProcessingAt = dto.getStartProcessingAt();
            settings.useProcessRange = safe(dto.getUseProcessRange());
            settings.processRange = dto.getProcessRange();
            settings.useAddOffset = safe(dto.getUseAddOffset());
            settings.addOffset = dto.getAddOffset();

            // debugging
            settings.useStartOnPageNumber = safe(dto.getUseStartOnPageNumber());
            settings.startOnPageNumber = dto.getStartOnPageNumber();
            settings.useDebugNumber = safe(dto.getUseDebugNumber());
            settings.debugNumber = dto.getDebugNumber();
            settings.useEraseOutputDirectory = safe(dto.getUseEraseOutputDirectory());

            // scanner controls
            settings.usePluginDirectories = safe(dto.getUsePluginDirectories());
            settings.pluginDirectories = dto.getPluginDirectories();
            settings.useSettableOptions = safe(dto.getUseSettableOptions());
            settings.settableOptions = dto.getSettableOptions();

            // scanners
            settings.scanners = dto.getScanners() != null
                    ? dto.getScanners().stream()
                    .map(ScanSettingsMapper::scannerToDomain)
                    .collect(Collectors.toList())
                    : List.of();

            return settings;
        }

        private static ScannerV1 scannerToDto(Scanner scanner) {
            ScannerV1 dto = new ScannerV1();
            dto.setName(scanner.getName());
            dto.setDefaultUse(scanner.isDefaultUseScanner());
            dto.setEnabled(scanner.isUseScanner());
            return dto;
        }

        private static Scanner scannerToDomain(ScannerV1 dto) {
            return new Scanner(dto.getName(), dto.getDefaultUse(), dto.getEnabled());
        }

        private static boolean safe(Boolean b) {
            return b != null && b;
        }
    }
}

