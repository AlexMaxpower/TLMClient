import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.util.Comparator;

public class ChartStage extends Stage {

    private ObservableList<XYChart.Data<String, Number>> dataTime = FXCollections.observableArrayList();

    private XYChart.Series<String, Number> series;

    private SortedList<XYChart.Data<String, Number>> sortedData = new SortedList<>(dataTime,
            Comparator.comparing(XYChart.Data::getXValue));
    final int POINTS_SIZE = 50;   // кол-во точек на графике

    public ChartStage(int width) {
        setTitle("Realtime Chart");
        setX(width + 80);
        setY(260);
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time,s");
        xAxis.setAnimated(false);
        yAxis.setLabel("Value");
        yAxis.setAnimated(false);

        final LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Данные по UDP");
        lineChart.setAnimated(false);

        series = new XYChart.Series<>(sortedData);
        series.setName("Service data");
        lineChart.getData().add(series);
        lineChart.setLegendVisible(false);

        setScene(new Scene(lineChart, 400, 360));
    }

    public void addData(String formattedDate,
                        double value) {
        XYChart.Data<String, Number> dataAtDate = dataTime.stream()
                .filter(d -> d.getXValue().equals(formattedDate))
                .findAny()
                .orElseGet(() -> {
                    XYChart.Data<String, Number> newData = new XYChart.Data<>(formattedDate, 0.0);
                    dataTime.add(newData);
                    return newData;
                });
        dataAtDate.setYValue(dataAtDate.getYValue().doubleValue() + value);
        if (series.getData().size() > POINTS_SIZE) {
            dataTime.remove(0);
        }
    }
}