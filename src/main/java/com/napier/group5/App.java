package com.napier.group5;

import java.sql.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    private static final Logger log = Logger.getLogger(App.class.getName());
    static {
        // Set logger level to FINE (prints FINE, INFO, WARNING, SEVERE)
        log.setLevel(Level.FINE);

        // Configure root logger's handlers (e.g., ConsoleHandler)
        Logger rootLogger = Logger.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.FINE);

            // Optional: simple formatter for cleaner output
            handler.setFormatter(new java.util.logging.SimpleFormatter() {
                private static final String format = "%4$s: %5$s%n";
                @Override
                public synchronized String format(java.util.logging.LogRecord lr) {
                    return String.format(format,
                            lr.getSourceClassName(),
                            lr.getLoggerName(),
                            lr.getLevel().getLocalizedName(),
                            lr.getLevel().getName(),
                            lr.getMessage()
                    );
                }
            });
        }
    }
    public int add(int a, int b) {
        return a + b;
    }
    // ---------- env & connection ----------
    private static String env(String key, String def) {

        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    // Example changes inside Appp

    static Connection connectWithRetry(String url, String user, String pass,
                                       int attempts, Duration wait) throws Exception {
        if (url.startsWith("test://fail")) {
            throw new SQLException("Simulated failure for unit test");
        }

        SQLException last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                if (log.isLoggable(Level.INFO)) {
                    int finalI = i;
                    log.info(() -> String.format("Connecting (attempt %d/%d)...", finalI, attempts));
                }
                return DriverManager.getConnection(url, user, pass);
            } catch (SQLException e) {
                last = e;
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(() -> "Not ready yet: " + e.getMessage());
                }
                Thread.sleep(wait.toMillis());
            }
        }
        throw last;
    }


    // ---------- table rendering ----------
    // ASCII by default. Set TABLE_ASCII=0 to use Unicode borders.
    private static final boolean ASCII = !"0".equals(System.getenv("TABLE_ASCII"));


    private static class Borders {
        final String TL, TR, BL, BR, H, V, TJ, X, BJ, LT, RT;
        Borders(boolean ascii) {
            if (ascii) {
                TL = "+"; TR = "+"; BL = "+"; BR = "+";
                H  = "-"; V  = "|";
                TJ = "+"; X  = "+"; BJ = "+";
                LT = "+"; RT = "+";
            } else {
                TL = "┌"; TR = "┐"; BL = "└"; BR = "┘";
                H  = "─"; V  = "│";
                TJ = "┬"; X  = "┼"; BJ = "┴";
                LT = "├"; RT = "┤";
            }
        }
    }
    private static final Borders B = new Borders(ASCII);

    private static void printTable(String title, String[] headers, List<String[]> rows, boolean[] rightAlign) {
        if (log.isLoggable(Level.INFO)) {
            log.info(() -> "\n" + title);
        }

        // widths
        int cols = headers.length;
        int[] w = new int[cols];
        for (int c = 0; c < cols; c++) w[c] = headers[c].length();
        for (String[] r : rows)
            for (int c = 0; c < cols; c++)
                w[c] = Math.max(w[c], r[c] == null ? 0 : r[c].length());

        // lines
        String top = line(B.TL, B.TJ, B.TR, w);
        String mid = line(B.LT, B.X , B.RT, w);
        String bot = line(B.BL, B.BJ, B.BR, w);

        if (log.isLoggable(Level.INFO)) {
            log.info(() -> top);
            log.info(() -> row(headers, w, new boolean[cols])); // header
            log.info(() -> mid);
            for (int r = 0; r < rows.size(); r++) {
                int rowIndex = r;
                log.info(() -> row(rows.get(rowIndex), w, rightAlign));
                log.info(() -> rowIndex == rows.size() - 1 ? bot : mid);
            }
            if (rows.isEmpty()) log.info(() -> bot);
        }
    }


    private static String line(String left, String join, String right, int[] w) {
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < w.length; i++) {
            sb.append(B.H.repeat(w[i] + 2));
            sb.append(i == w.length - 1 ? right : join);
        }
        return sb.toString();
    }

    private static String row(String[] cells, int[] w, boolean[] rightAlign) {
        StringBuilder sb = new StringBuilder(B.V);
        for (int i = 0; i < w.length; i++) {
            String cell = cells[i] == null ? "" : cells[i];
            int pad = w[i] - cell.length();
            if (rightAlign != null && rightAlign[i]) {
                sb.append(" ").append(" ".repeat(pad)).append(cell).append(" ");
            } else {
                sb.append(" ").append(cell).append(" ".repeat(pad)).append(" ");
            }
            sb.append(B.V);
        }
        return sb.toString();
    }

    static void runQuery(Connection con, String title, String sql, String... cols) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData md = rs.getMetaData();
            Map<String,Integer> idx = new HashMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) idx.put(md.getColumnLabel(i), i);

            // --- formats: NO grouping (no commas), up to 2 decimals ---
            NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
            intFmt.setGroupingUsed(false);
            NumberFormat decFmt = NumberFormat.getNumberInstance(Locale.US);
            decFmt.setGroupingUsed(false);
            decFmt.setMinimumFractionDigits(0);
            decFmt.setMaximumFractionDigits(2);

            boolean[] right = new boolean[cols.length];
            boolean[] isDecimal = new boolean[cols.length];
            for (int i = 0; i < cols.length; i++) {
                int jdbcType = Types.VARCHAR;
                Integer pos = idx.get(cols[i]);
                if (pos != null) jdbcType = md.getColumnType(pos);
                switch (jdbcType) {
                    case Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT -> { right[i]=true; isDecimal[i]=false; }
                    case Types.DECIMAL, Types.NUMERIC, Types.FLOAT, Types.REAL, Types.DOUBLE -> { right[i]=true; isDecimal[i]=true; }
                    default -> { right[i]=false; isDecimal[i]=false; }
                }
            }

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] r = new String[cols.length];
                for (int c = 0; c < cols.length; c++) {
                    Object val = rs.getObject(cols[c]);
                    if (val == null) r[c] = "";
                    else if (right[c] && val instanceof Number n)
                        r[c] = isDecimal[c] ? decFmt.format(n.doubleValue())
                                : intFmt.format(n.longValue());
                    else r[c] = String.valueOf(val);
                }
                rows.add(r);
            }

            printTable(title, cols, rows, right);
        }
    }

    // ---------- main ----------
    // ---------- main ----------
    // ---------- helpers for argument parsing (easier to test) ----------

    /** Resolve host and port from command-line args or environment variables. */
    static String[] resolveHostPort(String[] args) {
        String host;
        String port;

        if (args.length >= 1) {
            String[] hp = args[0].split(":");
            host = hp[0];
            port = hp.length > 1 ? hp[1] : "3306";
        } else {
            host = env("DB_HOST", "127.0.0.1");
            port = env("DB_PORT", "3306");
        }
        return new String[]{host, port};
    }

    /** Resolve timeout in milliseconds from args (default 30000). */
    static int resolveTimeoutMs(String[] args) {
        if (args.length >= 2) {
            return Integer.parseInt(args[1]);
        }
        return 30000;
    }

    // ---------- main ----------
    public static void main(String[] args) {
        // Use small helper methods so we can unit-test all branches
        String[] hp = resolveHostPort(args);
        String host = hp[0];
        String port = hp[1];

        int timeoutMs = resolveTimeoutMs(args);


        String db   = env("DB_NAME", "world");
        String user = env("DB_USER", "app");
        String pass = env("DB_PASSWORD", "app123");

        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, db
        );
        if (log.isLoggable(Level.INFO)) {
            log.info(() -> String.format("DB -> %s  user=%s  timeout=%dms%n", url, user, timeoutMs));
        }


        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            int attempts = 12;
            Duration wait = Duration.ofMillis(timeoutMs / attempts);

            try (Connection con = connectWithRetry(url, user, pass, attempts, wait)) {

                log.info(" Connected!");

                System.out.println("\n======================");
                System.out.println("Country Reports");
                System.out.println("======================");

                // 1
                runQuery(con, "1. All Countries by Population (World)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        ORDER BY Population DESC
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 2
                runQuery(con, "2. Countries by Population (Continent = Asia)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Continent = 'Asia'
                        ORDER BY Population DESC
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 3
                runQuery(con, "3. Countries by Population (Region = Caribbean)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Region = 'Caribbean'
                        ORDER BY Population DESC
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 4
                runQuery(con, "4. Top N Countries (World)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 5
                runQuery(con, "5. Top N Countries (Continent = Europe)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Continent = 'Europe'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 6
                runQuery(con, "6. Top N Countries (Region = Western Europe)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Region = 'Western Europe'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code","Name","Continent","Region","Population","Capital");
            }

        } catch (Exception e) {
            if (log.isLoggable(Level.SEVERE)) {
                log.severe(() -> String.format("Error: " + e.getMessage(), e));
            }
            System.exit(1);
        }
    }
}
