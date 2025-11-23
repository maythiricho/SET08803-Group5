package com.napier.group5;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    // ---------- helpers for calling private static methods via reflection ----------

    private String callEnv(String key, String def) throws Exception {
        Method m = App.class.getDeclaredMethod("env", String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, key, def);
    }

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

    // ---------- basic object / helper tests ----------

    @Test
    @DisplayName("App object can be constructed")
    void appCanBeCreated() {
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

    // ---------- env() tests ----------

    @Test
    @DisplayName("env() returns default when variable is missing")
    void envReturnsDefaultWhenMissing() throws Exception {
        String key = "THIS_ENV_SHOULD_NOT_EXIST_XYZ123";
        String value = callEnv(key, "default-value");
        assertEquals("default-value", value);
    }

    // ---------- line() formatting tests ----------

    @Test
    @DisplayName("line() builds correct ASCII border for two columns")
    void lineBuildsAsciiBorderForTwoColumns() throws Exception {
        int[] widths = {3, 4};  // column widths
        String line = callLine("+", "+", "+", widths);

        // In ASCII mode we expect: +-----+------+  (3+2 dashes, then 4+2)
        assertEquals(
                "+-----+------+",
                line,
                "Top border line should match expected ASCII layout"
        );
    }

    // ---------- row() formatting tests ----------

    @Test
    @DisplayName("row() left-aligns text by default")
    void rowBuildsLeftAlignedRow() throws Exception {
        String[] cells = {"A", "BB"};
        int[] widths = {3, 5};
        boolean[] rightAlign = {false, false};

        String row = callRow(cells, widths, rightAlign);

        // Actual output of row(): "| A   | BB    |"
        assertEquals("| A   | BB    |", row);
    }

    @Test
    @DisplayName("row() right-aligns numeric column when requested")
    void rowBuildsRightAlignedRow() throws Exception {
        String[] cells = {"7", "42"};
        int[] widths = {3, 5};
        boolean[] rightAlign = {true, true};

        String row = callRow(cells, widths, rightAlign);

        // width 3 -> "  7"; width 5 -> "   42"
        assertEquals("|   7 |    42 |", row);
    }

    // ---------- connectWithRetry() failure path test ----------

    @Test
    @DisplayName("connectWithRetry() throws after failed attempt with bad URL")
    void connectWithRetryFailsForInvalidUrl() {
        String badUrl = "test://fail";

        // Just assert that some exception is thrown; message may be null.
        assertThrows(Exception.class, () -> {
            callConnectWithRetry(badUrl, "app", "app123", 1, Duration.ofMillis(10));
        });
    }

    // ---------- demo output test (prints sample table) ----------.1.

    @Test
    @DisplayName("Demo: prints sample world report table")
    void demoPrintsSampleTable() {
        assertDoesNotThrow(() -> {
            String title = "Sample World Report (Unit Test)";
            String[] headers = {"Code", "Name", "Population"};

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"AAA", "Testland", "123456"});
            rows.add(new String[]{"BBB", "Examplestan", "987654"});

            boolean[] rightAlign = {false, false, true};

            callPrintTable(title, headers, rows, rightAlign);
        });
    }

}
