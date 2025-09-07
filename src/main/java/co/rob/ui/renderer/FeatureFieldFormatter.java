package co.rob.ui.renderer;

import co.rob.state.ImageModel;
import co.rob.util.UTF8Tools;
import co.rob.util.log.LogUtils;
import com.google.common.net.InetAddresses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The <code>FeatureFieldFormatter</code> formats a Feature's feature field
 * into a more presentable formatted form.
 * NOTE: returned values are truncated to avoid Swing hanging when working with large strings.
 */
public class FeatureFieldFormatter {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFieldFormatter.class);

    // possible EXIF headers
    private static final byte[] EXIF_0 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0};
    private static final byte[] EXIF_1 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe1};
    private static final byte[] EXIF_2 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe2};
    private static final byte[] EXIF_3 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe3};
    private static final byte[] EXIF_4 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe4};
    private static final byte[] EXIF_5 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe5};
    private static final byte[] EXIF_6 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe6};
    private static final byte[] EXIF_7 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe7};
    private static final byte[] EXIF_8 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe8};
    private static final byte[] EXIF_9 = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe9};
    private static final byte[] EXIF_A = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xea};
    private static final byte[] EXIF_B = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xeb};
    private static final byte[] EXIF_C = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xec};
    private static final byte[] EXIF_D = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xed};
    private static final byte[] EXIF_E = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xee};
    private static final byte[] EXIF_F = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xef};

    // photoshop PSD header
    private static final byte[] PSD = {'8', 'B', 'P', 'S', 0x00, 0x01};

    // TIFF marker
    private static final byte[] TIFF_MM = {'M', 'M', 0, 42};
    private static final byte[] TIFF_II = {'I', 'I', 42, 0};

    // EXIF, PSD, and TIFF define byte[][] EXIF
    private static final Vector<byte[]> EXIF = new Vector<byte[]>();

    static {
        EXIF.add(EXIF_0);
        EXIF.add(EXIF_1);
        EXIF.add(EXIF_2);
        EXIF.add(EXIF_3);
        EXIF.add(EXIF_4);
        EXIF.add(EXIF_5);
        EXIF.add(EXIF_6);
        EXIF.add(EXIF_7);
        EXIF.add(EXIF_8);
        EXIF.add(EXIF_9);
        EXIF.add(EXIF_A);
        EXIF.add(EXIF_B);
        EXIF.add(EXIF_C);
        EXIF.add(EXIF_D);
        EXIF.add(EXIF_E);
        EXIF.add(EXIF_F);
        EXIF.add(EXIF_C);
        EXIF.add(EXIF_D);
        EXIF.add(EXIF_E);
        EXIF.add(EXIF_F);
        EXIF.add(PSD);
        EXIF.add(TIFF_MM);
        EXIF.add(TIFF_II);
    }

    // ELF
    private static final byte[] ELF_MAGIC_NUMBER = {(byte) 0x7f, 'E', 'L', 'F'};
    private static final Vector<byte[]> ELF = new Vector<byte[]>();

    static {
        ELF.add(ELF_MAGIC_NUMBER);
    }

    private static final byte[] WINPE_MAGIC_NUMBER = {'P', 'E', (byte) 0, (byte) 0};
    private static final Vector<byte[]> WINPE = new Vector<byte[]>();

    static {
        WINPE.add(WINPE_MAGIC_NUMBER);
    }

    private static final int MAX_CHAR_WIDTH = 1000;
    // for parsing input such as exif which is formatted in XML
    private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    private FeatureFieldFormatter() {
    }

    /**
     * Provides feature text in a format suitable for reading.
     */
    public static String getFormattedFeatureText(File featuresFile, byte[] featureField, byte[] contextField) {
        String filename = featuresFile.getName();
        String formattedText = switch (filename) {
            case "gps.txt" -> getGPSFormat(contextField);
            case "exif.txt" -> getEXIFFormat(contextField);
            case "elf.txt" -> getELFFormat(contextField);
            case "winpe.txt" -> getWINPEFormat(contextField);
            default -> getGenericFormat(featureField);
        };

        // NOTE: Swing bug: Swing fails when attempting to render long strings,
        // so truncate long text to MAX_CHAR_WIDTH characters.
        if (formattedText.length() > MAX_CHAR_WIDTH) {
            // truncate and append ellipses ("...")
            formattedText = formattedText.substring(0, MAX_CHAR_WIDTH) + "…";
        }
        logger.debug("FeatureFieldFormatter formattedText: '{}'", formattedText);
        return formattedText;
    }

    private static String getGPSFormat(byte[] contextField) {
        // show GPS data as-is, encoded in the context field
        return new String(contextField);
    }

    private static String getEXIFFormat(byte[] contextField) {
        StringBuilder builder = new StringBuilder();

        // provide values as "<key>=<value>"
        try {
            final DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();

            // get string into document
            byte[] exifContextField = UTF8Tools.unescapeEscape(contextField);
            Document document = documentBuilder.parse(new ByteArrayInputStream(exifContextField));

            Element rootElement = document.getDocumentElement();
            NodeList nodeList = rootElement.getChildNodes();

            int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                Node node = nodeList.item(i);
                if (node.getFirstChild() != null) {
                    String key = node.getNodeName();
                    String value = node.getFirstChild().getNodeValue();
                    // shorten the key by removing suffix before "."
                    key = StringUtils.substringAfterLast(key, ".");
                    // append the key and value
                    builder.append(key).append("=").append(value);

                    if (i != length - 1) {
                        builder.append(", ");
                    }
                }
            }
        } catch (Exception e) {
            // let it go.
            builder.append("<EXIF parser failed, please see log>");
            logger.error("FeatureFieldFormatter.getEXIFFormat parser failure:", e);
            logger.error(LogUtils.formatLogBytes("on unescaped byte sequence", UTF8Tools.unescapeBytes(contextField)));
            logger.error(LogUtils.formatLogBytes("From escaped byte sequence", contextField));
        }

        return builder.toString();
    }

    // note: XML parsed output would look better.
    private static String getELFFormat(byte[] contextField) {
        byte[] elfContextField = UTF8Tools.unescapeEscape(contextField);
        return new String(elfContextField);
    }

    // note: XML parsed output would look better.
    private static String getWINPEFormat(byte[] contextField) {
        byte[] winPEContextField = UTF8Tools.unescapeEscape(contextField);
        return new String(winPEContextField);
    }

    private static String getGenericFormat(byte[] featureField) {
        // keep string pretty much as is
        byte[] escapedBytes = featureField;

        // if probable utf16, strip all NULLs, right or not.
        if (UTF8Tools.escapedLooksLikeUTF16(escapedBytes)) {
            escapedBytes = UTF8Tools.stripNulls(escapedBytes);
        }

        // unescape any escaped escape character so it looks better
        escapedBytes = UTF8Tools.unescapeEscape(escapedBytes);

        // use this as text
        return new String(escapedBytes, UTF8Tools.UTF_8);
    }

    // ************************************************************
    // Image highlight text
    // ************************************************************

    /**
     * Provides a vector of possible matching features as they would appear in the Image.
     */
    public static List<byte[]> getImageHighlightVector(ImageModel.ImagePage imagePage) {
        if (imagePage.featureLine() == null || imagePage.featureLine().featuresFile() == null) {
            return new ArrayList<>();
        }
        String filename = imagePage.featureLine().featuresFile().getName();

        switch (filename) {
            case "gps.txt" -> {
                return EXIF;
            }
            case "exif.txt" -> {
                return EXIF;
            }
            case "ip.txt" -> {
                return getIPImageFormat(imagePage);
            }
            case "tcp.txt" -> {
                return getTCPImageFormat(imagePage);
            }
            case "elf.txt" -> {
                return ELF;
            }
            case "winpe.txt" -> {
                return WINPE;
            }
            default -> {
                // for all else, add various UTF encodings of the feature field
                List<byte[]> textVector = new ArrayList<>();

                byte[] utf8Feature;
                byte[] utf16Feature;
                // for all else, add UTF8 and UTF16 encodings based on the feature field
                if (UTF8Tools.escapedLooksLikeUTF16(imagePage.featureLine().featureField())) {
                    // calculate UTF8 and set UTF16
                    utf8Feature = UTF8Tools.utf16To8Correct(imagePage.featureLine().featureField());
                    utf16Feature = imagePage.featureLine().featureField();
                } else {
                    // set UTF8 and calculate UTF16
                    utf8Feature = imagePage.featureLine().featureField();
                    utf16Feature = UTF8Tools.utf8To16Correct(imagePage.featureLine().featureField());
                }

                // now add the two filters
                textVector.add(utf8Feature);
                textVector.add(utf16Feature);

                return textVector;
            }
        }
    }

    /**
     * Now with IPv6 support!
     */
    private static byte[] getIPBytes(String ipString) {
        if (ipString == null || ipString.isBlank()) {
            return null;
        }

        // Strip off any port number (handles both IPv4 and IPv6 addresses with ports)
        int portMarkerIndex = ipString.lastIndexOf(':');
        if (portMarkerIndex >= 0) {
            // Correctly handle IPv6 addresses that have multiple colons
            if (ipString.contains("]")) { // Example: [::1]:8080
                ipString = ipString.substring(0, ipString.lastIndexOf("]:") + 1);
            } else { // Example: 192.168.1.1:8080
                ipString = ipString.substring(0, portMarkerIndex);
            }
        }

        // get the four IP values as an array of four strings
        String[] ipArray = ipString.split("\\.");

        // fail if there are not four values
        if (ipArray.length != 4) {
            return null;
        }

        try {
            InetAddress address = InetAddresses.forString(ipString);
            return address.getAddress();
        } catch (IllegalArgumentException e) {
            // Thrown by InetAddresses.forString() if the IP is malformed
            return null;
        }
    }


    /**
     * [2001:db8:85a3::8a2e:370:7334]:80
     * 192.168.1.1:8080
     */
    private static List<byte[]> getIPImageFormat(ImageModel.ImagePage imagePage) {
        // create the text vector to be returned
        List<byte[]> textVector = new ArrayList<>();

        // get the feature text which should contain IP as "D.D.D.D" where D is a decimal
        String featureString = new String(imagePage.featureLine().featureField());

        // get the IP bytes directly from the feature string
        byte[] ipBytes = getIPBytes(featureString);
        if (ipBytes == null) {
            // fail
            logger.warn("FeatureFieldFormatter.getIPImageFormat: Invalid IP in feature: ['{}']", featureString);
        } else {
            // add to Vector
            textVector.add(ipBytes);
        }

        return textVector;
    }

    private static List<byte[]> getTCPImageFormat(ImageModel.ImagePage imagePage) {
        if (imagePage.featureLine() == null || imagePage.featureLine().featuresFile() == null) {
            return new ArrayList<>();
        }

        // create the text vector to be returned
        List<byte[]> textVector = new ArrayList<>();

        // get the feature text which should contain source and destination IPs
        String featureString = new String(imagePage.featureLine().featureField());

        // split the feature string into parts, source D.D.D.D and destination D.D.D.D
        String[] featureFieldArray = featureString.split(" ");
        if (featureFieldArray.length < 3) {
            // fail
            logger.warn("FeatureFieldFormatter.getIPImageFormat: Invalid TCP in feature: ['{}']", featureString);
            return textVector;
        }

        // add source IP
        byte[] sourceIPBytes = getIPBytes(featureFieldArray[0]);
        if (sourceIPBytes == null) {
            // fail
            logger.warn("FeatureFieldFormatter.getIPImageFormat: Invalid source TCP in feature: ['{}']", featureString);
        } else {
            // add to Vector
            textVector.add(sourceIPBytes);
        }

        // add destination IP
        byte[] destinationIPBytes = getIPBytes(featureFieldArray[2]);
        if (destinationIPBytes == null) {
            // fail
            logger.warn("FeatureFieldFormatter.getIPImageFormat: Invalid destination TCP in feature: ['{}']", featureString);
        } else {
            // add to Vector
            textVector.add(destinationIPBytes);
        }

        return textVector;
    }
}

