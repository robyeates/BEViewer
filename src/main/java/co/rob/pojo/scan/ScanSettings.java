package co.rob.pojo.scan;

import co.rob.io.BulkExtractorScanListReader;
import co.rob.pojo.ImageSourceType;
import co.rob.ui.dialog.WError;
import co.rob.ui.dialog.scan.WScanBoxedControls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * The <code>ScanSettings</code> class is a container for all bulk_extractor
 * scan command options and provides interfaces for getting, setting, and
 * displaying these options.
 */

public class ScanSettings {

    private static final Logger logger = LoggerFactory.getLogger(ScanSettings.class);

    // required parameters
    public ImageSourceType imageSourceType;
    public String inputImage;
    public String outdir;

    // general options
    public boolean useBannerFile;
    public String bannerFile;
    public boolean useAlertlistFile;
    public String alertlistFile;
    public boolean useStoplistFile;
    public String stoplistFile;
    public boolean useFindRegexTextFile;
    public String findRegexTextFile;
    public boolean useFindRegexText;
    public String findRegexText;
    public boolean useRandomSampling;
    public String randomSampling;

    // tuning parameters
    public boolean useContextWindowSize;
    public String contextWindowSize;
    public boolean usePageSize;
    public String pageSize;
    public boolean useMarginSize;
    public String marginSize;
    public boolean useBlockSize;
    public String blockSize;
    public boolean useNumThreads;
    public String numThreads;
    public boolean useMaxRecursionDepth;
    public String maxRecursionDepth;
    public boolean useMaxWait;
    public String maxWait;

    // parallelizing
    public boolean useStartProcessingAt;
    public String startProcessingAt;
    public boolean useProcessRange;
    public String processRange;
    public boolean useAddOffset;
    public String addOffset;

    // Debugging options
    public boolean useStartOnPageNumber;
    public String startOnPageNumber;
    public boolean useDebugNumber;
    public String debugNumber;
    public boolean useEraseOutputDirectory;

    // Scanner controls
    public boolean usePluginDirectories;
    public String pluginDirectories;
    public boolean useSettableOptions;
    public String settableOptions;

    // scanners
    private List<Scanner> scanners;

    // parse state
    public boolean validTokens = true;

    /**
     * instantiate with default values
     */
    public ScanSettings() {

        // required parameters
        imageSourceType = ImageSourceType.IMAGE_FILE;
        inputImage = "";
        outdir = "";

        // general options
        useBannerFile = false;
        bannerFile = "";
        useAlertlistFile = false;
        alertlistFile = "";
        useStoplistFile = false;
        stoplistFile = "";
        useFindRegexTextFile = false;
        findRegexTextFile = "";
        useFindRegexText = false;
        findRegexText = "";
        useRandomSampling = false;
        randomSampling = "";

        // tuning parameters
        useContextWindowSize = false;
        contextWindowSize = "16";
        usePageSize = false;
        pageSize = "16777216";
        useMarginSize = false;
        marginSize = "4194304";
        useBlockSize = false;
        blockSize = "512";
        useNumThreads = false;
        numThreads = Integer.toString(Runtime.getRuntime().availableProcessors());
        useMaxRecursionDepth = false;
        maxRecursionDepth = "7";
        useMaxWait = false;
        maxWait = "60";

        // parallelizing
        useStartProcessingAt = false;
        startProcessingAt = "";
        useProcessRange = false;
        processRange = "";
        useAddOffset = false;
        addOffset = "";

        // Debugging options
        useStartOnPageNumber = false;
        startOnPageNumber = "0";
        useDebugNumber = false;
        debugNumber = "1";
        useEraseOutputDirectory = false;

        // Scanner controls
        usePluginDirectories = false;
        pluginDirectories = "";
        useSettableOptions = false;
        settableOptions = "";

        // scanners
        try {
            // get the default scanner list from bulk_extractor
            scanners = BulkExtractorScanListReader.readScanList(
                    WScanBoxedControls.isUsePluginDirectory(),
                    WScanBoxedControls.getPluginDirectoriesTextFieldText());
        } catch (IOException e) {
            WError.showError("""
                    Error in obtaining list of scanners from bulk_extractor.
                    Bulk_extractor is not available during this session.
                    Is bulk_extractor installed?""", "bulk_extractor failure", e);
            scanners = new Vector<>();
        }
    }

