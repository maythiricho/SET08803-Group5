package com.napier.group5;

import java.sql.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;

/**
 * Population Reporting CLI
 * ------------------------
 * What this program does:
 * - Connects to a MySQL database (the classic "world" sample DB) using JDBC.
 * - Runs a set of 20+ pre-defined reports (countries, cities, capitals, populations, languages).
 * - Prints each result as a nicely formatted ASCII/Unicode table to the console.
 *
 * How to run (examples):
 * 1) From your IDE (IntelliJ):
 *    - Set environment variables in your Run Configuration (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD).
 *    - Run the 'main' method.
 *
 * 2) From command line (JAR already built):
 *    DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=world DB_USER=app DB_PASSWORD=app123 \
 *    java -jar app.jar
 *
 * Docker/Compose tip:
 * - If Java runs in a container on the same Docker network as MySQL:
 *   DB_HOST should usually be the *service/container name* (e.g., "db") not 127.0.0.1.
 * - If Java runs on your Windows/macOS host and MySQL is in Docker with "-p 3306:3306":
 *   DB_HOST=127.0.0.1 and DB_PORT=3306 (or whatever host port you mapped).
 *
 * Pretty tables:
 * - By default we render ASCII borders. Set TABLE_ASCII=0 to use Unicode box-drawing characters.
 *
 * Troubleshooting:
 * - "Communications link failure" or "Connection refused": check host/port, container health, network.
 * - "Access denied": check DB_USER/DB_PASSWORD.
 * - "Unknown database 'world'": confirm DB_NAME and that the schema exists.
 * - SQL syntax errors: double-check the text blocks (""" ... """) and column names for your dataset.
 */
public class App {

    // ---------- env & connection ----------

