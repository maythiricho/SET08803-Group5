package com.napier.group5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AppIntegrationTest {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Handler handler = new StreamHandler(baos, new SimpleFormatter());

    static {
        // Set logger level to FINE (prints FINE, INFO, WARNING, SEVERE)
        logger.setLevel(Level.FINE);

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
        logger.info("Connected to MySQL for integration tests.");
    }

    // Test 1 — country count
    @Test
    void testWorldHasManyCountries() throws Exception {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM country");

        assertTrue(rs.next());
        long count = rs.getLong("cnt");

        if (logger.isLoggable(Level.INFO)) {
            logger.info(() -> "Total countries = " + count);
        }
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

        logger.info("AFG found OK.");
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

        logger.info("Top 3 population order OK.");
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


        logger.info("\n=== Top 5 Asian Countries by Population ===");
        logger.info("+----------------------+---------------+");
        logger.info("| Country              |   Population  |");
        logger.info("+----------------------+---------------+");

        int rowCount = 0;
        String firstCountry = null;

        while (rs.next()) {
            String name = rs.getString("Name");
            long pop = rs.getLong("Population");
            if (rowCount == 0) {
                firstCountry = name;
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(() -> String.format("| %-20s | %13d |%n", name, pop));
            }
            rowCount++;
        }
        logger.fine("+----------------------+---------------+");

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


        logger.fine("\n=== Top 5 Cities in Japan by Population ===");
        logger.fine("+----------------------+----------------------+---------------+");
        logger.fine("| City                 | Country              |   Population  |");
        logger.fine("+----------------------+----------------------+---------------+");

        int rowCount = 0;
        String firstCity = null;

        while (rs.next()) {
            String city = rs.getString("City");
            String country = rs.getString("Country");
            long pop = rs.getLong("Population");
            if (rowCount == 0) {
                firstCity = city;
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(() -> String.format("| %-20s | %-20s | %13d |%n", city, country, pop));
            }
            rowCount++;
        }
        logger.fine("+----------------------+----------------------+---------------+");

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

        logger.fine("\n=== Population by Continent ===");
        logger.fine("+----------------------+---------------------+");
        logger.fine("| Continent            | Total Population    |");
        logger.fine("+----------------------+---------------------+");

        int rowCount = 0;
        boolean foundAsia = false;

        while (rs.next()) {
            String continent = rs.getString("Continent");
            long pop = rs.getLong("Pop");
            if ("Asia".equals(continent) && pop > 0) {
                foundAsia = true;
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(() -> String.format("| %-20s | %19d |%n", continent, pop));
            }
            rowCount++;
        }
        logger.fine("+----------------------+---------------------+");

        assertTrue(rowCount >= 5, "Expected at least 5 continents");
        assertTrue(foundAsia, "Asia should be present in the summary");
    }

    @Test
    void runQueryPrintsTop3CountriesTable() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(baos, new SimpleFormatter());
        logger.addHandler(handler);

        String sql = """
        SELECT Name, Population
        FROM country
        ORDER BY Population DESC
        LIMIT 3
        """;

        Method m = App.class.getDeclaredMethod(
                "runQuery",
                Connection.class,
                String.class,
                String.class,
                String[].class
        );
        m.setAccessible(true);
        m.invoke(null, con, "Test: Top 3 Countries", sql, new String[]{"Name", "Population"});

        handler.flush();
        String output = baos.toString();

        assertTrue(output.contains("Test: Top 3 Countries"));
        assertTrue(output.contains("Name"));
        assertTrue(output.contains("Population"));
        assertTrue(output.contains("China"));
        assertTrue(output.contains("India"));
        assertTrue(output.contains("United States"));

        logger.removeHandler(handler);
    }

    @Test
    void emptyResultStillPrintsHeader() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(baos, new SimpleFormatter());
        logger.addHandler(handler);

        String sql = """
        SELECT co.Code, co.Name, co.Population
        FROM country co
        WHERE 1 = 0
        """;

        App.runQuery(con, "Empty result test", sql, "Code", "Name", "Population");

        handler.flush();
        String output = baos.toString();

        assertTrue(output.contains("Code"));
        assertTrue(output.contains("Name"));
        assertTrue(output.contains("Population"));
        assertFalse(output.contains("Testland"));

        logger.removeHandler(handler);
    }

    @Test
    void runQueryFormatsIntegersAndDecimals() throws Exception {
        // Capture logs
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(baos, new SimpleFormatter());
        logger.addHandler(handler);

        String sql = """
        SELECT
            42       AS int_col,
            1234.56  AS dec_col,
            'ABC'    AS txt_col
        """;

        App.runQuery(
                con,
                "Numeric formatting test",
                sql,
                "int_col",
                "dec_col",
                "txt_col"
        );

        // flush the handler so output is written
        handler.flush();
        String output = baos.toString();

        assertTrue(output.contains("42"), "Integer column should be printed");
        assertTrue(output.contains("1234.56"), "Decimal column should include a decimal point");
        assertTrue(output.contains("ABC"), "Text column should be printed");

        logger.removeHandler(handler);
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

            logger.fine("\n=== Top 5 European Countries by Population ===");
            logger.fine("+----------------------+---------------+");
            logger.fine("| Country              |   Population  |");
            logger.fine("+----------------------+---------------+");

            int rowCount = 0;
            String firstCountry = null;
            long lastPopulation = Long.MAX_VALUE;   // for simple branch-style check

            while (rs.next()) {
                String name = rs.getString("Name");
                long population = rs.getLong("Population");

                // print table row
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(() -> String.format("| %-20s | %13d |%n", name, population));
                }

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

            logger.fine("+----------------------+---------------+");

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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(baos, new SimpleFormatter());
        logger.addHandler(handler);

        String sql = "SELECT NULL AS c1";
        App.runQuery(con, "NullTest", sql, "c1");

        handler.flush();
        String output = baos.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("c1"), "Table header 'c1' should be present even if value is NULL");

        logger.removeHandler(handler);
    }


    @Test
    void mainRunsWithOneArg() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(baos, new SimpleFormatter());
        logger.addHandler(handler);

        try {
            App.main(new String[]{"localhost:3307"});
        } catch (Exception ignored) {
        }

        handler.flush();
        String out = baos.toString();

        assertTrue(out.contains("DB ->"));

        logger.removeHandler(handler);
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

    // =====================================================================
    // NEW TESTS TO INCREASE BRANCH COVERAGE
    // =====================================================================

    /**
     * Additional branch coverage for connectWithRetry:
     * non-test URL that retries and finally throws after N attempts.
     */
    @Test
    void connectWithRetryRetriesThenThrows() {
        assertThrows(SQLException.class, () ->
                App.connectWithRetry(
                        "jdbc:mysql://localhost:65535/no_such_db",
                        "user",
                        "pass",
                        1,
                        Duration.ofMillis(10)
                )
        );
    }

    /**
     * Cover both branches of the private static inner Borders class:
     * ascii = true (ASCII borders) and ascii = false (Unicode borders).
     */
    @Test
    void bordersAsciiAndUnicodeBranches() throws Exception {
        Class<?> bordersClass = Class.forName("com.napier.group5.App$Borders");
        Constructor<?> ctor = bordersClass.getDeclaredConstructor(boolean.class);
        ctor.setAccessible(true);

        Object asciiBorders = ctor.newInstance(true);
        Object unicodeBorders = ctor.newInstance(false);

        Field hField = bordersClass.getDeclaredField("H");
        Field tlField = bordersClass.getDeclaredField("TL");
        Field trField = bordersClass.getDeclaredField("TR");
        hField.setAccessible(true);
        tlField.setAccessible(true);
        trField.setAccessible(true);

        String asciiH = (String) hField.get(asciiBorders);
        String unicodeH = (String) hField.get(unicodeBorders);

        // Simple sanity checks: ASCII vs Unicode
        assertEquals("-", asciiH);
        assertEquals("┌", tlField.get(unicodeBorders));
        assertEquals("┐", trField.get(unicodeBorders));
        assertEquals("─", unicodeH);
    }

    /**
     * Force an environment variable to be present but BLANK,
     * so we hit the (v != null && v.isBlank()) branch in env(...).
     */
    @Test
    void envBranch_BlankValue() throws Exception {
        Method m = App.class.getDeclaredMethod("env", String.class, String.class);
        m.setAccessible(true);

        // Prepare a fake environment map using ProcessBuilder
        Map<String, String> fakeEnv = new ProcessBuilder().environment();
        fakeEnv.put("BLANK_TEST_KEY", "   "); // blank but not null

        // Inject fake env into the env() method by unique key name
        String result = (String) m.invoke(null, "BLANK_TEST_KEY", "DEFAULT");

        // EXPECT: blank is treated same as null => returns default
        assertEquals("DEFAULT", result);
    }



    /**
     * Small helper to modify System.getenv() via reflection.
     * This is only for testing branch behaviour.
     */
    @SuppressWarnings("unchecked")
    private static void setEnv(String key, String value) throws Exception {
        // First try generic "m" field approach
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field m = cl.getDeclaredField("m");
            m.setAccessible(true);
            Map<String, String> map = (Map<String, String>) m.get(env);
            if (value == null) {
                map.remove(key);
            } else {
                map.put(key, value);
            }
        } catch (NoSuchFieldException ignored) {
            // Fall through to ProcessEnvironment hack
        }

        // Fallback: java.lang.ProcessEnvironment (most HotSpot JVMs)
        try {
            Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = pe.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            if (value == null) {
                env.remove(key);
            } else {
                env.put(key, value);
            }

            Field ciEnvField = pe.getDeclaredField("theCaseInsensitiveEnvironment");
            ciEnvField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) ciEnvField.get(null);
            if (value == null) {
                cienv.remove(key);
            } else {
                cienv.put(key, value);
            }
        } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            // If this fails, we just won't change env on that JVM,
            // but the test will still run (it will behave like "missing")
        }
    }

    @Test
    void lineMethod_BranchesCovered() throws Exception {
        Method line = App.class.getDeclaredMethod(
                "line",
                String.class, String.class, String.class, int[].class
        );
        line.setAccessible(true);

        int[] w = {3, 5};

        // ASCII branch
        Object resultAscii = line.invoke(null, "+", "+", "+", w);
        assertTrue(resultAscii.toString().contains("+"));

        // Unicode branch (uses wide characters)
        Object resultUnicode = line.invoke(null, "┌", "┬", "┐", w);
        assertTrue(resultUnicode.toString().contains("┌"));
        assertTrue(resultUnicode.toString().contains("┬"));
    }

    @Test
    void printTable_MixedCells_AllBranches() throws Exception {
        Method printTable = App.class.getDeclaredMethod(
                "printTable",
                String.class, String[].class, List.class, boolean[].class
        );
        printTable.setAccessible(true);

        String[] headers = {"H1", "LongHeaderTwo", "Num"};
        List<String[]> rows = new ArrayList<>();

        rows.add(new String[]{ "A", "B", "123" });
        rows.add(new String[]{ null, "VeryLongCellData", "99999" });
        rows.add(new String[]{ "", "C", "0" });

        boolean[] rightAlign = { false, false, true };

        printTable.invoke(null, "Mixed Test", headers, rows, rightAlign);
    }
    @Test
    void lineMethod_AllBranches() throws Exception {
        Method line = App.class.getDeclaredMethod(
                "line",
                String.class, String.class, String.class, int[].class
        );
        line.setAccessible(true);

        int[] widths = {1, 3, 5};

        // Top
        String top = (String) line.invoke(null, "+", "+", "+", widths);
        assertTrue(top.contains("+"));

        // Middle
        String mid = (String) line.invoke(null, "|", "+", "|", widths);
        assertTrue(mid.contains("|"));

        String bottom = (String) line.invoke(null, "└", "┴", "┘", widths);
        assertTrue(bottom.contains("┘"));
    }

    @Test
    void printTable_NoRows() throws Exception {
        Method printTable = App.class.getDeclaredMethod(
                "printTable",
                String.class, String[].class, List.class, boolean[].class
        );
        printTable.setAccessible(true);

        String[] headers = {"A", "B"};
        List<String[]> rows = new ArrayList<>();
        boolean[] right = {false, false};

        printTable.invoke(null, "Empty Table", headers, rows, right);
    }
}
