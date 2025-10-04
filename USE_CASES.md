# Use Cases for Population Reporting System

---

## Use Case 1: View All Countries by Population
**Actor:** User  
**Description:** User wants to see a list of all countries sorted from largest to smallest population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “All Countries” report.
2. System queries the Country table in the database.
3. System sorts results by population (descending).
4. System displays results.

**Acceptance Criteria:**
- List of countries is complete.
- Countries are correctly sorted by population.

---

## Use Case 2: View Countries by Continent
**Actor:** User  
**Description:** User wants to see countries in a specific continent sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Countries by Continent” report.
2. System prompts user to select a continent.
3. System queries the database for countries in that continent.
4. System sorts results by population (descending).
5. System displays results.

**Acceptance Criteria:**
- Only countries in the selected continent are displayed.
- Countries are sorted correctly.

---

## Use Case 3: View Countries by Region
**Actor:** User  
**Description:** User wants to see countries in a specific region sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Countries by Region” report.
2. System prompts user to select a region.
3. System queries the database for countries in that region.
4. System sorts results by population (descending).
5. System displays results.

**Acceptance Criteria:**
- Only countries in the selected region are displayed.
- Countries are sorted correctly.

---

## Use Case 4: Top N Countries Globally
**Actor:** User  
**Description:** User wants to see the top N most populated countries worldwide.  
**Precondition:** Database connection is established.

**Flow:**
1. User inputs number N.
2. System queries the database.
3. System sorts results by population (descending) and returns top N countries.
4. System displays results.

**Acceptance Criteria:**
- Only top N countries are displayed correctly.

---

## Use Case 5: Top N Countries by Continent
**Actor:** User  
**Description:** User wants to see the top N most populated countries in a specific continent.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects a continent and inputs N.
2. System queries the database for that continent.
3. System sorts results by population and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Only top N countries for the selected continent are displayed correctly.

---

## Use Case 6: Top N Countries by Region
**Actor:** User  
**Description:** User wants to see the top N most populated countries in a specific region.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects a region and inputs N.
2. System queries the database for that region.
3. System sorts results by population and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Only top N countries for the selected region are displayed correctly.

---

## Use Case 7: View All Cities by Population
**Actor:** User  
**Description:** User wants to see all cities in the world sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “All Cities” report.
2. System queries the City table.
3. System sorts results by population (descending).
4. System displays results.

**Acceptance Criteria:**
- All cities are displayed.
- Cities are sorted correctly by population.

---

## Use Case 8: View Cities by Continent
**Actor:** User  
**Description:** User wants to see cities in a specific continent sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Cities by Continent” report.
2. System prompts user for a continent.
3. System queries the City table filtered by continent.
4. System sorts results by population (descending).
5. System displays results.

**Acceptance Criteria:**
- Only cities in the selected continent are displayed.
- Cities are sorted correctly.

---

## Use Case 9: View Cities by Region
**Actor:** User  
**Description:** User wants to see cities in a specific region sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Cities by Region” report.
2. System prompts user for a region.
3. System queries the City table filtered by region.
4. System sorts results by population.
5. System displays results.

**Acceptance Criteria:**
- Only cities in the selected region are displayed.
- Cities are sorted correctly.

---

## Use Case 10: View Cities by Country
**Actor:** User  
**Description:** User wants to see cities in a specific country sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Cities by Country” report.
2. System prompts user for a country.
3. System queries the City table filtered by country.
4. System sorts results by population.
5. System displays results.

**Acceptance Criteria:**
- Only cities in the selected country are displayed.
- Cities are sorted correctly.

---

## Use Case 11: View Cities by District
**Actor:** User  
**Description:** User wants to see cities in a specific district sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Cities by District” report.
2. System prompts user for a district.
3. System queries the City table filtered by district.
4. System sorts results by population.
5. System displays results.

**Acceptance Criteria:**
- Only cities in the selected district are displayed.
- Cities are sorted correctly.

---

## Use Case 12: Top N Cities Globally
**Actor:** User  
**Description:** User wants to see the top N most populated cities in the world.  
**Precondition:** Database connection is established.

**Flow:**
1. User inputs number N.
2. System queries the database.
3. System sorts results by population (descending) and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Only top N cities are displayed correctly.

---

## Use Case 13: Top N Cities by Continent
**Actor:** User  
**Description:** User wants to see the top N cities in a continent.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects continent and inputs N.
2. System queries database filtered by continent.
3. System sorts and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Top N cities in the continent are displayed correctly.

---

## Use Case 14: Top N Cities by Region
**Actor:** User  
**Description:** User wants to see the top N cities in a region.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects region and inputs N.
2. System queries database filtered by region.
3. System sorts and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Top N cities in the region are displayed correctly.

---

## Use Case 15: Top N Cities by Country
**Actor:** User  
**Description:** User wants to see the top N cities in a country.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects country and inputs N.
2. System queries database filtered by country.
3. System sorts and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Top N cities in the country are displayed correctly.

