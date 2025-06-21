// 🌟 BFS Visualizer X – All-in-One Interactive Version (Corrected Full Code with Fixes)

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

// 🌟 BFS Visualizer X – Enhanced Version with Node Selection Highlight and Edge Styling


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
        JCheckBox darkModeToggle = new JCheckBox("Dark Mode");
        darkModeToggle.setForeground(Color.WHITE);
        darkModeToggle.setBackground(new Color(40, 40, 70));

        topPanel.add(sourceLabel);
        topPanel.add(sourceField);
        topPanel.add(destLabel);
        topPanel.add(destField);
        topPanel.add(bfsBtn);
        topPanel.add(resetBtn);
        topPanel.add(darkModeToggle);

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

        darkModeToggle.addActionListener(e -> canvas.setDarkMode(darkModeToggle.isSelected()));

        JTextArea bfsOutput = new JTextArea();
        bfsOutput.setEditable(false);
        bfsOutput.setFont(new Font("Consolas", Font.PLAIN, 13));
        bfsOutput.setBackground(new Color(240, 240, 240));
        JScrollPane rightScroll = new JScrollPane(bfsOutput);
        rightScroll.setPreferredSize(new Dimension(300, getHeight()));
        canvas.setOutputArea(bfsOutput);

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
    private JTextArea outputArea;
    private Node draggingNode = null;
    private Point dragOffset = null;
    private Node selectedForEdge = null;
    private Node sourceNode = null;
    private Node destinationNode = null;
    private int nodeCounter = 0;
    private boolean darkMode = true;
    private final Image bgImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/WORLD.jpg"))).getImage();

    public GraphCanvas() {
        setBackground(new Color(30, 30, 60));

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

            // Inside GraphCanvas class (within mouseClicked event handler):
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
                            // 🔧 NEW: Check for negative weight
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
                        // 🔧 NEW: Check for self-loop
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

    public void setDarkMode(boolean enabled) {
        this.darkMode = enabled;
        setBackground(enabled ? new Color(30, 30, 60) : Color.WHITE);
        repaint();
    }

    public void setOutputArea(JTextArea area) {
        this.outputArea = area;
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
        if (outputArea != null) outputArea.setText("");
        repaint();
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

    // Inside startBFS() method (additions marked):

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

        if (outputArea != null) {
            outputArea.append("Starting BFS from " + sourceNode.name + " to " + destinationNode.name + "\n");
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (outputArea != null) outputArea.append("Visiting: " + current.name + "\n");
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

                    // 🔧 NEW: Logging routing decisions
                    if (outputArea != null) {
                        assert neighbor != null;
                        outputArea.append("Enqueueing: " + neighbor.name + " (parent: " + current.name + ")\n");
                    }
                }
            }
        }

        if (outputArea != null)
            outputArea.append("No path found from " + sourceNode.name + " to " + destinationNode.name + "\n");

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
        if (outputArea != null)
            outputArea.append("\nBest path found: " + msg.substring(0, msg.length() - 4));
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
                g2.setStroke(new BasicStroke(4));
            } else {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(3));
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
                g2.setColor(Color.ORANGE);
            } else {
                g2.setColor(darkMode ? Color.pink : new Color(100, 149, 237));
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