    /**
     * instantiate by copying
     */
    public ScanSettings(ScanSettings scanSettings) {

        // required parameters
        imageSourceType = scanSettings.imageSourceType;
        inputImage = scanSettings.inputImage;
        outdir = scanSettings.outdir;

        // general options
        useBannerFile = scanSettings.useBannerFile;
        bannerFile = scanSettings.bannerFile;
        useAlertlistFile = scanSettings.useAlertlistFile;
        alertlistFile = scanSettings.alertlistFile;
        useStoplistFile = scanSettings.useStoplistFile;
        stoplistFile = scanSettings.stoplistFile;
        useFindRegexTextFile = scanSettings.useFindRegexTextFile;
        findRegexTextFile = scanSettings.findRegexTextFile;
        useFindRegexText = scanSettings.useFindRegexText;
        findRegexText = scanSettings.findRegexText;
        useRandomSampling = scanSettings.useRandomSampling;
        randomSampling = scanSettings.randomSampling;

        // tuning parameters
        useContextWindowSize = scanSettings.useContextWindowSize;
        contextWindowSize = scanSettings.contextWindowSize;
        usePageSize = scanSettings.usePageSize;
        pageSize = scanSettings.pageSize;
        useMarginSize = scanSettings.useMarginSize;
        marginSize = scanSettings.marginSize;
        useBlockSize = scanSettings.useBlockSize;
        blockSize = scanSettings.blockSize;
        useNumThreads = scanSettings.useNumThreads;
        numThreads = scanSettings.numThreads;
        useMaxRecursionDepth = scanSettings.useMaxRecursionDepth;
        maxRecursionDepth = scanSettings.maxRecursionDepth;
        useMaxWait = scanSettings.useMaxWait;
        maxWait = scanSettings.maxWait;

        // parallelizing
        useStartProcessingAt = scanSettings.useStartProcessingAt;
        startProcessingAt = scanSettings.startProcessingAt;
        useProcessRange = scanSettings.useProcessRange;
        processRange = scanSettings.processRange;
        useAddOffset = scanSettings.useAddOffset;
        addOffset = scanSettings.addOffset;

        // Debugging options
        useStartOnPageNumber = scanSettings.useStartOnPageNumber;
        startOnPageNumber = scanSettings.startOnPageNumber;
        useDebugNumber = scanSettings.useDebugNumber;
        debugNumber = scanSettings.debugNumber;
        useEraseOutputDirectory = scanSettings.useEraseOutputDirectory;

        // Scanner controls
        usePluginDirectories = scanSettings.usePluginDirectories;
        pluginDirectories = scanSettings.pluginDirectories;
        useSettableOptions = scanSettings.useSettableOptions;
        settableOptions = scanSettings.settableOptions;

        // scanners
        scanners = new Vector<>();
        for (Scanner scanner : scanSettings.scanners) {
            scanners.add(new Scanner(scanner.getName(), scanner.isDefaultUseScanner(), scanner.isUseScanner()));
        }
    }

