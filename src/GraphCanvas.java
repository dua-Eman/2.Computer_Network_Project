/*This class handles the graphical representation of the graph,
  implements the BFS algorithm, and update the visualization.
  Hence, this class is for the visualization handling.
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;


public class GraphCanvas extends JPanel {
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();
    private final List<Node> nodeVisited = new ArrayList<>();
    private final List<Edge> edgePath = new ArrayList<>();
    private MessageConsole logPanel;
    private Node draggingNode = null;
    private Point dragOffset = null;
    private Node selectedForEdge = null;
    private Node srcNode = null;
    private Node destNode = null;
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
// this allow to detect the clicked node
            public void mousePressed(MouseEvent e) {
                Node clicked = getNodeAt(e.getPoint());
                if (clicked != null) {
//                    dragged node
                    draggingNode = clicked;
                    dragOffset = new Point(e.getX() - clicked.x, e.getY() - clicked.y);
                } else {
//                    7. add the node
                    attachNode(e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                draggingNode = null;
                dragOffset = null;
            }
// 2. this function help user to add node with a mouse click.
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) return;
                Node clicked = getNodeAt(e.getPoint());
                if (clicked != null) {
                    if (selectedForEdge == null) {
                        selectedForEdge = clicked;
                    } else if (!selectedForEdge.equals(clicked)) {
//                        2, similarly allows to add the weight of the 2 connected nodes.
                        String weightStr = JOptionPane.showInputDialog("Enter edge weight:");
//                        2. handles all the edge cases related to weights.
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

    public void setDirectedgraph(boolean directed) {
        this.isDirected = directed;
        updateAdjacencyList();
        repaint();
    }

    public void setOutputPanel(MessageConsole panel) {
        this.logPanel = panel;
    }
//  3. function to add the source
    public void setSource(String name) {
        srcNode = getNodeByName(name);
    }
//  3. function to add the destination
    public void setDestination(String name) {
        destNode = getNodeByName(name);
    }

    public void resetGraph() {
        nodes.clear();
        edges.clear();
        selectedForEdge = null;
        srcNode = null;
        destNode = null;
        nodeVisited.clear();
        edgePath.clear();
        adjacencyList.clear();
        lastDeleted = null;
        lastDeletedEdges.clear();
        nodeCounter = 0;
        if (logPanel != null) logPanel.removeMessages();
        repaint();
    }
/* 7. This is the function that is used to delete the node from the graph
      It accept the name of the node entered by user and successfully remove
      the node with its corresponding edges
 */
    public void nodeDeletion(String nodeName) {
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

        if (srcNode == nodeToDelete) srcNode = null;
        if (destNode == nodeToDelete) destNode = null;
        if (selectedForEdge == nodeToDelete) selectedForEdge = null;

        if (logPanel != null) {
            logPanel.insertMessage("Node " + nodeName + " deleted.");
        }
        repaint();
    }
