# Pathigai (பதிகை) - Backend API

<div align="center">

![Pathigai Logo](https://via.placeholder.com/150x150?text=Pathigai)

**Track. Train. Transform. | Guiding Every Step to Success.**

RESTful API for the Student Progress Tracking System

[![Deployed on DigitalOcean](https://img.shields.io/badge/Deployed%20on-DigitalOcean-0080FF?style=flat&logo=digitalocean)](https://www.digitalocean.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=flat&logo=mysql)](https://www.mysql.com/)

[API Documentation](https://your-backend-url.com/swagger-ui.html) • [Frontend Repo](https://github.com/nivedhapm/pathigai-frontend) • [Report Issue](https://github.com/nivedhapm/pathigai-backend/issues)

</div>

---

## 📖 About

This is the backend API for **Pathigai** - a comprehensive student progress tracking system. Built with Spring Boot, it provides secure, scalable REST APIs for managing the entire student training lifecycle from application to placement.

---

## ✨ Features (Implemented)

### 🔐 Module 1: Authentication & Access Control
- **JWT-Based Authentication** - Stateless, secure token-based auth
- **Role-Based Access Control (RBAC)** - Fine-grained permissions for 9 user roles
- **Password Security** - Bcrypt encryption with salt rounds
- **Mandatory Password Reset** - Force password change on first login
- **Password Recovery** - Forgot password with email-based reset tokens
- **CAPTCHA Validation** - Server-side verification for bot protection
- **Session Management** - Token expiration and refresh mechanisms

### 👥 Module 2: User Management
- **User CRUD Operations** - Create, read, update, delete users
- **Super Admin Setup** - Predefined admin account on deployment
- **Hierarchical User Management** - Admin can manage Faculty, HR, Mentors
- **Bulk User Operations** - Import/export user data
- **Account Status Management** - Activate, deactivate, or suspend accounts
- **Credential Generation** - Automatic Zoho email + temporary password creation
- **Audit Logging** - Track all user actions with timestamps

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 17** | Core programming language |
| **Spring Boot 3.x** | Application framework |
| **Spring Security** | Authentication & authorization |
| **Spring Data JPA** | Database abstraction layer |
| **MySQL 8.x** | Relational database |
| **JWT (jjwt)** | JSON Web Token implementation |
| **Bcrypt** | Password hashing algorithm |
| **JavaMailSender** | Email notification service |
| **Hibernate** | ORM framework |
| **Lombok** | Boilerplate code reduction |
| **Maven** | Dependency management |

---

## 📁 Project Structure

```
pathigai-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/pathigai/
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── JwtConfig.java
│   │   │       │   └── CorsConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   └── UserController.java
│   │   │       ├── model/
│   │   │       │   ├── User.java
│   │   │       │   ├── Role.java
│   │   │       │   └── ApplicationStatus.java
│   │   │       ├── repository/
│   │   │       │   ├── UserRepository.java
│   │   │       │   └── RoleRepository.java
│   │   │       ├── service/
│   │   │       │   ├── AuthService.java
│   │   │       │   ├── UserService.java
│   │   │       │   ├── EmailService.java
│   │   │       │   └── JwtService.java
│   │   │       ├── dto/
│   │   │       │   ├── LoginRequest.java
│   │   │       │   ├── LoginResponse.java
│   │   │       │   ├── UserDTO.java
│   │   │       │   └── PasswordResetRequest.java
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── ResourceNotFoundException.java
│   │   │       │   └── UnauthorizedException.java
│   │   │       ├── security/
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   └── CustomUserDetailsService.java
│   │   │       └── PathigaiApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── email-templates/
│   │           ├── welcome.html
│   │           ├── password-reset.html
│   │           └── status-update.html
│   └── test/
│       └── java/
│           └── com/pathigai/
│               ├── controller/
│               ├── service/
│               └── repository/
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK)** 17 or higher
- **Maven** 3.8+
- **MySQL** 8.0+
- **Git**
- **Postman** (optional, for API testing)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/nivedhapm/pathigai-backend.git
   cd pathigai-backend
   ```

2. **Set up MySQL Database**
   ```sql
   CREATE DATABASE pathigai_db;
   CREATE USER 'pathigai_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON pathigai_db.* TO 'pathigai_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Configure application properties**
   
   Create `src/main/resources/application-dev.properties`:
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:mysql://localhost:3306/pathigai_db
   spring.datasource.username=pathigai_user
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true

   # JWT Configuration
   jwt.secret=your-secret-key-minimum-256-bits-long
   jwt.expiration=86400000

   # Email Configuration (SMTP)
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your-email@example.com
   spring.mail.password=your-app-password
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true

   # CORS Configuration
   cors.allowed.origins=http://localhost:3000
   ```

4. **Install dependencies**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The API will be available at `http://localhost:8080`

### Building for Production

```bash
mvn clean package -DskipTests
java -jar target/pathigai-backend-1.0.0.jar --spring.profiles.active=prod
```

---

## 🌿 Branching Strategy

- **`main`** - Production-ready code, deployed on DigitalOcean Droplet
- **`dev`** - Active development branch for daily commits and testing

### Workflow
```bash
# Switch to dev branch
git checkout dev

# Make changes and commit
git add .
git commit -m "feat: add user management endpoints"

# Push to dev
git push origin dev

# When ready for production, merge to main
git checkout main
git merge dev
git push origin main
```

---

## 📡 API Endpoints

### Authentication Endpoints
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | Register new applicant | Public |
| POST | `/api/auth/login` | User login | Public |
| POST | `/api/auth/forgot-password` | Request password reset | Public |
| POST | `/api/auth/reset-password` | Reset password with token | Public |
| POST | `/api/auth/change-password` | Change password | Authenticated |
| POST | `/api/auth/logout` | User logout | Authenticated |

### User Management Endpoints
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/users` | Get all users | Admin |
| GET | `/api/users/{id}` | Get user by ID | Admin, Self |
| POST | `/api/users` | Create new user | Admin |
| PUT | `/api/users/{id}` | Update user | Admin, Self |
| DELETE | `/api/users/{id}` | Delete user | Admin |
| GET | `/api/users/role/{role}` | Get users by role | Admin, Faculty |
| PUT | `/api/users/{id}/status` | Update account status | Admin |

### Request/Response Examples

**Login Request:**
```json
{
  "email": "user@zsgs.in",
  "password": "SecurePass123!",
  "captchaToken": "03AGdBq27..."
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 123,
  "email": "user@zsgs.in",
  "role": "TRAINEE",
  "firstName": "John",
  "lastName": "Doe",
  "mustResetPassword": false
}
```

---

## 🔒 Security Features

1. **Password Encryption** - Bcrypt with 12 rounds
2. **JWT Token Security** - Signed tokens with expiration
3. **CORS Protection** - Configured allowed origins
4. **SQL Injection Prevention** - Parameterized queries via JPA
5. **XSS Protection** - Input sanitization
6. **Rate Limiting** - API request throttling (planned)
7. **Audit Logging** - All user actions tracked with timestamps

---

## 📧 Email Templates

The system sends automated emails for:
- **Welcome Email** - New user account creation
- **Password Reset** - Forgot password requests
- **Status Updates** - Application/interview status changes
- **Interview Schedules** - Interview date/time notifications
- **Placement Notifications** - Shortlisting and selection updates

Templates are located in `src/main/resources/email-templates/`

---

## 🗄️ Database Schema

### Key Tables
- **users** - User accounts and credentials
- **roles** - User role definitions
- **applications** - Trainee applications
- **assignments** - Faculty assignments
- **attendance** - Daily check-in/check-out records
- **tasks** - Task tracking
- **performance** - Monthly performance data
- **placement** - Placement status and history

---

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test Coverage
```bash
mvn jacoco:report
```

---

## 🚀 Deployment

### DigitalOcean Droplet Setup

1. **Create Droplet**
   - OS: Ubuntu 22.04 LTS
   - Size: 2GB RAM minimum
   - Enable monitoring

2. **Install Dependencies**
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jdk mysql-server nginx
   ```

3. **Configure MySQL**
   ```bash
   sudo mysql_secure_installation
   # Create production database
   ```

4. **Deploy Application**
   ```bash
   # Upload JAR file
   scp target/pathigai-backend-1.0.0.jar user@droplet-ip:/opt/pathigai/

   # Create systemd service
   sudo nano /etc/systemd/system/pathigai.service
   ```

5. **Setup Nginx as Reverse Proxy**
   ```nginx
   server {
       listen 80;
       server_name your-domain.com;

       location /api {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

6. **Enable SSL with Let's Encrypt**
   ```bash
   sudo certbot --nginx -d your-domain.com
   ```

---

## 🚧 Upcoming Features

### Phase 2 (In Development)
- Application & Interview Management APIs
- Trainee Onboarding Workflow
- Attendance Tracking APIs

### Phase 3 (Planned)
- Assignment Management System
- Task Management (Contests, Seminars, Problems)
- Performance Analytics Engine
- Placement Management APIs
- Mentorship Session Tracking
- Notification Service (WebSocket)

---

## 🤝 Contributing

Contributions are currently limited to project team members. If you're part of the team:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/NewEndpoint`)
3. Commit your changes (`git commit -m 'Add new endpoint for X'`)
4. Push to the branch (`git push origin feature/NewEndpoint`)
5. Open a Pull Request to `dev` branch

### Code Style Guidelines
- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Write unit tests for new features
- Keep controllers thin, business logic in services

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Contact

**Project Maintainer:** Nivedha PM

- GitHub: [@nivedhapm](https://github.com/nivedhapm)
- Email: your.email@example.com

**Project Links:**
- Backend Repository: [https://github.com/nivedhapm/pathigai-backend](https://github.com/nivedhapm/pathigai-backend)
- Frontend Repository: [https://github.com/nivedhapm/pathigai-frontend](https://github.com/nivedhapm/pathigai-frontend)
- API Documentation: [Your API Docs URL]

---

## 🙏 Acknowledgments

- Built for ZSGS (Zoho Schools of Graduate Studies)
- Spring Boot Community
- MySQL Team
- JWT.io for token implementation

---

<div align="center">

**Made with ☕ and ❤️ for tracking student progress**

</div>
