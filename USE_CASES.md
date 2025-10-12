# Use Cases for Population Reporting System

This document describes all the use cases for the **Population Reporting System**.  
The system allows users to generate various population-based reports using data from the **World Database**.  
Each use case corresponds to one of the menu categories shown in the **Use Case Diagram** — including *Country Reports*, *City Reports*, *Capital City Reports*, *Population Distribution*, *Population by Location*, and *Language Reports*.

---

##  Country Reports (UC1–UC6)

### Use Case 1: View All Countries by Population
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to see all countries sorted by population (descending).  
**Flow:**
1. User selects “All Countries” report.
2. System queries the Country table.
3. System sorts results by population.
4. System displays results.

**Acceptance Criteria:**
- All countries are listed.
- Countries are sorted correctly by population.

---

### Use Case 2: View Countries by Continent
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to view countries in a selected continent sorted by population.  
**Flow:**
1. User selects a continent.
2. System queries countries in that continent.
3. System sorts and displays results.

**Acceptance Criteria:**
- Only countries in the selected continent are shown.
- Sorting is correct.

---

### Use Case 3: View Countries by Region
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to view countries in a selected region sorted by population.  
**Flow:**
1. User selects a region.
2. System queries countries in that region.
3. System sorts and displays results.

**Acceptance Criteria:**
- Only countries in the selected region are shown.
- Sorting is correct.

---

### Use Case 4: Top N Countries Globally
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to see the top N most populated countries in the world.  
**Flow:**
1. User inputs number N.
2. System queries and sorts data.
3. System displays the top N countries.

**Acceptance Criteria:**
- Only N results are shown.
- Order is correct.

---

### Use Case 5: Top N Countries by Continent
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to see the top N most populated countries within a continent.  
**Flow:**
1. User selects a continent and inputs N.
2. System queries filtered data.
3. System displays top N results.

**Acceptance Criteria:**
- Only top N countries from the selected continent are shown.

---

### Use Case 6: Top N Countries by Region
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to see top N most populated countries in a region.  
**Flow:**
1. User selects region and inputs N.
2. System queries data.
3. System displays sorted top N results.

**Acceptance Criteria:**
- Only top N countries in the region are shown.

---

##  City Reports (UC7–UC16)

### Use Case 7: View All Cities by Population
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to see all cities sorted by population.  
**Flow:**
1. User selects “All Cities” report.
2. System queries City table.
3. System sorts and displays results.

**Acceptance Criteria:**
- All cities are listed and sorted correctly.

---

### Use Case 8–11: Cities by Continent / Region / Country / District
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to view cities filtered by continent, region, country, or district.  
**Flow:**
1. User selects location type (continent, region, etc.).
2. System queries filtered data.
3. System sorts results.
4. System displays results.

**Acceptance Criteria:**
- Only cities from the selected area are displayed.
- Sorting is correct.

---

### Use Case 12–16: Top N Cities (Global / Continent / Region / Country / District)
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to view the top N most populated cities globally or within a chosen location.  
**Flow:**
1. User inputs N and selects filter (optional).
2. System queries database.
3. System sorts by population and returns top N.

**Acceptance Criteria:**
- Top N cities are displayed correctly.

---

## Capital City Reports (UC17–UC22)

### Use Case 17–19: View Capital Cities (All / by Continent / by Region)
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to view capital cities globally or filtered by continent or region.  
**Flow:**
1. User selects the report type.
2. System queries and filters capital city data.
3. System sorts results.
4. System displays list.

**Acceptance Criteria:**
- Correct capitals shown.
- Sorted by population (descending).

---

### Use Case 20–22: Top N Capital Cities (Global / Continent / Region)
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to see the top N most populated capital cities globally or within a selected area.  
**Flow:**
1. User inputs N and selects filter.
2. System queries and sorts capital cities.
3. System displays top N.

**Acceptance Criteria:**
- Top N capitals are displayed correctly.

---

##  Population Distribution Reports (UC23–UC25)

### Use Case 23: Population Distribution by Continent
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants population totals and percentages for each continent.  
**Flow:**
1. User selects “Population by Continent.”
2. System queries continent-level data.
3. System calculates totals, in-city, and non-city values.
4. System displays results.

**Acceptance Criteria:**
- Data is accurate and includes correct percentages.

---

### Use Case 24: Population Distribution by Region
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants population distribution by region.  
**Flow:**
1. User selects “Population by Region.”
2. System queries and calculates totals and percentages.
3. System displays data.

**Acceptance Criteria:**
- Values and percentages are correct.

---

### Use Case 25: Population Distribution by Country
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to see total and city vs. non-city population by country.  
**Flow:**
1. User selects “Population by Country.”
2. System queries and calculates.
3. System displays results.

**Acceptance Criteria:**
- Data is complete and correct.

---

## Population by Location (UC26–UC31)

### Use Case 26–31: View Population Total (World / Continent / Region / Country / District / City)
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to view total population for a chosen area (global or specific location).  
**Flow:**
1. User selects report type and location.
2. System queries database for total population.
3. System displays the result.

**Acceptance Criteria:**
- Population totals match database records.

---

## Language Reports (UC32)

### Use Case 32: Language Report (Top 5 Languages)
**Actor:** User  
**Precondition:** Database connection is established.  
**Description:** User wants to view the population of speakers for Chinese, English, Hindi, Spanish, and Arabic, including percentage of the world population.  
**Flow:**
1. User selects “Language Report.”
2. System queries language data.
3. System calculates speaker counts and percentages.
4. System sorts and displays results.

**Acceptance Criteria:**
- Languages are sorted by total speakers.
- Percentages are accurate.

---


