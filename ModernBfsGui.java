import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import javax.swing.*;

// 🌟 BFS Visualizer X – Enhanced Version with Directed Edges, Node/Edge Logging, and Robust Image Loading

public class ModernBfsGui extends JFrame {
    private final GraphCanvas canvas = new GraphCanvas();
    private final JTextField sourceField = new JTextField(5);
    private final JTextField destField = new JTextField(5);
    private final MessagePanel messagePanel = new MessagePanel();
    private final JScrollPane rightScroll;

    public ModernBfsGui() {
        setTitle("BFS Visualizer X - Final");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(new Color(40, 40, 70));

        JLabel sourceLabel = new JLabel("Source:");
        sourceLabel.setForeground(Color.WHITE);
        JLabel destLabel = new JLabel("Destination:");
        destLabel.setForeground(Color.WHITE);

        JButton bfsBtn = new JButton("Start BFS");
        JButton resetBtn = new JButton("Reset Graph");
        JButton deleteNodeBtn = new JButton("Delete Node");
        JButton undoBtn = new JButton("Undo");
        JButton toggleViewBtn = new JButton("Paper View");
        JCheckBox directedGraphCheckBox = new JCheckBox("Directed Graph", false);
        directedGraphCheckBox.setToolTipText("Toggle between directed and undirected edges");
        directedGraphCheckBox.setForeground(Color.black);

        topPanel.add(sourceLabel);
        topPanel.add(sourceField);
        topPanel.add(destLabel);
        topPanel.add(destField);
        topPanel.add(bfsBtn);
        topPanel.add(resetBtn);
        topPanel.add(deleteNodeBtn);
        topPanel.add(undoBtn);
        topPanel.add(toggleViewBtn);
        topPanel.add(directedGraphCheckBox);

        bfsBtn.addActionListener(e -> {
            String src = sourceField.getText().trim();
            String dst = destField.getText().trim();
            if (src.isEmpty() || dst.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter both source and destination nodes.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            canvas.setSource(src);
            canvas.setDestination(dst);
            new Thread(canvas::startBFS).start();
        });

        resetBtn.addActionListener(e -> {
            canvas.resetGraph();
            sourceField.setText("");
            destField.setText("");
        });

        deleteNodeBtn.addActionListener(e -> {
            String nodeName = JOptionPane.showInputDialog("Enter node name to delete:");
            if (nodeName != null && !nodeName.trim().isEmpty()) {
                canvas.deleteNode(nodeName.trim());
            }
        });

        undoBtn.addActionListener(e -> canvas.undoDelete());

        toggleViewBtn.addActionListener(e -> {
            canvas.toggleBackgroundImage();
            toggleViewBtn.setText(canvas.isPaperView() ? "World Map" : "Paper View");
            updateMessagePanelColors();
        });

        directedGraphCheckBox.addActionListener(e -> canvas.setDirected(directedGraphCheckBox.isSelected()));

        // In ModernBfsGui constructor
        Color lightSkyBlue = new Color(209, 230, 250);
        messagePanel.setBackground(lightSkyBlue);
        rightScroll = new JScrollPane(messagePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.setPreferredSize(new Dimension(350, getHeight()));
        rightScroll.setBackground(lightSkyBlue);
        rightScroll.getViewport().setBackground(lightSkyBlue);
        rightScroll.setBorder(BorderFactory.createEmptyBorder()); // Optional: Remove border for cleaner look

        canvas.setOutputPanel(messagePanel);

        add(topPanel, BorderLayout.NORTH);
        add(rightScroll, BorderLayout.EAST);
        add(canvas, BorderLayout.CENTER);
    }

    private void updateMessagePanelColors() {
        Color backgroundColor = canvas.isPaperView() ? new Color(210, 180, 140) : new Color(209, 230, 250);
        messagePanel.setBackground(backgroundColor);
        rightScroll.setBackground(backgroundColor);
        rightScroll.getViewport().setBackground(backgroundColor);
        messagePanel.repaint();
        rightScroll.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ModernBfsGui().setVisible(true));
    }
}

class GraphCanvas extends JPanel {
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();
    private final List<Node> visitedNodes = new ArrayList<>();
    private final List<Edge> pathEdges = new ArrayList<>();
    private MessagePanel outputPanel;
    private Node draggingNode = null;
    private Point dragOffset = null;
    private Node selectedForEdge = null;
    private Node sourceNode = null;
    private Node destinationNode = null;
    private int nodeCounter = 0;
    private Image bgImage;
    private final Image worldImage;
    private final Image paperImage;
    private boolean isPaperView = false;
    private Node lastDeletedNode = null;
    private final List<Edge> lastDeletedEdges = new ArrayList<>();
    private boolean isDirected = false;

