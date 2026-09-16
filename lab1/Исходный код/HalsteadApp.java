import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Программа-парсер: анализирует исходный код на языке C#
 * и рассчитывает метрики Холстеда (6 базовых + 3 расширенные).
 *
 * Логика анализа не меняется — меняется только визуальное оформление
 * (тёмная палитра, сплит-панель вместо трёх секций друг под другом,
 * вкладки для таблицы/метрик, карточки метрик вместо простых подписей).
 */
public class HalsteadApp extends JFrame {

    // ---------- палитра ----------
    private static final Color BG_MAIN     = new Color(0x1E1F29);
    private static final Color BG_PANEL    = new Color(0x252734);
    private static final Color BG_FIELD    = new Color(0x2B2E3D);
    private static final Color BORDER_CLR  = new Color(0x3A3D4D);
    private static final Color ACCENT      = new Color(0xFF9F45);
    private static final Color ACCENT_SOFT = new Color(0x3A2E22);
    private static final Color TEXT_MAIN   = new Color(0xE7E7EE);
    private static final Color TEXT_MUTED  = new Color(0x9B9CB0);

    private final JTextArea codeArea = new JTextArea();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"j", "Оператор", "f1j", "i", "Операнд", "f2i"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private final JLabel eta1Value = new JLabel("-");
    private final JLabel eta2Value = new JLabel("-");
    private final JLabel n1Value   = new JLabel("-");
    private final JLabel n2Value   = new JLabel("-");

    private final JLabel vocabValue  = new JLabel("η = η1 + η2 = -");
    private final JLabel lengthValue = new JLabel("N = N1 + N2 = -");
    private final JLabel volumeValue = new JLabel("V = N·log2(η) = -");

    private final JLabel statusLabel = new JLabel("Готово к анализу");