---

## Use Case 16: Top N Cities by District
**Actor:** User  
**Description:** User wants to see the top N cities in a district.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects district and inputs N.
2. System queries database filtered by district.
3. System sorts and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Top N cities in the district are displayed correctly.

---

## Use Case 17: View All Capital Cities
**Actor:** User  
**Description:** User wants to see all capital cities sorted by population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “All Capital Cities” report.
2. System queries CapitalCity table.
3. System sorts results by population (descending).
4. System displays results.

**Acceptance Criteria:**
- All capitals are displayed.
- Capitals are sorted correctly.

---

## Use Case 18: View Capital Cities by Continent
**Actor:** User  
**Description:** User wants to see capital cities in a specific continent.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Capital Cities by Continent” report.
2. System prompts for continent.
3. System queries CapitalCity table filtered by continent.
4. System sorts results.
5. System displays results.

**Acceptance Criteria:**
- Only capitals in the continent are displayed.
- Capitals sorted correctly.

---

## Use Case 19: View Capital Cities by Region
**Actor:** User  
**Description:** User wants to see capital cities in a specific region.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Capital Cities by Region” report.
2. System prompts for region.
3. System queries CapitalCity table filtered by region.
4. System sorts results.
5. System displays results.

**Acceptance Criteria:**
- Only capitals in the region are displayed.
- Capitals sorted correctly.

---

## Use Case 20: Top N Capital Cities Globally
**Actor:** User  
**Description:** User wants to see top N most populated capital cities worldwide.  
**Precondition:** Database connection is established.

**Flow:**
1. User inputs number N.
2. System queries database.
3. System sorts by population and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Top N capitals are displayed correctly.

---

## Use Case 21: Top N Capital Cities by Continent
**Actor:** User  
**Description:** User wants to see top N capitals in a continent.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects continent and inputs N.
2. System queries filtered database.
3. System sorts and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Top N capitals in the continent displayed correctly.

---

## Use Case 22: Top N Capital Cities by Region
**Actor:** User  
**Description:** User wants to see top N capitals in a region.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects region and inputs N.
2. System queries filtered database.
3. System sorts and returns top N.
4. System displays results.

**Acceptance Criteria:**
- Top N capitals in the region displayed correctly.

---

## Use Case 23: Population Report by Continent
**Actor:** User  
**Description:** User wants population distribution for each continent.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Population Report by Continent.”
2. System queries population data for continent.
3. System calculates total, in cities, not in cities, and percentages.
4. System displays results.

**Acceptance Criteria:**
- Correct total population, city population, and non-city population.
- Percentages are accurate.

---

## Use Case 24: Population Report by Region
**Actor:** User  
**Description:** User wants population distribution for each region.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Population Report by Region.”
2. System queries population data for region.
3. System calculates totals and percentages.
4. System displays results.

**Acceptance Criteria:**
- Correct totals and percentages.

---

## Use Case 25: Population Report by Country
**Actor:** User  
**Description:** User wants population distribution for each country.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Population Report by Country.”
2. System queries population data for country.
3. System calculates totals and percentages.
4. System displays results.

**Acceptance Criteria:**
- Correct totals and percentages.

---

## Use Case 26: View World Population
**Actor:** User  
**Description:** User wants to see the total population of the world.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “World Population.”
2. System queries database for total population.
3. System displays result.

**Acceptance Criteria:**
- World population is correct.

---

## Use Case 27: View Population by Continent
**Actor:** User  
**Description:** User wants to see population of a specific continent.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects continent.
2. System queries database for total population.
3. System displays result.

**Acceptance Criteria:**
- Population matches database.

---

## Use Case 28: View Population by Region
**Actor:** User  
**Description:** User wants to see population of a specific region.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects region.
2. System queries database for total population.
3. System displays result.

**Acceptance Criteria:**
- Population matches database.

---

## Use Case 29: View Population by Country
**Actor:** User  
**Description:** User wants to see population of a specific country.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects country.
2. System queries database for total population.
3. System displays result.

**Acceptance Criteria:**
- Population matches database.

---

## Use Case 30: View Population by District
**Actor:** User  
**Description:** User wants to see population of a specific district.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects district.
2. System queries database for population.
3. System displays result.

**Acceptance Criteria:**
- Population matches database.

---

## Use Case 31: View Population by City
**Actor:** User  
**Description:** User wants to see population of a specific city.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects city.
2. System queries database for population.
3. System displays result.

**Acceptance Criteria:**
- Population matches database.

---

## Use Case 32: Language Population Reports
**Actor:** User  
**Description:** User wants the number of people speaking Chinese, English, Hindi, Spanish, Arabic with % of world population.  
**Precondition:** Database connection is established.

**Flow:**
1. User selects “Language Report.”
2. System queries database for language speakers.
3. System calculates percentage of world population.
4. System displays results.

**Acceptance Criteria:**
- Numbers and percentages are correct.
- Languages sorted from largest to smallest speakers.
