import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


public class TLMClient extends Application {

    private final static int WIDTH = 800;
    private final static int HEIGHT = 600;
    private int delay = 5; // задержка в мс для эмуляции длительности вычислений по умолчанию
    private TableView tableView;
    private Service<Void> service;
    private Stage primaryStage;
    private LogStage logStage;
    private ChartStage chartStage;
    private int threads = 0;
    private int currentThreads = 0;
    private DateTimeFormatter customFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {

        this.primaryStage = primaryStage;
        logStage = new LogStage(WIDTH, customFormat, delay);
        chartStage = new ChartStage(WIDTH);
        work();
        service = new ReadService(delay, tableView, customFormat, chartStage, logStage);
        primaryStage.show();
        logStage.show();
        chartStage.show();
        primaryStage.toFront();
    }

    @Override
    public void stop() throws Exception {
        if (service != null) {
            service.cancel();
        }
        super.stop();
    }

    public void work() {

        Label delayLabel = new Label();
        delayLabel.setText("Задержка в потоке, мс ");
        TextField textField = new TextField();
        textField.setPrefColumnCount(10);
        textField.setText(Integer.toString(delay));
        // в поле только цифры
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        String goText = "▶ Поехали! ";
        Button playButton = new Button(goText);
        playButton.setMinWidth(100);
        playButton.requestFocus();
        playButton.setOnAction(event -> {
            if (service.isRunning()) {
                service.cancel();
                service.reset();
                playButton.setText(goText);
            } else {
                tableView.setPlaceholder(new Label("Ждем данные от сервера..."));
                service.start();
                playButton.setText("■ Остановить!");
            }
        });

        Button delayButton = new Button("Применить");
        delayButton.setOnAction(event -> {
            delay = Integer.parseInt(textField.getText());
            ReadService.setDelay(delay);
            TextFlow logView = (TextFlow) logStage.getScene().lookup("#logString");
            logView.getChildren().add(new Text("\n  " + LocalDateTime.now().format(customFormat) +
                    "  Delay: " + delay + " ms"));
        });

        HBox hbox = new HBox(0, delayLabel, textField, delayButton);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        HBox hbox1 = new HBox(0, playButton);
        hbox1.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.add(hbox1, 0, 0);
        grid.add(hbox, 1, 0);
        GridPane.setHalignment(hbox, HPos.RIGHT);
        GridPane.setHgrow(hbox, Priority.ALWAYS);

        tableView = new TableView();
        tableView.setMinWidth(WIDTH);
        tableView.setMinHeight(HEIGHT - 50);

        tableView.setPlaceholder(new Label("Для получения данных нажмите кнопку \"Поехали!\""));

        TableColumn<PackageData, Long> column1 =
                new TableColumn<>("Номер пакета");

        column1.setCellValueFactory(
                new PropertyValueFactory<>("number"));

        TableColumn<PackageData, Instant> column2 =
                new TableColumn<>("Время");

        column2.setCellValueFactory(
                new PropertyValueFactory<>("timeSec"));

        TableColumn<PackageData, Double> column3 =
                new TableColumn<>("Данные");

        column3.setCellValueFactory(
                new PropertyValueFactory<>("serviceData"));

        TableColumn<PackageData, Integer> column4 =
                new TableColumn<>("КС");

        column4.setCellValueFactory(
                new PropertyValueFactory<>("intCRC"));

        TableColumn<PackageData, String> column5 =
                new TableColumn<>("Проверка");

        column5.setCellValueFactory(
                new PropertyValueFactory<>("check"));

        column1.setStyle("-fx-alignment: CENTER-RIGHT;");
        column2.setStyle("-fx-alignment: CENTER;");
        column3.setStyle("-fx-alignment: CENTER-RIGHT;");
        column4.setStyle("-fx-alignment: CENTER;");
        column5.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().add(column1);
        tableView.getColumns().add(column2);
        tableView.getColumns().add(column3);
        tableView.getColumns().add(column4);
        tableView.getColumns().add(column5);

        column1.setSortType(TableColumn.SortType.ASCENDING);
        tableView.getSortOrder().add(column1);

        column1.prefWidthProperty().bind(tableView.widthProperty().multiply(0.2));
        column2.prefWidthProperty().bind(tableView.widthProperty().multiply(0.25));
        column3.prefWidthProperty().bind(tableView.widthProperty().multiply(0.25));
        column4.prefWidthProperty().bind(tableView.widthProperty().multiply(0.15));
        column5.prefWidthProperty().bind(tableView.widthProperty().multiply(0.15));

        tableView.setRowFactory(tv -> new TableRow<PackageData>() {
            @Override
            public void updateItem(PackageData item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setStyle("");
                } else if (item.getCheck().equals("error")) {
                    setStyle("-fx-background-color: lightpink;");
                } else {
                    setStyle("");
                }
            }
        });

        VBox vbox = new VBox(grid, tableView);
        primaryStage.setScene(new Scene(vbox, WIDTH, HEIGHT));
        primaryStage.setX(60);
        primaryStage.setY(20);
        primaryStage.setTitle("TLMClient");
        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);
    }

    private void closeWindowEvent(WindowEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        ButtonType buttonYes = new ButtonType("Да");
        ButtonType buttonCancel = new ButtonType("Отмена");
        alert.getButtonTypes().setAll(buttonYes, buttonCancel);
        alert.setHeaderText("Подтвердите действие");
        alert.setTitle("Завершение работы");
        alert.setContentText(String.format("Вы действительно закрыть приложение?"));
        alert.initOwner(primaryStage.getOwner());
        Optional<ButtonType> res = alert.showAndWait();

        if (res.isPresent()) {
            if (res.get().equals(buttonYes)) {
                if (chartStage != null) {
                    chartStage.close();
                }
                if (logStage != null) {
                    logStage.close();
                }
            } else {
                event.consume();
            }
        }
    }
}