import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogStage extends Stage {
    public LogStage(int width, DateTimeFormatter customFormat, int delay) {
        TextFlow logView = new TextFlow(new Text("  " + LocalDateTime.now().format(customFormat) + "  Delay: " +
                delay + " ms"));
        logView.setId("logString");
        logView.setPrefWidth(400);
        ScrollPane scrollPane = new ScrollPane(logView);
        HBox hbox = new HBox(10, scrollPane);
        hbox.setAlignment(Pos.TOP_LEFT);
        setScene(new Scene(hbox, 400, 200));
        setX(width + 80);
        setY(20);
        setTitle("Log");
    }
}
