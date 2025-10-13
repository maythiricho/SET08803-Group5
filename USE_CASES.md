# Population Reporting System
**Version:** v0.1.0.2  
**Document:** Use Case Specification  
**Author:** Group 5

---

##  Introduction
This document describes the **primary use cases** for the *Population Reporting System (v0.1.0.2)* — a data-driven analytical application that enables users to explore, sort, and compare population information across global, continental, regional, and national levels.

Each use case is presented with structured details such as preconditions, actors, triggers, main success scenarios, and possible extensions.  
This specification serves as a foundation for design, testing, and deployment planning.

---

## Country Reports

### CHARACTERISTIC INFORMATION
**Goal in Context:**  
Allow users (data analysts and researchers) to view, sort, and compare country population data at global, continental, and regional levels.

**Scope:**  
Population Reporting System (Backend and Reporting Module)

**Level:**  
Primary Task

**Preconditions:**
- Population data is available in the database
- User is authenticated (if required)

**Success End Condition:**  
User can view accurate, sorted, and filtered country population reports.

**Failed End Condition:**  
System fails to retrieve or sort population data; user receives an error message.

**Primary Actor:**  
Data Analyst / Researcher

**Trigger:**  
User selects a population report type or sorting/filtering option in the system UI.

---

### MAIN SUCCESS SCENARIO
1. User opens the reporting interface.
2. User selects **“Country Reports.”**
3. System displays available report filters (World / Continent / Region / Top N).
4. User chooses criteria and provides N if applicable.
5. System retrieves relevant population data.
6. System sorts and presents the results in descending order by population.
7. User views or exports the report.

---

### EXTENSIONS
- **3a:** No data found → Display “No data available for selected criteria.”
- **5a:** Database connection error → Display “Unable to retrieve data. Please try again.”

---

### SUB-VARIATIONS
- View all countries (World, Continent, Region)
- View Top N most populated countries (Global or by area)

---

### SCHEDULE
**Due Date:** Version v0.1.0.2  

---

##  City Reports

### CHARACTERISTIC INFORMATION
**Goal in Context:**  
Provide detailed city-level population reports across multiple geographic divisions.

**Scope:**  
Population Reporting System

**Level:**  
Primary Task

**Preconditions:**  
City data exists and is linked to corresponding countries, regions, and districts.

**Success End Condition:**  
User can view and compare cities by population across different levels.

**Failed End Condition:**  
Incomplete or missing data prevents proper reporting.

**Primary Actor:**  
Data Analyst / Researcher

**Trigger:**  
User selects “City Reports” in the interface.

---

### MAIN SUCCESS SCENARIO
1. User navigates to **“City Reports.”**
2. User selects a filter (World, Continent, Region, Country, District, or Top N).
3. System retrieves city population data.
4. System sorts results by population.
5. System displays report.
6. User may export or save the results.

---

### EXTENSIONS
- **2a:** Invalid N value → System prompts valid input.
- **3a:** Missing region/country data → Display “Incomplete geographic data.”

---

### SUB-VARIATIONS
- Reports for World, Continent, Region, Country, District
- Top N most populated cities at each level

---

### SCHEDULE
**Due Date:** Version v0.1.0.2

---

##   Capital City Reports

### CHARACTERISTIC INFORMATION
**Goal in Context:**  
Enable analysis of capital city populations globally, by continent, or by region.

**Scope:**  
Population Reporting System

**Level:**  
Primary Task

**Preconditions:**  
Capital city data is linked to countries and regions, and population data is current.

**Success End Condition:**  
Capital cities are displayed and sorted correctly by population per selected criteria.

**Failed End Condition:**  
System returns empty or inaccurate data.

**Primary Actor:**  
Data Analyst / Researcher

**Trigger:**  
User selects “Capital City Reports.”

---

