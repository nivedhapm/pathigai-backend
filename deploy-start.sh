#!/bin/bash
# This script ensures environment variables are explicitly passed to the Java process.

APP_JAR="target/pathigai-0.0.1-SNAPSHOT.jar"

# 1. Stop any currently running application instance
echo "Stopping existing application..."
pkill -f "java -jar ${APP_JAR}" || true

# 2. Build the new JAR package
echo "Building new package with Maven..."
./mvnw clean package -DskipTests

# 3. Start the application using 'env' to explicitly provide the variables to the Java process.
# This forces the variables to be available to Spring Boot.
echo "Starting application with explicit ENV variables..."
nohup env \
  SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}" \
  SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME}" \
  SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}" \
  JWT_SECRET="${JWT_SECRET}" \
  EMAIL_USER="${EMAIL_USER}" \
  EMAIL_PASSWORD="${EMAIL_PASSWORD}" \
  FAST2SMS_API_KEY="${FAST2SMS_API_KEY}" \
  FAST2SMS_SENDER_ID="${FAST2SMS_SENDER_ID}" \
  RECAPTCHA_SITE_KEY="${RECAPTCHA_SITE_KEY}" \
  RECAPTCHA_SECRET_KEY="${RECAPTCHA_SECRET_KEY}" \
  CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS}" \
  java -jar ${APP_JAR} --spring.profiles.active=prod > app.log 2>&1 &

# 4. Wait for the application to start up and detach fully
sleep 15

# 5. Display the last 15 lines of the log for immediate feedback
echo "Deployment initiated. Showing startup logs (waiting 15s)..."
tail -n 15 app.log

# 6. Exit with success status
exit 0