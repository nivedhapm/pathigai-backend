#!/bin/bash
# This script sources environment variables from a file written by the action.

APP_JAR="target/pathigai-0.0.1-SNAPSHOT.jar"

# 1. Stop any currently running application instance
echo "Stopping existing application..."
pkill -f "java -jar ${APP_JAR}" || true

# 2. Build the new JAR package
echo "Building new package with Maven..."
./mvnw clean package -DskipTests

# 3. 💡 CRITICAL: SOURCE the environment variables explicitly
echo "Sourcing environment variables from /tmp/pathigai_env.sh"
. /tmp/pathigai_env.sh
# The dot (.) operator (source) loads the variables into the current shell.

# 4. Start the application. It now has the variables in its environment.
echo "Starting application with configuration loaded."
nohup java -jar ${APP_JAR} --spring.profiles.active=prod > app.log 2>&1 &

# 5. Wait for the application to start up and detach fully
sleep 15

# 6. Display the last 15 lines of the log for immediate feedback in GitHub Actions
echo "Deployment initiated. Showing startup logs (waiting 15s)..."
tail -n 15 app.log

# 7. Clean up the temporary file
rm /tmp/pathigai_env.sh

# 8. Exit with success status
exit 0