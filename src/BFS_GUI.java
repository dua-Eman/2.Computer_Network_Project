import java.awt.*;
import java.util.Objects;
import javax.swing.*;

public class BFS_GUI extends JFrame {
    private final GraphCanvas canvas = new GraphCanvas();
    private final JTextField sourceField = new JTextField(5);
    private final JTextField destField = new JTextField(5);
    private final MessagePanel messagePanel = new MessagePanel();
    private final JScrollPane rightScroll;

    public BFS_GUI() {
        setTitle("BFS Visualizer X - Final");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 10));
        topPanel.setBackground(new Color(40, 40, 70));

        JLabel sourceLabel = new JLabel("Source:");
        sourceLabel.setForeground(Color.WHITE);
        sourceLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel destLabel = new JLabel("Destination:");
        destLabel.setForeground(Color.WHITE);
        destLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton bfsBtn = new JButton("Start BFS");
        JButton resetBtn = new JButton("Reset Graph");
        JButton deleteNodeBtn = new JButton("Delete Node");
        JButton deleteEdgeBtn = new JButton("Delete Edge");
        JButton undoBtn = new JButton("Undo");
        JButton toggleViewBtn = new JButton("Paper View");
        JCheckBox directedGraphCheckBox = new JCheckBox("Directed Graph", false);
        directedGraphCheckBox.setToolTipText("Toggle between directed and undirected edges");
        directedGraphCheckBox.setForeground(Color.BLACK);

        // Apply consistent style to all buttons
        bfsBtn.setBackground(new Color(60, 120, 180));
        bfsBtn.setForeground(Color.WHITE);
        bfsBtn.setFont(new Font("Arial", Font.BOLD, 15));
        bfsBtn.setFocusPainted(false);
        bfsBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        resetBtn.setBackground(new Color(60, 120, 180));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFont(new Font("Arial", Font.BOLD, 15));
        resetBtn.setFocusPainted(false);
        resetBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        deleteNodeBtn.setBackground(new Color(60, 120, 180));
        deleteNodeBtn.setForeground(Color.WHITE);
        deleteNodeBtn.setFont(new Font("Arial", Font.BOLD, 15));
        deleteNodeBtn.setFocusPainted(false);
        deleteNodeBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        deleteEdgeBtn.setBackground(new Color(60, 120, 180));
        deleteEdgeBtn.setForeground(Color.WHITE);
        deleteEdgeBtn.setFont(new Font("Arial", Font.BOLD, 15));
        deleteEdgeBtn.setFocusPainted(false);
        deleteEdgeBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        undoBtn.setBackground(new Color(60, 120, 180));
        undoBtn.setForeground(Color.WHITE);
        undoBtn.setFont(new Font("Arial", Font.BOLD, 15));
        undoBtn.setFocusPainted(false);
        undoBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        toggleViewBtn.setBackground(new Color(60, 120, 180));
        toggleViewBtn.setForeground(Color.WHITE);
        toggleViewBtn.setFont(new Font("Arial", Font.BOLD, 15));
        toggleViewBtn.setFocusPainted(false);
        toggleViewBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        topPanel.add(sourceLabel);
        topPanel.add(sourceField);
        topPanel.add(destLabel);
        topPanel.add(destField);
        topPanel.add(bfsBtn);
        topPanel.add(resetBtn);
        topPanel.add(deleteNodeBtn);
        topPanel.add(deleteEdgeBtn);
        topPanel.add(undoBtn);
        topPanel.add(toggleViewBtn);
        topPanel.add(directedGraphCheckBox);

        bfsBtn.addActionListener(e -> {
            String src = sourceField.getText().trim().toUpperCase();
            String dst = destField.getText().trim().toUpperCase();
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
                canvas.deleteNode(nodeName.trim().toUpperCase());
            }
            if(Objects.requireNonNull(nodeName).isEmpty()){
                JOptionPane.showMessageDialog(null, "Invalid Node.");
            }
        });

        deleteEdgeBtn.addActionListener(e -> {
            String edgeInput = JOptionPane.showInputDialog("Enter edge to delete (e.g., A to C):");
            if (edgeInput != null && !edgeInput.trim().isEmpty()) {
                canvas.deleteEdge(edgeInput.trim().toLowerCase());
            }
            if(Objects.requireNonNull(edgeInput).isEmpty()){
                JOptionPane.showMessageDialog(null, "Invalid Edge.");
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
        Color lightSkyBlue = new Color(88, 187, 211);
        messagePanel.setBackground(lightSkyBlue);
        rightScroll = new JScrollPane(messagePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.setPreferredSize(new Dimension(350, getHeight()));
        rightScroll.setBackground(lightSkyBlue);
        rightScroll.getViewport().setBackground(lightSkyBlue);
        rightScroll.setBorder(BorderFactory.createEmptyBorder()); // Optional: Remove border for cleaner look

        canvas.setOutputPanel(messagePanel);

        add(topPanel, BorderLayout.SOUTH);
        add(rightScroll, BorderLayout.EAST);
        add(canvas, BorderLayout.CENTER);
    }

    private void updateMessagePanelColors() {
        Color backgroundColor = canvas.isPaperView() ? new Color(234, 223, 197) : new Color(69, 192, 216);
        messagePanel.setBackground(backgroundColor);
        rightScroll.setBackground(backgroundColor);
        rightScroll.getViewport().setBackground(backgroundColor);
        messagePanel.repaint();
        rightScroll.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BFS_GUI().setVisible(true));
    }
}