#!/bin/bash
# Script to build and start the Spring Boot application.
# It uses environment variables EXPORTED by the GitHub Action's shell.

APP_JAR="target/pathigai-0.0.1-SNAPSHOT.jar"

# 1. Stop any currently running application instance
echo "Stopping existing application..."
pkill -f "java -jar ${APP_JAR}" || true

# 2. Build the new JAR package
echo "Building new package with Maven..."
./mvnw clean package -DskipTests

# 3. Start the application with all environment variables explicitly
# Crucially, we use the environment variables (e.g., $SPRING_DATASOURCE_URL)
echo "Starting application with configuration..."
nohup java \
  -Dspring.datasource.url="${SPRING_DATASOURCE_URL}" \
  -Dspring.datasource.username="${SPRING_DATASOURCE_USERNAME}" \
  -Dspring.datasource.password="${SPRING_DATASOURCE_PASSWORD}" \
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
  -Dapp.jwt.secret="${JWT_SECRET}" \
  -Dspring.mail.username="${EMAIL_USER}" \
  -Dspring.mail.password="${EMAIL_PASSWORD}" \
  -Dapp.sms.fast2sms.api-key="${FAST2SMS_API_KEY}" \
  -Dapp.sms.fast2sms.sender-id="${FAST2SMS_SENDER_ID}" \
  -Dapp.recaptcha.site-key="${RECAPTCHA_SITE_KEY}" \
  -Dapp.recaptcha.secret-key="${RECAPTCHA_SECRET_KEY}" \
  -Dapp.security.cors.allowed-origins="${CORS_ALLOWED_ORIGINS}" \
  -jar ${APP_JAR} --spring.profiles.active=prod > app.log 2>&1 &

# 4. Wait for the application to start up and detach fully
sleep 15

# 5. Display the last 15 lines of the log for immediate feedback in GitHub Actions
echo "Deployment initiated. Showing startup logs (waiting 15s)..."
tail -n 15 app.log

# 6. Exit with success status
exit 0