    /**
     * Reads an environment variable with a fallback default when not set/blank.
     * This keeps the program configurable without hard-coding connection details.
     */
    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    /**
     * Tries to connect to MySQL several times with a delay between attempts.
     * Useful when the DB container takes a few seconds to become "ready".
     *
     * @param url      JDBC URL
     * @param user     DB username
     * @param pass     DB password
     * @param attempts number of tries before giving up
     * @param wait     delay between tries (e.g., 3 seconds)
     * @return a live JDBC Connection
     */
    private static Connection connectWithRetry(String url, String user, String pass,
                                               int attempts, Duration wait) throws Exception {
        SQLException last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                System.out.printf("Connecting (attempt %d/%d)...%n", i, attempts);
                return DriverManager.getConnection(url, user, pass);
            } catch (SQLException e) {
                last = e;
                // Common when DB is still starting; we sleep and try again.
                System.out.println("Not ready yet: " + e.getMessage());
                Thread.sleep(wait.toMillis());
            }
        }
        // After all retries, surface the last error.
        throw last;
    }

    // ---------- table rendering ----------
    // We print results in a grid. You can switch ASCII vs. Unicode borders using TABLE_ASCII env var.

    // ASCII by default. Set TABLE_ASCII=0 to use Unicode borders.
    private static final boolean ASCII = !"0".equals(System.getenv("TABLE_ASCII"));

    /**
     * Small holder for all the border characters we need to draw a table.
     * Keeps the printing logic clean and swappable (ASCII vs. Unicode).
     */
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

    /**
     * Prints a full table with a title, a header row, and all data rows.
     * Right-align is applied to numeric columns to improve readability.
     */
    private static void printTable(String title, String[] headers, List<String[]> rows, boolean[] rightAlign) {
        System.out.println();
        System.out.println(title);  // e.g., "32. Population by language (...)"

        // 1) Compute column widths based on header + data cell lengths.
        int cols = headers.length;
        int[] w = new int[cols];
        for (int c = 0; c < cols; c++) w[c] = headers[c].length();
        for (String[] r : rows)
            for (int c = 0; c < cols; c++)
                w[c] = Math.max(w[c], r[c] == null ? 0 : r[c].length());

        // 2) Prebuild top/middle/bottom rules using the chosen border style.
        String top = line(B.TL, B.TJ, B.TR, w);
        String mid = line(B.LT, B.X , B.RT, w);
        String bot = line(B.BL, B.BJ, B.BR, w);

        // 3) Render the grid.
        System.out.println(top);
        System.out.println(row(headers, w, new boolean[cols])); // header left aligned
        System.out.println(mid);
        for (int r = 0; r < rows.size(); r++) {
            System.out.println(row(rows.get(r), w, rightAlign));
            // Draw a rule after every row (classic grid look).
            System.out.println(r == rows.size() - 1 ? bot : mid);
        }
        if (rows.isEmpty()) System.out.println(bot);
    }

    /** Builds a horizontal rule like ┌────┬────┐ based on column widths. */
    private static String line(String left, String join, String right, int[] w) {
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < w.length; i++) {
            sb.append(B.H.repeat(w[i] + 2)); // +2 for padding spaces around content
            sb.append(i == w.length - 1 ? right : join);
        }
        return sb.toString();
    }

    /** Builds a single data row with left/right alignment per column. */
    private static String row(String[] cells, int[] w, boolean[] rightAlign) {
        StringBuilder sb = new StringBuilder(B.V);
        for (int i = 0; i < w.length; i++) {
            String cell = cells[i] == null ? "" : cells[i];
            int pad = w[i] - cell.length();
            if (rightAlign != null && rightAlign[i]) {
                // Numbers look better right-aligned (units line up)
                sb.append(" ").append(" ".repeat(pad)).append(cell).append(" ");
            } else {
                sb.append(" ").append(cell).append(" ".repeat(pad)).append(" ");
            }
            sb.append(B.V);
        }
        return sb.toString();
    }

    /**
     * Executes a SQL query, auto-detects which columns are numeric,
     * formats numbers (no grouping, up to 2 decimals), collects rows,
     * and then prints them as a formatted table.
     *
     * @param con    live DB connection
     * @param title  printed above the table
     * @param sql    SQL text (uses Java text blocks for readability)
     * @param cols   the list/order of columns to display (by label)
     */
    private static void runQuery(Connection con, String title, String sql, String... cols) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Map column labels -> index for quick access
            ResultSetMetaData md = rs.getMetaData();
            Map<String,Integer> idx = new HashMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) idx.put(md.getColumnLabel(i), i);

            // --- number formats: NO thousand separators, 0-2 decimals for floating types ---
            NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
            intFmt.setGroupingUsed(false);
            NumberFormat decFmt = NumberFormat.getNumberInstance(Locale.US);
            decFmt.setGroupingUsed(false);
            decFmt.setMinimumFractionDigits(0);
            decFmt.setMaximumFractionDigits(2);

            // Detect numeric columns from JDBC types to align/format properly.
            boolean[] right = new boolean[cols.length];
            boolean[] isDecimal = new boolean[cols.length];
            for (int i = 0; i < cols.length; i++) {
                int jdbcType = Types.VARCHAR; // default assume string
                Integer pos = idx.get(cols[i]);
                if (pos != null) jdbcType = md.getColumnType(pos);
                switch (jdbcType) {
                    case Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT -> { right[i]=true; isDecimal[i]=false; }
                    case Types.DECIMAL, Types.NUMERIC, Types.FLOAT, Types.REAL, Types.DOUBLE -> { right[i]=true; isDecimal[i]=true; }
                    default -> { right[i]=false; isDecimal[i]=false; }
                }
            }

            // Read all rows, convert to strings with proper number formatting.
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] r = new String[cols.length];
                for (int c = 0; c < cols.length; c++) {
                    Object val = rs.getObject(cols[c]);
                    if (val == null) {
                        r[c] = "";
                    } else if (right[c] && val instanceof Number n) {
                        r[c] = isDecimal[c] ? decFmt.format(n.doubleValue())
                                : intFmt.format(n.longValue());
                    } else {
                        r[c] = String.valueOf(val);
                    }
                }
                rows.add(r);
            }

            // Finally print the table for this query.
            printTable(title, cols, rows, right);
        }
    }

    // ---------- main ----------

    /**
     * Entry point:
     * - Reads DB connection info from env vars (with safe defaults).
     * - Establishes a JDBC connection (with retry to handle cold starts).
     * - Runs each report in a clear section order.
     */
    public static void main(String[] args) {
        // Tip: override these via environment variables rather than editing code.
        String host = env("DB_HOST", "127.0.0.1");   // in compose network, often "db"
        String port = env("DB_PORT", "3306");
        String db   = env("DB_NAME", "world");
        String user = env("DB_USER", "app");
        String pass = env("DB_PASSWORD", "app123");

        // AllowPublicKeyRetrieval = handy for MySQL 8+ with caching_sha2_password.
        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, db
        );
        System.out.printf("DB -> %s  user=%s%n", url, user);

        try {
            // Explicitly load the MySQL driver (many runtimes do this automatically).
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Retry helps when DB container is still starting up.
            try (Connection con = connectWithRetry(url, user, pass, 12, Duration.ofSeconds(3))) {
                System.out.println(" Connected!");

                // ----------------- section titles + the 21 reports -----------------

                System.out.println("\n======================");
                System.out.println("Country Reports");
                System.out.println("======================");

                // 1) All countries in the world by population (desc)
                runQuery(con, "1. All Countries by Population (World)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        ORDER BY Population DESC
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 2) Filter by Continent
                runQuery(con, "2. Countries by Population (Continent = Asia)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Continent = 'Asia'
                        ORDER BY Population DESC
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 3) Filter by Region
                runQuery(con, "3. Countries by Population (Region = Caribbean)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Region = 'Caribbean'
                        ORDER BY Population DESC
                        """,
                        "Code","Name","Continent","Region","Population","Capital");


                System.out.println("\n======================");
                System.out.println("City Reports");
                System.out.println("======================");

                // 7) All cities (left join so countries with nulls still show)
                runQuery(con, "7. All cities in world",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 8) Cities filtered by continent
                runQuery(con, "8. Cities by continent (Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Continent = 'Africa'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 9) Cities filtered by region
                runQuery(con, "9. Cities by region (Central Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Region = 'Central Africa'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 10) Cities filtered by country
                runQuery(con, "10. Cities by country (Argentina)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Name = 'Argentina'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 11) Cities filtered by district
                runQuery(con, "11. Cities by district (Limburg)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE ci.District = 'Limburg'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");


                System.out.println("\n======================");
                System.out.println("Capital City Reports");
                System.out.println("======================");

                // 17) All capital cities (inner join via country.Capital -> city.ID)
                runQuery(con, "17. All capital cities",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        ORDER BY ci.Population DESC
                        """,
                        "Name","Country","Population");

                // 18) Capital cities filtered by Continent
                runQuery(con, "18. Capitals by continent (Asia)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Continent = 'Asia'
                        ORDER BY ci.Population DESC
                        """,
                        "Name","Country","Population");

                // 19) Capital cities filtered by Region
                runQuery(con, "19. Capitals by region (Eastern Asia)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Region = 'Eastern Asia'
                        ORDER BY ci.Population DESC
                        """,
                        "Name","Country","Population");


                System.out.println("\n======================");
                System.out.println("Population Distribution and Population by Location");
                System.out.println("======================");

                // 23) Population distribution by Continent:
                //     - Total population
                //     - % living in cities (sum of city pops / total)
                //     - % not living in cities (complement)
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
                        "Name","Total Population","Population in Cities (%)","Population not in Cities (%)");

                // 24) Same logic by Region
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
                        "Name","Total Population","Population in Cities (%)","Population not in Cities (%)");

                // 25) Same logic per Country
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
                        "Country","Total Population","Population in Cities (%)","Population not in Cities (%)");

                // 26) One-number report: total world population
                runQuery(con, "26. World population",
                        """
                        SELECT SUM(Population) AS total_world_population
                        FROM country
                        """,
                        "total_world_population");

                // 27) Drill into a single continent (Africa)
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
                        "Name","Total Population","Population in Cities (%)","Population not in Cities (%)");

                // 28) Drill into a single region (Central Africa)
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
                        "Name","Total Population","Population in Cities (%)","Population not in Cities (%)");

                // 29) Drill into a single country (Spain)
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
                        "Name","Total Population","Population in Cities (%)","Population not in Cities (%)");

                // 30) District-level breakdown (example: Limburg)
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
                        "District","Total Population","Population in Cities (%)","Population not in Cities (%)");

                // 31) Single city report (example: London)
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
                        "city_name","Total Population","Population in Cities (%)","Population not in Cities (%)");

                System.out.println("\n======================");
                System.out.println("Language Reports");
                System.out.println("======================");

                // 32) People counts by language and percentage of world population
                //     Note: Uses ROUND with default half-up rounding; adjust as needed.
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
                        "Language","Num_of_people","Percent_of_world");
            }
        } catch (Exception e) {
            // We print a friendly error + full stack trace for debugging.
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
