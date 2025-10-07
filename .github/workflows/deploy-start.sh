#!/bin/bash
# Script to build and start the Spring Boot application from the root path.
# $1 holds the entire string of -D properties (Java arguments) passed from the GitHub Action.

APP_JAR="target/pathigai-0.0.1-SNAPSHOT.jar"

# 1. Stop any currently running application instance
echo "Stopping existing application..."
pkill -f "java -jar ${APP_JAR}" || true

# 2. Build the new JAR package
echo "Building new package with Maven..."
./mvnw clean package -DskipTests

# 3. Start the application in a fully detached background process
echo "Starting application with configuration..."
nohup java $1 -jar ${APP_JAR} --spring.profiles.active=prod > app.log 2>&1 &

# 4. Wait for the application to start up and detach fully
sleep 15

# 5. Display the last 15 lines of the log for immediate feedback
echo "Deployment initiated. Showing startup logs (waiting 15s)..."
tail -n 15 app.log

# 6. Exit with success status
exit 0