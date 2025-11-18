package com.napier.group5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AppIntegrationTest {

    private Connection con;

    @BeforeAll
    void init() throws Exception {
        // Load MySQL driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Local port in docker-compose: 3307
        con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3307/world?useSSL=false&allowPublicKeyRetrieval=true",
                "app",
                "app123"
        );

        assertNotNull(con);
        System.out.println("Connected to MySQL for integration tests.");
    }

    // Test 1 — country count
    @Test
    void testWorldHasManyCountries() throws Exception {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM country");

        assertTrue(rs.next());
        long count = rs.getLong("cnt");

        System.out.println("Total countries = " + count);
        assertTrue(count > 150);
    }

    // Test 2 — check specific country
    @Test
    void testCountryAfghanistanExists() throws Exception {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT Code, Name FROM country WHERE Code = 'AFG'"
        );

        assertTrue(rs.next());

        assertEquals("AFG", rs.getString("Code"));
        assertEquals("Afghanistan", rs.getString("Name"));

        System.out.println("AFG found OK.");
    }

    // Test 3 — top 3 by population
    @Test
    void testTop3CountriesByPopulation() throws Exception {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT Code FROM country ORDER BY Population DESC LIMIT 3"
        );

        rs.next();
        assertEquals("CHN", rs.getString("Code"));

        rs.next();
        assertEquals("IND", rs.getString("Code"));

        rs.next();
        assertEquals("USA", rs.getString("Code"));

        System.out.println("Top 3 population order OK.");
    }

    @Test
    void tableTop5AsianCountriesByPopulation() throws Exception {
        String sql =
                "SELECT Name, Population " +
                        "FROM country " +
                        "WHERE Continent = 'Asia' " +
                        "ORDER BY Population DESC " +
                        "LIMIT 5";

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        System.out.println();
        System.out.println("=== Top 5 Asian Countries by Population ===");
        System.out.println("+----------------------+---------------+");
        System.out.println("| Country              |   Population  |");
        System.out.println("+----------------------+---------------+");

        int rowCount = 0;
        String firstCountry = null;

        while (rs.next()) {
            String name = rs.getString("Name");
            long pop = rs.getLong("Population");
            if (rowCount == 0) {
                firstCountry = name;
            }
            System.out.printf("| %-20s | %13d |%n", name, pop);
            rowCount++;
        }
        System.out.println("+----------------------+---------------+");

        assertEquals(5, rowCount, "Expected 5 Asian countries in table");
        assertEquals("China", firstCountry, "China should be the most populated Asian country");
    }

    /**
     * Table 2: Top 5 cities in Japan by population (Name, Country, Population).
     */
    @Test
    void tableTop5CitiesInJapan() throws Exception {
        String sql =
                "SELECT ci.Name AS City, co.Name AS Country, ci.Population " +
                        "FROM city ci " +
                        "JOIN country co ON ci.CountryCode = co.Code " +
                        "WHERE co.Code = 'JPN' " +
                        "ORDER BY ci.Population DESC " +
                        "LIMIT 5";

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        System.out.println();
        System.out.println("=== Top 5 Cities in Japan by Population ===");
        System.out.println("+----------------------+----------------------+---------------+");
        System.out.println("| City                 | Country              |   Population  |");
        System.out.println("+----------------------+----------------------+---------------+");

        int rowCount = 0;
        String firstCity = null;

        while (rs.next()) {
            String city = rs.getString("City");
            String country = rs.getString("Country");
            long pop = rs.getLong("Population");
            if (rowCount == 0) {
                firstCity = city;
            }
            System.out.printf("| %-20s | %-20s | %13d |%n", city, country, pop);
            rowCount++;
        }
        System.out.println("+----------------------+----------------------+---------------+");

        assertEquals(5, rowCount, "Expected 5 Japanese cities in table");
        assertEquals("Tokyo", firstCity, "Tokyo should be the largest city in Japan");
    }

    /**
     * Table 3: Population summary per continent.
     */
    @Test
    void tableContinentPopulationSummary() throws Exception {
        String sql =
                "SELECT Continent, SUM(Population) AS Pop " +
                        "FROM country " +
                        "GROUP BY Continent " +
                        "ORDER BY Pop DESC";

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        System.out.println();
        System.out.println("=== Population by Continent ===");
        System.out.println("+----------------------+---------------------+");
        System.out.println("| Continent            | Total Population    |");
        System.out.println("+----------------------+---------------------+");

        int rowCount = 0;
        boolean foundAsia = false;

        while (rs.next()) {
            String continent = rs.getString("Continent");
            long pop = rs.getLong("Pop");
            if ("Asia".equals(continent) && pop > 0) {
                foundAsia = true;
            }
            System.out.printf("| %-20s | %19d |%n", continent, pop);
            rowCount++;
        }
        System.out.println("+----------------------+---------------------+");

        assertTrue(rowCount >= 5, "Expected at least 5 continents");
        assertTrue(foundAsia, "Asia should be present in the summary");
    }

    @Test
    void runQueryPrintsTop3CountriesTable() throws Exception {
        // SQL that matches your schema/world DB
        String sql = """
            SELECT Name, Population
            FROM country
            ORDER BY Population DESC
            LIMIT 3
            """;

        // Capture System.out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

        try {
            // Use reflection to call private static runQuery(Connection, String, String, String...)
            Method m = App.class.getDeclaredMethod(
                    "runQuery",
                    Connection.class,
                    String.class,
                    String.class,
                    String[].class
            );
            m.setAccessible(true);

            m.invoke(
                    null,                 // static method -> no instance
                    con,                  // existing DB connection from @BeforeAll
                    "Test: Top 3 Countries",  // title
                    sql,
                    new String[]{"Name", "Population"}  // columns
            );
        } finally {
            // Always restore System.out
            System.setOut(originalOut);
        }

        // Get printed output
        String out = baos.toString(StandardCharsets.UTF_8);

        // Basic checks that the table + data are there
        assertTrue(out.contains("Test: Top 3 Countries"));
        assertTrue(out.contains("Name"));
        assertTrue(out.contains("Population"));

        // For the standard MySQL world database, the top 3 by population are:
        // China, India, United States
        assertTrue(out.contains("China"));
        assertTrue(out.contains("India"));
        assertTrue(out.contains("United States"));
    }

    @Test
    void runAllReportsProducesOutput() throws Exception {
        // Capture System.out so we can assert on the text
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

        try {
            // host:port must match your docker-compose mapping (here: localhost:3307)
            App.main(new String[]{"localhost:3307", "60000"});
        } finally {
            System.setOut(oldOut);
        }

        String output = baos.toString(StandardCharsets.UTF_8);

        // Basic checks that the big report actually ran
        assertTrue(output.contains("Country Reports"), "Should print Country Reports header");
        assertTrue(output.contains("City Reports"), "Should print City Reports header");
        assertTrue(output.contains("Capital City Reports"), "Should print Capital City Reports header");
        assertTrue(output.contains("Language Reports"), "Should print Language Reports header");
        assertTrue(output.contains("32. Population by language"), "Last report should be present");
    }

    @Test
    void emptyResultStillPrintsHeader() throws Exception {
        // given: a query that returns 0 rows
        String sql = """
            SELECT co.Code, co.Name, co.Population
            FROM country co
            WHERE 1 = 0
            """;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (PrintStream capture = new PrintStream(baos)) {
            System.setOut(capture);

            // when: we run the query through the app table renderer
            App.runQuery(
                    con,
                    "Empty result test",
                    sql,
                    "Code",
                    "Name",
                    "Population"
            );
        } finally {
            System.setOut(originalOut);
        }

        String output = baos.toString();

        // then: we still see a table header, but no data rows
        assertTrue(output.contains("Code"));
        assertTrue(output.contains("Name"));
        assertTrue(output.contains("Population"));
        // our renderer prints only header + bottom line when no rows
        assertFalse(output.contains("Testland")); // or any real country
    }

    @Test
    void runQueryFormatsIntegersAndDecimals() throws Exception {
        String sql = """
            SELECT
                42       AS int_col,
                1234.56  AS dec_col,
                'ABC'    AS txt_col
            """;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (PrintStream capture = new PrintStream(baos)) {
            System.setOut(capture);

            App.runQuery(
                    con,
                    "Numeric formatting test",
                    sql,
                    "int_col",
                    "dec_col",
                    "txt_col"
            );
        } finally {
            System.setOut(originalOut);
        }

        String output = baos.toString();

        // int column should be printed without decimal point
        assertTrue(output.contains("42"));

        // decimal column should include a decimal point
        assertTrue(output.contains("1234") && output.contains("."));
    }

    @Test
    void tableTop5EuropeanCountriesByPopulation() throws Exception {
        // SQL for Top 5 European countries, highest population first
        String sql =
                "SELECT Name, Population " +
                        "FROM country " +
                        "WHERE Continent = 'Europe' " +
                        "ORDER BY Population DESC " +
                        "LIMIT 5";

        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println();
            System.out.println("=== Top 5 European Countries by Population ===");
            System.out.println("+----------------------+---------------+");
            System.out.println("| Country              |   Population  |");
            System.out.println("+----------------------+---------------+");

            int rowCount = 0;
            String firstCountry = null;
            long lastPopulation = Long.MAX_VALUE;   // for simple branch-style check

            while (rs.next()) {
                String name = rs.getString("Name");
                long population = rs.getLong("Population");

                // print table row
                System.out.printf("| %-20s | %13d |%n", name, population);

                // remember first row
                if (rowCount == 0) {
                    firstCountry = name;
                }

                // branch-style check: each next row must be <= previous (DESC order)
                if (population > lastPopulation) {
                    throw new AssertionError("Rows not in descending population order");
                }
                lastPopulation = population;

                rowCount++;
            }

            System.out.println("+----------------------+---------------+");

            // Basic assertions
            assertEquals(5, rowCount, "Expected 5 European countries in the result");

            // In the standard MySQL world database, this should be Russian Federation
            assertEquals("Russian Federation", firstCountry,
                    "Top European country by population should be Russian Federation");
        }
    }

    @Test
    void connectWithRetryFailsFastForTestUrl() {
        // This exercises the "test://" fast-fail branch inside App.connectWithRetry(...)
        assertThrows(SQLException.class, () ->
                App.connectWithRetry(
                        "test://fail-immediately",
                        "user",
                        "pass",
                        1,
                        java.time.Duration.ofMillis(1)
                )
        );
    }

    // ========= EXTRA TESTS JUST FOR BRANCH COVERAGE =========

    /**
     * Cover the branch in env(...) where the env var does NOT exist
     * and the default value is returned.
     */
    @Test
    void envReturnsDefaultWhenMissing() throws Exception {
        Method m = App.class.getDeclaredMethod("env", String.class, String.class);
        m.setAccessible(true);

        String key = "SURELY_MISSING_" + System.nanoTime();
        String result = (String) m.invoke(null, key, "fallback-value");

        assertEquals("fallback-value", result);
    }

    /**
     * Cover the branch in env(...) where the env var DOES exist
     * and the default is NOT used. We use PATH because it should
     * always be set on normal systems.
     */
    @Test
    void envUsesValueWhenPresent() throws Exception {
        Method m = App.class.getDeclaredMethod("env", String.class, String.class);
        m.setAccessible(true);

        String result = (String) m.invoke(null, "PATH", "fallback-value");

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertNotEquals("fallback-value", result);
    }

    @Test
    void runQueryHandlesNullCells() throws Exception {
        String sql = "SELECT NULL AS c1";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));

        try {
            App.runQuery(con, "NullTest", sql, "c1");
        } finally {
            System.setOut(old);
        }

        String out = baos.toString();
        assertTrue(out.contains("c1"));
    }

    @Test
    void mainRunsWithOneArg() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));

        try {
            App.main(new String[]{"localhost:3307"});
        } catch (Exception ignored) {}
        finally {
            System.setOut(old);
        }

        String out = baos.toString();
        assertTrue(out.contains("DB ->"));
    }

    // put near the bottom of AppIntegrationTest, after your other tests

    @Test
    void envFunctionBranches() throws Exception {
        // access private static env(String, String)
        Method m = App.class.getDeclaredMethod("env", String.class, String.class);
        m.setAccessible(true);

        String def = "DEFAULT";

        // 1) key that does NOT exist  -> v == null -> returns default
        String missingKey = "THIS_ENV_VARIABLE_SHOULD_NOT_EXIST_12345";
        String missingValue = (String) m.invoke(null, missingKey, def);
        assertEquals(def, missingValue, "When env var is missing, default should be returned");

        // 2) key that DOES exist and is not blank -> returns real value
        String existingKey = "PATH";  // on Windows / Linux this is almost always defined
        String existingValue = (String) m.invoke(null, existingKey, def);
        assertNotNull(existingValue);
        assertFalse(existingValue.isBlank());
        assertNotEquals(def, existingValue, "When env var exists, real value should be returned");
    }

    // ---------- Extra branch-coverage tests for argument parsing ----------

    @Test
    void resolveHostPort_WithHostAndPort() {
        String[] hp = App.resolveHostPort(new String[]{"db:3307"});
        assertEquals("db", hp[0]);
        assertEquals("3307", hp[1]);
    }

    @Test
    void resolveHostPort_WithHostOnly_UsesDefaultPort() {
        String[] hp = App.resolveHostPort(new String[]{"db"});
        assertEquals("db", hp[0]);
        // when only host is given, port should default to 3306
        assertEquals("3306", hp[1]);
    }

    @Test
    void resolveHostPort_WithNoArgs_UsesEnvDefaults() {
        String[] hp = App.resolveHostPort(new String[]{});   // no args
        // If DB_HOST/DB_PORT are not set, you get the hard-coded defaults:
        assertEquals("127.0.0.1", hp[0]);
        assertEquals("3306", hp[1]);
    }

    @Test
    void resolveTimeoutMs_BothBranchesCovered() {
        // With second arg -> parse that value
        int t1 = App.resolveTimeoutMs(new String[]{"db:3307", "60000"});
        assertEquals(60000, t1);

        // Without second arg -> 30000 default
        int t2 = App.resolveTimeoutMs(new String[]{"db:3307"});
        assertEquals(30000, t2);
    }




}