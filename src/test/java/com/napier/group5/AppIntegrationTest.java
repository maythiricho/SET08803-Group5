package com.napier.group5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
}
