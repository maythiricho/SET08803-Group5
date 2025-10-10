package com.napier.group5;

import java.sql.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;

public class App {

    // ---------- env & connection ----------
    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static Connection connectWithRetry(String url, String user, String pass,
                                               int attempts, Duration wait) throws Exception {
        SQLException last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                System.out.printf("Connecting (attempt %d/%d)...%n", i, attempts);
                return DriverManager.getConnection(url, user, pass);
            } catch (SQLException e) {
                last = e;
                System.out.println("Not ready yet: " + e.getMessage());
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
        System.out.println();
        System.out.println(title);  // e.g., "32. Population by language (...)"

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

        // render grid
        System.out.println(top);
        System.out.println(row(headers, w, new boolean[cols])); // header left aligned
        System.out.println(mid);
        for (int r = 0; r < rows.size(); r++) {
            System.out.println(row(rows.get(r), w, rightAlign));
            // horizontal rule after every row (grid look)
            System.out.println(r == rows.size() - 1 ? bot : mid);
        }
        if (rows.isEmpty()) System.out.println(bot);
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

    private static void runQuery(Connection con, String title, String sql, String... cols) throws SQLException {
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
    public static void main(String[] args) {
        String host = env("DB_HOST", "127.0.0.1");   // in compose: "db"
        String port = env("DB_PORT", "3306");
        String db   = env("DB_NAME", "world");
        String user = env("DB_USER", "app");
        String pass = env("DB_PASSWORD", "app123");

        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, db
        );
        System.out.printf("DB -> %s  user=%s%n", url, user);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = connectWithRetry(url, user, pass, 12, Duration.ofSeconds(3))) {
                System.out.println("✅ Connected!");

                System.out.println("\n======================");
                System.out.println("Capital City Reports");
                System.out.println("======================");

                // 18
                runQuery(con, "18. Capitals by continent (Asia)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Continent = 'Asia'
                        ORDER BY ci.Population DESC
                        """,
                        "Name","Country","Population");

                // 19
                runQuery(con, "19. Capitals by region (Eastern Asia)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Region = 'Eastern Asia'
                        ORDER BY ci.Population DESC
                        """,
                        "Name","Country","Population");

            }

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
