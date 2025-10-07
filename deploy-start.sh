#!/bin/bash
# Simplified and more reliable version

APP_JAR="target/pathigai-0.0.1-SNAPSHOT.jar"

# 1. Stop existing application
echo "Stopping existing application..."
pkill -f "pathigai-0.0.1-SNAPSHOT.jar" || true
sleep 3

# 2. Build new package
echo "Building new package with Maven..."
./mvnw clean package -DskipTests

# 3. Check if build was successful
if [ ! -f "$APP_JAR" ]; then
    echo "ERROR: Build failed - JAR file not found!"
    exit 1
fi

# 4. Start application with explicit configuration
echo "Starting application with injected configuration..."
nohup java -jar ${APP_JAR} \
  --spring.profiles.active=prod \
  --spring.datasource.url="jdbc:mysql://64.227.142.243:3306/pathigai_app?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  --spring.datasource.username="pathigai_user" \
  --spring.datasource.password="Footprints-1" \
  --app.jwt.secret="${JWT_SECRET}" \
  --spring.mail.username="${EMAIL_USER}" \
  --spring.mail.password="${EMAIL_PASSWORD}" \
  --app.sms.fast2sms.api-key="${FAST2SMS_API_KEY}" \
  --app.sms.fast2sms.sender-id="${FAST2SMS_SENDER_ID}" \
  --app.recaptcha.site-key="${RECAPTCHA_SITE_KEY}" \
  --app.recaptcha.secret-key="${RECAPTCHA_SECRET_KEY}" \
  --app.security.cors.allowed-origins="${CORS_ALLOWED_ORIGINS}" \
  > ~/app.log 2>&1 &

# 5. Wait and show logs
sleep 10
echo "Application started. Showing startup logs..."
tail -n 30 ~/app.log

exit 0