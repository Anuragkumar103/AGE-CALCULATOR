import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.io.*;
import java.nio.file.*;

public class AgeCalculator extends JFrame {

    // ── Theme colors ──────────────────────────────────────────────────────────
    static final Color[] DAY_BG      = {new Color(0xFF6B6B), new Color(0xFFA07A),
                                        new Color(0xFFD700), new Color(0x98FB98)};
    static final Color[] NIGHT_BG    = {new Color(0x0D0221), new Color(0x190535),
                                        new Color(0x1C0645), new Color(0x120230)};
    static final Color DAY_CARD      = new Color(255,255,255,220);
    static final Color NIGHT_CARD    = new Color(25, 10, 60, 220);
    static final Color DAY_TEXT      = new Color(0x2C1810);
    static final Color NIGHT_TEXT    = new Color(0xE8D5FF);
    static final Color DAY_ACCENT    = new Color(0xFF4757);
    static final Color NIGHT_ACCENT  = new Color(0x9D4EDD);
    static final Color DAY_BTN       = new Color(0xFF6B35);
    static final Color NIGHT_BTN     = new Color(0x7B2FBE);

    // ── State ─────────────────────────────────────────────────────────────────
    boolean isNightMode = false;
    float   animProgress = 0f;
    int     pulseAngle   = 0;
    List<String[]> history = new ArrayList<>();
    static final String DATA_FILE = System.getProperty("user.home") + "/age_calculator_data.csv";

    // ── UI components ─────────────────────────────────────────────────────────
    JPanel     bgPanel;
    JPanel     cardPanel;
    JTextField nameField;
    JSpinner   daySpinner, monthSpinner, yearSpinner;
    JLabel     resultLabel, liveLabel, daysLabel, nextBdayLabel;
    JTable     historyTable;
    DefaultTableModel tableModel;
    AnimatedButton calculateBtn, clearBtn, themeBtn;
    JLabel     themeIcon;
    javax.swing.Timer bgAnimTimer, pulseTimer, liveTimer;

    public AgeCalculator() {
        setTitle("✨ Age Calculator");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        loadHistory();
        buildUI();
        startAnimations();
        setVisible(true);
    }

    // ── Build full UI ─────────────────────────────────────────────────────────
    void buildUI() {
        bgPanel = new GradientPanel();
        bgPanel.setLayout(new BorderLayout());

        // ─── TOP BAR ───
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(18, 30, 10, 30));

