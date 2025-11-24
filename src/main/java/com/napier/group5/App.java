package com.napier.group5;

import java.sql.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application class for the World Population reporting tool.
 * It connects to a MySQL "world" database and prints many reports
 * (countries, cities, capitals, population breakdowns, languages)
 * in nicely formatted text tables.
 */
public class App {

    /**
     * Logger used for all application messages (info, warnings, errors).
     */
    private static final Logger log = Logger.getLogger(App.class.getName());

    // Static initialiser block – runs once when the class is loaded.
    static {
        // Set this logger’s level to FINE (so INFO, WARNING, SEVERE, FINE are all shown).
        log.setLevel(Level.FINE);

        // Get the root logger (global logger for all Java util logging)
        Logger rootLogger = Logger.getLogger("");
        // For every handler attached to the root logger (e.g., console output),
        // set its level and a simple format.
        for (var handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.FINE);

            // Optional: use a very simple formatter:
            //   LEVEL: message
            handler.setFormatter(new java.util.logging.SimpleFormatter() {
                private static final String format = "%4$s: %5$s%n";

                @Override
                public synchronized String format(java.util.logging.LogRecord lr) {
                    return String.format(
                            format,
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

    /**
     * Simple helper method just used in unit tests (example of a pure method).
     */
    public int add(int a, int b) {
        return a + b;
    }

    // -------------------------------------------------------------------------
    // Environment variable helper
    // -------------------------------------------------------------------------

    /**
     * Reads an environment variable and returns a default value if it is:
     *  - not set (null) OR
     *  - blank ("" or only spaces).
     *
     * @param key Name of environment variable
     * @param def Default value to use if not set or blank
     * @return The environment value or the default
     */
    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    // -------------------------------------------------------------------------
    // Database connection with retry
    // -------------------------------------------------------------------------

    /**
     * Attempts to connect to the database multiple times with a delay between tries.
     * This is useful when MySQL in Docker might not be ready the instant the app starts.
     *
     * @param url      JDBC URL (jdbc:mysql://host:port/db)
     * @param user     DB username
     * @param pass     DB password
     * @param attempts Number of times to try
     * @param wait     Delay between attempts
     * @return a live {@link Connection} if successful
     * @throws Exception last SQLException if all attempts fail
     */
    static Connection connectWithRetry(String url, String user, String pass,
                                       int attempts, Duration wait) throws Exception {

        // Special case for unit tests – if URL starts with this prefix, we deliberately fail.
        if (url.startsWith("test://fail")) {
            throw new SQLException("Simulated failure for unit test");
        }

        SQLException last = null;

        // Try up to "attempts" times
        for (int i = 1; i <= attempts; i++) {
            try {
                // Log attempt number
                if (log.isLoggable(Level.INFO)) {
                    int finalI = i;
                    log.info(() -> String.format("Connecting (attempt %d/%d)...", finalI, attempts));
                }
                // Try to get a connection
                return DriverManager.getConnection(url, user, pass);

            } catch (SQLException e) {
                // Save exception so we can rethrow the last one if all attempts fail
                last = e;

                // Log warning message (for example: "Not ready yet: Connection refused")
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(() -> "Not ready yet: " + e.getMessage());
                }

                // Wait before trying again
                Thread.sleep(wait.toMillis());
            }
        }

        // If we get here, all attempts failed, so throw the last exception
        throw last;
    }

    // -------------------------------------------------------------------------
    // Table rendering configuration (ASCII / Unicode)
    // -------------------------------------------------------------------------

    /**
     * If environment variable TABLE_ASCII is "0", then use Unicode borders.
     * For any other value (or not set) we use ASCII borders.
     */
    private static final boolean ASCII = !"0".equals(System.getenv("TABLE_ASCII"));

    /**
     * Small helper class that stores which characters to use for drawing the table borders.
     * It supports either simple ASCII (+, -, |) or nicer Unicode box drawing characters.
     */
    private static class Borders {
        final String TL, TR, BL, BR; // top-left, top-right, bottom-left, bottom-right corners
        final String H, V;           // horizontal, vertical
        final String TJ, X, BJ;      // top-join, cross, bottom-join
        final String LT, RT;         // left-tee, right-tee

        Borders(boolean ascii) {
            if (ascii) {
                // ASCII mode
                TL = "+"; TR = "+"; BL = "+"; BR = "+";
                H  = "-"; V  = "|";
                TJ = "+"; X  = "+"; BJ = "+";
                LT = "+"; RT = "+";
            } else {
                // Unicode mode
                TL = "┌"; TR = "┐"; BL = "└"; BR = "┘";
                H  = "─"; V  = "│";
                TJ = "┬"; X  = "┼"; BJ = "┴";
                LT = "├"; RT = "┤";
            }
        }
    }

    /**
     * Global Borders instance, chosen once based on the TABLE_ASCII environment variable.
     */
    private static final Borders B = new Borders(ASCII);

    // -------------------------------------------------------------------------
    // Table rendering helpers: printTable / line / row
    // -------------------------------------------------------------------------

    /**
     * Prints a formatted table (with borders) to the logger.
     *
     * @param title      Title of report (printed as a line before the table)
     * @param headers    Column headers
     * @param rows       List of data rows, each row is a String[] of cell values
     * @param rightAlign For each column, whether numbers should be right-aligned
     */
    private static void printTable(String title, String[] headers, List<String[]> rows, boolean[] rightAlign) {
        if (log.isLoggable(Level.INFO)) {
            // Print title as separate line
            log.info(() -> "\n" + title);
        }

        // Number of columns
        int cols = headers.length;

        // Array holding max width of each column
        int[] w = new int[cols];

        // First, start with header length
        for (int c = 0; c < cols; c++) {
            w[c] = headers[c].length();
        }

        // Then, check each row's cell and update column width
        for (String[] r : rows) {
            for (int c = 0; c < cols; c++) {
                int len = (r[c] == null ? 0 : r[c].length());
                w[c] = Math.max(w[c], len);
            }
        }

        // Build the three border lines (top, middle, bottom)
        String top = line(B.TL, B.TJ, B.TR, w);
        String mid = line(B.LT, B.X , B.RT, w);
        String bot = line(B.BL, B.BJ, B.BR, w);

        if (log.isLoggable(Level.INFO)) {
            // Top border
            log.info(() -> top);
            // Header row (headers are always left aligned here)
            log.info(() -> row(headers, w, new boolean[cols]));
            // Header separator
            log.info(() -> mid);

            // Print each data row
            for (int r = 0; r < rows.size(); r++) {
                int rowIndex = r;
                log.info(() -> row(rows.get(rowIndex), w, rightAlign));
                // After each row, print mid or bottom border
                log.info(() -> rowIndex == rows.size() - 1 ? bot : mid);
            }

            // If there are no data rows, still close the table
            if (rows.isEmpty()) {
                log.info(() -> bot);
            }
        }
    }

    /**
     * Builds a horizontal border line given a left corner, join character, right corner, and column widths.
     *
     * Example: ┌─────┬────────┐
     */
    private static String line(String left, String join, String right, int[] w) {
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < w.length; i++) {
            // Add "width + 2" dashes (1 space padding on each side of cell)
            sb.append(B.H.repeat(w[i] + 2));
            // Final column uses right corner, others use join character
            sb.append(i == w.length - 1 ? right : join);
        }
        return sb.toString();
    }

    /**
     * Builds a single row of the table including left/right border and padding.
     *
     * @param cells      Values for each column
     * @param w          Precomputed widths
     * @param rightAlign For each column, whether to right align
     * @return a String representing the table row (e.g. │  London │ 12345 │)
     */
    private static String row(String[] cells, int[] w, boolean[] rightAlign) {
        StringBuilder sb = new StringBuilder(B.V);
        for (int i = 0; i < w.length; i++) {
            String cell = cells[i] == null ? "" : cells[i];
            int pad = w[i] - cell.length();

            if (rightAlign != null && rightAlign[i]) {
                // Right align: spaces on the left
                sb.append(" ");
                sb.append(" ".repeat(pad));
                sb.append(cell);
                sb.append(" ");
            } else {
                // Left align: value, then spaces
                sb.append(" ");
                sb.append(cell);
                sb.append(" ".repeat(pad));
                sb.append(" ");
            }

            // Column separator
            sb.append(B.V);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Query runner – executes a SQL SELECT and prints a formatted table
    // -------------------------------------------------------------------------

    /**
     * Runs a SELECT query, fetches all rows, and prints them as a table using {@link #printTable}.
     * It also automatically detects numeric columns and formats them without commas,
     * aligned to the right, with up to 2 decimal places for decimals.
     *
     * @param con   Open JDBC connection
     * @param title Title for this report
     * @param sql   SQL text (can be a Java text block)
     * @param cols  Column labels to display and fetch
     * @throws SQLException if the query fails
     */
    static void runQuery(Connection con, String title, String sql, String... cols) throws SQLException {
        // PreparedStatement used here even though there are no parameters,
        // so that it is safe to extend later if needed.
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData md = rs.getMetaData();

            // Map each column label -> index (1-based in JDBC)
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                idx.put(md.getColumnLabel(i), i);
            }

            // --- Number formatting: NO grouping (no commas), up to 2 decimals ---

            // For integer-like columns
            NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
            intFmt.setGroupingUsed(false);

            // For decimal-like columns
            NumberFormat decFmt = NumberFormat.getNumberInstance(Locale.US);
            decFmt.setGroupingUsed(false);
            decFmt.setMinimumFractionDigits(0);
            decFmt.setMaximumFractionDigits(2);

            // For each requested column, decide if it should be right-aligned and if it is decimal.
            boolean[] right = new boolean[cols.length];
            boolean[] isDecimal = new boolean[cols.length];

            for (int i = 0; i < cols.length; i++) {
                int jdbcType = Types.VARCHAR; // default type if not found
                Integer pos = idx.get(cols[i]);
                if (pos != null) {
                    jdbcType = md.getColumnType(pos);
                }

                // Switch over JDBC types to decide formatting
                switch (jdbcType) {
                    case Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT -> {
                        right[i] = true;
                        isDecimal[i] = false;
                    }
                    case Types.DECIMAL, Types.NUMERIC, Types.FLOAT, Types.REAL, Types.DOUBLE -> {
                        right[i] = true;
                        isDecimal[i] = true;
                    }
                    default -> {
                        right[i] = false;
                        isDecimal[i] = false;
                    }
                }
            }

            // Collect all rows as list of String[]
            List<String[]> rows = new ArrayList<>();

            while (rs.next()) {
                String[] r = new String[cols.length];

                for (int c = 0; c < cols.length; c++) {
                    Object val = rs.getObject(cols[c]);

                    if (val == null) {
                        r[c] = "";
                    } else if (right[c] && val instanceof Number n) {
                        // Format numbers with the right number format
                        r[c] = isDecimal[c]
                                ? decFmt.format(n.doubleValue())
                                : intFmt.format(n.longValue());
                    } else {
                        // Default: just toString
                        r[c] = String.valueOf(val);
                    }
                }

                rows.add(r);
            }

            // Finally, print the table for this query
            printTable(title, cols, rows, right);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers for parsing command-line arguments
    // -------------------------------------------------------------------------

    /**
     * Resolve the host and port from:
     *   1) Command line args[0] in form "host:port" (e.g. "db:3306"), OR
     *   2) Environment variables DB_HOST and DB_PORT, OR
     *   3) Defaults "127.0.0.1" and "3306".
     *
     * @param args The full args array from main()
     * @return a String[] of length 2: [0] = host, [1] = port
     */
    static String[] resolveHostPort(String[] args) {
        String host;
        String port;

        if (args.length >= 1) {
            // If first argument is present, split by ":".
            String[] hp = args[0].split(":");
            host = hp[0];
            // If port is not provided, default to 3306
            port = hp.length > 1 ? hp[1] : "3306";
        } else {
            // No arguments → use environment or defaults
            host = env("DB_HOST", "127.0.0.1");
            port = env("DB_PORT", "3306");
        }

        return new String[]{host, port};
    }

    /**
     * Resolve the timeout in milliseconds from command-line arguments or default.
     * If args[1] is present, parse it as an integer. Otherwise default to 30000 ms.
     *
     * @param args Args from main()
     * @return timeout in milliseconds
     */
    static int resolveTimeoutMs(String[] args) {
        if (args.length >= 2) {
            return Integer.parseInt(args[1]);
        }
        return 30000;
    }

    // -------------------------------------------------------------------------
    // main() – entry point
    // -------------------------------------------------------------------------

    /**
     * Entry point of the application.
     *
     * Usage examples:
     *   java com.napier.group5.App               // uses env/default host, port, timeout
     *   java com.napier.group5.App db:3306 60000 // host=db, port=3306, timeout=60000ms
     *
     * It will:
     *   1. Resolve DB host, port, timeout
     *   2. Build JDBC URL
     *   3. Connect with retry
     *   4. Run a series of 32 reports and print them as formatted tables
     */
    public static void main(String[] args) {
        // 1. Resolve host and port from args/env/defaults
        String[] hp = resolveHostPort(args);
        String host = hp[0];
        String port = hp[1];

        // 2. Resolve timeout (ms)
        int timeoutMs = resolveTimeoutMs(args);

        // 3. Read DB name, user, password from environment or use defaults
        String db   = env("DB_NAME", "world");
        String user = env("DB_USER", "app");
        String pass = env("DB_PASSWORD", "app123");

        // 4. Build the MySQL JDBC URL
        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, db
        );

        if (log.isLoggable(Level.INFO)) {
            log.info(() -> String.format("DB -> %s  user=%s  timeout=%dms%n", url, user, timeoutMs));
        }

        try {
            // 5. Ensure MySQL JDBC driver is loaded
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Number of attempts and wait between them for connectWithRetry
            int attempts = 12;
            Duration wait = Duration.ofMillis(timeoutMs / attempts);

            // 6. Open connection with retry logic
            try (Connection con = connectWithRetry(url, user, pass, attempts, wait)) {

                log.info(" Connected!");

                // -----------------------------------------------------------------
                // From this point, we run all 32 required reports.
                // Each report is a call to runQuery(...) with a SQL text block.
                // -----------------------------------------------------------------

                log.info("\n======================");
                log.info("Country Reports");
                log.info("======================");

                // 1. All Countries by Population (World)
                runQuery(con, "1. All Countries by Population (World)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        ORDER BY Population DESC
                        """,
                        "Code", "Name", "Continent", "Region", "Population", "Capital");

                // 2. Countries by Population (Continent = Asia)
                runQuery(con, "2. Countries by Population (Continent = Asia)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Continent = 'Asia'
                        ORDER BY Population DESC
                        """,
                        "Code", "Name", "Continent", "Region", "Population", "Capital");

                // 3. Countries by Population (Region = Caribbean)
                runQuery(con, "3. Countries by Population (Region = Caribbean)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Region = 'Caribbean'
                        ORDER BY Population DESC
                        """,
                        "Code", "Name", "Continent", "Region", "Population", "Capital");

                // 4. Top 10 Countries (World)
                runQuery(con, "4. Top 10 Countries (World)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code", "Name", "Continent", "Region", "Population", "Capital");

                // 5. Top 10 Countries (Continent = Europe)
                runQuery(con, "5. Top 10 Countries (Continent = Europe)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Continent = 'Europe'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code", "Name", "Continent", "Region", "Population", "Capital");

                // 6. Top 10 Countries (Region = Western Europe)
                runQuery(con, "6. Top 10 Countries (Region = Western Europe)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Region = 'Western Europe'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code", "Name", "Continent", "Region", "Population", "Capital");

                log.info("\n======================");
                log.info("City Reports");
                log.info("======================");

                // 7. All cities in world
                runQuery(con, "7. All cities in world",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        ORDER BY Population DESC
                        """,
                        "Name", "Country", "District", "Population");

                // 8. Cities by continent (Africa)
                runQuery(con, "8. Cities by continent (Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Continent = 'Africa'
                        ORDER BY Population DESC
                        """,
                        "Name", "Country", "District", "Population");

                // 9. Cities by region (Central Africa)
                runQuery(con, "9. Cities by region (Central Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Region = 'Central Africa'
                        ORDER BY Population DESC
                        """,
                        "Name", "Country", "District", "Population");

                // 10. Cities by country (Argentina)
                runQuery(con, "10. Cities by country (Argentina)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Name = 'Argentina'
                        ORDER BY Population DESC
                        """,
                        "Name", "Country", "District", "Population");

                // 11. Cities by district (Limburg)
                runQuery(con, "11. Cities by district (Limburg)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE ci.District = 'Limburg'
                        ORDER BY Population DESC
                        """,
                        "Name", "Country", "District", "Population");

                // 12. Top 10 cities in world
                runQuery(con, "12. Top 10 cities in world",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "District", "Population");

                // 13. Top 10 cities by continent (Africa)
                runQuery(con, "13. Top 10 cities by continent (Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Continent = 'Africa'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "District", "Population");

                // 14. Top 10 cities by region (Central Africa)
                runQuery(con, "14. Top 10 cities by region (Central Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Region = 'Central Africa'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "District", "Population");

                // 15. Top 10 cities by country (Argentina)
                runQuery(con, "15. Top 10 cities by country (Argentina)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Name = 'Argentina'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "District", "Population");

                // 16. Top 10 cities by district (Limburg)
                runQuery(con, "16. Top 10 cities by district (Limburg)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE ci.District = 'Limburg'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "District", "Population");

                log.info("\n======================");
                log.info("Capital City Reports");
                log.info("======================");

                // 17. All capital cities
                runQuery(con, "17. All capital cities",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        ORDER BY ci.Population DESC
                        """,
                        "Name", "Country", "Population");

                // 18. Capitals by continent (Asia)
                runQuery(con, "18. Capitals by continent (Asia)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Continent = 'Asia'
                        ORDER BY ci.Population DESC
                        """,
                        "Name", "Country", "Population");

                // 19. Capitals by region (Eastern Asia)
                runQuery(con, "19. Capitals by region (Eastern Asia)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Region = 'Eastern Asia'
                        ORDER BY ci.Population DESC
                        """,
                        "Name", "Country", "Population");

                // 20. Top 10 capitals in world
                runQuery(con, "20. Top 10 capitals in world",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        ORDER BY ci.Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "Population");

                // 21. Top 10 capitals by continent (Africa)
                runQuery(con, "21. Top 10 capitals by continent (Africa)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Continent = 'Africa'
                        ORDER BY ci.Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "Population");

                // 22. Top 10 capitals by region (Western Europe)
                runQuery(con, "22. Top 10 capitals by region (Western Europe)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Region = 'Western Europe'
                        ORDER BY ci.Population DESC
                        LIMIT 10
                        """,
                        "Name", "Country", "Population");

                log.info("\n======================");
                log.info("Population Distribution and Population by Location");
                log.info("======================");

                // 23. Population Report (Continent)
                runQuery(con, "23. Population Report (Continent)",
                        """
                        SELECT
                            co.Continent AS Name,
                            SUM(co.Population) AS `Total Population`,
                            ROUND(SUM(ci.City_Pop) / SUM(co.Population) * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.City_Pop) / SUM(co.Population)) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        LEFT JOIN (
                            SELECT CountryCode, SUM(Population) AS City_Pop
                            FROM city
                            GROUP BY CountryCode
                        ) ci ON co.Code = ci.CountryCode
                        GROUP BY co.Continent
                        ORDER BY `Total Population` DESC
                        """,
                        "Name", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                // 24. Population Report (Region)
                runQuery(con, "24. Population Report (Region)",
                        """
                        SELECT
                            co.Region AS Name,
                            SUM(co.Population) AS `Total Population`,
                            ROUND(SUM(ci.City_Pop) / SUM(co.Population) * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.City_Pop) / SUM(co.Population)) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        LEFT JOIN (
                            SELECT CountryCode, SUM(Population) AS City_Pop
                            FROM city
                            GROUP BY CountryCode
                        ) ci ON co.Code = ci.CountryCode
                        GROUP BY co.Region
                        ORDER BY `Total Population` DESC
                        """,
                        "Name", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                // 25. Population Report (Country)
                runQuery(con, "25. Population Report (Country)",
                        """
                        SELECT
                            co.Name AS Country,
                            co.Population AS `Total Population`,
                            ROUND(SUM(ci.Population) / co.Population * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.Population) / co.Population) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        LEFT JOIN city ci ON co.Code = ci.CountryCode
                        GROUP BY co.Code, co.Name, co.Population
                        ORDER BY `Total Population` DESC
                        """,
                        "Country", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                // 26. World population (single number)
                runQuery(con, "26. World population",
                        """
                        SELECT SUM(Population) AS total_world_population
                        FROM country
                        """,
                        "total_world_population");

                // 27. Continent population (Africa)
                runQuery(con, "27. Continent population (Africa)",
                        """
                        SELECT
                            co.Continent AS Name,
                            SUM(co.Population) AS `Total Population`,
                            ROUND(SUM(ci.City_Pop) / SUM(co.Population) * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.City_Pop) / SUM(co.Population)) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        LEFT JOIN (
                            SELECT CountryCode, SUM(Population) AS City_Pop
                            FROM city
                            GROUP BY CountryCode
                        ) ci ON co.Code = ci.CountryCode
                        WHERE co.Continent = 'Africa'
                        GROUP BY co.Continent
                        ORDER BY `Total Population` DESC
                        """,
                        "Name", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                // 28. Region population (Central Africa)
                runQuery(con, "28. Region population (Central Africa)",
                        """
                        SELECT
                            co.Region AS Name,
                            SUM(co.Population) AS `Total Population`,
                            ROUND(SUM(ci.City_Pop) / SUM(co.Population) * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.City_Pop) / SUM(co.Population)) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        LEFT JOIN (
                            SELECT CountryCode, SUM(Population) AS City_Pop
                            FROM city
                            GROUP BY CountryCode
                        ) ci ON co.Code = ci.CountryCode
                        WHERE co.Region = 'Central Africa'
                        GROUP BY co.Region
                        ORDER BY `Total Population` DESC
                        """,
                        "Name", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                // 29. Country population (Spain)
                runQuery(con, "29. Country population (Spain)",
                        """
                        SELECT
                            co.Name AS Name,
                            SUM(co.Population) AS `Total Population`,
                            ROUND(SUM(ci.City_Pop) / SUM(co.Population) * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.City_Pop) / SUM(co.Population)) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        LEFT JOIN (
                            SELECT CountryCode, SUM(Population) AS City_Pop
                            FROM city
                            GROUP BY CountryCode
                        ) ci ON co.Code = ci.CountryCode
                        WHERE co.Name = 'Spain'
                        GROUP BY co.Name
                        ORDER BY `Total Population` DESC
                        """,
                        "Name", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                // 30. District population (Limburg)
                runQuery(con, "30. District population (Limburg)",
                        """
                        SELECT
                            ci.District AS District,
                            SUM(ci.City_Pop) AS `Total Population`,
                            ROUND(SUM(ci.City_Pop) / SUM(co.Population) * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.City_Pop) / SUM(co.Population)) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        INNER JOIN (
                            SELECT CountryCode, District, SUM(Population) AS City_Pop
                            FROM city
                            WHERE District = 'Limburg'
                            GROUP BY CountryCode, District
                        ) ci ON co.Code = ci.CountryCode
                        GROUP BY ci.District
                        ORDER BY `Total Population` DESC
                        """,
                        "District", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                // 31. City population (London)
                runQuery(con, "31. City population (London)",
                        """
                        SELECT
                            ci.Name AS city_name,
                            SUM(ci.City_Pop) AS `Total Population`,
                            ROUND(SUM(ci.City_Pop) / SUM(co.Population) * 100, 2) AS `Population in Cities (%)`,
                            ROUND((1 - SUM(ci.City_Pop) / SUM(co.Population)) * 100, 2) AS `Population not in Cities (%)`
                        FROM country co
                        INNER JOIN (
                            SELECT CountryCode, Name, SUM(Population) AS City_Pop
                            FROM city
                            WHERE Name = 'London'
                            GROUP BY CountryCode, Name
                        ) ci ON co.Code = ci.CountryCode
                        GROUP BY ci.Name
                        ORDER BY `Total Population` DESC
                        """,
                        "city_name", "Total Population", "Population in Cities (%)", "Population not in Cities (%)");

                log.info("\n======================");
                log.info("Language Reports");
                log.info("======================");

                // 32. Population by language (Chinese, English, Hindi, Spanish, Arabic)
                runQuery(con, "32. Population by language (Chinese, English, Hindi, Spanish, Arabic)",
                        """
                        SELECT
                            cl.Language AS Language,
                            ROUND(SUM(c.Population * cl.Percentage / 100)) AS Num_of_people,
                            ROUND(
                                (SUM(c.Population * cl.Percentage / 100) /
                                 (SELECT SUM(Population) FROM country) * 100), 2
                            ) AS Percent_of_world
                        FROM countrylanguage cl
                        JOIN country c ON cl.CountryCode = c.Code
                        WHERE cl.Language IN ('Chinese','English','Hindi','Spanish','Arabic')
                        GROUP BY cl.Language
                        ORDER BY Num_of_people DESC
                        """,
                        "Language", "Num_of_people", "Percent_of_world");
            }
        } catch (Exception e) {
            // If anything goes wrong (connection, query, etc.), log the error and exit with status 1
            if (log.isLoggable(Level.SEVERE)) {
                log.severe(() -> String.format("Error: " + e.getMessage(), e));
            }
            System.exit(1);
        }
    }
}
