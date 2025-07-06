import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;


public class GraphCanvas extends JPanel {
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
    private Object lastDeleted = null; // Tracks the last deleted item (Node or Edge)
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
                            if (weight <= 0) {
                                JOptionPane.showMessageDialog(null, "Error: Negative/empty weights are not allowed in BFS.");
                                selectedForEdge = null;
                                return;
                            }
                            edges.add(new Edge(selectedForEdge, clicked, weight));
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
        this.isDirected = directed;
        updateAdjacencyList();
        repaint();
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
        lastDeleted = null;
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

        lastDeleted = nodeToDelete;
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

    public void deleteEdge(String edgeInput) {
        String[] edgeParts = edgeInput.split("\\s+to\\s+");
        if (edgeParts.length != 2) {
            JOptionPane.showMessageDialog(null, "Invalid edge format. Use 'source to destination' (e.g., A to C).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String fromName = edgeParts[0].trim();
        String toName = edgeParts[1].trim();
        Node fromNode = getNodeByName(fromName);
        Node toNode = getNodeByName(toName);

        if (fromNode == null || toNode == null) {
            JOptionPane.showMessageDialog(null, "One or both nodes (" + fromName + ", " + toName + ") not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean edgeFound = false;
        List<Edge> edgesToRemove = new ArrayList<>();
        for (Edge e : edges) {
            if (e.from == fromNode && e.to == toNode) {
                edgesToRemove.add(e);
                edgeFound = true;
            } else if (!isDirected && e.from == toNode && e.to == fromNode) {
                edgesToRemove.add(e);
                edgeFound = true;
            }
        }

        if (!edgeFound) {
            JOptionPane.showMessageDialog(null, "Edge from " + fromName + " to " + toName + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        lastDeleted = edgesToRemove.get(0); // Store the first edge as the last deleted item
        lastDeletedEdges.clear();
        lastDeletedEdges.addAll(edgesToRemove);
        edges.removeAll(edgesToRemove);
        updateAdjacencyList();

        if (outputPanel != null) {
            outputPanel.addMessage("Edge " + fromName + (isDirected ? " -> " : " <-> ") + toName + " deleted.");
        }
        repaint();
    }

    public void undoDelete() {
        if (lastDeleted == null) {
            JOptionPane.showMessageDialog(null, "No deletion to undo.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (lastDeleted instanceof Node) {
            Node nodeToRestore = (Node) lastDeleted;
            nodes.add(nodeToRestore);
            edges.addAll(lastDeletedEdges);
            updateAdjacencyList();
            if (outputPanel != null) {
                outputPanel.addMessage("Undo: Restored node " + nodeToRestore.name + " and its edges.");
            }
        } else if (lastDeleted instanceof Edge) {
            edges.addAll(lastDeletedEdges);
            updateAdjacencyList();
            if (outputPanel != null) {
                Edge edge = (Edge) lastDeleted;
                outputPanel.addMessage("Undo: Restored edge " + edge.from.name + (isDirected ? " -> " : " <-> ") + edge.to.name + ".");
            }
        }

        lastDeleted = null;
        lastDeletedEdges.clear();
        repaint();
    }

    private void addNode(int x, int y) {
        String name = String.valueOf((char) ('A' + nodeCounter++));
        nodes.add(new Node(name, x, y));
        if (outputPanel != null) {
            outputPanel.addMessage("Node " + name + " added.");
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
            g2.setColor(Color.WHITE);
            g2.drawString(weightStr, midX, midY);
        }

        for (Node n : nodes) {
            if (visitedNodes.contains(n)) {
                g2.setColor(Color.ORANGE);
            } else {
                g2.setColor(Color.WHITE);
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

        int arrowX2 = (int) (endX + xm - h * sin); // Second point
        int arrowY2 = (int) (endY + ym + h * cos);
        int arrowX3 = (int) (endX + xm + h * sin); // Third point
        int arrowY3 = (int) (endY + ym - h * cos);

        // Draw line
        g2.drawLine(x1, y1, endX, endY);

        // Draw arrowhead with debug color
        g2.setColor(Color.BLACK); // Temporary debug color
        int arrowY1 = endY;
        int arrowX1 = endX;
        g2.fillPolygon(new int[] {arrowX1, arrowX2, arrowX3}, new int[] {arrowY1, arrowY2, arrowY3}, 3);

        // Reset color for subsequent drawings
        g2.setColor(pathEdges.contains(new Edge(new Node("", x1, y1), new Node("", x2, y2), 0)) ? Color.RED : Color.BLACK);
    }
}