    public GraphCanvas() {
        setBackground(Color.WHITE);
        Image tempWorldImage = null;
        Image tempPaperImage = null;
        try {
            tempWorldImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/WORLD.jpg"))).getImage();
        } catch (NullPointerException e) {
            System.err.println("Error: Could not load image /WORLD.jpg. Ensure the file is in the resources folder.");
        }
        try {
            tempPaperImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/paper_view.jpg"))).getImage();
        } catch (NullPointerException e) {
            System.err.println("Error: Could not load image /paper_view.jpg. Ensure the file is in the resources folder.");
        }
        worldImage = tempWorldImage;
        paperImage = tempPaperImage;
        bgImage = worldImage != null ? worldImage : paperImage;
        if (bgImage == null) {
            System.err.println("Warning: Both images failed to load. Using default background.");
        }

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Node clicked = getNodeAt(e.getPoint());
                if (clicked != null) {
                    draggingNode = clicked;
                    dragOffset = new Point(e.getX() - clicked.x, e.getY() - clicked.y);
                } else {
                    addNode(e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                draggingNode = null;
                dragOffset = null;
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) return;
                Node clicked = getNodeAt(e.getPoint());
                if (clicked != null) {
                    if (selectedForEdge == null) {
                        selectedForEdge = clicked;
                    } else if (!selectedForEdge.equals(clicked)) {
                        String weightStr = JOptionPane.showInputDialog("Enter edge weight:");
                        try {
                            int weight = Integer.parseInt(weightStr);
                            if (weight < 0) {
                                JOptionPane.showMessageDialog(null, "Error: Negative weights are not allowed in BFS.");
                                selectedForEdge = null;
                                return;
                            }
                            edges.add(new Edge(selectedForEdge, clicked, weight));
                            if (outputPanel != null) {
//                                outputPanel.addMessage("Edge added: " + selectedForEdge.name + (isDirected ? " -> " : " <-> ") + clicked.name + " (weight: " + weight + ")");
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Invalid weight.");
                        }
                        selectedForEdge = null;
                    } else {
                        JOptionPane.showMessageDialog(null, "Error: Self-loops are not allowed.");
                        selectedForEdge = null;
                    }
                }
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (draggingNode != null && dragOffset != null) {
                    draggingNode.x = e.getX() - dragOffset.x;
                    draggingNode.y = e.getY() - dragOffset.y;
                    repaint();
                }
            }
        });
    }

    public void setDirected(boolean directed) {
        System.out.println("Setting isDirected to: " + directed); // Debug output
        this.isDirected = directed;
        updateAdjacencyList();
        repaint(); // Force repaint after changing direction
    }

    public void setOutputPanel(MessagePanel panel) {
        this.outputPanel = panel;
    }

    public void setSource(String name) {
        sourceNode = getNodeByName(name);
    }

    public void setDestination(String name) {
        destinationNode = getNodeByName(name);
    }

    public void resetGraph() {
        nodes.clear();
        edges.clear();
        selectedForEdge = null;
        sourceNode = null;
        destinationNode = null;
        visitedNodes.clear();
        pathEdges.clear();
        adjacencyList.clear();
        lastDeletedNode = null;
        lastDeletedEdges.clear();
        nodeCounter = 0;
        if (outputPanel != null) outputPanel.clearMessages();
        repaint();
    }