### MAIN SUCCESS SCENARIO
1. User accesses **Capital City Reports.**
2. Selects filter (World, Continent, Region, or Top N).
3. System fetches capital cities and their population data.
4. Data is sorted in descending order by population.
5. Report is displayed.

---

### EXTENSIONS
- **2a:** Invalid region input → System prompts for correction.
- **3a:** Data inconsistency → Log error and alert administrator.

---

### SUB-VARIATIONS
- View all capital cities (World, Continent, Region)
- View Top N capital cities (Global or by area)

---

### SCHEDULE
**Due Date:** Version v0.1.0.2

---

##  Population Reports (Urbanisation)

### CHARACTERISTIC INFORMATION
**Goal in Context:**  
Generate reports showing total, city, and non-city populations for continents, regions, and countries.

**Scope:**  
Population Reporting and Analysis System

**Level:**  
Primary Task

**Preconditions:**  
Population and city data exist for all geographic entities.

**Success End Condition:**  
User receives urbanisation statistics (% in cities vs. non-cities).

**Failed End Condition:**  
System fails to compute or retrieve required data.

**Primary Actor:**  
Data Analyst / Researcher

**Trigger:**  
User selects “Population Reports.”

---

### MAIN SUCCESS SCENARIO
1. User opens **Population Reports.**
2. Selects level (Continent, Region, or Country).
3. System retrieves total, city, and non-city populations.
4. System calculates urbanisation percentages.
5. Report is displayed.

---

### EXTENSIONS
- **4a:** Missing data → Display “Incomplete data for selected entity.”

---

### SUB-VARIATIONS
- Continent-level reports
- Region-level reports
- Country-level reports

---

### SCHEDULE
**Due Date:** Version v0.1.0.2

---

##   World and Area Populations

### CHARACTERISTIC INFORMATION
**Goal in Context:**  
Provide total population reports at different geographic levels — World, Continent, Region, Country, District, and City.

**Scope:**  
Population Reporting System

**Level:**  
Primary Task

**Preconditions:**  
Geographic hierarchy and population data are validated and complete.

**Success End Condition:**  
Accurate population totals are displayed for each selected level.

**Failed End Condition:**  
Data retrieval or aggregation error occurs.

**Primary Actor:**  
Data Analyst / Researcher

**Trigger:**  
User selects “Population by Area.”

---

### MAIN SUCCESS SCENARIO
1. User chooses desired area level.
2. System queries and aggregates corresponding population data.
3. System returns totals.
4. Report is displayed or exported.

---

### EXTENSIONS
- **2a:** Database connection issue → System retries and notifies user.

---

### SUB-VARIATIONS
- Reports by World, Continent, Region, Country, District, City.

---

### SCHEDULE
**Due Date:** Version v0.1.0.2

---

##  Language Reports

### CHARACTERISTIC INFORMATION
**Goal in Context:**  
Show the number and percentage of people speaking major world languages (e.g., Chinese, English, Hindi, Spanish, Arabic).

**Scope:**  
Population Reporting System

**Level:**  
Primary Task

**Preconditions:**  
Language and population data are available and mapped to global totals.

**Success End Condition:**  
System displays population counts and percentage shares for each language.

**Failed End Condition:**  
Missing data or failed calculations.

**Primary Actor:**  
Data Analyst

**Trigger:**  
User selects “Language Reports.”

---

### MAIN SUCCESS SCENARIO
1. User opens **Language Reports.**
2. System retrieves data for selected major languages.
3. System computes total speakers and global percentages.
4. Data is displayed as a table or chart.

---

### EXTENSIONS
- **3a:** Missing world population total → Display “Percentage cannot be calculated.”

---

### SUB-VARIATIONS
- Include all major languages or top 5 globally.

---

### SCHEDULE
**Due Date:** Version v0.1.0.2

---

##  Document Summary
This document defines all major **reporting use cases** for *Population Reporting System v0.1.0.2*.  
It supports development, testing, and stakeholder review by clearly identifying user goals, expected outcomes, and exception handling for each module.

---
