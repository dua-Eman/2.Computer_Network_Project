import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import java.util.List;

public class MessagePanel extends JPanel {
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private static final Color DEFAULT_BORDER_COLOR = new Color(14, 81, 119);
    private static final Color PATH_FOUND_BORDER_COLOR = new Color(42, 75, 218);
    private static final Color PAPER_BORDER_COLOR = new Color(139, 69, 19);
    private static final Color PAPER_PATH_FOUND_BORDER_COLOR = new Color(101, 67, 33);
    private static final int ARC_SIZE = 20;
    private static final int MARGIN = 10;
    private static final int PADDING = 10;
    private boolean isPaperView = false;

    public MessagePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(0, 188, 212));
    }

    public void setPaperView(boolean paperView) {
        this.isPaperView = paperView;
        setBackground(paperView ? new Color(210, 180, 140) : new Color(0, 188, 212));
        repaint();
    }

    public void addMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (messages.isEmpty() || !isSimilarMessage(messages.get(messages.size() - 1), message)) {
                messages.add(message);
            } else {
                int lastIndex = messages.size() - 1;
                messages.set(lastIndex, messages.get(lastIndex) + "\n" + message);
            }
            revalidate();
            repaint();
            // Auto-scroll to the bottom
            SwingUtilities.invokeLater(() -> {
                JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
                if (scrollPane != null) {
                    JScrollBar vertical = scrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
            });
        });
    }

    public void clearMessages() {
        SwingUtilities.invokeLater(() -> {
            messages.clear();
            revalidate();
            repaint();
        });
    }

    private boolean isSimilarMessage(String msg1, String msg2) {
        String[] prefixes = {"Visiting:", "Enqueueing:"};
        for (String prefix : prefixes) {
            if (msg1.startsWith(prefix) && msg2.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(getFont());

        int y = MARGIN;
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight() + PADDING;

        for (String message : messages) {
            String[] lines = message.split("\n");
            int maxWidth = 0;
            for (String line : lines) {
                maxWidth = Math.max(maxWidth, fm.stringWidth(line));
            }
            int width = maxWidth + 40;
            int height = (lines.length * lineHeight) + PADDING;

            // Beautiful gradient border
            Color borderColor = message.startsWith("Best path found:") ?
                    (isPaperView ? PAPER_PATH_FOUND_BORDER_COLOR : PATH_FOUND_BORDER_COLOR) :
                    (isPaperView ? PAPER_BORDER_COLOR : DEFAULT_BORDER_COLOR);
            GradientPaint gradient = new GradientPaint(
                    MARGIN, y, borderColor,
                    MARGIN + width, y + height, Color.BLACK, true
            );
            g2.setPaint(gradient);
            g2.setStroke(new BasicStroke(4));
            g2.drawRoundRect(MARGIN - 2, y - 2, width + 4, height + 4, ARC_SIZE + 4, ARC_SIZE + 4);

            // Subtle shadow effect
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillRoundRect(MARGIN + 2, y + 2, width, height, ARC_SIZE, ARC_SIZE);

            // Inner background
            g2.setColor(new Color(255, 255, 255));
            g2.fillRoundRect(MARGIN, y, width, height, ARC_SIZE, ARC_SIZE);

            // Message text
            g2.setColor(Color.BLACK);
            for (int i = 0; i < lines.length; i++) {
                g2.drawString(lines[i], MARGIN + 20, y + (i + 1) * lineHeight - 5);
            }

            y += height + MARGIN;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int maxWidth = 0;
        int totalHeight = MARGIN;

        for (String message : messages) {
            String[] lines = message.split("\n");
            int messageWidth = 0;
            for (String line : lines) {
                messageWidth = Math.max(messageWidth, fm.stringWidth(line));
            }
            maxWidth = Math.max(maxWidth, messageWidth + 60);
            totalHeight += (lines.length * (fm.getHeight() + PADDING)) + PADDING + MARGIN;
        }

        // Ensure minimum width and height
        maxWidth = Math.max(maxWidth, 300);
        totalHeight = Math.max(totalHeight, 100);

        return new Dimension(maxWidth, totalHeight);
    }
}