    /**
     * instantiate from a String
     */
    public ScanSettings(String scanSettingsString) {
        // start from default and then modify with input string
        this();
        String[] command = scanSettingsString.split("\\s+");

        // parse input string, modifying default
        int index = 0;
        int parameterCount = 0;
        while (index < command.length) {
            String a = command[index];
            String b = (index + 1 < command.length) ? command[index + 1] : "";

            // allow the first token to be the bulk_extractor program name
            if (index == 0 && a.equals("bulk_extractor")) {
                index++;

                // required parameters
            } else if (a.equals("-o")) {
                outdir = b;
                index += 2;

                // general options
            } else if (a.equals("-b")) {
                useBannerFile = true;
                bannerFile = b;
                index += 2;
            } else if (a.equals("-r")) {
                useAlertlistFile = true;
                alertlistFile = b;
                index += 2;
            } else if (a.equals("-w")) {
                useStoplistFile = true;
                stoplistFile = b;
                index += 2;
            } else if (a.equals("-F")) {
                useFindRegexTextFile = true;
                findRegexTextFile = b;
                index += 2;
            } else if (a.equals("-s")) {
                useRandomSampling = true;
                randomSampling = b;
                index += 2;

                // tuning parameters
            } else if (a.equals("-C")) {
                useContextWindowSize = true;
                contextWindowSize = b;
                index += 2;
            } else if (a.equals("-G")) {
                usePageSize = true;
                pageSize = b;
                index += 2;
            } else if (a.equals("-g")) {
                useMarginSize = true;
                marginSize = b;
                index += 2;
            } else if (a.equals("-B")) {
                useBlockSize = true;
                blockSize = b;
                index += 2;
            } else if (a.equals("-j")) {
                useNumThreads = true;
                numThreads = b;
                index += 2;
            } else if (a.equals("-M")) {
                useMaxRecursionDepth = true;
                maxRecursionDepth = b;
                index += 2;
            } else if (a.equals("-m")) {
                useMaxWait = true;
                maxWait = b;
                index += 2;

                // parallelizing
            } else if (a.equals("-Y")) {
                if (b.indexOf('-') == -1) {
                    // use <o1> form
                    useStartProcessingAt = true;
                    startProcessingAt = b;
                    index += 2;
                } else {
                    // use <o1>-<o2> form
                    useProcessRange = true;
                    processRange = b;
                    index += 2;
                }
            } else if (a.equals("-A")) {
                useAddOffset = true;
                addOffset = b;
                index += 2;

                // debugging
            } else if (a.equals("-z")) {
                useStartOnPageNumber = true;
                startOnPageNumber = b;
                index += 2;
            } else if (a.equals("-d")) {
                useDebugNumber = true;
                debugNumber = b;
                index += 2;
            } else if (a.equals("-Z")) {
                useEraseOutputDirectory = true;
                index += 1;

                // controls
            } else if (a.equals("-P")) {
                usePluginDirectories = true;
                pluginDirectories = b;
                index += 2;
            } else if (a.equals("-S")) {
                useSettableOptions = true;
                if (!settableOptions.isEmpty()) {
                    // add "|" to separate this from the last
                    settableOptions += "|"; //TODO Hmmmm.
                }
                settableOptions += b;
                index += 2;

                // scanners
            } else if (a.equals("-x")) {
                boolean foundX = false;
                for (Scanner xScanner : scanners) {
                    if (b.equals(xScanner.getName())) {
                        xScanner.setUseScanner(false);
                        foundX = true;
                        break;
                    }
                }
                if (!foundX) {
                    validTokens = false;
                    logger.error("ScanSettings -x parse error: no scanner named '{}'", b);
                    WError.showError("Invalid scanner name '" + b + "'", "Scanner deselection error", null);
                }
                index += 2;
            } else if (a.equals("-e")) {
                boolean foundE = false;
                for (Scanner eScanner : scanners) {
                    if (b.equals(eScanner.getName())) {
                        eScanner.setUseScanner(true);
                        foundE = true;
                        break;
                    }
                }
                if (!foundE) {
                    validTokens = false;
                    logger.error("ScanSettings -e parse error: no scanner named '{}'", b);
                    WError.showError("Invalid scanner name '" + b + "'", "Scanner selection error", null);
                }
                index += 2;
            } else if (a.equals("-R")) {
                imageSourceType = ImageSourceType.DIRECTORY_OF_FILES;
                index += 1;
            } else {
                // parse this as a parameter rather than as an option
                inputImage = a;
                parameterCount++;
                index += 1;
            }
        }

        // validate that the parsing went to completion
        if (index != command.length || parameterCount != 1) {
            // mark that the input tokens were invalid
            validTokens = false;

            // indicate that the input tokens were invalid
            logger.error("ScanSettings input error: '{}'", scanSettingsString);
            logger.error("ScanSettings command length: {}, parameter count: {}", command.length, parameterCount);
            WError.showError("Invalid scan settings text: '" + scanSettingsString + "'", "Scan Settings Text error", null);
        }
    }

    public List<Scanner> getScanners() {
        return Collections.unmodifiableList(scanners);
    }

