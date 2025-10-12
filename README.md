# Population Information Reporting System

This repository is part of the **Software Engineering Methods (SET08803)** coursework project.  
It contains all source code, documentation, and configurations related to the **Population Information Reporting System**, a Java-based application that generates reports using the **world database**.

---

##  Purpose

The purpose of this repository is to:
- Demonstrate the team's ability to collaborate using **Scrum** and **DevOps** practices.
- Show understanding of **Java programming**, **database connectivity**, and **report generation**.
- Implement **continuous integration** and **Dockerized deployment**.
---
## Project Overview

The **Population Information Reporting System** allows users to:
- View population data by **country**, **city**, or **region**.
- Generate formatted population reports.
- Retrieve statistics such as total, urban, and rural populations.
- Access data directly from the **world MySQL database**.

---
## Technologies Used

- **Java** (core application)
- **MySQL** (world database)
- **Maven** (build management)
- **JUnit** (testing)
- **GitHub Actions** (CI/CD)
- **Docker** & **docker-compose**
- **Zube.io** for Scrum management
---
## Build and License Status

| Branch | Build Status |  
|---------|---------------|  
| **master** | ![Build Status (master)](https://img.shields.io/github/actions/workflow/status/maythiricho/SET08803-Group5/main.yml?branch=master) |  
| **develop** | ![Build Status (develop)](https://img.shields.io/github/actions/workflow/status/maythiricho/SET08803-Group5/main.yml?branch=develop) |  

**License:**  
[![License](https://img.shields.io/github/license/maythiricho/SET08803-Group5.svg?style=flat-square)](https://github.com/maythiricho/SET08803-Group5/blob/master/LICENSE)

**Latest Release:**  
[![Releases](https://img.shields.io/github/release/maythiricho/SET08803-Group5/all.svg?style=flat-square)](https://github.com/maythiricho/SET08803-Group5/releases)

---
## Scrum & Collaboration 
This project follows Scrum methodology:
- Managed via Zube.io integrated with GitHub.
- Work is divided into Sprints and User Stories.
- Progress tracked through Kanban Boards.
- Code of Conduct, Use Cases, and Backlog documented in `.md` files.

---

## Team Roles

| Role | Member                                                                                                                                                             | Responsibilities |
|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|
| Product Owner | *[May Thiri Cho](https://github.com/maythiricho)*                                                                                                                  | Defines backlog, ensures requirements meet goals |
| Scrum Master | *[Phyo Zaw Aung](https://github.com/phyozawaung005)*                                                                                                               | Facilitates Scrum meetings and team workflow |
| Developers/Testers | *[Kyaw Zayar Min](https://github.com/KyawZayarMin1234)*, *[Phyo Thura Kyaw](https://github.com/Segma-tech)*, *[Htet Arkar Saw Naung](https://github.com/Rayyy990)* | Implement, test, and document features |

---

## Code of Conduct
All team members must follow the Code of Conduct, ensuring professionalism, collaboration, and academic integrity.  
You can view the full [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md).

---

## License 
This project is licensed under the MIT License.  
[View License](./LICENSE)

---

## Version 
Current Release: v1.0.0-alpha  
[View all releases](https://github.com/maythiricho/SET08803-Group5/releases)

---

## Contact 
If you have questions regarding this coursework:

- **Module:** SET08803 - Software Engineering Methods
- **Institution:** Edinburgh Napier University
- **GitHub Repo:** [maythiricho/SET08803-Group5](https://github.com/maythiricho/SET08803-Group5)mvn -q -DskipTests package