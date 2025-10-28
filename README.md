# Pathigai (பதிகை) - Backend API

<div align="center">

![Pathigai Logo](https://raw.githubusercontent.com/nivedhapm/pathigai-logo/main/pathigai-logo.png)

**Track. Train. Transform. | Guiding Every Step to Success.**

RESTful API for the Comprehensive Training & Progress Tracking System

[![Deployed on DigitalOcean](https://img.shields.io/badge/Deployed%20on-DigitalOcean-0080FF?style=flat&logo=digitalocean)](https://www.digitalocean.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=flat&logo=mysql)](https://www.mysql.com/)

[Frontend Repo](https://github.com/nivedhapm/pathigai-frontend) • [Report Issue](https://github.com/nivedhapm/pathigai-backend/issues)

</div>

---

## About

This is the backend API for **Pathigai** - a comprehensive multi-tenant training and progress tracking system. Built with Spring Boot 3.5.5 and Java 21, it provides secure, scalable REST APIs for managing the entire lifecycle from user onboarding to performance tracking across multiple organizations.

**Key Highlights:**
- Multi-tenant architecture with company isolation
- Advanced JWT-based authentication with refresh tokens
- Dual-channel verification (Email + SMS)
- Hierarchical role & profile-based access control
- Asynchronous email processing with outbox pattern
- Session management with device fingerprinting
- reCAPTCHA integration for bot protection

---

## Features (Implemented)

### Module 1: Authentication & Authorization

#### **Signup Flow**
- **Two-Step Registration Process**
  - Step 1: Basic user registration with email/phone
  - Step 2: Company creation and profile setup
- **Multi-Channel OTP Verification**
  - Email OTP via JavaMailSender, Mailtrap
  - SMS OTP via Fast2SMS integration
  - Configurable expiry times (2 minutes default)
  - Resend limits with attempt tracking
- **Email/Phone Uniqueness Validation**
  - Real-time availability checking
  - Duplicate prevention at database level

#### **Login & Session Management**
- **JWT-Based Stateless Authentication**
  - Access tokens (2 hours validity)
  - Refresh tokens (24 hours validity)
  - Token rotation on refresh
  - Clock skew tolerance (5 minutes)
- **Device Fingerprinting**
  - Unique device identification
  - Multi-device session tracking
  - Session limit enforcement (max 3 concurrent)
- **Enhanced Login Flow**
  - Two-step authentication process
  - IP address and User-Agent tracking
  - Automatic session creation
- **Session Cleanup**
  - Scheduled cleanup of expired sessions
  - Configurable cleanup intervals

#### **Password Management**
- **Secure Password Storage**
  - BCrypt encryption with 12 rounds
  - Salt generation per password
- **Temporary Password Workflow**
  - Auto-generated passwords for new users
  - Mandatory reset on first login
  - Flag-based enforcement
- **Forgot Password Flow**
  - Email-based OTP verification
  - Secure password reset process
  - Token expiration handling

#### **reCAPTCHA Protection**
- Server-side Google reCAPTCHA v2 validation
- Configurable enable/disable via properties
- Bot protection on sensitive endpoints

### Module 2: Multi-Tenant User Management

#### **Company Management**
- **Multi-Tenant Architecture**
  - Company isolation at database level
  - Company creator designation
- **Organization Hierarchy**
  - Company-level user grouping
  - Cross-company access prevention

#### **User Creation & Management**
- **Single User Creation**
  - Role and profile assignment
  - Automatic credential generation
  - Welcome email with temp password
- **Bulk User Import**
  - CSV-based bulk upload
  - Batch processing with error handling
  - Success/failure reporting per user
  - Automatic credential generation
  - Welcome email with temp password
- **User Status Management**
  - ACTIVE, INACTIVE, SUSPENDED states
  - Soft delete with timestamp tracking

#### **Role & Profile System**
- **9 Predefined Roles**
  - ADMIN, MANAGER, HR, FACULTY, MENTOR
  - INTERVIEW_PANELIST, EMPLOYEE, TRAINEE, APPLICANT
- **6 Hierarchical Profiles**
  - SUPER_ADMIN (Level 1) - System-wide access
  - ADMIN (Level 2) - Company-wide access
  - MANAGEMENT (Level 3) - Department/team access
  - TRAINER (Level 4) - Training operations
  - PLACEMENT (Level 5) - Placement operations
  - TRAINEE (Level 6) - Limited access
- **Role-Profile Mapping Validation**
  - SUPER_ADMIN can create: ADMIN, MANAGER, HR, FACULTY, MENTOR, INTERVIEW_PANELIST, EMPLOYEE, TRAINEE...(EVERYONE).
  - ADMIN can create: MANAGER, HR, FACULTY, MENTOR, INTERVIEW_PANELIST,EMPLOYEE, TRAINEE.... (EVERYONE EXCEPT SUPER_ADMIN)
  - MANAGEMENT can create: FACULTY, MENTOR, INTERVIEW_PANELIST, EMPLOYEE, TRAINEE
  - Hierarchical permission enforcement

### Module 3: Email & Notification System

#### **Email Service Architecture**
- **Dual Email Provider Support**
  - **Development**: Gmail SMTP (Port 587, STARTTLS)
  - **Production**: Mailtrap SMTP (Port 2525)
  - Environment-based auto-switching
- **Asynchronous Processing**
  - Non-blocking email sending
  - Email outbox pattern for reliability
  - Retry mechanism for failures
- **Email Types**
  - Welcome emails with credentials
  - OTP verification emails
  - Password reset emails
  - User invitation emails
- **HTML Email Templates**
  - Branded email design with logo
  - Responsive layout
  - Embedded credentials box
  - Security notes and instructions

#### **SMS Service**
- **Fast2SMS Integration**
  - OTP delivery via SMS
  - Configurable sender ID
  - API key-based authentication
  - Development mode bypass option

### Module 4: Dashboard & Navigation

#### **Dynamic Navigation**
- **Role-Based Menu Items**
  - Navigation links based on user profile
  - Hierarchical menu structure
  - Icon and route mapping
- **Dashboard Data**
  - User-specific statistics
  - Session information
  - Activity summaries

#### **Profile Management**
- **Profile Hierarchy Endpoint**
  - Ordered by hierarchy level
  - Profile descriptions
  - Active profile filtering
- **Allowed Profiles API**
  - Context-aware profile suggestions
  - Creation permission validation

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Core programming language |
| **Spring Boot** | 3.5.5 | Application framework |
| **Spring Security** | 6.x | Authentication & authorization |
| **Spring Data JPA** | 3.x | Database abstraction layer |
| **Hibernate** | 6.x | ORM framework |
| **MySQL** | 8.0+ | Relational database |
| **JWT (jjwt)** | 0.11.5 | JSON Web Token implementation |
| **BCrypt** | Built-in | Password hashing algorithm |
| **JavaMailSender** | 2.x | Email service (Spring Mail) |
| **Mailtrap** | API | Production email delivery |
| **Fast2SMS** | API | SMS OTP delivery |
| **Lombok** | 1.18.38 | Boilerplate code reduction |
| **Maven** | 3.8+ | Dependency management |
| **WebFlux** | 6.x | Reactive REST client (reCAPTCHA) |

---

## Project Structure

```
pathigai-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/nivedha/pathigai/
│   │   │       ├── auth/                    # Authentication Module
│   │   │       │   ├── controllers/
│   │   │       │   │   ├── SignupController.java
│   │   │       │   │   ├── LoginController.java
│   │   │       │   │   ├── PasswordResetController.java
│   │   │       │   │   ├── VerificationController.java
│   │   │       │   │   ├── SessionController.java
│   │   │       │   │   └── EnhancedAuthController.java
│   │   │       │   ├── services/
│   │   │       │   │   ├── SignupService.java
│   │   │       │   │   ├── LoginService.java
│   │   │       │   │   ├── PasswordResetService.java
│   │   │       │   │   ├── VerificationService.java
│   │   │       │   │   ├── SessionService.java
│   │   │       │   │   ├── SessionManagementService.java
│   │   │       │   │   ├── SessionCleanupService.java
│   │   │       │   │   ├── RefreshTokenService.java
│   │   │       │   │   ├── RecaptchaService.java
│   │   │       │   │   └── external/
│   │   │       │   │       ├── EmailService.java
│   │   │       │   │       └── SmsService.java
│   │   │       │   ├── entities/
│   │   │       │   │   ├── User.java
│   │   │       │   │   ├── Company.java
│   │   │       │   │   ├── Role.java
│   │   │       │   │   ├── Profile.java
│   │   │       │   │   ├── Session.java
│   │   │       │   │   ├── Verification.java
│   │   │       │   │   ├── Gender.java (enum)
│   │   │       │   │   ├── UserStatus.java (enum)
│   │   │       │   │   ├── VerificationType.java (enum)
│   │   │       │   │   └── VerificationContext.java (enum)
│   │   │       │   ├── repositories/
│   │   │       │   │   ├── UserRepository.java
│   │   │       │   │   ├── CompanyRepository.java
│   │   │       │   │   ├── RoleRepository.java
│   │   │       │   │   ├── ProfileRepository.java
│   │   │       │   │   ├── SessionRepository.java
│   │   │       │   │   └── VerificationRepository.java
│   │   │       │   ├── dto/
│   │   │       │   │   ├── request/
│   │   │       │   │   │   ├── SignupRegisterRequest.java
│   │   │       │   │   │   ├── SignupCompleteRequest.java
│   │   │       │   │   │   ├── LoginRequest.java
│   │   │       │   │   │   └── PasswordResetRequest.java
│   │   │       │   │   └── response/
│   │   │       │   │       ├── SignupRegisterResponse.java
│   │   │       │   │       ├── SignupCompleteResponse.java
│   │   │       │   │       └── LoginResponse.java
│   │   │       │   └── exceptions/
│   │   │       │       └── (custom exceptions)
│   │   │       │
│   │   │       ├── user/                    # User Management Module
│   │   │       │   ├── controllers/
│   │   │       │   │   └── UserManagementController.java
│   │   │       │   ├── services/
│   │   │       │   │   ├── UserManagementService.java
│   │   │       │   │   ├── UserEmailService.java
│   │   │       │   │   ├── UserService.java
│   │   │       │   │   └── EmailProcessorService.java
│   │   │       │   ├── entities/
│   │   │       │   │   ├── EmailOutbox.java
│   │   │       │   │   └── EmailType.java (enum)
│   │   │       │   ├── repositories/
│   │   │       │   │   └── EmailOutboxRepository.java
│   │   │       │   └── dto/
│   │   │       │       ├── request/
│   │   │       │       │   ├── CreateUserRequest.java
│   │   │       │       │   └── BulkCreateUsersRequest.java
│   │   │       │       └── response/
│   │   │       │           ├── CreateUserResponse.java
│   │   │       │           ├── BulkCreateUsersResponse.java
│   │   │       │           └── UserResponse.java
│   │   │       │
│   │   │       ├── profile/                 # Profile & Permissions Module
│   │   │       │   ├── controllers/
│   │   │       │   │   └── ProfileController.java
│   │   │       │   ├── services/
│   │   │       │   │   ├── ProfileService.java
│   │   │       │   │   └── RoleProfileMappingService.java
│   │   │       │   └── dto/
│   │   │       │       ├── ProfileResponse.java
│   │   │       │       └── ProfileMappingResponse.java
│   │   │       │
│   │   │       ├── dashboard/               # Dashboard Module
│   │   │       │   ├── controllers/
│   │   │       │   │   └── DashboardController.java
│   │   │       │   ├── services/
│   │   │       │   │   ├── DashboardService.java
│   │   │       │   │   └── NavigationService.java
│   │   │       │   └── dto/
│   │   │       │       ├── DashboardDataResponse.java
│   │   │       │       └── NavigationResponse.java
│   │   │       │
│   │   │       ├── config/                  # Configuration Classes
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── JwtConfig.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── MailConfig.java
│   │   │       │   ├── WebConfig.java
│   │   │       │   ├── AsyncConfig.java
│   │   │       │   ├── AppConfig.java
│   │   │       │   ├── RestTemplateConfig.java
│   │   │       │   ├── ValidationConfig.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       │
│   │   │       └── PathigaiApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties         # Development config (Gmail)
│   │       ├── application-prod.properties    # Production config (Mailtrap)
│   │       └── db/
│   │           └── migration/
│   │               └── V2__enhance_session_management.sql
│   │
│   └── test/
│       └── java/
│           └── com/nivedha/pathigai/
│               └── PathigaiApplicationTests.java
│
├── target/                                   # Build output
├── .gitignore
├── pom.xml
├── mvnw                                      # Maven wrapper (Unix)
├── mvnw.cmd                                  # Maven wrapper (Windows)
├── deploy-start.sh                           # Production deployment script
└── README.md
```

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)** 21 or higher
- **Maven** 3.8+
- **MySQL** 8.0+
- **Git**
- **Postman** or **cURL** (optional, for API testing)

### Installation

#### 1. Clone the repository

```bash
git clone https://github.com/nivedhapm/pathigai-backend.git
cd pathigai-backend
```

#### 2. Set up MySQL Database

```sql
CREATE DATABASE pathigai_app;
CREATE USER 'pathigai_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON pathigai_app.* TO 'pathigai_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 3. Configure application properties

Edit `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/pathigai_app?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=pathigai_user
spring.datasource.password=your_password

# Mail Configuration (Gmail for development)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# JWT Secret (Generate a secure 256-bit key)
app.jwt.secret=your-secret-key-minimum-256-bits-long

# SMS Configuration (Fast2SMS)
app.sms.fast2sms.api-key=your-fast2sms-api-key
app.sms.fast2sms.sender-id=FSTSMS

# reCAPTCHA Configuration
app.recaptcha.site-key=your-recaptcha-site-key
app.recaptcha.secret-key=your-recaptcha-secret-key
```

#### 4. Install dependencies

```bash
mvn clean install
```

#### 5. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Building for Production

```bash
mvn clean package -DskipTests
```

The JAR file will be created at `target/pathigai-0.0.1-SNAPSHOT.jar`

---

## API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| **POST** | `/api/v1/signup/register` | Register new user (Step 1) | Public |
| **POST** | `/api/v1/signup/complete` | Complete signup with company (Step 2) | Public |
| **GET** | `/api/v1/signup/check-email` | Check email availability | Public |
| **GET** | `/api/v1/signup/check-phone` | Check phone availability | Public |
| **POST** | `/api/v1/login/authenticate` | User login (Step 1) | Public |
| **POST** | `/api/v1/login/complete` | Complete login after verification (Step 2) | Public |
| **POST** | `/api/v1/login/reset-temporary-password` | Reset temporary password | Authenticated |
| **POST** | `/api/v1/login/logout` | User logout | Authenticated |
| **POST** | `/api/v1/password-reset/initiate` | Initiate forgot password | Public |
| **POST** | `/api/v1/password-reset/complete` | Complete password reset | Public |

### Verification Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| **POST** | `/api/v1/verification/send-otp` | Send OTP (Email/SMS) | Public |
| **POST** | `/api/v1/verification/verify-otp` | Verify OTP | Public |
| **POST** | `/api/v1/verification/resend-otp` | Resend OTP | Public |

### User Management Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| **POST** | `/api/v1/users/create` | Create new user | Super_Admin, Admin, Management |
| **POST** | `/api/v1/users/bulk-create` | Bulk create users from CSV | Super_Admin, Admin, Management |
| **GET** | `/api/v1/users/profiles/allowed` | Get allowed creation profiles | Authenticated |

### Profile Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| **GET** | `/api/v1/profiles` | Get all profiles | Authenticated |
| **GET** | `/api/v1/profiles/hierarchy` | Get profile hierarchy | Authenticated |
| **GET** | `/api/v1/profiles/mappings` | Get role-profile mappings | Super_Admin |
| **GET** | `/api/v1/profiles/allowed-for-role/{roleName}` | Get allowed profiles for role | Super_Admin |

### Dashboard Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| **GET** | `/api/v1/dashboard/data` | Get dashboard data | Authenticated |
| **GET** | `/api/v1/dashboard/navigation` | Get navigation menu | Authenticated |
| **POST** | `/api/v1/dashboard/extend-session` | Extend user session | Authenticated |

### Session Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| **GET** | `/api/v1/sessions/active` | Get active sessions | Authenticated |
| **DELETE** | `/api/v1/sessions/{sessionId}` | Terminate specific session | Authenticated |
| **POST** | `/api/v1/sessions/refresh` | Refresh access token | Public |

---

## Request/Response Examples

### Signup Flow

**1. Register User (Step 1)**

```json
POST /api/v1/signup/register

{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "dateOfBirth": "1995-05-15",
  "gender": "MALE",
  "password": "SecurePass@123"
}
```

**Response:**

```json
{
  "userId": 29,
  "email": "john.doe@example.com",
  "message": "Registration successful. Please verify your email and phone.",
  "requiresVerification": true
}
```

**2. Complete Signup (Step 2)**

```json
POST /api/v1/signup/complete

{
  "userId": 29,
  "companyName": "Tech Solutions Inc",
  "industry": "Information Technology",
  "companyWebsite": "https://techsolutions.com",
  "workLocation": "Chennai, India"
}
```

**Response:**

```json
{
  "userId": 29,
  "companyId": 15,
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Signup completed successfully"
}
```

### Login Flow

**1. Authenticate User**

```json
POST /api/v1/login/authenticate

{
  "email": "john.doe@example.com",
  "password": "SecurePass@123",
  "captchaToken": "03AGdBq27..."
}
```

**Response (if temp password):**

```json
{
  "userId": 29,
  "email": "john.doe@example.com",
  "requiresPasswordReset": true,
  "message": "Please reset your temporary password"
}
```

**Response (if regular password):**

```json
{
  "userId": 29,
  "email": "john.doe@example.com",
  "requiresVerification": true,
  "verificationType": "EMAIL",
  "message": "OTP sent to your email"
}
```

**2. Complete Login**

```json
POST /api/v1/login/complete?userId=29
```

**Response:**

```json
{
  "userId": 29,
  "companyId": 15,
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "ADMIN",
  "profile": "SUPER_ADMIN",
  "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "sessionId": 42,
  "expiresAt": "2025-10-28T14:30:00"
}
```

### Create User

```json
POST /api/v1/users/create
Authorization: Bearer <token>

{
  "fullName": "Jane Smith",
  "email": "jane.smith@techsolutions.com",
  "phone": "9876543211",
  "dateOfBirth": "1998-03-20",
  "gender": "FEMALE",
  "roleName": "FACULTY",
  "profileName": "TRAINER",
  "workLocation": "Bangalore, India"
}
```

**Response:**

```json
{
  "success": true,
  "message": "User created successfully",
  "userId": 30,
  "user": {
    "userId": 30,
    "fullName": "Jane Smith",
    "email": "jane.smith@techsolutions.com",
    "phone": "9876543211",
    "role": "FACULTY",
    "profile": "TRAINER",
    "temporaryPassword": "Xy8pQ@mN2kL",
    "companyName": "Tech Solutions Inc"
  }
}
```

---

## Security Features

### 1. **Authentication Security**
- JWT tokens with HMAC-SHA256 signing
- Access token expiry: 2 hours
- Refresh token expiry: 24 hours
- Token rotation on refresh
- Secure token storage in session table

### 2. **Password Security**
- BCrypt hashing with 12 rounds
- Automatic salt generation
- Temporary password enforcement
- Password complexity validation
- Last password reset tracking

### 3. **Session Security**
- Device fingerprinting for unique identification
- Maximum 3 concurrent sessions per user
- IP address and User-Agent tracking
- Automatic cleanup of expired sessions
- Session version tracking for token invalidation

### 4. **API Security**
- CORS configuration with allowed origins
- Request validation with Bean Validation
- SQL injection prevention via JPA
- XSS protection with input sanitization
- Rate limiting (planned)

### 5. **Verification Security**
- OTP with 2-minute expiry
- Maximum 3 verification attempts
- Maximum 3 OTP resends
- BCrypt-hashed OTP storage
- Context-based verification (Signup/Login/Reset)

### 6. **Bot Protection**
- Google reCAPTCHA v2 integration
- Server-side validation
- Configurable threshold scoring

---

## 📧 Email System

### Email Providers

**Development Environment:**
- Provider: Gmail SMTP
- Host: smtp.gmail.com
- Port: 587 (STARTTLS)
- From: nivi110401@gmail.com

**Production Environment:**
- Provider: Mailtrap
- Host: live.smtp.mailtrap.io
- Port: 2525 (DigitalOcean compatible)
- From: noreply@pathigai.app

### Email Types

1. **Welcome Email** - Sent when admin creates a new user
   - Contains temporary credentials
   - Login instructions
   - Password reset reminder

2. **OTP Verification Email** - Sent during signup/login
   - 6-digit OTP code
   - 2-minute validity
   - Security instructions

3. **Password Reset Email** - Sent for forgot password
   - OTP-based reset flow
   - Secure reset link
   - Expiry information

4. **User Invitation Email** - Sent for team invitations
   - Organization details
   - Role information
   - Getting started guide

### Asynchronous Processing

- **Email Outbox Pattern**
  - Emails queued in database
  - Background processing
  - Retry on failure
  - Send status tracking

- **Non-blocking Sends**
  - User operations don't wait for email
  - Immediate API responses
  - Asynchronous execution with @Async

---

## Database Schema

### Core Tables

#### **users**
```sql
- user_id (PK, Auto Increment)
- email (Unique, Indexed)
- phone (Unique)
- full_name
- password_hash
- date_of_birth
- gender (ENUM)
- work_location
- enabled (Boolean)
- user_status (ENUM: ACTIVE, INACTIVE, SUSPENDED)
- is_temporary_password (Boolean)
- is_company_creator (Boolean)
- company_id (FK -> companies)
- primary_role_id (FK -> roles)
- primary_profile_id (FK -> profiles)
- created_by_user_id (FK -> users, self-reference)
- email_verified, phone_verified (Boolean)
- last_password_reset
- created_at, updated_at, deleted_at (Timestamps)
```

#### **companies**
```sql
- company_id (PK, Auto Increment)
- company_name
- industry
- company_website
- created_at (Timestamp)
```

#### **roles**
```sql
- role_id (PK, Auto Increment)
- name (Unique: ADMIN, MANAGER, HR, FACULTY, MENTOR, 
       INTERVIEW_PANELIST, EMPLOYEE, TRAINEE, APPLICANT)
```

#### **profiles**
```sql
- profile_id (PK, Auto Increment)
- name (Unique: SUPER_ADMIN, ADMIN, MANAGEMENT, TRAINER, PLACEMENT, TRAINEE)
- description
- hierarchy_level (1-6)
```

#### **sessions**
```sql
- session_id (PK, Auto Increment)
- user_id (FK -> users)
- device_fingerprint (Unique per user)
- device_name
- ip_address
- user_agent
- access_token_hash
- refresh_token_hash
- refresh_token_version
- expires_at, refresh_expires_at
- last_accessed
- is_active (Boolean)
- created_at (Timestamp)
- UNIQUE CONSTRAINT (user_id, device_fingerprint)
```

#### **verifications**
```sql
- verification_id (PK, Auto Increment)
- user_id (FK -> users)
- verification_type (ENUM: EMAIL, SMS)
- context (ENUM: SIGNUP, LOGIN, PASSWORD_RESET)
- otp_hash (BCrypt)
- expires_at
- verified (Boolean)
- attempt_count, resend_count
- last_resend
- created_at (Timestamp)
```

#### **email_outbox**
```sql
- email_id (PK, Auto Increment)
- recipient_email
- subject
- body (TEXT)
- email_type (ENUM: WELCOME, OTP_VERIFICATION, PASSWORD_RESET, INVITATION)
- related_user_id (FK -> users)
- related_invitation_id
- sent (Boolean)
- sent_at
- error_message (TEXT)
- retry_count
- created_at (Timestamp)
```

---

## ⚙️ Configuration

### Application Properties

**Development (`application.properties`)**
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/pathigai_app
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Mail (Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# JWT
app.jwt.secret=${JWT_SECRET}
app.jwt.access-token-expiration=7200000  # 2 hours
app.jwt.refresh-token-expiration=86400000  # 24 hours

# Session
app.session.max-concurrent-sessions=3
app.session.cleanup-interval-hours=1

# Verification
app.verification.otp-length=6
app.verification.sms-expiry-minutes=2
app.verification.email-expiry-minutes=2
app.verification.max-attempts=3
app.verification.max-resends=3

# reCAPTCHA
app.recaptcha.site-key=${RECAPTCHA_SITE_KEY}
app.recaptcha.secret-key=${RECAPTCHA_SECRET_KEY}
app.recaptcha.enabled=true

# CORS
app.security.cors.allowed-origins=http://localhost:3306,http://localhost:5173,https://pathigai.vercel.app , https://pathigai.app
```

**Production (`application-prod.properties`)**
```properties
# Mail (Mailtrap)
spring.mail.host=live.smtp.mailtrap.io
spring.mail.port=2525
# Username and password via environment variables

# All other configs passed via GitHub Secrets and deployment script
```

---

## Deployment

### DigitalOcean Droplet Setup

**1. Server Specifications**
- OS: Ubuntu 22.04 LTS
- RAM: 2GB minimum
- Storage: 50GB SSD
- Location: Bangalore, India

**2. Install Dependencies**

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 21
sudo apt install openjdk-21-jdk -y

# Install MySQL 8
sudo apt install mysql-server -y
sudo mysql_secure_installation

# Install Nginx
sudo apt install nginx -y
```

**3. Configure MySQL**

```bash
sudo mysql -u root -p

CREATE DATABASE pathigai_app;
CREATE USER 'pathigai_user'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON pathigai_app.* TO 'pathigai_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**4. Deploy Application**

```bash
# Create deployment directory
sudo mkdir -p /home/deploy/pathigai-backend
sudo chown deploy:deploy /home/deploy/pathigai-backend

# Upload JAR (via GitHub Actions or SCP)
scp target/pathigai-0.0.1-SNAPSHOT.jar deploy@your-server:/home/deploy/pathigai-backend/

# Create systemd service
sudo nano /etc/systemd/system/pathigai.service
```

**pathigai.service:**
```ini
[Unit]
Description=Pathigai Spring Boot Application
After=mysql.service

[Service]
User=deploy
ExecStart=/usr/bin/java -jar /home/deploy/pathigai-backend/target/pathigai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable pathigai
sudo systemctl start pathigai
sudo systemctl status pathigai
```

**5. Configure Nginx Reverse Proxy**

```bash
sudo nano /etc/nginx/sites-available/pathigai
```

**nginx configuration:**
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/pathigai /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

**6. SSL with Let's Encrypt**

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

**7. Firewall Configuration**

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw allow 2525/tcp  # Mailtrap SMTP
sudo ufw enable
```

### GitHub Actions CI/CD

**Deployment Script (`deploy-start.sh`)**
```bash
#!/bin/bash
# Automated deployment script
# Accepts environment variables from GitHub Secrets
# Starts application with production profile
```

**GitHub Secrets Required:**
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`
- `MAIL_USERNAME`, `MAIL_PASSWORD`
- `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`
- `FAST2SMS_API_KEY`
- `SSH_PRIVATE_KEY`, `SERVER_IP`

---

## Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Test Coverage Report

```bash
mvn jacoco:report
```

Report available at: `target/site/jacoco/index.html`

---

## Roadmap & Upcoming Features

### 🔄 In Progress
- [ ] User profile update and delete operations
- [ ] User search and filtering

### Planned Features

**Phase 2: Application & Interview Management**
- Application submission workflow
- Interview scheduling system
- Panel assignment
- Feedback collection
- Status tracking

**Phase 3: Training Management**
- Trainee onboarding workflow
- Batch management
- Attendance tracking (Check-in/Check-out)
- Faculty assignment

**Phase 4: Task & Performance**
- Assignment creation and submission
- Contest management
- Seminar scheduling
- Monthly performance evaluation

**Phase 5: Placement Management**
- Student shortlisting
- Placement status tracking
- Offer management

**Phase 6: Advanced Features**
- Real-time notifications (WebSocket)
- Analytics dashboard
- Report generation (PDF/Excel)
- Mentorship session tracking

---

## Contributing

This project is currently in active development. Contributions are welcome from team members.

### Development Workflow

1. Fork the repository
2. Create your feature branch
   ```bash
   git checkout -b feature/Feature-Name
   ```
3. Commit your changes
   ```bash
   git commit -m 'Add some Feature-Name'
   ```
4. Push to the branch
   ```bash
   git push origin feature/Feature-Name
   ```
5. Open a Pull Request to `dev` branch


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contact

**Project Maintainer:** Nivedha PM

- GitHub: [@nivedhapm](https://github.com/nivedhapm)
- Email: nivedha110401@gmail.com

**Project Links:**
- Backend Repository: [pathigai-backend](https://github.com/nivedhapm/pathigai-backend)
- Frontend Repository: [pathigai-frontend](https://github.com/nivedhapm/pathigai-frontend)
- Live Application: [pathigai.vercel.app](https://pathigai.app)
- API Endpoint: [64.227.142.243/api/v1](https://64.227.142.243/api/v1)

---

## Acknowledgments

- Spring Boot Community
- MySQL Development Team
- JWT.io for token implementation
- Mailtrap for email delivery
- Fast2SMS for SMS services
- Google reCAPTCHA for bot protection

---

<div align="center">

**Made for comprehensive training management**

⭐ Star this repo if you find it helpful!

</div>

