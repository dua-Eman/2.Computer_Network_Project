//  A graphical interface that include text fields, buttons, and options to reset undo or change the view.

import java.awt.*;
import java.util.Objects;
import javax.swing.*;

public class BFS_GUI extends JFrame {
    private final GraphCanvas canvas = new GraphCanvas();
    private final JTextField srcField = new JTextField(5);
    private final JTextField destField = new JTextField(5);
    private final MessageConsole logPanel = new MessageConsole();
    private final JScrollPane rightScrollMsg;

/*    1. This constructor contains all the text fields for input, buttons to start BFS,
*     and different options to reset or undo the graph.
*/
    public BFS_GUI() {
        setTitle("BFS Visualizer");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 10));
        bottomPanel.setBackground(new Color(40, 40, 70));

//      3. button(way) to insert the source
        JLabel srcLabel = new JLabel("Source:");
        srcLabel.setForeground(Color.WHITE);
        srcLabel.setFont(new Font("Arial", Font.BOLD, 18));

//       3. button(way) to insert the destination
        JLabel destLabel = new JLabel("Destination:");
        destLabel.setForeground(Color.WHITE);
        destLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JButton BFSbtn = new JButton("Start BFS");
        JButton BFSresetBtn = new JButton("Reset");
//      7. button to delete the node and edges
        JButton nodeDltBtn = new JButton("Delete Node");
        JButton edgeDltBtn = new JButton("Delete Edge");

        JButton undoBtn = new JButton("Undo");
        JButton toggleViewBtn = new JButton("Paper View");
        JCheckBox directedCheckBox = new JCheckBox("Directed Graph", false);
        directedCheckBox.setToolTipText("Toggle between directed and undirected edges");
        directedCheckBox.setForeground(Color.BLACK);

        BFSbtn.setBackground(new Color(60, 120, 180));
        BFSbtn.setForeground(Color.WHITE);
        BFSbtn.setFont(new Font("Arial", Font.BOLD, 15));
        BFSbtn.setFocusPainted(false);
        BFSbtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        BFSresetBtn.setBackground(new Color(60, 120, 180));
        BFSresetBtn.setForeground(Color.WHITE);
        BFSresetBtn.setFont(new Font("Arial", Font.BOLD, 15));
        BFSresetBtn.setFocusPainted(false);
        BFSresetBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        nodeDltBtn.setBackground(new Color(60, 120, 180));
        nodeDltBtn.setForeground(Color.WHITE);
        nodeDltBtn.setFont(new Font("Arial", Font.BOLD, 15));
        nodeDltBtn.setFocusPainted(false);
        nodeDltBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

        edgeDltBtn.setBackground(new Color(60, 120, 180));
        edgeDltBtn.setForeground(Color.WHITE);
        edgeDltBtn.setFont(new Font("Arial", Font.BOLD, 15));
        edgeDltBtn.setFocusPainted(false);
        edgeDltBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200), 2));

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
//      1. The panel whwere all the buttons and text field appear to simulate the BFS
        bottomPanel.add(srcLabel);
        bottomPanel.add(srcField);
        bottomPanel.add(destLabel);
        bottomPanel.add(destField);
        bottomPanel.add(BFSbtn);
        bottomPanel.add(BFSresetBtn);
        bottomPanel.add(nodeDltBtn);
        bottomPanel.add(edgeDltBtn);
        bottomPanel.add(undoBtn);
        bottomPanel.add(toggleViewBtn);
        bottomPanel.add(directedCheckBox);

        BFSbtn.addActionListener(e -> {
            String src = srcField.getText().trim().toUpperCase();
            String dst = destField.getText().trim().toUpperCase();
//           1. Handles the invalid source or destination
            if (src.isEmpty() || dst.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter both source and destination nodes.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            canvas.setSource(src);
            canvas.setDestination(dst);
            new Thread(canvas::launchBFS).start();
        });
//      1. rest the whole graph
        BFSresetBtn.addActionListener(e -> {
            canvas.resetGraph();
            srcField.setText("");
            destField.setText("");
        });
//      1,7. delete the node button
        nodeDltBtn.addActionListener(e -> {
            String nodeName = JOptionPane.showInputDialog("Enter Node Name to Delete:");
            if (nodeName != null && !nodeName.trim().isEmpty()) {
                canvas.nodeDeletion(nodeName.trim().toUpperCase());
            }
            if(Objects.requireNonNull(nodeName).isEmpty()){
                JOptionPane.showMessageDialog(null, "Invalid Node.");
            }
        });
//      1,7. delete link button
        edgeDltBtn.addActionListener(e -> {
            String edgeInput = JOptionPane.showInputDialog("Enter Edge to Delete (e.g., A to C):");
            if (edgeInput != null && !edgeInput.trim().isEmpty()) {
                canvas.edgeDeletion(edgeInput.trim().toLowerCase());
            }
            if(Objects.requireNonNull(edgeInput).isEmpty()){
                JOptionPane.showMessageDialog(null, "Invalid Edge.");
            }
        });

        undoBtn.addActionListener(e -> canvas.undoDelete());

        toggleViewBtn.addActionListener(e -> {
            canvas.switchBackgroundImage();
            toggleViewBtn.setText(canvas.isPaperView() ? "World Map" : "Paper View");
            changeMessagePanelColors();
        });

        directedCheckBox.addActionListener(e -> canvas.setDirectedgraph(directedCheckBox.isSelected()));


        Color lightSkyBlue = new Color(88, 187, 211);
        logPanel.setBackground(lightSkyBlue);
        rightScrollMsg = new JScrollPane(logPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightScrollMsg.setPreferredSize(new Dimension(350, getHeight()));
        rightScrollMsg.setBackground(lightSkyBlue);
        rightScrollMsg.getViewport().setBackground(lightSkyBlue);
        rightScrollMsg.setBorder(BorderFactory.createEmptyBorder());

        canvas.setOutputPanel(logPanel);

        add(bottomPanel, BorderLayout.SOUTH);
        add(rightScrollMsg, BorderLayout.EAST);
        add(canvas, BorderLayout.CENTER);
    }

    private void changeMessagePanelColors() {
        Color backgroundColor = canvas.isPaperView() ? new Color(234, 223, 197) : new Color(69, 192, 216);
        logPanel.setBackground(backgroundColor);
        rightScrollMsg.setBackground(backgroundColor);
        rightScrollMsg.getViewport().setBackground(backgroundColor);
        logPanel.repaint();
        rightScrollMsg.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BFS_GUI().setVisible(true));
    }
}