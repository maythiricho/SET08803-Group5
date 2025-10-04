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

## Use Case 2: View Countries by Continent or Region
**Actor:** User  
**Description:** User wants to filter countries by continent or region and sort by population.

**Flow:**
1. User selects “Countries by Continent/Region.”
2. System prompts user for continent or region.
3. System queries the database and sorts by population.
4. System displays results.

**Acceptance Criteria:**
- Only countries in the selected continent/region are displayed.
- Countries are sorted correctly.

---

## Use Case 3: Top N Populated Countries
**Actor:** User  
**Description:** User wants to see the top N most populated countries.

**Flow:**
1. User inputs number N.
2. System queries the database.
3. System sorts results by population (descending) and returns top N.

**Acceptance Criteria:**
- Only top N countries are displayed correctly.

---

## Use Case 4: View All Cities / Cities by Continent, Region, Country, District
**Actor:** User  
**Description:** User wants city reports with sorting/filtering.

**Flow:**
1. User selects city report type.
2. System queries the City table with the selected filter.
3. System sorts results by population.
4. System displays results.

**Acceptance Criteria:**
- Correct cities are displayed.
- Cities are sorted correctly.

---

## Use Case 5: Top N Populated Cities
**Actor:** User  
**Description:** User wants top N cities globally, by continent, region, country, or district.

**Flow:**
- Same as Top N Countries but applied to cities.

**Acceptance Criteria:**
- Top N cities displayed correctly per filter.

---

## Use Case 6: Capital Cities Reports
**Actor:** User  
**Description:** User wants all or top N capital cities sorted by population.

**Flow:**
1. User selects report.
2. System queries Capital city table.
3. System sorts results by population.
4. System displays results.

**Acceptance Criteria:**
- Correct capitals are displayed.
- Capitals are sorted correctly.

---

## Use Case 7: Population Reports
**Actor:** User  
**Description:** User wants population distribution for world, continent, region, country, district.

**Flow:**
1. User selects level (world, continent, region, country, district).
2. System queries population, calculates city vs non-city population, percentages.
3. System displays results.

**Acceptance Criteria:**
- Numbers and percentages match database.

---

## Use Case 8: Language Population Reports
**Actor:** User  
**Description:** User wants top languages spoken with population % of world.

**Flow:**
1. User selects language report.
2. System queries database for Chinese, English, Hindi, Spanish, Arabic.
3. System calculates percentage of world population.
4. System displays results.

**Acceptance Criteria:**
- Numbers and percentages are correct.