    public String[] getCommandArray() {
        Vector<String> cmd = new Vector<>();

        // basic usage: bulk_extractor [options] imagefile
        // program name
        cmd.add("bulk_extractor");

        // options
        // required parameters
        if (!outdir.isEmpty()) {
            // don't emit "-o" unless outdir exists
            cmd.add("-o");
            cmd.add(outdir);
        }
        // general options
        if (useBannerFile) {
            cmd.add("-b");
            cmd.add(bannerFile);
        }
        if (useAlertlistFile) {
            cmd.add("-r");
            cmd.add(alertlistFile);
        }
        if (useStoplistFile) {
            cmd.add("-w");
            cmd.add(stoplistFile);
        }
        if (useFindRegexTextFile) {
            cmd.add("-F");
            cmd.add(findRegexTextFile);
        }
        if (useFindRegexText) {
            cmd.add("-f");
            cmd.add(findRegexText);
        }
        if (useRandomSampling) {
            cmd.add("-s");
            cmd.add(randomSampling);
        }

        // tuning parameters
        if (useContextWindowSize) {
            cmd.add("-C");
            cmd.add(contextWindowSize);
        }
        if (usePageSize) {
            cmd.add("-G");
            cmd.add(pageSize);
        }
        if (useMarginSize) {
            cmd.add("-g");
            cmd.add(marginSize);
        }
        if (useBlockSize) {
            cmd.add("-B");
            cmd.add(blockSize);
        }
        if (useNumThreads) {
            cmd.add("-j");
            cmd.add(numThreads);
        }
        if (useMaxRecursionDepth) {
            cmd.add("-M");
            cmd.add(maxRecursionDepth);
        }
        if (useMaxWait) {
            cmd.add("-m");
            cmd.add(maxWait);
        }

        // parallelizing
        if (useStartProcessingAt) {
            cmd.add("-Y");
            cmd.add(startProcessingAt);
        }
        if (useProcessRange) {
            cmd.add("-Y");
            cmd.add(processRange);
        }
        if (useAddOffset) {
            cmd.add("-A");
            cmd.add(addOffset);
        }

        // Debugging
        if (useStartOnPageNumber) {
            cmd.add("-z");
            cmd.add(startOnPageNumber);
        }
        if (useDebugNumber) {
            cmd.add("-d" + debugNumber);
        }
        if (useEraseOutputDirectory) {
            cmd.add("-Z");
        }

        // controls
        if (usePluginDirectories && !pluginDirectories.isEmpty()) {
            String[] pluginDirectoriesArray = pluginDirectories.split("\\|");
            for (String directoryName : pluginDirectoriesArray) {
                cmd.add("-P");
                cmd.add(directoryName);
            }
        }
        if (useSettableOptions && !settableOptions.isEmpty()) {
            String[] settableOptionsArray = settableOptions.split("\\|");
            for (String optionName : settableOptionsArray) {
                cmd.add("-S");
                cmd.add(optionName);
            }
        }

        // Scanners
        for (Scanner scanner : scanners) {
            if (scanner.isDefaultUseScanner()) {
                if (!scanner.isUseScanner()) {
                    // disable this scanner that is enabled by default
                    cmd.add("-x");
                    cmd.add(scanner.getName());
                }
            } else {
                if (scanner.isUseScanner()) {
                    // enable this scanner that is disabled by default
                    cmd.add("-e");
                    cmd.add(scanner.getName());
                }
            }
        }

        // required imagefile or directory of files
        if (imageSourceType == ImageSourceType.DIRECTORY_OF_FILES) {
            // recurse through a directory of files
            cmd.add("-R");
        }
        cmd.add(inputImage);

        // convert the cmd vector to string array
        // return the command string array
        return cmd.toArray(new String[0]);
    }

