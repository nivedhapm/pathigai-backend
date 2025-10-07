#!/bin/bash
APP_JAR="target/pathigai-0.0.1-SNAPSHOT.jar"

echo "Stopping existing application..."
pkill -f "pathigai-0.0.1-SNAPSHOT.jar" || true
sleep 3

echo "Building new package with Maven..."
./mvnw clean package -DskipTests

if [ ! -f "$APP_JAR" ]; then
    echo "ERROR: Build failed - JAR file not found!"
    exit 1
fi

echo "Starting application with environment variables from GitHub Actions..."
nohup java -jar ${APP_JAR} \
  --spring.profiles.active=prod \
  --spring.datasource.url="jdbc:mysql://localhost:3306/pathigai_app?useSSL=false&serverTimezone=UTC"\
  --spring.datasource.username="pathigai_user" \
  --spring.datasource.password="vivo-v23-5g" \
  --spring.datasource.driver-class-name="com.mysql.cj.jdbc.Driver" \
  --spring.jpa.hibernate.ddl-auto=validate \
  --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect \
  --app.jwt.secret="${JWT_SECRET}" \
  --spring.mail.username="${EMAIL_USER}" \
  --spring.mail.password="${EMAIL_PASSWORD}" \
  --app.sms.fast2sms.api-key="${FAST2SMS_API_KEY}" \
  --app.sms.fast2sms.sender-id="${FAST2SMS_SENDER_ID}" \
  --app.recaptcha.site-key="${RECAPTCHA_SITE_KEY}" \
  --app.recaptcha.secret-key="${RECAPTCHA_SECRET_KEY}" \
  --app.security.cors.allowed-origins="${CORS_ALLOWED_ORIGINS}" \
  --logging.level.com.nivedha.pathigai=INFO \
  --logging.level.org.springframework.security=INFO \
  > ~/app.log 2>&1 &

sleep 10
echo "Application started. Showing startup logs..."
tail -n 50 ~/app.log

echo ""
echo "================================"
echo "Deployment Summary:"
echo "================================"
echo "API URL: https://64.227.142.243/api/v1"
echo "CORS Origins: ${CORS_ALLOWED_ORIGINS}"
echo "================================"
echo ""

sleep 5
if pgrep -f "pathigai-0.0.1-SNAPSHOT.jar" > /dev/null; then
    echo "✅ Application is running successfully"
    echo "📝 Full logs: tail -f ~/app.log"
    echo "🏥 Test health: curl -k https://64.227.142.243/api/v1/health"
else
    echo "❌ Application failed to start. Check logs:"
    tail -n 100 ~/app.log
    exit 1
fi

exit 0