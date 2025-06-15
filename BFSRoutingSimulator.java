import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;

public class BFSRoutingSimulator extends JFrame {
    private final Map<String, List<Edge>> graph = new HashMap<>();
    private final Map<String, Point> nodePositions = new HashMap<>();
    private final JTextArea logArea;
    private final JTextField srcField;
    private final JTextField destField;
    private final DrawPanel drawPanel;
    private final List<String> visitedNodes = new ArrayList<>();
    private final List<String> pathFound = new ArrayList<>();
    private final Image pinImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/pin_icon.jpg"))).getImage();
    private final Image backgroundImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/map_background.jpeg"))).getImage();


    static class Edge {
        String to;
        int weight;

        Edge(String to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    public BFSRoutingSimulator() {
        setTitle("BFS Routing Simulator");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 4));
        srcField = new JTextField();
        destField = new JTextField();

        inputPanel.add(new JLabel("Source Node:"));
        inputPanel.add(srcField);
        inputPanel.add(new JLabel("Destination Node:"));
        inputPanel.add(destField);

        JButton addEdgeBtn = new JButton("Add Edge");
        addEdgeBtn.addActionListener(e -> addEdgeDialog());
        inputPanel.add(addEdgeBtn);

        JButton startBtn = new JButton("Start BFS");
        startBtn.addActionListener(e -> startBFSAnimated());
        inputPanel.add(startBtn);

        JButton resetBtn = new JButton("Reset Graph");
        resetBtn.addActionListener(e -> resetGraph());
        inputPanel.add(resetBtn);

        add(inputPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        drawPanel = new DrawPanel();
        drawPanel.setPreferredSize(new Dimension(500, 600));
        splitPane.setLeftComponent(drawPanel);

        logArea = new JTextArea();
        logArea.setEditable(false);
        splitPane.setRightComponent(new JScrollPane(logArea));

        add(splitPane, BorderLayout.CENTER);
    }

    private void resetGraph() {
        graph.clear();
        nodePositions.clear();
        visitedNodes.clear();
        pathFound.clear();
        logArea.setText("");
        drawPanel.repaint();
    }

    private void addEdgeDialog() {
        JTextField fromField = new JTextField();
        JTextField toField = new JTextField();
        JTextField weightField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(3, 2));

        panel.add(new JLabel("From:"));
        panel.add(fromField);
        panel.add(new JLabel("To:"));
        panel.add(toField);
        panel.add(new JLabel("Weight:"));
        panel.add(weightField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add Edge", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String from = fromField.getText().trim();
            String to = toField.getText().trim();
            String weightText = weightField.getText().trim();
            if (from.isEmpty() || to.isEmpty() || weightText.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int weight = Integer.parseInt(weightText);
                addEdge(from, to, weight);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please enter a valid number for weight.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    private void addEdge(String from, String to, int weight) {
        graph.putIfAbsent(from, new ArrayList<>());
        graph.putIfAbsent(to, new ArrayList<>());
        graph.get(from).add(new Edge(to, weight));
        graph.get(to).add(new Edge(from, weight));

        if (!nodePositions.containsKey(from)) nodePositions.put(from, randomPoint());
        if (!nodePositions.containsKey(to)) nodePositions.put(to, randomPoint());

        log("Edge added: " + from + " <-> " + to + " (" + weight + ")");
        drawPanel.repaint();
    }

    private Point randomPoint() {
        int x = 50 + new Random().nextInt(400);
        int y = 50 + new Random().nextInt(450);
        return new Point(x, y);
    }

    private void startBFSAnimated() {

        new Thread(() -> {
            String src = srcField.getText().trim();
            String dest = destField.getText().trim();
            visitedNodes.clear();
            pathFound.clear();

            if (!graph.containsKey(src) || !graph.containsKey(dest)) {
                log("Invalid source or destination node.");
                return;
            }

            Set<String> visited = new HashSet<>();
            Map<String, String> parent = new HashMap<>();
            Queue<String> queue = new LinkedList<>();
            queue.offer(src);
            visited.add(src);
            log("Starting BFS from: " + src);

            boolean found = false;
            while (!queue.isEmpty()) {
                String current = queue.poll();
                visitedNodes.add(current);
                log("Visiting: " + current);
                drawPanel.repaint();
                sleep();

                if (current.equals(dest)) {
                    found = true;
                    break;
                }
                for (Edge edge : graph.getOrDefault(current, new ArrayList<>())) {
                    if (!visited.contains(edge.to)) {
                        visited.add(edge.to);
                        parent.put(edge.to, current);
                        queue.offer(edge.to);
                        log("  Queueing: " + edge.to);
                    }
                }
            }

            if (found) {
                String step = dest;
                while (step != null) {
                    pathFound.add(step);
                    step = parent.get(step);
                }
                Collections.reverse(pathFound);
                log("Path found: " + String.join(" -> ", pathFound));
            } else {
                log("No path found from " + src + " to " + dest);
            }
            drawPanel.repaint();
        }).start();
    }

    private void sleep() {
        try {
            Thread.sleep(700);
        } catch (InterruptedException ignored) {}
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    class DrawPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.setFont(new Font("Arial", Font.BOLD, 14));

            // Draw edges
            for (String from : graph.keySet()) {
                Point p1 = nodePositions.get(from);
                for (Edge edge : graph.get(from)) {
                    Point p2 = nodePositions.get(edge.to);
                    g2.setColor(Color.BLACK);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                    int midX = (p1.x + p2.x) / 2;
                    int midY = (p1.y + p2.y) / 2;
                    g2.setColor(Color.BLACK);
                    g2.drawString(String.valueOf(edge.weight), midX, midY);
                }
            }

            // Highlight visited nodes
            for (String node : visitedNodes) {
                Point p = nodePositions.get(node);
                g2.setColor(Color.YELLOW);
                g2.fillOval(p.x - 10, p.y - 10, 20, 20);
            }

            // Highlight path found
            for (int i = 0; i < pathFound.size() - 1; i++) {
                Point p1 = nodePositions.get(pathFound.get(i));
                Point p2 = nodePositions.get(pathFound.get(i + 1));
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            // Draw nodes as pin icons
            for (String node : nodePositions.keySet()) {
                Point p = nodePositions.get(node);
                g2.drawImage(pinImage, p.x - 15, p.y - 30, 30, 30, this);
                g2.setColor(Color.BLACK);
                g2.drawString(node, p.x - 5, p.y + 5);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BFSRoutingSimulator().setVisible(true));

    }
}