    public String getCommandString() {
        String[] commandArray = getCommandArray();

        // build the string by concatenating tokens separated by space
        // and quoting any tokens containing space.
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < commandArray.length; i++) {
            // append space separator between command array parts
            if (i > 0) {
                builder.append(" ");
            }

            // append command array part
            if (commandArray[i].contains(" ")) {
                // append with quotes
                builder.append("\"").append(commandArray[i]).append("\"");
            } else {
                // append without quotes
                builder.append(commandArray[i]);
            }
        }
        return builder.toString();
    }

    // validator for integers
    private boolean isInt(String value, String name) {
        try {
            int i = Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            WError.showError("Invalid input for " + name + ": " + value, "Settings error", null);
            return false;
        }
    }

    /**
     * This is a courtesy function for providing some validation of the settings,
     * returns true if existing checks pass.
     */
    public boolean validateSomeSettings() {

        // required parameters

        // validate the input image
        File image = new File(inputImage);
        if (imageSourceType == ImageSourceType.IMAGE_FILE) {
            // validate the image file as readable and not a directory
            if (image.isDirectory() || !image.canRead()) {
                WError.showError("The image file provided,\n'" + inputImage + "', is not valid." + "\nPlease verify that this path exists and is accessible.", "Settings error", null);
                return false;
            }
        } else if (imageSourceType == ImageSourceType.RAW_DEVICE) {
            // validate the device as readable and not a directory
            if (image.isDirectory() || !image.canRead()) {
                WError.showError("The image device provided,\n'" + inputImage + "', is not valid." + "\nPlease verify that this path exists and is accessible.", "Settings error", null);
                return false;
            }
        } else if (imageSourceType == ImageSourceType.DIRECTORY_OF_FILES) {
            // validate the input directory
            if (!image.isDirectory() || !image.canRead()) {
                WError.showError("The input image directory provided,\n'" + inputImage + "', is not valid." + "\nPlease verify that this path exists and is accessible.", "Settings error", null);
                return false;
            }
        }

        // validate that the directory above the output feature directory exists
        File directory = new File(outdir);
        File parent = directory.getParentFile();
        if (parent == null || !parent.isDirectory()) {
            WError.showError("The folder to contain Output Feature directory\n'" + directory + "' is not valid." + "\nPlease verify that this folder exists and is accessible.", "Settings error", null);
            return false;
        }

        // general options

        // banner file
        if (useBannerFile) {
            File bf = new File(bannerFile);
            if (!bf.isFile() || !bf.canRead()) {
                WError.showError("The banner file \n'" + bannerFile + "' is not valid." + "\nPlease verify that this file exists and is accessible.", "Settings error", null);
                return false;
            }
        }
        // alert list file
        if (useAlertlistFile) {
            File alf = new File(alertlistFile);
            if (!alf.isFile() || !alf.canRead()) {
                WError.showError("The alert list file \n'" + alertlistFile + "' is not valid." + "\nPlease verify that this file exists and is accessible.", "Settings error", null);
                return false;
            }
        }
        // stoplist file
        if (useStoplistFile) {
            File slf = new File(stoplistFile);
            if (!slf.isFile() || !slf.canRead()) {
                WError.showError("The alert list file \n'" + stoplistFile + "' is not valid." + "\nPlease verify that this file exists and is accessible.", "Settings error", null);
                return false;
            }
        }
        // find regex text file
        if (useFindRegexTextFile) {
            File frtf = new File(findRegexTextFile);
            if (!frtf.isFile() || !frtf.canRead()) {
                WError.showError("The alert list file \n'" + findRegexTextFile + "' is not valid." + "\nPlease verify that this file exists and is accessible.", "Settings error", null);
                return false;
            }
        }

        // tuning parameters
        if (useContextWindowSize && !isInt(contextWindowSize, "context window size")) {
            return false;
        }
        if (usePageSize && !isInt(pageSize, "page size")) {
            return false;
        }
        if (useMarginSize && !isInt(marginSize, "margin size")) {
            return false;
        }
        if (useBlockSize && !isInt(blockSize, "block size")) {
            return false;
        }
        if (useNumThreads && !isInt(numThreads, "number of threads")) {
            return false;
        }
        if (useMaxRecursionDepth && !isInt(maxRecursionDepth, "maximum recursion depth")) {
            return false;
        }

        // parallelizing
        // no checks at this time

        // debugging options
        if (useStartOnPageNumber && !isInt(startOnPageNumber, "start on page number")) {
            return false;
        }
        return !useDebugNumber || isInt(debugNumber, "debug mode number");

        // scanner controls
        // no checks at this time

        // scanners
        // no checks
    }
}

