import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.Instant;

public class MainView {
    private final static int WIDTH = 800;
    private final static int HEIGHT = 800;
    private DefaultTableModel model;

    public MainView() {

        JFrame frame = new JFrame("TLMClient");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);

        model = new DefaultTableModel();
        JTable table = new JTable(model) {
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? getBackground() : Color.LIGHT_GRAY);
                    int modelRow = convertRowIndexToModel(row);
                    String type = (String) getModel().getValueAt(modelRow, 4);
                    if ("error".equals(type)) c.setBackground(Color.PINK);
                }

                return c;
            }
        };
        table.setDefaultRenderer(Object.class, new TableInfoRenderer());

        model.addColumn("Номер пакета");
        model.addColumn("Время");
        model.addColumn("Данные");
        model.addColumn("КС");
        model.addColumn("Проверка");

        table.getColumnModel().getColumn(0).setPreferredWidth(20);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(table);

        frame.getContentPane().add(scrollPane);
        frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void addData(Long number, Instant timesec, Double serviceData, Integer intCRC, Boolean check) {
        model.addRow(new Object[]{number, timesec, serviceData, intCRC, check ? "ok" : "error"});
    }
}

class TableInfoRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        if (column != 4) {
            c.setHorizontalAlignment(RIGHT);
        } else {
            c.setHorizontalAlignment(CENTER);
        }

    /*    if (value.equals("error")) {
            c.setBackground(Color.red);
        } else {
            c.setBackground(Color.white);
        } */

        return c;
    }
}