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

// 🌟 BFS Visualizer X – Enhanced Version with Node Selection Highlight, Edge Styling, Delete Node, and Undo

public class ModernBfsGui extends JFrame {
    private final GraphCanvas canvas = new GraphCanvas();
    private final JTextField sourceField = new JTextField(5);
    private final JTextField destField = new JTextField(5);

    public ModernBfsGui() {
        setTitle("BFS Visualizer X - Final");
        setSize(1000, 700);
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

        topPanel.add(sourceLabel);
        topPanel.add(sourceField);
        topPanel.add(destLabel);
        topPanel.add(destField);
        topPanel.add(bfsBtn);
        topPanel.add(resetBtn);
        topPanel.add(deleteNodeBtn);
        topPanel.add(undoBtn);

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

        Color lightSkyBlue = new Color(209, 230, 250); // Very light sky blue

MessagePanel messagePanel = new MessagePanel();
messagePanel.setBackground(lightSkyBlue); // Message panel background

JScrollPane rightScroll = new JScrollPane(messagePanel);
rightScroll.setPreferredSize(new Dimension(300, getHeight()));

// Set background for scroll pane and its viewport
rightScroll.setBackground(lightSkyBlue);
rightScroll.getViewport().setBackground(lightSkyBlue);

canvas.setOutputPanel(messagePanel);


canvas.setOutputPanel(messagePanel);


        add(topPanel, BorderLayout.NORTH);
        add(rightScroll, BorderLayout.EAST);
        add(canvas, BorderLayout.CENTER);
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
    private final Image bgImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/WORLD.jpg"))).getImage();
    private Node lastDeletedNode = null;
    private List<Edge> lastDeletedEdges = new ArrayList<>();

    public GraphCanvas() {
        setBackground(Color.WHITE); // Default to light mode

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
                                JOptionPane.showMessageDialog(null, "Negative weights are not allowed in BFS.");
                                selectedForEdge = null;
                                return;
                            }
                            edges.add(new Edge(selectedForEdge, clicked, weight));
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Invalid weight.");
                        }
                        selectedForEdge = null;
                    } else {
                        JOptionPane.showMessageDialog(null, "Self-loops are not allowed.");
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
        nodeCounter = 0; // Reset nodeCounter to start labeling from 'A' again
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
                if (!visited.contains(neighbor)) {
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
            adjacencyList.computeIfAbsent(edge.to.name, k -> new ArrayList<>()).add(new Edge(edge.to, edge.from, edge.weight));
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
                if ((e.from.equals(from) && e.to.equals(to)) || (e.from.equals(to) && e.to.equals(from))) {
                    pathEdges.add(e);
                    break;
                }
            }
        }
        StringBuilder msg = new StringBuilder("Shortest path: ");
        for (Node n : path) msg.append(n.name).append(" -> ");
        if (outputPanel != null)
            outputPanel.addMessage("Best path found: " + msg.substring(0, msg.length() - 4));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Arial", Font.BOLD, 14));

        for (Edge e : edges) {
            if (pathEdges.contains(e)) {
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
            } else {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
            }
            g2.drawLine(e.from.x, e.from.y, e.to.x, e.to.y);

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
                g2.setColor(Color.ORANGE); // Nodes turn orange when visited
            } else {
                g2.setColor(Color.PINK); // Set node color to pink
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

// Updated class for custom message panel
class MessagePanel extends JPanel {
    private final List<String> messages = new ArrayList<>();
    private static final Color DEFAULT_BORDER_COLOR = new Color(128, 0, 128); // Purple border
    private static final Color PATH_FOUND_BORDER_COLOR = new Color(75, 0, 130); // Darker purple for "Best path found"
    private static final int ARC_SIZE = 20; // Rounded corners
    private static final int MARGIN = 10;

    public MessagePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(240, 240, 240));
    }

    public void addMessage(String message) {
        if (messages.isEmpty() || !isSimilarMessage(messages.get(messages.size() - 1), message)) {
            messages.add(message);
        } else {
            // Append to the last message with a newline
            int lastIndex = messages.size() - 1;
            messages.set(lastIndex, messages.get(lastIndex) + "\n" + message);
        }
        repaint();
        revalidate();
    }

    public void clearMessages() {
        messages.clear();
        repaint();
        revalidate();
    }

    private boolean isSimilarMessage(String msg1, String msg2) {
        // Check if messages start with the same prefix (e.g., "Visiting:", "Enqueueing:")
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

        int y = MARGIN;
        FontMetrics fm = g2.getFontMetrics(getFont());
        int lineHeight = fm.getHeight() + 10;

        for (String message : messages) {
            int width = fm.stringWidth(message.replace("\n", " ")) + 40; // Account for longest line
            int height = (message.split("\n").length * lineHeight) + 10;

            // Determine border color based on message type
            Color borderColor = message.startsWith("Best path found:") ? PATH_FOUND_BORDER_COLOR : DEFAULT_BORDER_COLOR;

            // Draw rounded rectangle with purple border
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(MARGIN, y, width, height, ARC_SIZE, ARC_SIZE);

            // Fill with a light background
            g2.setColor(new Color(230, 230, 250)); // Light lavender background
            g2.fillRoundRect(MARGIN + 1, y + 1, width - 2, height - 2, ARC_SIZE, ARC_SIZE);

            // Draw the text with newlines
            g2.setColor(Color.BLACK);
            String[] lines = message.split("\n");
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
        for (String message : messages) {
            int width = fm.stringWidth(message.replace("\n", " ")) + 60; // Padding for longest line
            maxWidth = Math.max(maxWidth, width);
        }
        return new Dimension(maxWidth, messages.size() * (fm.getHeight() + 20) + MARGIN);
    }
}
