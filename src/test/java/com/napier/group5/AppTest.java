package com.napier.group5;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for App.java
 *
 * ⚡ What this class tests:
 *  - Basic object creation of App
 *  - add() method behavior
 *  - env(), line(), row(), printTable(), connectWithRetry() via reflection
 *  - Table formatting correctness (left/right align)
 *  - Error handling paths (connectWithRetry failure)
 *
 * These tests DO NOT require a real database.
 * They only test pure logic and output formatting.
 */
public class AppTest {

    // -------------------------------------------------------------------------
    // Helper methods for calling PRIVATE STATIC methods using reflection
    // -------------------------------------------------------------------------

    /** Call private env(String, String) */
    private String callEnv(String key, String def) throws Exception {
        Method m = App.class.getDeclaredMethod("env", String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, key, def);
    }

    /** Call private line(...) which renders ASCII/Unicode border lines */
    private String callLine(String left, String join, String right, int[] widths) throws Exception {
        Method m = App.class.getDeclaredMethod(
                "line",
                String.class,
                String.class,
                String.class,
                int[].class
        );
        m.setAccessible(true);
        return (String) m.invoke(null, left, join, right, widths);
    }

    /** Call private row(...) which builds one row of a table */
    private String callRow(String[] cells, int[] widths, boolean[] rightAlign) throws Exception {
        Method m = App.class.getDeclaredMethod(
                "row",
                String[].class,
                int[].class,
                boolean[].class
        );
        m.setAccessible(true);
        return (String) m.invoke(null, (Object) cells, (Object) widths, (Object) rightAlign);
    }

    /** Call private connectWithRetry(...) */
    private Connection callConnectWithRetry(String url, String user, String pass,
                                            int attempts, Duration wait) throws Exception {
        Method m = App.class.getDeclaredMethod(
                "connectWithRetry",
                String.class,
                String.class,
                String.class,
                int.class,
                Duration.class
        );
        m.setAccessible(true);
        return (Connection) m.invoke(null, url, user, pass, attempts, wait);
    }

    /** Call private printTable(...) */
    private void callPrintTable(String title,
                                String[] headers,
                                List<String[]> rows,
                                boolean[] rightAlign) throws Exception {
        Method m = App.class.getDeclaredMethod(
                "printTable",
                String.class,
                String[].class,
                List.class,
                boolean[].class
        );
        m.setAccessible(true);
        m.invoke(null, title, headers, rows, rightAlign);
    }

    // -------------------------------------------------------------------------
    // Basic App object + add() method tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("App object can be constructed")
    void appCanBeCreated() {
        // Simply ensures constructor works
        App app = new App();
        assertNotNull(app);
    }

    @Test
    @DisplayName("add() sums two positive numbers")
    void addTwoPositiveNumbers() {
        App app = new App();
        int result = app.add(2, 3);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("add() handles positive and negative")
    void addPositiveAndNegative() {
        App app = new App();
        int result = app.add(10, -4);
        assertEquals(6, result);
    }

    @Test
    @DisplayName("add() with zero returns same number")
    void addWithZero() {
        App app = new App();
        int result = app.add(7, 0);
        assertEquals(7, result);
    }

    // -------------------------------------------------------------------------
    // env() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("env() returns default when variable is missing")
    void envReturnsDefaultWhenMissing() throws Exception {
        // Provide a key that definitely does not exist
        String key = "THIS_ENV_SHOULD_NOT_EXIST_XYZ123";
        String value = callEnv(key, "default-value");

        // Should return the default
        assertEquals("default-value", value);
    }

    // -------------------------------------------------------------------------
    // line() tests — table border creation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("line() builds correct ASCII border for two columns")
    void lineBuildsAsciiBorderForTwoColumns() throws Exception {
        int[] widths = {3, 4};  // widths of columns

        // Call App.line()
        String line = callLine("+", "+", "+", widths);

        // Expected ASCII border:
        // +-----+------+
        // dash count = colWidth + 2 padding spaces
        assertEquals(
                "+-----+------+",
                line,
                "Top border line should match expected ASCII layout"
        );
    }

    // -------------------------------------------------------------------------
    // row() tests — formatting of table rows
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("row() left-aligns text by default")
    void rowBuildsLeftAlignedRow() throws Exception {
        String[] cells = {"A", "BB"}; // test values
        int[] widths = {3, 5};        // column widths
        boolean[] rightAlign = {false, false}; // left-align both

        String row = callRow(cells, widths, rightAlign);

        // Expected row: each cell padded on the right
        assertEquals("| A   | BB    |", row);
    }

    @Test
    @DisplayName("row() right-aligns numeric column when requested")
    void rowBuildsRightAlignedRow() throws Exception {
        // Numeric cells
        String[] cells = {"7", "42"};
        int[] widths = {3, 5};
        boolean[] rightAlign = {true, true};

        // Right alignment -> padded on the left
        String row = callRow(cells, widths, rightAlign);

        assertEquals("|   7 |    42 |", row);
    }

    // -------------------------------------------------------------------------
    // connectWithRetry() failure branch test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("connectWithRetry() throws after failed attempt with bad URL")
    void connectWithRetryFailsForInvalidUrl() {
        // special test URL "test://fail" triggers forced failure
        String badUrl = "test://fail";

        assertThrows(Exception.class, () -> {
            // Only 1 attempt, guaranteed failure
            callConnectWithRetry(badUrl, "app", "app123", 1, Duration.ofMillis(10));
        });
    }

    // -------------------------------------------------------------------------
    // Demonstration table print test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Demo: prints sample world report table")
    void demoPrintsSampleTable() {
        // Does not assert content — just checks printTable does NOT throw errors
        assertDoesNotThrow(() -> {
            String title = "Sample World Report (Unit Test)";
            String[] headers = {"Code", "Name", "Population"};

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"AAA", "Testland", "123456"});
            rows.add(new String[]{"BBB", "Examplestan", "987654"});

            boolean[] rightAlign = {false, false, true}; // last column right-aligned

            callPrintTable(title, headers, rows, rightAlign);
        });
    }
}
