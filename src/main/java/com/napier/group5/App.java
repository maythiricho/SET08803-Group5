package com.napier.group5;

import java.sql.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;

public class App {
    public int add(int a, int b) {
        return a + b;
    }
    // ---------- env & connection ----------
    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static Connection connectWithRetry(String url, String user, String pass,
                                               int attempts, Duration wait) throws Exception {
        if (url.startsWith("test://fail")) {
            throw new SQLException("Simulated failure for unit test");
        }

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
                System.out.println(" Connected!");

                // ----------------- section titles + the 21 reports -----------------

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
                runQuery(con, "4. Top 10 Countries (World)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 5
                runQuery(con, "5. Top 10 Countries (Continent = Europe)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Continent = 'Europe'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code","Name","Continent","Region","Population","Capital");

                // 6
                runQuery(con, "6. Top 10 Countries (Region = Western Europe)",
                        """
                        SELECT Code, Name, Continent, Region, Population, Capital
                        FROM country
                        WHERE Region = 'Western Europe'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Code","Name","Continent","Region","Population","Capital");


                System.out.println("\n======================");
                System.out.println("City Reports");
                System.out.println("======================");

                // 7
                runQuery(con, "7. All cities in world",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 8
                runQuery(con, "8. Cities by continent (Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Continent = 'Africa'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 9
                runQuery(con, "9. Cities by region (Central Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Region = 'Central Africa'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 10
                runQuery(con, "10. Cities by country (Argentina)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Name = 'Argentina'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 11
                runQuery(con, "11. Cities by district (Limburg)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE ci.District = 'Limburg'
                        ORDER BY Population DESC
                        """,
                        "Name","Country","District","Population");

                // 12
                runQuery(con, "12. Top 10 cities in world",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","District","Population");

                // 13
                runQuery(con, "13. Top 10 cities by continent (Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Continent = 'Africa'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","District","Population");

                // 14
                runQuery(con, "14. Top 10 cities by region (Central Africa)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Region = 'Central Africa'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","District","Population");

                // 15
                runQuery(con, "15. Top 10 cities by country (Argentina)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE c.Name = 'Argentina'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","District","Population");

                // 16
                runQuery(con, "16. Top 10 cities by district (Limburg)",
                        """
                        SELECT ci.Name AS Name, c.Name AS Country, ci.District AS District, ci.Population AS Population
                        FROM city ci
                        LEFT JOIN country c ON ci.CountryCode = c.Code
                        WHERE ci.District = 'Limburg'
                        ORDER BY Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","District","Population");


                System.out.println("\n======================");
                System.out.println("Capital City Reports");
                System.out.println("======================");

                // 17
                runQuery(con, "17. All capital cities",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        ORDER BY ci.Population DESC
                        """,
                        "Name","Country","Population");

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

                // 20
                runQuery(con, "20. Top 10 capitals in world",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        ORDER BY ci.Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","Population");

                // 21
                runQuery(con, "21. Top 10 capitals by continent (Africa)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Continent = 'Africa'
                        ORDER BY ci.Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","Population");

                // 22
                runQuery(con, "22. Top 10 capitals by region (Western Europe)",
                        """
                        SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                        FROM city ci
                        INNER JOIN country co ON ci.ID = co.Capital
                        WHERE co.Region = 'Western Europe'
                        ORDER BY ci.Population DESC
                        LIMIT 10
                        """,
                        "Name","Country","Population");


                System.out.println("\n======================");
                System.out.println("Population Distribution and Population by Location");
                System.out.println("======================");

                // 23
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

                // 24
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

                // 25
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

                // 26
                runQuery(con, "26. World population",
                        """
                        SELECT SUM(Population) AS total_world_population
                        FROM country
                        """,
                        "total_world_population");

                // 27
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

                // 28
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

                // 29
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

                // 30
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

                // 31
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

                // 32
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
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
