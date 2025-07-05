import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.*;
import javafx.scene.input.MouseEvent;

public class GraphCanvas extends Canvas {
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();
    private final List<Node> visitedNodes = new ArrayList<>();
    private final List<Edge> pathEdges = new ArrayList<>();
    private MessagePanel outputPanel;
    private Node draggingNode = null;
    private double dragOffsetX, dragOffsetY;
    private Node selectedForEdge = null;
    private Node sourceNode = null;
    private Node destinationNode = null;
    private int nodeCounter = 0;
    private Image bgImage;
    private final Image worldImage;
    private final Image paperImage;
    private boolean isPaperView = false;
    private Object lastDeleted = null;
    private final List<Edge> lastDeletedEdges = new ArrayList<>();
    private boolean isDirected = false;

    public GraphCanvas() {
        setWidth(850);
        setHeight(700);
        Image tempWorldImage = null;
        Image tempPaperImage = null;
        try {
            tempWorldImage = new Image(Objects.requireNonNull(getClass().getResource("/WORLD.jpg")).toExternalForm());
        } catch (NullPointerException e) {
            System.err.println("Error: Could not load image /WORLD.jpg. Ensure the file is in the resources folder.");
        }
        try {
            tempPaperImage = new Image(Objects.requireNonNull(getClass().getResource("/paper_view.jpg")).toExternalForm());
        } catch (NullPointerException e) {
            System.err.println("Error: Could not load image /paper_view.jpg. Ensure the file is in the resources folder.");
        }
        worldImage = tempWorldImage;
        paperImage = tempPaperImage;
        bgImage = worldImage != null ? worldImage : paperImage;
        if (bgImage == null) {
            System.err.println("Warning: Both images failed to load. Using default background.");
        }

        setOnMousePressed(this::handleMousePressed);
        setOnMouseReleased(this::handleMouseReleased);
        setOnMouseClicked(this::handleMouseClicked);
        setOnMouseDragged(this::handleMouseDragged);
    }

