package co.rob.util;

import co.rob.DaggerContext;
import co.rob.pojo.scan.ScanSettings;
import co.rob.state.BEPreferences;
import co.rob.state.ScanSettingsListModel;
import co.rob.ui.dialog.WError;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Parses command line arguments and executes up to one command.
 * <p>
 * Supported commands:
 * <ul>
 *   <li>{@code -s "<scanner arguments>"} : Start a bulk_extractor scan with given arguments</li>
 *   <li>{@code -clear_preferences} : Clear saved preferences</li>
 * </ul>
 *
 * Example usage:
 * <pre>
 *   beviewer.exe -clear_preferences
 *   ./beviewer.app/Contents/MacOS/beviewer -s "-o output_dir input.dd"
 *   ./beviewer/bin/beviewer -s "-o output_dir input.dd"
 * </pre>
 */
public class BEArgsParser {

    private BEArgsParser() {
        // prevent instantiation
    }

    /**
     * Inner class that defines available CLI options.
     */
    @Command(name = "beviewer",
            mixinStandardHelpOptions = true,
            description = "Bulk Extractor Viewer command line interface.")
    static class Args implements Runnable {

        @Option(names = "-s",
                paramLabel = "\"<scanner arguments>\"",
                description = "Run a bulk_extractor scan with the given arguments. "
                        + "Wrap scanner args in quotes if they contain spaces.")
        String scanArgs;

        @Option(names = "-clear_preferences",
                description = "Clear all saved preferences.")
        boolean clearPreferences;

        @Override
        public void run() {
            // validate exclusive options
            if (scanArgs != null && clearPreferences) {
                WError.showMessage("Only one command may be specified at a time.",
                        "Command Line Error");
                return;
            }

            if (scanArgs != null) {
                runScan(scanArgs);
            } else if (clearPreferences) {
                BEPreferences.clearPreferences();
            }
        }

        private void runScan(String args) {
            ScanSettings scanSettings = new ScanSettings(args);

            ScanSettingsListModel scanSettingsListModel = DaggerContext.get().scanSettingsListModel();
            scanSettingsListModel.add(scanSettings);
        }
    }

    /**
     * Entry point for parsing arguments.
     * @param args command line arguments
     */
    public static void parseArgs(String[] args) {
        if (args.length == 0) {
            return; // no arguments, nothing to do
        }

        CommandLine cmd = new CommandLine(new Args());
        try {
            cmd.execute(args);
        } catch (CommandLine.ParameterException ex) {
            // Invalid usage → show error in GUI and print usage to console
            WError.showMessage(ex.getMessage(), "Command Line Error");
            cmd.usage(System.out);
        }
    }
}
