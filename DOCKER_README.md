# DOCKER_README.md

This project runs a **MySQL “world” database** and a **Java (Maven) app** that prints 32 population-related reports directly to the console.

---

##  Project Structure

```
AssessmentforDevOpsgp5/
├── db/
│   ├── Dockerfile               # Builds MySQL image with world.sql
│   └── world-db/world.sql       # Database initialization script
├── src/main/java/com/napier/group5/App.java  # Main Java app (32 SQL reports)
├── Dockerfile.app               # Multi-stage build for Java app
├── docker-compose.yml           # Orchestrates db + app containers
├── pom.xml                      # Maven dependencies and build setup
└── README.md / DOCKER_README.md
```

---

##  Requirements

- Docker & Docker Compose v2+
- Internet access for pulling base images
- Port **3307** must be free on your machine

---

##  Quick Start

Run the following commands from the project root:

```bash
  docker compose down -v          # Clean previous containers/volumes
  docker compose up -d --build    # Build and start containers
```

Check container status:

```bash
  docker ps
```

You should see both containers:
- `devopsdb1` → healthy (MySQL)
- `devopsapp1` → running (Java app)

View app output (all 32 reports):

```bash
   docker logs -f devopsapp1
```

---

##  Configuration Details

### Database (`db`)

| Setting | Value |
|----------|--------|
| Image | `devops-db` (built from `./db/Dockerfile`) |
| Port | Host `3307` → Container `3306` |
| Database | `world` |
| Root user | `root` / `example` |
| App user | `app` / `app123` |

Health check runs:
```bash
   mysqladmin ping -h 127.0.0.1 -uroot -pexample
```

---

### Application (`app`)

| Environment Variable | Default |
|----------------------|----------|
| DB_HOST | db |
| DB_PORT | 3306 |
| DB_NAME | world |
| DB_USER | app |
| DB_PASSWORD | app123 |

**Build process:**
- Stage 1: Maven compiles and packages App.java → fat JAR
- Stage 2: Eclipse Temurin JRE runs the JAR

**Logs show output of 32 queries:**
```
=== 1) All Countries by Population (World) ===
...
=== 32) Population by Language (Chinese, English, Hindi, Spanish, Arabic) ===
```

---

##  Common Commands

### Stop all containers
```bash
   docker compose down
```

### Remove volumes (to re-run world.sql)
```bash
   docker compose down -v
```

### Restart app only
```bash
   docker compose up -d --build app
```

### Connect to database
```bash
   docker exec -it devopsdb1 mysql -uapp -papp123 world
```

---

##  Troubleshooting

###  Port 3307 already in use
Check usage:
```bash
   netstat -ano | findstr :3307
```
Then change port mapping in `docker-compose.yml` if needed.

---

###  Access denied for user
If app user is missing, recreate inside MySQL:
```bash
docker exec -it devopsdb1 mysql -uroot -pexample -e "
CREATE USER IF NOT EXISTS 'app'@'%' IDENTIFIED BY 'app123';
GRANT ALL PRIVILEGES ON world.* TO 'app'@'%';
FLUSH PRIVILEGES;"
```

---

###  Only 1 result shown in logs
Update `App.java` to include all 32 queries and rebuild:
```bash
   docker compose build app
   docker compose up -d app
```

---

##  Local Run (without Docker)

1. Start MySQL container:
   ```bash
   docker compose up -d db
   ```
2. Set environment variables:
   ```powershell
   $env:DB_HOST="127.0.0.1"
   $env:DB_PORT="3307"
   $env:DB_NAME="world"
   $env:DB_USER="app"
   $env:DB_PASSWORD="app123"
   ```
3. Run app locally:
   ```bash
   mvn -q -DskipTests package
   java -jar target/Assessmentforgp5-1.0-SNAPSHOT-shaded.jar
   ```

---

##  Verify Output

Check logs:

```bash
   docker logs -f devopsapp1
```

Expected output example:
```
 Connected!
=== 1) All Countries by Population (World) ===
CHN  China         Asia     Eastern Asia   1,277,558,000
IND  India         Asia     Southern Asia  1,013,662,000
...
=== 32) Population by Language (Chinese, English, Hindi, Spanish, Arabic) ===
Chinese  1,100,000,000  17.5%
English    500,000,000   8.2%
...
```

---

##  Summary

 **db** → MySQL 8 with `world` data  
 **app** → Java app auto-runs 32 queries on container start  
 **docker-compose** → connects them automatically  
 **healthchecks** ensure app waits for DB readiness

---

### Tip:
If you modify SQL or Java files frequently:
```bash
  docker compose build app
  docker compose restart app
```
---

 *Author: Group 5 — Napier DevOps Assessment*  
 *Version: 0.1.0.2 (October 2025)*  
 *"Build. Connect. Report."*