    private void handleMousePressed(MouseEvent e) {
        Node clicked = getNodeAt(e.getX(), e.getY());
        if (clicked != null) {
            draggingNode = clicked;
            dragOffsetX = e.getX() - clicked.x;
            dragOffsetY = e.getY() - clicked.y;
        } else {
            addNode((int)e.getX(), (int)e.getY());
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        draggingNode = null;
    }

    private void handleMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) return;
        Node clicked = getNodeAt(e.getX(), e.getY());
        if (clicked != null) {
            if (selectedForEdge == null) {
                selectedForEdge = clicked;
            } else if (!selectedForEdge.equals(clicked)) {
                javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
                dialog.setTitle("Add Edge");
                dialog.setHeaderText("Enter edge weight:");
                dialog.showAndWait().ifPresent(weightStr -> {
                    try {
                        int weight = Integer.parseInt(weightStr);
                        if (weight <= 0) {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Error: Negative/empty weights are not allowed in BFS.", javafx.scene.control.ButtonType.OK);
                            alert.showAndWait();
                            selectedForEdge = null;
                            return;
                        }
                        edges.add(new Edge(selectedForEdge, clicked, weight));
                    } catch (Exception ex) {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Invalid weight.", javafx.scene.control.ButtonType.OK);
                        alert.showAndWait();
                    }
                    selectedForEdge = null;
                });
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Error: Self-loops are not allowed.", javafx.scene.control.ButtonType.OK);
                alert.showAndWait();
                selectedForEdge = null;
            }
        }
        redraw();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (draggingNode != null) {
            draggingNode.x = (int)(e.getX() - dragOffsetX);
            draggingNode.y = (int)(e.getY() - dragOffsetY);
            redraw();
        }
    }

    public void setDirected(boolean directed) {
        this.isDirected = directed;
        updateAdjacencyList();
        redraw();
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
        redraw();
    }

    public void deleteNode(String nodeName) {
        Node nodeToDelete = getNodeByName(nodeName);
        if (nodeToDelete == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Node " + nodeName + " not found.", javafx.scene.control.ButtonType.OK);
            alert.showAndWait();
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
        redraw();
    }

    public void deleteEdge(String edgeInput) {
        String[] edgeParts = edgeInput.split("\\s+to\\s+");
        if (edgeParts.length != 2) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Invalid edge format. Use 'source to destination' (e.g., A to C).", javafx.scene.control.ButtonType.OK);
            alert.showAndWait();
            return;
        }

        String fromName = edgeParts[0].trim();
        String toName = edgeParts[1].trim();
        Node fromNode = getNodeByName(fromName);
        Node toNode = getNodeByName(toName);

        if (fromNode == null || toNode == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "One or both nodes (" + fromName + ", " + toName + ") not found.", javafx.scene.control.ButtonType.OK);
            alert.showAndWait();
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
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Edge from " + fromName + " to " + toName + " not found.", javafx.scene.control.ButtonType.OK);
            alert.showAndWait();
            return;
        }

        lastDeleted = edgesToRemove.get(0);
        lastDeletedEdges.clear();
        lastDeletedEdges.addAll(edgesToRemove);
        edges.removeAll(edgesToRemove);
        updateAdjacencyList();

        if (outputPanel != null) {
            outputPanel.addMessage("Edge " + fromName + (isDirected ? " -> " : " <-> ") + toName + " deleted.");
        }
        redraw();
    }

    public void undoDelete() {
        if (lastDeleted == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "No deletion to undo.", javafx.scene.control.ButtonType.OK);
            alert.showAndWait();
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
        redraw();
    }

    private void addNode(int x, int y) {
        String name = String.valueOf((char) ('A' + nodeCounter++));
        nodes.add(new Node(name, x, y));
        if (outputPanel != null) {
            outputPanel.addMessage("Node " + name + " added.");
        }
        redraw();
    }

    private Node getNodeAt(double x, double y) {
        for (Node n : nodes) {
            if (Math.hypot(n.x - x, n.y - y) <= n.r) return n;
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
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Invalid source or destination node.", javafx.scene.control.ButtonType.OK);
            alert.showAndWait();
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
            redraw();
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

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "No path found from " + sourceNode.name + " to " + destinationNode.name);
        alert.showAndWait();
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
        redraw();
    }

    public void toggleBackgroundImage() {
        isPaperView = !isPaperView;
        bgImage = isPaperView ? paperImage : worldImage;
        if (outputPanel != null) {
            outputPanel.setPaperView(isPaperView);
        }
        redraw();
    }

    public boolean isPaperView() {
        return isPaperView;
    }

    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        if (bgImage != null) {
            gc.drawImage(bgImage, 0, 0, getWidth(), getHeight());
        }

        gc.setLineWidth(2);
        gc.setFont(new javafx.scene.text.Font("Arial", 14));

        for (Edge e : edges) {
            gc.setStroke(pathEdges.contains(e) ? Color.RED : Color.BLACK);
            gc.setLineWidth(pathEdges.contains(e) ? 3 : 2);
            if (isDirected) {
                drawArrowLine(gc, e.from.x, e.from.y, e.to.x, e.to.y, 10, 10);
            } else {
                gc.strokeLine(e.from.x, e.from.y, e.to.x, e.to.y);
            }

            double midX = (e.from.x + e.to.x) / 2;
            double midY = (e.from.y + e.to.y) / 2;
            String weightStr = String.valueOf(e.weight);

            gc.setFill(Color.BLACK);
            gc.fillRect(midX - 3, midY - 10, gc.getFont().getSize() + 6, gc.getFont().getSize());
            gc.setFill(Color.WHITE);
            gc.fillText(weightStr, midX, midY + 5);
        }

        for (Node n : nodes) {
            gc.setFill(visitedNodes.contains(n) ? Color.ORANGE : Color.WHITE);
            gc.fillOval(n.x - n.r, n.y - n.r, 2 * n.r, 2 * n.r);

            if (n == selectedForEdge) {
                gc.setStroke(Color.RED);
                gc.setLineWidth(3);
                gc.strokeOval(n.x - n.r - 2, n.y - n.r - 2, 2 * n.r + 4, 2 * n.r + 4);
                gc.setLineWidth(1);
            } else {
                gc.setStroke(Color.BLACK);
                gc.strokeOval(n.x - n.r, n.y - n.r, 2 * n.r, 2 * n.r);
            }

            gc.setFill(Color.BLACK);
            gc.fillText(n.name, n.x - 5, n.y + 5);

            if (n == sourceNode) drawTag(gc, "S", n.x - 40, n.y - 40, Color.GREEN);
            if (n == destinationNode) drawTag(gc, "D", n.x + 40, n.y - 40, Color.RED);
        }
    }

    private void drawTag(GraphicsContext gc, String label, double x, double y, Color color) {
        gc.setFill(Color.BLACK);
        gc.fillRect(x - 4, y - 14, 20, 18);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x - 4, y - 14, 20, 18);
        gc.setFill(color);
        gc.setFont(new javafx.scene.text.Font("Arial", 13));
        gc.fillText(label, x, y);
    }

    private void drawArrowLine(GraphicsContext gc, double x1, double y1, double x2, double y2, double d, double h) {
        double dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx * dx + dy * dy);
        if (D == 0) return;

        double sin = dy / D, cos = dx / D;
        double adjust = 20;
        double endX = x1 + (dx * (D - adjust) / D);
        double endY = y1 + (dy * (D - adjust) / D);

        double xm = -d * cos;
        double ym = -d * sin;
        double xn = xm - h * sin;
        double yn = ym + h * cos;

        double arrowX2 = endX + xm - h * sin;
        double arrowY2 = endY + ym + h * cos;
        double arrowX3 = endX + xm + h * sin;
        double arrowY3 = endY + ym - h * cos;

        gc.strokeLine(x1, y1, endX, endY);
        gc.fillPolygon(new double[]{endX, arrowX2, arrowX3}, new double[]{endY, arrowY2, arrowY3}, 3);
    }
}