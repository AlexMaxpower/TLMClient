import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;


public class TLMClient extends Application {

    private static final int PORT = 15000;
    private final static int WIDTH = 800;
    private final static int HEIGHT = 600;
    private static final long markerTLM = 0x12345678L;
    private static int delay = 20; // задержка в мс для эмуляции длительности вычислений по умолчанию
    private static final String pFilename = LocalDateTime.now().toString().replaceAll("[:,.]", "")
            + ".xlsx";
    private volatile TableView tableView;
    private Service<Void> service;
    private Stage secondStage;

    DateTimeFormatter customFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("TLMClient");
        work(primaryStage);
        secondStage = new Stage();
        Label label = new Label(LocalDateTime.now().format(customFormat) + " Delay: " + delay + " ms");
        label.setId("delay");
        HBox hbox = new HBox(10, label);
        hbox.setAlignment(Pos.TOP_LEFT);
        secondStage.setScene(new Scene(hbox, 400, 200));
        secondStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (service != null) {
            service.cancel();
        }
        super.stop();
    }

    public void work(Stage primaryStage) {

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

        Button delayButton = new Button("Применить");
        delayButton.setOnAction(event -> {
            delay = Integer.parseInt(textField.getText());
            Label lbl = (Label) secondStage.getScene().lookup("#delay");
            lbl.setText(lbl.getText() + "\n" + LocalDateTime.now().format(customFormat) + " Delay: " + delay + " ms");
        });
        HBox hbox = new HBox(0, delayLabel, textField, delayButton);
        hbox.setAlignment(Pos.CENTER_RIGHT);

        tableView = new TableView();
        tableView.setMinWidth(WIDTH);
        tableView.setMinHeight(HEIGHT - 50);

        tableView.setPlaceholder(new Label("Ждем данные от сервера..."));

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

        VBox vbox = new VBox(hbox, tableView);
        primaryStage.setScene(new Scene(vbox, WIDTH, HEIGHT));

        service = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        readData(tableView);
                        return null;
                    }
                };
            }
        };

        service.start();
        primaryStage.show();
    }

    private void readData(TableView tableView) {
        try (DatagramSocket data = new DatagramSocket(PORT)) {

            while (true) {
                byte[] commonBuffer = new byte[651];
                int numberPackets = 0;

                while (numberPackets < 26) {
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    data.receive(packet);
                    buf = packet.getData();

                    boolean containsData = false;
                    for (int i = 0; i < packet.getLength(); i++) {
                        if (buf[i] != 0) {
                            containsData = true;
                        }
                    }

                    if (containsData) {
                        System.arraycopy(buf, 0, commonBuffer, numberPackets * 25, packet.getLength());
                        System.out.print(numberPackets + " : ");
                        for (int i = numberPackets * 25; i < (numberPackets * 25) + packet.getLength(); i++) {
                            System.out.print(commonBuffer[i] + " ");
                        }
                        System.out.println();
                        numberPackets += 1;
                    }
                }

                InputStream targetStream = new ByteArrayInputStream(commonBuffer);
                Thread readThread = new Thread(() -> {
                    for (int i = 0; i < 26; i++) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        byte[] forCRC = new byte[24];

                        // маркер
                        byte[] markerBytes;
                        try {
                            markerBytes = targetStream.readNBytes(4);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.arraycopy(markerBytes, 0, forCRC, 0, markerBytes.length);
                        long marker = toUnsignedInt(reverse(markerBytes));

                        if (marker == markerTLM) {
                            // номер пакета
                            byte[] packageBytes;
                            try {
                                packageBytes = targetStream.readNBytes(4);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            System.arraycopy(packageBytes, 0, forCRC, markerBytes.length, packageBytes.length);
                            long numberTLMPackage = toUnsignedInt(reverse(packageBytes));

                            // время пакета
                            byte[] timeBytes;
                            try {
                                timeBytes = targetStream.readNBytes(8);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            System.arraycopy(timeBytes, 0, forCRC,
                                    markerBytes.length + packageBytes.length, timeBytes.length);
                            Double myDouble = ByteBuffer.wrap(reverse(timeBytes)).getDouble(0);
                            long seconds = (long) myDouble.doubleValue();
                            long milli = (long) ((myDouble.doubleValue() - seconds) * 1000);
                            Instant timeSec = Instant.ofEpochMilli(seconds * 1000 + milli);

                            // данные
                            byte[] dataBytes;
                            try {
                                dataBytes = targetStream.readNBytes(8);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            System.arraycopy(dataBytes, 0, forCRC,
                                    markerBytes.length + packageBytes.length + dataBytes.length, dataBytes.length);
                            Double serviceData = ByteBuffer.wrap(reverse(dataBytes)).getDouble(0);

                            // контрольная сумма
                            byte[] bytesCRC;
                            try {
                                bytesCRC = targetStream.readNBytes(2);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            int intCRC = 0xFFFF & ByteBuffer.wrap(bytesCRC).order(ByteOrder.LITTLE_ENDIAN).getShort();
                            int calcCRC = crc16(forCRC, 0, forCRC.length);
                            tableView.getItems().add(new PackageData(
                                    numberTLMPackage,
                                    timeSec,
                                    serviceData,
                                    intCRC,
                                    intCRC == calcCRC ? "ok" : "error"));

                            // пишем в файл только верные данные
                            if (intCRC == calcCRC) {
                                try {
                                    writeToXLS(numberTLMPackage, timeSec, serviceData, calcCRC);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                    System.out.println(Thread.currentThread().getName() + " completed!");
                });
                readThread.start();
                System.out.println(readThread.getName() + " started!");
                System.out.println("Now in main thread: " + Thread.currentThread().getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] fromUnsignedInt(long value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(value);
        return Arrays.copyOfRange(bytes, 4, 8);
    }

    public static long toUnsignedInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8).put(new byte[]{0, 0, 0, 0}).put(bytes);
        buffer.position(0);
        return buffer.getLong();
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] reverse(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte tmp = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = tmp;
        }
        return bytes;
    }

    private static int crc16(byte[] data, int offset, int length) {
        if (data == null || offset < 0 || offset > data.length - 1 || offset + length > data.length) {
            return 0;
        }

        int crc = 0xFFFF;
        for (int i = 0; i < length; ++i) {
            crc ^= data[offset + i] << 8;
            for (int j = 0; j < 8; ++j) {
                crc = (crc & 0x8000) > 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return crc & 0xFFFF;
    }

    private static synchronized void writeToXLS(Long numberTLMPackage, Instant timeSec,
                                                Double serviceData, Integer calcCRC) throws IOException {
        Path p = Paths.get(pFilename);
        String fileName = p.toString();

        if (!Files.exists(p)) {
            Files.createFile(p);
            try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(fileName))) {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("TLMClient");
                sheet.setColumnWidth(0, 5000);
                sheet.setColumnWidth(1, 7000);
                sheet.setColumnWidth(2, 6000);
                sheet.setColumnWidth(3, 4000);

                Row header = sheet.createRow(0);

                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);

                XSSFFont font = ((XSSFWorkbook) workbook).createFont();
                font.setFontName("Arial");
                font.setFontHeightInPoints((short) 12);
                font.setBold(true);
                headerStyle.setFont(font);

                Cell headerCell = header.createCell(0);
                headerCell.setCellValue("Номер пакета");
                headerCell.setCellStyle(headerStyle);

                headerCell = header.createCell(1);
                headerCell.setCellValue("Время");
                headerCell.setCellStyle(headerStyle);

                headerCell = header.createCell(2);
                headerCell.setCellValue("Данные");
                headerCell.setCellStyle(headerStyle);

                headerCell = header.createCell(3);
                headerCell.setCellValue("CRC");
                headerCell.setCellStyle(headerStyle);

                workbook.write(fos);
                workbook.close();
            }
        }

        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(fileName))) {
            ZipSecureFile.setMinInflateRatio(0);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getLastRowNum();
            Row row = sheet.createRow(rowCount + 1);
            CellStyle cellMarkedStyle = workbook.createCellStyle();
            cellMarkedStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            cellMarkedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cellMarkedStyle.setAlignment(HorizontalAlignment.RIGHT);
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.RIGHT);
            if (numberTLMPackage == 1468306787) {
                cellStyle = cellMarkedStyle;
            }

            Cell cell = row.createCell(0);
            cell.setCellValue(numberTLMPackage);
            cell.setCellStyle(cellStyle);
            cell = row.createCell(1);
            cell.setCellValue(timeSec.toString());
            cell.setCellStyle(cellStyle);
            cell = row.createCell(2);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(serviceData);
            cell = row.createCell(3);
            cell.setCellValue(calcCRC);
            cell.setCellStyle(cellStyle);

            try (BufferedOutputStream fio = new BufferedOutputStream(new FileOutputStream(fileName))) {
                workbook.write(fio);
            }
        }
    }
}