        JLabel title = new JLabel("✨  Age Calculator");
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 28));
        title.setForeground(Color.WHITE);

        themeBtn = new AnimatedButton("🌙  Night Mode");
        themeBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        themeBtn.addActionListener(e -> toggleTheme());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(themeBtn, BorderLayout.EAST);
        bgPanel.add(topBar, BorderLayout.NORTH);

        // ─── CENTER CONTENT ───
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.BOTH;

        // -- Input card --
        JPanel inputCard = makeCard();
        inputCard.setLayout(new GridBagLayout());
        GridBagConstraints ic = new GridBagConstraints();
        ic.insets = new Insets(8, 10, 8, 10);
        ic.fill = GridBagConstraints.HORIZONTAL;

        JLabel inputTitle = makeCardTitle("Enter Your Details");
        ic.gridx=0; ic.gridy=0; ic.gridwidth=3; inputCard.add(inputTitle, ic);

        ic.gridwidth=1;
        ic.gridy=1; ic.gridx=0; inputCard.add(makeLabel("Full Name:"), ic);
        nameField = new JTextField(18);
        styleField(nameField);
        ic.gridx=1; ic.gridwidth=2; inputCard.add(nameField, ic);

        ic.gridwidth=1; ic.gridy=2; ic.gridx=0; inputCard.add(makeLabel("Birth Date:"), ic);
        daySpinner   = makeSpinner(1, 31, 1);
        monthSpinner = makeSpinner(1, 12, 1);
        yearSpinner  = makeSpinner(1900, LocalDate.now().getYear(), 2000);
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dateRow.setOpaque(false);
        dateRow.add(makeTiny("Day")); dateRow.add(daySpinner);
        dateRow.add(makeTiny("Month")); dateRow.add(monthSpinner);
        dateRow.add(makeTiny("Year")); dateRow.add(yearSpinner);
        ic.gridx=1; ic.gridwidth=2; inputCard.add(dateRow, ic);

        calculateBtn = new AnimatedButton("🎂  Calculate Age");
        calculateBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        calculateBtn.setPreferredSize(new Dimension(200, 46));
        calculateBtn.addActionListener(e -> calculateAge());
        clearBtn = new AnimatedButton("🗑  Clear");
        clearBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        clearBtn.addActionListener(e -> clearFields());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        btnRow.add(calculateBtn); btnRow.add(clearBtn);
        ic.gridy=3; ic.gridx=0; ic.gridwidth=3; inputCard.add(btnRow, ic);

        // -- Result card --
        JPanel resultCard = makeCard();
        resultCard.setLayout(new GridBagLayout());
        GridBagConstraints rc = new GridBagConstraints();
        rc.insets = new Insets(6, 10, 6, 10);
        rc.gridx=0; rc.gridy=0;
        resultCard.add(makeCardTitle("Your Age"), rc);
        resultLabel = new JLabel("—", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 38));
        rc.gridy=1; resultCard.add(resultLabel, rc);
        daysLabel = new JLabel("", SwingConstants.CENTER);
        daysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        rc.gridy=2; resultCard.add(daysLabel, rc);
        nextBdayLabel = new JLabel("", SwingConstants.CENTER);
        nextBdayLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        rc.gridy=3; resultCard.add(nextBdayLabel, rc);
        liveLabel = new JLabel("", SwingConstants.CENTER);
        liveLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        rc.gridy=4; resultCard.add(liveLabel, rc);

        // -- History card --
        JPanel histCard = makeCard();
        histCard.setLayout(new BorderLayout(0,10));
        histCard.add(makeCardTitle("📋  Saved Records"), BorderLayout.NORTH);
        String[] cols = {"Name", "DOB", "Age", "Days Lived", "Saved On"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(tableModel);
        styleTable();
        reloadTable();
        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(0, 180));
        histCard.add(scroll, BorderLayout.CENTER);

        AnimatedButton deleteBtn = new AnimatedButton("🗑  Delete Selected");
        deleteBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        deleteBtn.addActionListener(e -> deleteSelected());
        histCard.add(deleteBtn, BorderLayout.SOUTH);

        // Layout cards into center
        gbc.weightx=0.42; gbc.weighty=0.5; gbc.gridx=0; gbc.gridy=0;
        centerWrapper.add(inputCard, gbc);
        gbc.weightx=0.28; gbc.gridx=1;
        centerWrapper.add(resultCard, gbc);
        gbc.weightx=1.0; gbc.gridx=0; gbc.gridy=1; gbc.gridwidth=2;
        centerWrapper.add(histCard, gbc);

        bgPanel.add(centerWrapper, BorderLayout.CENTER);

        // ─── STATUS BAR ───
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusBar.setOpaque(false);
        JLabel status = new JLabel("🌟  All records saved to: " + DATA_FILE);
        status.setForeground(new Color(255,255,255,180));
        status.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        statusBar.add(status);
        bgPanel.add(statusBar, BorderLayout.SOUTH);

        setContentPane(bgPanel);
        applyTheme();
    }

    // ── Age Calculation ───────────────────────────────────────────────────────
    void calculateAge() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { showMsg("Please enter a name!"); return; }

        int d = (int) daySpinner.getValue();
        int m = (int) monthSpinner.getValue();
        int y = (int) yearSpinner.getValue();
        LocalDate dob, today = LocalDate.now();
        try { dob = LocalDate.of(y, m, d); }
        catch (Exception ex) { showMsg("Invalid date!"); return; }
        if (dob.isAfter(today)) { showMsg("Birth date cannot be in the future!"); return; }

        Period p = Period.between(dob, today);
        long totalDays = ChronoUnit.DAYS.between(dob, today);

        // Next birthday
        LocalDate nextBday = dob.withYear(today.getYear());
        if (!nextBday.isAfter(today)) nextBday = nextBday.plusYears(1);
        long daysUntil = ChronoUnit.DAYS.between(today, nextBday);

        String ageStr = p.getYears()+" yrs, "+p.getMonths()+" mo, "+p.getDays()+" days";
        resultLabel.setText("<html><center>"+ageStr+"</center></html>");
        daysLabel.setText("📅  Total days lived: " + String.format("%,d", totalDays));
        nextBdayLabel.setText(daysUntil==0 ? "🎉  Happy Birthday today!" :
                "🎂  Next birthday in "+daysUntil+" days");

        // Save record
        String savedOn = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
        String dobStr = String.format("%02d/%02d/%04d", d, m, y);
        String[] row = {name, dobStr, p.getYears()+" yrs", String.format("%,d",totalDays), savedOn};
        history.add(row);
        saveHistory();
        reloadTable();

        // Animate result
        animProgress = 0f;
        pulseAngle = 0;
    }

    void clearFields() {
        nameField.setText("");
        daySpinner.setValue(1); monthSpinner.setValue(1); yearSpinner.setValue(2000);
        resultLabel.setText("—");
        daysLabel.setText(""); nextBdayLabel.setText(""); liveLabel.setText("");
    }

    // ── Theme Toggle ──────────────────────────────────────────────────────────
    void toggleTheme() {
        isNightMode = !isNightMode;
        themeBtn.setText(isNightMode ? "☀️  Day Mode" : "🌙  Night Mode");
        applyTheme();
        bgPanel.repaint();
    }

    void applyTheme() {
        Color cardCol  = isNightMode ? NIGHT_CARD  : DAY_CARD;
        Color textCol  = isNightMode ? NIGHT_TEXT  : DAY_TEXT;
        Color accentCol= isNightMode ? NIGHT_ACCENT: DAY_ACCENT;
        Color btnCol   = isNightMode ? NIGHT_BTN   : DAY_BTN;

        applyToCards(bgPanel, cardCol, textCol, accentCol, btnCol);
        if (historyTable != null) {
            historyTable.setForeground(textCol);
            historyTable.setBackground(isNightMode ? new Color(30,10,60,180) : new Color(255,255,255,200));
            historyTable.getTableHeader().setBackground(isNightMode ? new Color(80,20,140) : new Color(255,100,50));
            historyTable.getTableHeader().setForeground(Color.WHITE);
        }
    }

    void applyToCards(Container c, Color card, Color text, Color accent, Color btn) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof AnimatedButton ab) {
                ab.baseColor = btn; ab.repaint();
            } else if (comp.getClass().getSimpleName().equals("CardPanel")) {
                comp.setBackground(card);
            } else if (comp instanceof JLabel lbl) {
                if (lbl == resultLabel) lbl.setForeground(accent);
                else if (lbl == daysLabel || lbl == nextBdayLabel || lbl == liveLabel)
                    lbl.setForeground(text);
            } else if (comp instanceof JTextField tf) {
                tf.setForeground(text);
                tf.setBackground(isNightMode ? new Color(40,10,80) : Color.WHITE);
                tf.setCaretColor(accent);
            }
            if (comp instanceof Container sub) applyToCards(sub, card, text, accent, btn);
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────
    void startAnimations() {
        bgAnimTimer = new javax.swing.Timer(50, e -> {
            pulseAngle = (pulseAngle + 2) % 360;
            bgPanel.repaint();
        });
        bgAnimTimer.start();

        liveTimer = new javax.swing.Timer(1000, e -> {
            if (!daysLabel.getText().isEmpty()) {
                liveLabel.setText("⏱  " + LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        });
        liveTimer.start();
    }

    // ── Data Persistence ──────────────────────────────────────────────────────
    void saveHistory() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE))) {
            pw.println("Name,DOB,Age,DaysLived,SavedOn");
            for (String[] row : history)
                pw.println(String.join(",", row));
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    void loadHistory() {
        history.clear();
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line; boolean first=true;
            while ((line = br.readLine()) != null) {
                if (first) { first=false; continue; }
                history.add(line.split(",", -1));
            }
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    void reloadTable() {
        tableModel.setRowCount(0);
        for (String[] row : history) tableModel.addRow(row);
    }

    void deleteSelected() {
        int sel = historyTable.getSelectedRow();
        if (sel < 0) { showMsg("Select a row to delete."); return; }
        history.remove(sel);
        saveHistory();
        reloadTable();
    }

    // ── Helper factories ──────────────────────────────────────────────────────
    JPanel makeCard() {
        JPanel p = new JPanel() {
            { setOpaque(true); }
            public String getSimpleName() { return "CardPanel"; }
        };
        p.setBackground(DAY_CARD);
        p.setBorder(new CompoundBorder(
            new LineBorder(new Color(255,255,255,120), 1, true),
            new EmptyBorder(18, 22, 18, 22)));
        // rounded via custom paint
        return p;
    }

    JLabel makeCardTitle(String s) {
        JLabel l = new JLabel(s);
        l.setFont(new Font("Segoe UI Emoji", Font.BOLD, 17));
        l.setForeground(DAY_ACCENT);
        return l;
    }

    JLabel makeLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(DAY_TEXT);
        return l;
    }

    JLabel makeTiny(String s) {
        JLabel l = new JLabel(s);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0x888888));
        return l;
    }

    void styleField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tf.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xCCCCCC), 1, true),
            new EmptyBorder(4, 8, 4, 8)));
    }

    JSpinner makeSpinner(int min, int max, int val) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setPreferredSize(new Dimension(70, 32));
        sp.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return sp;
    }

    void styleTable() {
        historyTable.setRowHeight(28);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setShowGrid(false);
        historyTable.setIntercellSpacing(new Dimension(0, 4));
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        historyTable.setSelectionBackground(new Color(0xFF4757, false));
        historyTable.setOpaque(false);
    }

    void showMsg(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Age Calculator", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Gradient Background Panel ─────────────────────────────────────────────
    class GradientPanel extends JPanel {
        GradientPanel() { setOpaque(true); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color[] colors = isNightMode ? NIGHT_BG : DAY_BG;
            int w = getWidth(), h = getHeight();

            // Animated gradient background
            GradientPaint gp = new GradientPaint(
                (float)(w * 0.5 + w * 0.3 * Math.cos(Math.toRadians(pulseAngle))), 0, colors[0],
                (float)(w * 0.5 + w * 0.3 * Math.cos(Math.toRadians(pulseAngle + 180))), h, colors[2]
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // Decorative blobs
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(pulseAngle + i * 90);
                int bx = (int)(w * 0.5 + w * 0.38 * Math.cos(angle));
                int by = (int)(h * 0.5 + h * 0.35 * Math.sin(angle));
                int br = 180 + i * 30;
                g2.setColor(colors[i % colors.length]);
                g2.fillOval(bx - br/2, by - br/2, br, br);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    // ── Animated Button ───────────────────────────────────────────────────────
    static class AnimatedButton extends JButton {
        Color baseColor;
        float hoverAlpha = 0f;
        boolean hovering = false;
        javax.swing.Timer hoverTimer;

        AnimatedButton(String text) {
            super(text);
            baseColor = DAY_BTN;
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(160, 40));

            hoverTimer = new javax.swing.Timer(20, e -> {
                hoverAlpha += hovering ? 0.1f : -0.1f;
                hoverAlpha = Math.max(0f, Math.min(1f, hoverAlpha));
                repaint();
                if ((hovering && hoverAlpha >= 1f) || (!hovering && hoverAlpha <= 0f))
                    hoverTimer.stop();
            });

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovering=true; hoverTimer.restart(); }
                public void mouseExited(MouseEvent e)  { hovering=false; hoverTimer.restart(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Base fill
            g2.setColor(baseColor);
            g2.fillRoundRect(0, 0, w, h, 22, 22);

            // Hover glow
            if (hoverAlpha > 0) {
                Color lighter = baseColor.brighter();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hoverAlpha * 0.5f));
                g2.setColor(lighter);
                g2.fillRoundRect(0, 0, w, h, 22, 22);
            }

            // Border shimmer
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setColor(new Color(255,255,255,80));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w-2, h-2, 20, 20);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Set modern look
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("OptionPane.background", Color.WHITE);

        SwingUtilities.invokeLater(AgeCalculator::new);
    }
}