    public HalsteadApp() {
        super("Halstead Metrics — анализ C#");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());
        setSize(1150, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ---------- верхняя плашка ----------
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, ACCENT));

        JLabel title = new JLabel("  Метрики Холстеда · C#");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_MAIN);
        title.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));

        JLabel subtitle = new JLabel("6 базовых + 3 расширенные метрики  ");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        return header;
    }

    // ---------- центральная сплит-панель ----------
    private JComponent buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildCodePanel(), buildResultsTabs());
        split.setDividerLocation(560);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(BG_MAIN);
        split.setContinuousLayout(true);
        return split;
    }

    // ---------- левая часть: код + панель кнопок ----------
    private JComponent buildCodePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));

        JLabel caption = new JLabel("ИСХОДНЫЙ КОД НА C#");
        caption.setFont(new Font("SansSerif", Font.BOLD, 12));
        caption.setForeground(TEXT_MUTED);
        caption.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));

        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        codeArea.setTabSize(4);
        codeArea.setText(SAMPLE_CODE);
        codeArea.setBackground(BG_FIELD);
        codeArea.setForeground(TEXT_MAIN);
        codeArea.setCaretColor(ACCENT);
        codeArea.setSelectionColor(ACCENT_SOFT);
        codeArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));
        codeScroll.getViewport().setBackground(BG_FIELD);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_MAIN);
        top.add(caption, BorderLayout.NORTH);
        top.add(codeScroll, BorderLayout.CENTER);

        panel.add(top, BorderLayout.CENTER);
        panel.add(buildButtonBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setBackground(BG_MAIN);

        JButton openButton = pillButton("📂  Открыть .cs", BG_FIELD, TEXT_MAIN);
        openButton.addActionListener(e -> openFile());

        JButton analyzeButton = pillButton("▶  Рассчитать метрики", ACCENT, BG_MAIN);
        analyzeButton.addActionListener(e -> analyze());

        JButton clearButton = pillButton("✕  Очистить", BG_FIELD, TEXT_MAIN);
        clearButton.addActionListener(e -> clearAll());

        bar.add(openButton);
        bar.add(analyzeButton);
        bar.add(clearButton);
        return bar;
    }

    private JButton pillButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        return b;
    }

    // ---------- правая часть: вкладки "Таблица" / "Метрики" ----------
    private JComponent buildResultsTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG_PANEL);
        tabs.setForeground(TEXT_MAIN);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Таблица операторов/операндов", buildTablePanel());
        tabs.addTab("Метрики", buildMetricsPanel());
        return tabs;
    }

    private JComponent buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setBackground(BG_FIELD);
        table.setForeground(TEXT_MAIN);
        table.setGridColor(BORDER_CLR);
        table.setSelectionBackground(ACCENT_SOFT);
        table.setSelectionForeground(TEXT_MAIN);
        table.setFont(new Font("Consolas", Font.PLAIN, 13));

        table.getTableHeader().setBackground(BG_PANEL);
        table.getTableHeader().setForeground(ACCENT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));
        scroll.getViewport().setBackground(BG_FIELD);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildMetricsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel baseGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        baseGrid.setBackground(BG_PANEL);
        baseGrid.add(metricCard("η1", "словарь операторов", eta1Value));
        baseGrid.add(metricCard("η2", "словарь операндов", eta2Value));
        baseGrid.add(metricCard("N1", "число операторов", n1Value));
        baseGrid.add(metricCard("N2", "число операндов", n2Value));

        JPanel basePanel = new JPanel(new BorderLayout(0, 8));
        basePanel.setBackground(BG_PANEL);
        basePanel.add(sectionLabel("6 БАЗОВЫХ МЕТРИК"), BorderLayout.NORTH);
        basePanel.add(baseGrid, BorderLayout.CENTER);

        JPanel extList = new JPanel(new GridLayout(3, 1, 0, 10));
        extList.setBackground(BG_PANEL);
        extList.add(derivedRow(vocabValue));
        extList.add(derivedRow(lengthValue));
        extList.add(derivedRow(volumeValue));

        JPanel extPanel = new JPanel(new BorderLayout(0, 8));
        extPanel.setBackground(BG_PANEL);
        extPanel.add(sectionLabel("3 РАСШИРЕННЫЕ (ПРОИЗВОДНЫЕ) МЕТРИКИ"), BorderLayout.NORTH);
        extPanel.add(extList, BorderLayout.CENTER);

        panel.add(basePanel, BorderLayout.NORTH);
        panel.add(extPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(ACCENT);
        return l;
    }

    private JComponent metricCard(String symbol, String caption, JLabel valueLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_FIELD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel symbolLabel = new JLabel(symbol);
        symbolLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        symbolLabel.setForeground(ACCENT);

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        captionLabel.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(TEXT_MAIN);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        symbolLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(symbolLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(captionLabel);
        return card;
    }

    private JComponent derivedRow(JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_FIELD);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        valueLabel.setForeground(TEXT_MAIN);
        row.add(valueLabel, BorderLayout.WEST);
        return row;
    }

    // ---------- нижняя строка состояния ----------
    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    // ================= логика (без изменений) =================

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите файл с кодом на C# (*.cs)");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Файлы C# (*.cs)", "cs");
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);

        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".cs")) {
            JOptionPane.showMessageDialog(this,
                    "Можно открывать только файлы с расширением .cs\nВыбран файл: " + file.getName(),
                    "Неверный тип файла", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            codeArea.setText(content);
            statusLabel.setText("Загружен файл: " + file.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось прочитать файл: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAll() {
        codeArea.setText("");
        tableModel.setRowCount(0);
        eta1Value.setText("-");
        eta2Value.setText("-");
        n1Value.setText("-");
        n2Value.setText("-");
        vocabValue.setText("η = η1 + η2 = -");
        lengthValue.setText("N = N1 + N2 = -");
        volumeValue.setText("V = N·log2(η) = -");
        statusLabel.setText("Готово к анализу");
    }

    private void analyze() {
        String source = codeArea.getText();
        if (source == null || source.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Сначала вставьте или загрузите код на C#.",
                    "Нет данных", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<CSharpTokenizer.Token> tokens = CSharpTokenizer.tokenize(source);
        HalsteadMetrics hm = HalsteadMetrics.compute(tokens);

        fillTable(hm);

        eta1Value.setText(String.valueOf(hm.eta1));
        eta2Value.setText(String.valueOf(hm.eta2));
        n1Value.setText(String.valueOf(hm.N1));
        n2Value.setText(String.valueOf(hm.N2));

        vocabValue.setText(String.format(Locale.ROOT,
                "η = η1 + η2 = %d", hm.vocabulary));
        lengthValue.setText(String.format(Locale.ROOT,
                "N = N1 + N2 = %d", hm.length));
        volumeValue.setText(String.format(Locale.ROOT,
                "V = N·log2(η) = %.2f", hm.volume));

        statusLabel.setText(String.format(Locale.ROOT,
                "Проанализировано: N1=%d, N2=%d, η=%d, N=%d, V=%.2f",
                hm.N1, hm.N2, hm.vocabulary, hm.length, hm.volume));
    }

    private void fillTable(HalsteadMetrics hm) {
        tableModel.setRowCount(0);
        List<String> opNames = new java.util.ArrayList<>(hm.operatorFreq.keySet());
        List<String> operandNames = new java.util.ArrayList<>(hm.operandFreq.keySet());
        int rows = Math.max(opNames.size(), operandNames.size());

        for (int i = 0; i < rows; i++) {
            Object j = i < opNames.size() ? (i + 1) : "";
            Object opName = i < opNames.size() ? opNames.get(i) : "";
            Object f1j = i < opNames.size() ? hm.operatorFreq.get(opNames.get(i)) : "";

            Object idx = i < operandNames.size() ? (i + 1) : "";
            Object operandName = i < operandNames.size() ? operandNames.get(i) : "";
            Object f2i = i < operandNames.size() ? hm.operandFreq.get(operandNames.get(i)) : "";

            tableModel.addRow(new Object[]{j, opName, f1j, idx, operandName, f2i});
        }
    }

    private static final String SAMPLE_CODE =
            "using System;\n" +
                    "\n" +
                    "class Demo\n" +
                    "{\n" +
                    "    static void Main(string[] args)\n" +
                    "    {\n" +
                    "        int a = 5;\n" +
                    "        int b = 10;\n" +
                    "        int sum = Add(a, b);\n" +
                    "        Console.WriteLine(\"Сумма = \" + sum);\n" +
                    "    }\n" +
                    "\n" +
                    "    static int Add(int x, int y)\n" +
                    "    {\n" +
                    "        return x + y;\n" +
                    "    }\n" +
                    "}\n";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HalsteadApp().setVisible(true));
    }
}