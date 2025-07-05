import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.scene.control.ScrollPane;

public class MessagePanel extends VBox {
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private static final Color DEFAULT_BORDER_COLOR = Color.rgb(14, 81, 119);
    private static final Color PATH_FOUND_BORDER_COLOR = Color.rgb(42, 75, 218);
    private static final Color PAPER_BORDER_COLOR = Color.rgb(139, 69, 19);
    private static final Color PAPER_PATH_FOUND_BORDER_COLOR = Color.rgb(101, 67, 33);
    private boolean isPaperView = false;

    public MessagePanel() {
        setPrefWidth(350);
        setStyle("-fx-background-color: #00BCD4;");
        ScrollPane scrollPane = new ScrollPane(this);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        getChildren().add(scrollPane);
    }

    public void setPaperView(boolean paperView) {
        this.isPaperView = paperView;
        setStyle("-fx-background-color: " + (paperView ? "#D2B48C" : "#00BCD4") + ";");
        redraw();
    }

    public void addMessage(String message) {
        if (messages.isEmpty() || !isSimilarMessage(messages.get(messages.size() - 1), message)) {
            messages.add(message);
        } else {
            int lastIndex = messages.size() - 1;
            messages.set(lastIndex, messages.get(lastIndex) + "\n" + message);
        }
        redraw();
    }

    public void clearMessages() {
        messages.clear();
        getChildren().clear();
        getChildren().add(((VBox)getChildren().get(0)).getChildren().get(0)); // Keep ScrollPane
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

    private void redraw() {
        getChildren().clear();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(10));

        for (String message : messages) {
            String[] lines = message.split("\n");
            VBox messageBox = new VBox(5);
            Color borderColor = message.startsWith("Best path found:") ?
                    (isPaperView ? PAPER_PATH_FOUND_BORDER_COLOR : PATH_FOUND_BORDER_COLOR) :
                    (isPaperView ? PAPER_BORDER_COLOR : DEFAULT_BORDER_COLOR);
            messageBox.setStyle("-fx-background-color: white; -fx-border-color: " + toRGBCode(borderColor) + "; -fx-border-width: 4; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 5;");
            for (String line : lines) {
                Text text = new Text(line);
                text.setFill(Color.BLACK);
                messageBox.getChildren().add(text);
            }
            content.getChildren().add(messageBox);
        }
        scrollPane.setContent(content);
        getChildren().add(scrollPane);
        scrollPane.setVvalue(1.0); // Scroll to bottom
    }

    private String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X", (int)(color.getRed() * 255), (int)(color.getGreen() * 255), (int)(color.getBlue() * 255));
    }
}