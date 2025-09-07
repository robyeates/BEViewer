package co.rob.ui.icons;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class SvgIconLoader {

    private SvgIconLoader() {}

    /**
     * Load an SVG from the classpath and convert it into an ImageIcon.
     *
     * @param resourcePath Path to the SVG (e.g. "/icons/bookmark.svg").
     * @param size Target icon size (width & height in px).
     * @return Swing ImageIcon
     * @throws IOException if the resource cannot be read
     */
    public static @NotNull ImageIcon loadSvgIcon(String resourcePath, int size) throws IOException {
        URL url = SvgIconLoader.class.getResource(resourcePath);
        if (url == null) {
            throw new IOException("SVG resource not found: " + resourcePath);
        }

        // Load SVG
        SVGLoader loader = new SVGLoader();
        SVGDocument doc = loader.load(url);

        // Rasterize at the requested size
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            Objects.requireNonNull(doc).render(null, g);
        } finally {
            g.dispose();
        }

        return new ImageIcon(img);
    }
}