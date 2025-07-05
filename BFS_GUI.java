import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class BFS_GUI extends Application {
    private GraphCanvas canvas = new GraphCanvas();
    private TextField sourceField = new TextField();
    private TextField destField = new TextField();
    private MessagePanel messagePanel = new MessagePanel();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("BFS Visualizer X - Final");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);

        // Top panel setup
        HBox topPanel = new HBox(25);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: #282846;");
        topPanel.setAlignment(Pos.CENTER_LEFT);

        sourceField.setPrefWidth(50);
        destField.setPrefWidth(50);

        Button bfsBtn = new Button("Start BFS");
        Button resetBtn = new Button("Reset Graph");
        Button deleteNodeBtn = new Button("Delete Node");
        Button deleteEdgeBtn = new Button("Delete Edge");
        Button undoBtn = new Button("Undo");
        Button toggleViewBtn = new Button("Paper View");
        CheckBox directedGraphCheckBox = new CheckBox("Directed Graph");
        directedGraphCheckBox.setTooltip(new javafx.scene.control.Tooltip("Toggle between directed and undirected edges"));

        // Style buttons
        String buttonStyle = "-fx-background-color: #3c78b4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-focus-color: transparent; -fx-border-color: #508cd8; -fx-border-width: 2;";
        bfsBtn.setStyle(buttonStyle);
        resetBtn.setStyle(buttonStyle);
        deleteNodeBtn.setStyle(buttonStyle);
        deleteEdgeBtn.setStyle(buttonStyle);
        undoBtn.setStyle(buttonStyle);
        toggleViewBtn.setStyle(buttonStyle);

        topPanel.getChildren().addAll(
                new javafx.scene.control.Label("Source:"), sourceField,
                new javafx.scene.control.Label("Destination:"), destField,
                bfsBtn, resetBtn, deleteNodeBtn, deleteEdgeBtn, undoBtn, toggleViewBtn, directedGraphCheckBox
        );

        // Layout
        BorderPane root = new BorderPane();
        root.setBottom(topPanel);
        root.setCenter(canvas);
        root.setRight(messagePanel);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Event handlers
        bfsBtn.setOnAction(e -> {
            String src = sourceField.getText().trim().toUpperCase();
            String dst = destField.getText().trim().toUpperCase();
            if (src.isEmpty() || dst.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Please enter both source and destination nodes.", javafx.scene.control.ButtonType.OK);
                alert.showAndWait();
                return;
            }
            canvas.setSource(src);
            canvas.setDestination(dst);
            new Thread(canvas::startBFS).start();
        });

        resetBtn.setOnAction(e -> {
            canvas.resetGraph();
            sourceField.setText("");
            destField.setText("");
        });

        deleteNodeBtn.setOnAction(e -> {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Delete Node");
            dialog.setHeaderText("Enter node name to delete:");
            dialog.showAndWait().ifPresent(nodeName -> {
                if (!nodeName.trim().isEmpty()) {
                    canvas.deleteNode(nodeName.trim().toUpperCase());
                }
                if (nodeName.trim().isEmpty()) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Invalid Node.", javafx.scene.control.ButtonType.OK);
                    alert.showAndWait();
                }
            });
        });

        deleteEdgeBtn.setOnAction(e -> {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Delete Edge");
            dialog.setHeaderText("Enter edge to delete (e.g., A to C):");
            dialog.showAndWait().ifPresent(edgeInput -> {
                if (!edgeInput.trim().isEmpty()) {
                    canvas.deleteEdge(edgeInput.trim().toLowerCase());
                }
                if (edgeInput.trim().isEmpty()) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Invalid Edge.", javafx.scene.control.ButtonType.OK);
                    alert.showAndWait();
                }
            });
        });

        undoBtn.setOnAction(e -> canvas.undoDelete());

        toggleViewBtn.setOnAction(e -> {
            canvas.toggleBackgroundImage();
            toggleViewBtn.setText(canvas.isPaperView() ? "World Map" : "Paper View");
            updateMessagePanelColors();
        });

        directedGraphCheckBox.setOnAction(e -> canvas.setDirected(directedGraphCheckBox.isSelected()));

        canvas.setOutputPanel(messagePanel);
    }

    private void updateMessagePanelColors() {
        String color = canvas.isPaperView() ? "#EADFC5" : "#45C0D8";
        messagePanel.setStyle("-fx-background-color: " + color + ";");
    }

    public static void main(String[] args) {
        launch(args);
    }
}