    public void deleteNode(String nodeName) {
        Node nodeToDelete = getNodeByName(nodeName);
        if (nodeToDelete == null) {
            JOptionPane.showMessageDialog(null, "Node " + nodeName + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        lastDeletedNode = nodeToDelete;
        lastDeletedEdges.clear();
        for (Edge e : edges) {
            if (e.from == nodeToDelete || e.to == nodeToDelete) {
                lastDeletedEdges.add(e);
            }
        }

        nodes.remove(nodeToDelete);
        edges.removeIf(e -> e.from == nodeToDelete || e.to == nodeToDelete);
        updateAdjacencyList();

        if (sourceNode == nodeToDelete) sourceNode = null;
        if (destinationNode == nodeToDelete) destinationNode = null;
        if (selectedForEdge == nodeToDelete) selectedForEdge = null;

        if (outputPanel != null) {
            outputPanel.addMessage("Node " + nodeName + " deleted.");
        }
        repaint();
    }

    public void undoDelete() {
        if (lastDeletedNode == null) {
            JOptionPane.showMessageDialog(null, "No node deletion to undo.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        nodes.add(lastDeletedNode);
        edges.addAll(lastDeletedEdges);
        updateAdjacencyList();

        if (outputPanel != null) {
            outputPanel.addMessage("Undo: Restored node " + lastDeletedNode.name + " and its edges.");
        }
        repaint();

        lastDeletedNode = null;
        lastDeletedEdges.clear();
    }

    private void addNode(int x, int y) {
        String name = String.valueOf((char) ('A' + nodeCounter++));
        nodes.add(new Node(name, x, y));
        if (outputPanel != null) {
            outputPanel.addMessage("Node " + name + " added at (" + x + ", " + y + ")");
        }
        repaint();
    }

    private Node getNodeAt(Point p) {
        for (Node n : nodes) {
            if (n.contains(p)) return n;
        }
        return null;
    }

    private Node getNodeByName(String name) {
        for (Node n : nodes) {
            if (n.name.equalsIgnoreCase(name)) return n;
        }
        return null;
    }

    public void startBFS() {
        if (sourceNode == null || destinationNode == null) {
            JOptionPane.showMessageDialog(null, "Invalid source or destination node.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        updateAdjacencyList();
        visitedNodes.clear();
        pathEdges.clear();
        Map<Node, Node> parent = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(sourceNode);
        visited.add(sourceNode);

        if (outputPanel != null) {
            outputPanel.addMessage("Starting BFS from " + sourceNode.name + " to " + destinationNode.name);
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (outputPanel != null) outputPanel.addMessage("Visiting: " + current.name);
            visitedNodes.add(current);
            repaint();
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}

            if (current.equals(destinationNode)) {
                drawPath(parent);
                return;
            }

            for (Edge edge : adjacencyList.getOrDefault(current.name, new ArrayList<>())) {
                Node neighbor = getNodeByName(edge.to.name);
                if (neighbor != null && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                    if (outputPanel != null) {
                        outputPanel.addMessage("Enqueueing: " + neighbor.name + " (parent: " + current.name + ")");
                    }
                }
            }
        }

        if (outputPanel != null)
            outputPanel.addMessage("No path found from " + sourceNode.name + " to " + destinationNode.name);

        JOptionPane.showMessageDialog(null, "No path found from " + sourceNode.name + " to " + destinationNode.name);
    }

    private void updateAdjacencyList() {
        adjacencyList.clear();
        for (Edge edge : edges) {
            adjacencyList.computeIfAbsent(edge.from.name, k -> new ArrayList<>()).add(new Edge(edge.from, edge.to, edge.weight));
            if (!isDirected) {
                adjacencyList.computeIfAbsent(edge.to.name, k -> new ArrayList<>()).add(new Edge(edge.to, edge.from, edge.weight));
            }
        }
    }

    private void drawPath(Map<Node, Node> parent) {
        List<Node> path = new ArrayList<>();
        Node current = destinationNode;
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }
        Collections.reverse(path);
        pathEdges.clear();
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            for (Edge e : edges) {
                if (isDirected) {
                    if (e.from.equals(from) && e.to.equals(to)) {
                        pathEdges.add(e);
                        break;
                    }
                } else {
                    if ((e.from.equals(from) && e.to.equals(to)) || (e.from.equals(to) && e.to.equals(from))) {
                        pathEdges.add(e);
                        break;
                    }
                }
            }
        }
        StringBuilder msg = new StringBuilder("Shortest path: ");
        for (Node n : path) msg.append(n.name).append(" -> ");
        if (outputPanel != null)
            outputPanel.addMessage("Best path found: " + msg.substring(0, msg.length() - 4));
        repaint();
    }

    public void toggleBackgroundImage() {
        isPaperView = !isPaperView;
        bgImage = isPaperView ? paperImage : worldImage;
        if (outputPanel != null) {
            outputPanel.setPaperView(isPaperView);
        }
        repaint();
    }

    public boolean isPaperView() {
        return isPaperView;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Arial", Font.BOLD, 14));

        for (Edge e : edges) {
            if (pathEdges.contains(e)) {
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
                System.out.println("Drawing path edge: " + e.from.name + " to " + e.to.name + " (Directed: " + isDirected + ")");
            } else {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
            }
            if (isDirected) {
                drawArrowLine(g2, e.from.x, e.from.y, e.to.x, e.to.y, 10, 10);
            } else {
                g2.drawLine(e.from.x, e.from.y, e.to.x, e.to.y);
            }

            int midX = (e.from.x + e.to.x) / 2;
            int midY = (e.from.y + e.to.y) / 2;
            String weightStr = String.valueOf(e.weight);

            FontMetrics fm = g2.getFontMetrics();
            int width = fm.stringWidth(weightStr) + 6;
            int height = fm.getHeight();

            g2.setColor(Color.BLACK);
            g2.fillRect(midX - 3, midY - height + 5, width, height);
            g2.setColor(Color.YELLOW);
            g2.drawString(weightStr, midX, midY);
        }

        for (Node n : nodes) {
            if (visitedNodes.contains(n)) {
                g2.setColor(Color.ORANGE);
            } else {
                g2.setColor(Color.PINK);
            }
            g2.fillOval(n.x - n.r, n.y - n.r, 2 * n.r, 2 * n.r);

            if (n == selectedForEdge) {
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(n.x - n.r - 2, n.y - n.r - 2, 2 * n.r + 4, 2 * n.r + 4);
                g2.setStroke(new BasicStroke(1));
            } else {
                g2.setColor(Color.BLACK);
                g2.drawOval(n.x - n.r, n.y - n.r, 2 * n.r, 2 * n.r);
            }

            g2.setColor(Color.BLACK);
            g2.drawString(n.name, n.x - 5, n.y + 5);

            if (n == sourceNode) drawTag(g2, "S", n.x - 40, n.y - 40, Color.GREEN);
            if (n == destinationNode) drawTag(g2, "D", n.x + 40, n.y - 40, Color.RED);
        }
    }

    private void drawTag(Graphics2D g2, String label, int x, int y, Color color) {
        g2.setColor(Color.BLACK);
        g2.fillRect(x - 4, y - 14, 20, 18);
        g2.setColor(Color.BLACK);
        g2.drawRect(x - 4, y - 14, 20, 18);
        g2.setColor(color);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString(label, x, y);
    }

    private void drawArrowLine(Graphics2D g2, int x1, int y1, int x2, int y2, int d, int h) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx * dx + dy * dy);
        if (D == 0) return; // Avoid division by zero

        // Normalize direction
        double sin = dy / D, cos = dx / D;

        // Adjust end point to avoid node overlap (20 pixels from node center)
        double adjust = 20;
        int endX = (int) (x1 + (dx * (D - adjust) / D));
        int endY = (int) (y1 + (dy * (D - adjust) / D));

        // Calculate arrowhead points relative to the end point, pointing back toward the line
        double xm = -d * cos; // Move back along the line
        double ym = -d * sin;
        double xn = xm - h * sin; // Perpendicular to the line
        double yn = ym + h * cos;

        int arrowX1 = endX;
        int arrowY1 = endY;
        int arrowX2 = (int) (endX + xm - h * sin); // Second point
        int arrowY2 = (int) (endY + ym + h * cos);
        int arrowX3 = (int) (endX + xm + h * sin); // Third point
        int arrowY3 = (int) (endY + ym - h * cos);

        // Draw line
        g2.drawLine(x1, y1, endX, endY);

        // Draw arrowhead with debug color
        g2.setColor(Color.GREEN); // Temporary debug color
        g2.fillPolygon(new int[] {arrowX1, arrowX2, arrowX3}, new int[] {arrowY1, arrowY2, arrowY3}, 3);

        // Reset color for subsequent drawings
        g2.setColor(pathEdges.contains(new Edge(new Node("", x1, y1), new Node("", x2, y2), 0)) ? Color.RED : Color.BLACK);
    }
}

class Node {
    String name;
    int x, y, r = 20;
    Node(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }
    boolean contains(Point p) {
        return p.distance(x, y) <= r;
    }
}

class Edge {
    Node from, to;
    int weight;
    Edge(Node from, Node to, int weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }
}

class MessagePanel extends JPanel {
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private static final Color DEFAULT_BORDER_COLOR = new Color(128, 0, 128);
    private static final Color PATH_FOUND_BORDER_COLOR = new Color(75, 0, 130);
    private static final Color PAPER_BORDER_COLOR = new Color(139, 69, 19);
    private static final Color PAPER_PATH_FOUND_BORDER_COLOR = new Color(101, 67, 33);
    private static final int ARC_SIZE = 20;
    private static final int MARGIN = 10;
    private static final int PADDING = 10;
    private boolean isPaperView = false;

    public MessagePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(209, 230, 250));
    }

    public void setPaperView(boolean paperView) {
        this.isPaperView = paperView;
        setBackground(paperView ? new Color(210, 180, 140) : new Color(209, 230, 250));
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

            Color borderColor = message.startsWith("Best path found:") ?
                    (isPaperView ? PAPER_PATH_FOUND_BORDER_COLOR : PATH_FOUND_BORDER_COLOR) :
                    (isPaperView ? PAPER_BORDER_COLOR : DEFAULT_BORDER_COLOR);

            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(MARGIN, y, width, height, ARC_SIZE, ARC_SIZE);

            g2.setColor(new Color(230, 230, 250));
            g2.fillRoundRect(MARGIN + 1, y + 1, width - 2, height - 2, ARC_SIZE, ARC_SIZE);

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
