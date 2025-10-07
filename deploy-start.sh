#!/bin/bash
# This script relies on all configuration variables being EXPORTED
# in the shell environment by the GitHub Actions script block.

APP_JAR="target/pathigai-0.0.1-SNAPSHOT.jar"

# 1. Stop any currently running application instance
echo "Stopping existing application..."
pkill -f "java -jar ${APP_JAR}" || true

# 2. Build the new JAR package
echo "Building new package with Maven..."
./mvnw clean package -DskipTests

# 3. Start the application by letting Spring Boot read its own environment variables.
# We DO NOT use -D properties here, as they are failing.
# Spring Boot automatically maps capitalized and underscored ENV vars (e.g., SPRING_DATASOURCE_URL)
# to its corresponding properties (spring.datasource.url).
echo "Starting application, relying on exported ENV variables..."
nohup java -jar ${APP_JAR} --spring.profiles.active=prod > app.log 2>&1 &

# 4. Wait for the application to start up and detach fully
sleep 15

# 5. Display the last 15 lines of the log for immediate feedback in GitHub Actions
echo "Deployment initiated. Showing startup logs (waiting 15s)..."
tail -n 15 app.log

# 6. Exit with success status
exit 0