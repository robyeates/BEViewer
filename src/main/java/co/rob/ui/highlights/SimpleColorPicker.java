package co.rob.ui.highlights;

import co.rob.ui.selection.HighlightColorSelectionModel;
import com.formdev.flatlaf.util.ScaledEmptyBorder;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class SimpleColorPicker extends JPanel {

    private HighlightColorSelectionModel selectionModel;
    private ColorPaletteComponent colorPalette;

    //public SimpleColorPicker() {
    //    this(new ColorPickerSelectionModel());
    //}

    public SimpleColorPicker(HighlightColorSelectionModel selectionModel) {
        init(selectionModel);
    }

    private void init(HighlightColorSelectionModel selectionModel) {
        setLayout(new MigLayout("wrap,fillx,gap 0,insets 0 0 5 0", "[fill,280]"));
        setSelectionModel(selectionModel);
        add(createColorPalette());
    }

    private Component createColorPalette() {
        colorPalette = new ColorPaletteComponent(new ColorPaletteData(), new ColorPaletteItemPainter());
        colorPalette.addChangeListener(_ -> {
            Color color = colorPalette.getColorAt(colorPalette.getSelectedIndex());
            if (color != null) {
                getSelectionModel().setSelectedColor(color);
            }
        });
        return colorPalette;
    }

    public HighlightColorSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(HighlightColorSelectionModel selectionModel) {
        this.selectionModel = Objects.requireNonNull(selectionModel, "color selectionModel can't be null");
    }

    public Color getSelectedColor() {
        return getSelectionModel().getSelectedColor();
    }

    public static Color showDialog(Component component, String title, SimpleColorPicker colorPicker) {
        int option = JOptionPane.showConfirmDialog(component, colorPicker, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return (option == JOptionPane.OK_OPTION) ? colorPicker.getSelectedColor() : null;
    }

    private static class ColorPaletteItemPainter {
        private final Dimension itemSize = new Dimension(20, 20);
        public Dimension getItemSize() { return itemSize; }
        public int getMaxRow() { return 2; }
        public int getItemGap() { return 10; }
        public int getItemBorderSize() { return 3; }
        protected float getArc() { return 6; }
       // protected Color getBorderColor(Color color) { return null; }

        public void paintItem(Graphics g, Color color, int width, int height, boolean isSelected, boolean hasFocus) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            float arc = Math.min(UIScale.scale(getArc()), Math.min(width, height));
            g2.fill(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
            int border = UIScale.scale(getItemBorderSize()); // TODO filter out dialog buttons
            if (border > 0) {
                g2.translate(-border, -border);
                paintItemBorder(g2, width + border * 2, height + border * 2, isSelected, hasFocus);
            }
        }

        protected void paintItemBorder(Graphics2D g2, int width, int height, boolean isSelected, boolean hasFocus) {
            if (!isSelected && !hasFocus) return;
            g2.setComposite(AlphaComposite.SrcOver.derive(isSelected ? 0.6f : 0.35f));
            int border = UIScale.scale(getItemBorderSize());
            float arc = UIScale.scale(getArc() + getItemBorderSize() * 2);
            arc = Math.min(arc, Math.min(width, height));
            Area area = new Area(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
            int inWidth = width - border * 2;
            int inHeight = height - border * 2;
            float inArc = Math.min(UIScale.scale(getArc()), Math.min(inWidth, inHeight));
            area.subtract(new Area(new RoundRectangle2D.Float(border, border, inWidth, inHeight, inArc, inArc)));
            g2.fill(area);
        }
    }

    public static class ColorPaletteComponent extends JComponent {
        private final List<Item> items = new ArrayList<>();
        private final ColorPaletteData colorData;
        private final ColorPaletteItemPainter itemPainter;
        private int hoverIndex = -1;
        private int selectedIndex = -1;

        @SuppressWarnings("ClassEscapesDefinedScope")
        public ColorPaletteComponent(ColorPaletteData colorData, ColorPaletteItemPainter itemPainter) {
            this.colorData = colorData;
            this.itemPainter = itemPainter;
            init();
        }

        private void init() {
            setBorder(new ScaledEmptyBorder(10, 10, 5, 10));
            MouseAdapter mouseListener = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        int index = getIndexOf(e.getPoint());
                        if (index != -1) setSelectedIndex(index);
                    }
                }
                @Override
                public void mouseMoved(MouseEvent e) {
                    int index = getIndexOf(e.getPoint());
                    if (hoverIndex != index) {
                        int oldIndex = hoverIndex;
                        hoverIndex = index;
                        repaintAt(hoverIndex);
                        repaintAt(oldIndex);
                    }
                }
            };
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseListener);
        }

        public int getSelectedIndex() { return selectedIndex; }

        public void setSelectedIndex(int selectedIndex) {
            int oldIndex = this.selectedIndex;
            this.selectedIndex = selectedIndex;
            repaintAt(oldIndex);
            repaintAt(selectedIndex);
            if (oldIndex != selectedIndex) selectedIndex(selectedIndex);
        }

        public Color getColorAt(int index) {
            return (index >= 0 && index < items.size()) ? items.get(index).getColor() : null;
        }

        public void addChangeListener(ChangeListener listener) { listenerList.add(ChangeListener.class, listener); }

        private int getIndexOf(Point point) {
            return IntStream.range(0, items.size())
                    .filter(i -> items.get(i) != null && items.get(i).getSize().contains(point))
                    .findFirst()
                    .orElse(-1);
        }

        private void repaintAt(int index) {
            if (index >= 0 && index < items.size()) {
                int border = UIScale.scale(itemPainter.getItemBorderSize());
                Rectangle itemRec = items.get(index).getSize();
                int x = itemRec.x - border;
                int y = itemRec.y - border;
                int width = itemRec.width + border * 2;
                int height = itemRec.height + border * 2;
                repaint(x, y, width, height);
            }
        }

        private void selectedIndex(int index) {
            if (index >= 0 && index < items.size()) fireSelectionChanged(new ChangeEvent(this));
        }

        @Override
        protected void paintComponent(Graphics g) {
            prepareItemSize();
            items.stream().filter(Objects::nonNull).forEach(item -> item.paint(g));
            super.paintComponent(g);
        }

        private void prepareItemSize() {
            Insets insets = getInsets();
            int width = getWidth() - (insets.left + insets.right);
            int gap = UIScale.scale(itemPainter.getItemGap());
            Dimension itemSize = UIScale.scale(itemPainter.getItemSize());
            int itemCount = colorData.size();
            int column = (width + gap) / (itemSize.width + gap);
            int targetWidth = (column * itemSize.width) + ((column - 1) * gap);
            int x = insets.left + (width - targetWidth) / 2;
            int y = insets.top;
            items.clear();

            IntStream.range(0, itemCount).forEach(i -> {
                int row = i / column;
                int col = i % column;
                Color color = colorData.get(i);
                int lx = x + col * (itemSize.width + gap);
                int ly = y + row * (itemSize.height + gap);
                items.add(new Item(color, i, lx, ly, itemSize.width, itemSize.height));
            });
        }

        @Override
        public Dimension getPreferredSize() {
            int maxRow = itemPainter.getMaxRow();
            int gap = UIScale.scale(itemPainter.getItemGap());
            Insets insets = getInsets();
            Dimension itemSize = UIScale.scale(itemPainter.getItemSize());
            int height = (maxRow * itemSize.height) + ((maxRow - 1) * gap) + (insets.top + insets.bottom);
            return new Dimension(50, height);
        }

        public void fireSelectionChanged(ChangeEvent event) {
            Object[] listeners = listenerList.getListenerList();
            IntStream.iterate(listeners.length - 2, i -> i >= 0, i -> i - 2)
                    .filter(i -> listeners[i] == ChangeListener.class)
                    .forEach(i -> ((ChangeListener) listeners[i + 1]).stateChanged(event));
        }

        private class Item {
            private final Color color;
            private final int index;
            private final Rectangle size;

            public Item(Color color, int index, int x, int y, int width, int height) {
                this.color = color;
                this.index = index;
                this.size = new Rectangle(x, y, width, height);
            }

            public Color getColor() { return color; }
            public Rectangle getSize() { return size; }
            public boolean isSelected() { return selectedIndex == index; }
            public boolean isHasFocus() { return hoverIndex == index; }

            public void paint(Graphics g) {
                if (g.getClip() == null || g.getClip().intersects(size)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.translate(size.x, size.y);
                    itemPainter.paintItem(g2, color, size.width, size.height, isSelected(), isHasFocus());
                    g2.dispose();
                }
            }
        }
    }

    private static class ColorPaletteData {
        private final List<Color> listColors = new ArrayList<>();

        public ColorPaletteData() {
            init();
        }

        private void init() {
            final int alpha = 102; // 40% of 255 ≈ 102
            listColors.addAll(List.of(
                    new Color(239, 68, 68, alpha),   new Color(249, 115, 22, alpha),   new Color(234, 179, 8, alpha),
                    new Color(132, 204, 22, alpha),  new Color(34, 197, 94, alpha),    new Color(16, 185, 129, alpha),
                    new Color(20, 184, 166, alpha),  new Color(6, 182, 212, alpha),    new Color(14, 165, 233, alpha),
                    new Color(59, 130, 246, alpha),  new Color(99, 102, 241, alpha),   new Color(139, 92, 246, alpha),
                    new Color(168, 85, 247, alpha),  new Color(217, 70, 239, alpha),   new Color(236, 72, 153, alpha),
                    new Color(244, 63, 94, alpha),   new Color(251, 146, 60, alpha),   new Color(202, 138, 4, alpha)
            ));
        }


        public int size() { return listColors.size(); }
        public Color get(int i) {
            return listColors.get(i);
        }
    }
}