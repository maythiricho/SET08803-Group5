# Use official MySQL 8 image
FROM mysql:8.0

LABEL maintainer="Kyaw Zayar Min"

# Auto-run any SQL files on first container start (schema/data/users)
# Put your world.sql (and optional 99_create_app_user.sql) in db/world-db/
COPY world-db/*.sql /docker-entrypoint-initdb.d/

# MySQL bootstrap settings
ENV MYSQL_ROOT_PASSWORD=example
ENV MYSQL_DATABASE=world

# Optional: simple healthcheck
HEALTHCHECK --interval=10s --timeout=5s --retries=10 CMD \
  mysqladmin ping -h 127.0.0.1 -uroot -pexample || exit 1

# Default MySQL port
EXPOSE 3306
