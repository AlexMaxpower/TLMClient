import org.apache.poi.ss.usermodel.*;
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
import java.util.Arrays;

public class TLMClient {

    private static final int PORT = 15000;
    private static final long markerTLM = 0x12345678L;
    private static final int DELAY = 20; // задержка в мс для эмуляции длительности вычислений
    private static final String pFilename = LocalDateTime.now().toString().replaceAll("[:,.]", "")
            + ".xlsx";

    public TLMClient() {

        MainView view = new MainView();

        try {

            DatagramSocket data = new DatagramSocket(PORT);
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
                            Thread.sleep(DELAY);
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
                            view.addData(numberTLMPackage, timeSec, serviceData, calcCRC, intCRC == calcCRC);

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
                    System.out.println("Поток " + Thread.currentThread().getName() + " завершен!");
                });
                System.out.println("Поток " + readThread.getName() + " запущен!");
                readThread.start();
                System.out.println("Идем дальше из основного потока - " + Thread.currentThread().getName());
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

    private byte[] reverse(byte[] bytes) {
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