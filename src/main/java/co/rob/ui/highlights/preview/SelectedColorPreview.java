package co.rob.ui.highlights.preview;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SelectedColorPreview extends JButton {
    private Color color;

    public SelectedColorPreview() {
        setColor(Color.YELLOW);
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (color != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);

            // Draw a filled rounded rectangle
            int width = getWidth();
            int height = getHeight();
            int arc = 8; // Adjust for a pleasing curve
            g2.fill(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));

            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(16, 16);
    }
}