/* 7. this method helps in changing the topology e.g deleting the edge
   it remove the node dynamically by accepting the input entered by user
 */
    public void edgeDeletion(String edgeInput) {
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

        if (logPanel != null) {
            logPanel.insertMessage("Edge " + fromName + (isDirected ? " -> " : " <-> ") + toName + " deleted.");
        }
        repaint();
    }

    public void undoDelete() {
        if (lastDeleted == null) {
            JOptionPane.showMessageDialog(null, "No deletion to undo.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (lastDeleted instanceof Node nodeToRestore) {
            nodes.add(nodeToRestore);
            edges.addAll(lastDeletedEdges);
            updateAdjacencyList();
            if (logPanel != null) {
                logPanel.insertMessage("Undo: Restored node " + nodeToRestore.name + " and its edges.");
            }
        } else if (lastDeleted instanceof Edge) {
            edges.addAll(lastDeletedEdges);
            updateAdjacencyList();
            if (logPanel != null) {
                Edge edge = (Edge) lastDeleted;
                logPanel.insertMessage("Undo: Restored edge " + edge.from.name + (isDirected ? " -> " : " <-> ") + edge.to.name + ".");
            }
        }

        lastDeleted = null;
        lastDeletedEdges.clear();
        repaint();
    }
// 7. This helps to add the node in the graph by just clicking on the canvas.
    private void attachNode(int x, int y) {
        String name = String.valueOf((char) ('A' + nodeCounter++));
        nodes.add(new Node(name, x, y));
        if (logPanel != null) {
            logPanel.insertMessage("Node " + name + " added.");
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
// 4. implementation and simulation of the BFS algorithm
    public void launchBFS() {
        if (srcNode == null || destNode == null) {
            JOptionPane.showMessageDialog(null, "Invalid source or destination node.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        updateAdjacencyList();
        nodeVisited.clear();
        edgePath.clear();
        Map<Node, Node> parent = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(srcNode);
        visited.add(srcNode);

        if (logPanel != null) {
            logPanel.insertMessage("Starting BFS from " + srcNode.name + " to " + destNode.name);
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (logPanel != null) logPanel.insertMessage("Visiting: " + current.name);
            nodeVisited.add(current);
            repaint();
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}

            if (current.equals(destNode)) {
                makePath(parent);
                return;
            }

            for (Edge edge : adjacencyList.getOrDefault(current.name, new ArrayList<>())) {
                Node neighbor = getNodeByName(edge.to.name);
                if (neighbor != null && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                    if (logPanel != null) {
                        logPanel.insertMessage("Enqueueing: " + neighbor.name + " (parent: " + current.name + ")");
                    }
                }
            }
        }

        if (logPanel != null)
            logPanel.insertMessage("No path found from " + srcNode.name + " to " + destNode.name);

        JOptionPane.showMessageDialog(null, "No path found from " + srcNode.name + " to " + destNode.name);
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
// 4. function to display the shortest/best path calculated by the algorithm
    private void makePath(Map<Node, Node> parent) {
        List<Node> path = new ArrayList<>();
        Node current = destNode;
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }
        Collections.reverse(path);
        edgePath.clear();
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            for (Edge e : edges) {
                if (isDirected) {
                    if (e.from.equals(from) && e.to.equals(to)) {
                        edgePath.add(e);
                        break;
                    }
                } else {
                    if ((e.from.equals(from) && e.to.equals(to)) || (e.from.equals(to) && e.to.equals(from))) {
                        edgePath.add(e);
                        break;
                    }
                }
            }
        }
        StringBuilder msg = new StringBuilder("Shortest path: ");
        for (Node n : path) msg.append(n.name).append(" -> ");
        if (logPanel != null)
            logPanel.insertMessage("Best path found: " + msg.substring(0, msg.length() - 4));
        repaint();
    }

    public void switchBackgroundImage() {
        isPaperView = !isPaperView;
        bgImage = isPaperView ? paperImage : worldImage;
        if (logPanel != null) {
            logPanel.setPaperView(isPaperView);
        }
        repaint();
    }

    public boolean isPaperView() {
        return isPaperView;
    }
/*  6. This funcction helps in visualizing the entire routing process
    from visiting the node(orange) adding it in the queue after applying the BFS
    algorithm displaying the best path in red color.
 */
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
            if (edgePath.contains(e)) {
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
            } else {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
            }
            if (isDirected) {
                drawDirection(g2, e.from.x, e.from.y, e.to.x, e.to.y);
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
            g2.fillRect(midX - 3, midY - height + 3, width + 5, height + 5);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString(weightStr, midX, midY);
        }

        for (Node n : nodes) {
            if (nodeVisited.contains(n)) {
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

            if (n == srcNode) createTag(g2, "S", n.x - 50, n.y - 50);
            if (n == destNode) createTag(g2, "D", n.x + 50, n.y - 50);
        }
    }
// 6. helps in paintComponent method to generate the tag
    private void createTag(Graphics2D g2, String label, int x, int y) {
        g2.setColor(Color.BLACK);
        g2.fillRect(x - 4, y - 14, 20, 20);
        g2.setColor(Color.BLACK);
        g2.drawRect(x - 4, y - 14, 20, 20);
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString(label, x, y);
    }
//  help in paintComponent method to show the direction of the edge
    private void drawDirection(Graphics2D g2, int x1, int y1, int x2, int y2) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx * dx + dy * dy);
        if (D == 0) return;

        double sin = dy / D, cos = dx / D;

        double adjust = 20;
        int endX = (int) (x1 + (dx * (D - adjust) / D));
        int endY = (int) (y1 + (dy * (D - adjust) / D));

        double xm = -10 * cos;
        double ym = -10 * sin;

        int arrowX2 = (int) (endX + xm - 10 * sin);
        int arrowY2 = (int) (endY + ym + 10 * cos);
        int arrowX3 = (int) (endX + xm + 10 * sin);
        int arrowY3 = (int) (endY + ym - 10 * cos);

        // Draw line
        g2.drawLine(x1, y1, endX, endY);

        g2.setColor(Color.BLACK);
        g2.fillPolygon(new int[] {endX, arrowX2, arrowX3}, new int[] {endY, arrowY2, arrowY3}, 3);


        g2.setColor(edgePath.contains(new Edge(new Node("", x1, y1), new Node("", x2, y2), 0)) ? Color.RED : Color.BLACK);
    }
}
