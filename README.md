# Votum Backend

Votum is a secure electronic voting system backend built with Spring Boot, designed to facilitate transparent and tamper-proof elections. The system supports user registration with biometric verification, admin-managed elections, ballot creation, candidate management, and secure voting through kiosks.

## Features

- **User Management**: Secure user registration with Aadhaar verification, biometric data storage, and role-based access control.
- **Election Management**: Create and manage elections with configurable start/end dates and status tracking.
- **Ballot System**: Support for multiple ballots per election with customizable selection limits.
- **Candidate Management**: Add candidates to ballots with party affiliations and symbols.
- **Secure Voting**: JWT-based authentication and kiosk-based voting to prevent fraud.
- **Admin Dashboard**: Administrative controls for user approval, election oversight, and system management.
- **Biometric Integration**: Facial recognition and document verification for enhanced security.
- **File Storage**: Secure storage for user photos and Aadhaar documents.

## Tech Stack

- **Framework**: Spring Boot 3.5.10
- **Language**: Java 21
- **Database**: PostgreSQL
- **ORM**: Hibernate/JPA
- **Security**: Spring Security with JWT
- **Build Tool**: Maven
- **Containerization**: Docker
- **Other Libraries**:
  - Lombok for boilerplate reduction
  - JJWT for JWT token management
  - Spring Dotenv for environment variable management

## Architecture Overview

The Votum backend follows a layered architecture pattern with clear separation of concerns:

### Component Connections

1. **Controllers** → **Services**: Controllers handle HTTP requests and delegate business logic to services. For example, `AuthController` uses `UserService` for registration and login operations.

2. **Services** → **Repositories**: Services contain business logic and interact with repositories for data persistence. `UserService` uses `UserRepository` and `UserBiometricsRepository` for user data management.

3. **Services** → **Security Components**: Services integrate with security utilities like `JwtUtil` for token generation and `PasswordEncoder` for password hashing.

4. **Security Layer**: `SecurityConfig` configures Spring Security with JWT authentication via `JwtFilter`. CORS is handled by `CorsConfig`.

5. **Data Flow**: 
   - User requests → Controllers → Services → Repositories → Database
   - Authentication → JwtFilter → JwtUtil → User details from database
   - File uploads → Services → Secure file storage system

6. **Entity Relationships**:
   - `Election` → `Ballot` (One-to-Many)
   - `Ballot` → `Candidate` (One-to-Many)
   - `User` → `Vote` (One-to-Many, with election uniqueness constraint)
   - `User` → `UserBiometrics` (One-to-One)

### Key Workflows

- **User Registration**: Multipart form data → Controller → Service validates and stores user + biometrics + files
- **Authentication**: Credentials → Service → JWT token generation
- **Voting**: Kiosk login → Biometric verification → Vote casting with integrity checks
- **Admin Management**: Admin approval of users → Election creation → Ballot and candidate management

## Detailed File Structure

```
votum-backend/
├── src/
│   ├── main/
│   │   ├── java/com/votum/votum_backend/
│   │   │   ├── Application.java                    # Main Spring Boot application class
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java                 # CORS configuration for cross-origin requests
│   │   │   │   └── SecurityConfig.java             # Spring Security configuration with JWT
│   │   │   ├── controller/
│   │   │   │   ├── AdminController.java            # Admin operations (user approval, elections)
│   │   │   │   ├── AuthController.java             # Authentication endpoints (register/login)
│   │   │   │   ├── KioskController.java            # Kiosk-based voting operations
│   │   │   │   └── UserController.java             # User profile and election access
│   │   │   ├── dto/
│   │   │   │   ├── CreateBallotRequest.java       # Data transfer object for ballot creation
│   │   │   │   ├── CreateCandidateRequest.java     # DTO for candidate creation
│   │   │   │   ├── CreateElectionRequest.java      # DTO for election creation
│   │   │   │   ├── KioskLoginRequest.java          # DTO for kiosk authentication
│   │   │   │   ├── LoginRequest.java               # DTO for user login
│   │   │   │   ├── RegisterRequest.java            # DTO for user registration
│   │   │   │   └── UserProfileResponse.java        # DTO for user profile responses
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java     # Centralized exception handling
│   │   │   ├── model/
│   │   │   │   ├── Admin.java                      # Admin entity model
│   │   │   │   ├── Ballot.java                     # Ballot entity with election relationship
│   │   │   │   ├── Candidate.java                  # Candidate entity with ballot relationship
│   │   │   │   ├── Election.java                   # Election entity with ballot relationships
│   │   │   │   ├── User.java                       # User entity model
│   │   │   │   ├── UserBiometrics.java             # Biometric data entity
│   │   │   │   └── Vote.java                       # Vote entity with integrity constraints
│   │   │   ├── repository/
│   │   │   │   ├── AdminRepository.java            # Data access for admin entities
│   │   │   │   ├── BallotRepository.java           # Data access for ballot entities
│   │   │   │   ├── CandidateRepository.java        # Data access for candidate entities
│   │   │   │   ├── ElectionRepository.java         # Data access for election entities
│   │   │   │   ├── UserBiometricsRepository.java   # Data access for biometric data
│   │   │   │   ├── UserRepository.java             # Data access for user entities
│   │   │   │   └── VoteRepository.java             # Data access for vote entities
│   │   │   ├── security/
│   │   │   │   ├── JwtFilter.java                  # JWT authentication filter
│   │   │   │   └── JwtUtil.java                    # JWT token utilities
│   │   │   └── service/
│   │   │       ├── AdminElectionService.java       # Business logic for election management
│   │   │       ├── AdminService.java               # Business logic for admin operations
│   │   │       ├── KioskService.java               # Business logic for kiosk voting
│   │   │       └── UserService.java                # Business logic for user management
│   │   └── resources/
│   │       ├── application.properties              # Application configuration
│   │       └── application-test.properties         # Test-specific configuration
│   └── test/
│       └── java/com/votum/votum_backend/
│           └── ApplicationTests.java               # Basic application tests
├── secure_storage/                                 # Secure file storage directory
│   ├── aadhaar/                                    # Aadhaar document storage
│   └── photos/                                      # User photo storage
├── target/                                          # Maven build output directory
├── Dockerfile                                       # Docker container configuration
├── docker-compose.yml                               # Docker Compose setup
├── HELP.md                                          # Spring Boot generated help
├── mvnw & mvnw.cmd                                  # Maven wrapper scripts
├── pom.xml                                          # Maven project configuration
└── README.md                                        # Project documentation
```

### Key Components Explained

- **Controllers**: REST API endpoints that receive HTTP requests and return responses. They validate input and delegate to services.
- **Services**: Core business logic layer. Handle complex operations like user registration, vote validation, and file processing.
- **Repositories**: Data access layer using Spring Data JPA. Provide CRUD operations for entities.
- **Models**: JPA entity classes representing database tables with relationships and constraints.
- **DTOs**: Data Transfer Objects for API communication, ensuring clean separation between internal models and external APIs.
- **Security**: JWT-based authentication system with role-based access control (ADMIN/USER).
- **Configuration**: Spring configuration classes for security, CORS, and other framework settings.
- **Exception Handling**: Global exception handler for consistent error responses.

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 12+
- Docker (optional, for containerized deployment)

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Votum-SecurVote/Votum-backend.git
   cd Votum-backend/votum-backend
   ```

2. **Install dependencies**:
   ```bash
   mvn clean install
   ```

3. **Set up environment variables**:
   Create a `.env` file in the project root with the following variables:
   ```env
   DB_PASSWORD=your_postgresql_password
   JWT_SECRET=SuperSecretKeyForEVotingSystem123456
   ```

   For Windows:
   ```cmd
   set DB_PASSWORD=your_postgresql_password
   ```

   For Mac/Linux:
   ```bash
   export DB_PASSWORD=your_postgresql_password
   ```

## Database Setup

The application uses PostgreSQL as the database. Ensure PostgreSQL is running and create the required database and tables.

1. **Create the database**:
   ```sql
   CREATE DATABASE votum;
   ```

2. **Enable pgcrypto extension**:
   ```sql
   CREATE EXTENSION IF NOT EXISTS "pgcrypto";
   ```

3. **Create tables**:

   **Users Table**:
   ```sql
   CREATE TABLE users (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     full_name TEXT NOT NULL,
     email TEXT UNIQUE NOT NULL,
     phone TEXT UNIQUE NOT NULL,
     password_hash TEXT NOT NULL,
     aadhaar_hash TEXT UNIQUE NOT NULL,
     dob DATE NOT NULL,
     gender TEXT,
     address TEXT,
     role VARCHAR(20) DEFAULT 'USER',
     status TEXT CHECK (status IN ('PENDING','APPROVED','REJECTED')) DEFAULT 'PENDING',
     created_at TIMESTAMP DEFAULT NOW(),
     updated_at TIMESTAMP DEFAULT NOW()
   );
   ```

   **User Biometrics Table**:
   ```sql
   CREATE TABLE user_biometrics (
     user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
     face_embedding BYTEA NOT NULL,
     photo_path TEXT,
     aadhaar_pdf_path TEXT
   );
   ```

   **Admins Table**:
   ```sql
   CREATE TABLE admins (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       full_name TEXT NOT NULL,
       email TEXT UNIQUE NOT NULL,
       password_hash TEXT NOT NULL,
       role VARCHAR(20) DEFAULT 'ADMIN',
       created_at TIMESTAMPTZ DEFAULT NOW(),
       updated_at TIMESTAMPTZ DEFAULT NOW()
   );
   ```

   **Elections Table**:
   ```sql
   CREATE TABLE elections (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       title TEXT NOT NULL,
       description TEXT,
       start_date TIMESTAMP NOT NULL,
       end_date TIMESTAMP NOT NULL,
       status VARCHAR(20) DEFAULT 'DRAFT',
       created_at TIMESTAMP DEFAULT NOW()
   );
   ```

   **Ballots Table**:
   ```sql
   CREATE TABLE ballots (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
       title TEXT NOT NULL,
       description TEXT,
       max_selections INTEGER DEFAULT 1,
       status VARCHAR(20) DEFAULT 'DRAFT',
       created_at TIMESTAMP DEFAULT NOW()
   );
   ```

   **Candidates Table**:
   ```sql
   CREATE TABLE candidates (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       ballot_id UUID NOT NULL REFERENCES ballots(id) ON DELETE CASCADE,
       name TEXT NOT NULL,
       party TEXT,
       symbol TEXT,
       created_at TIMESTAMP DEFAULT NOW()
   );
   ```

   **Votes Table**:
   ```sql
   CREATE TABLE votes (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       user_id UUID NOT NULL,
       election_id UUID NOT NULL,
       ballot_id UUID NOT NULL,
       candidate_id UUID NOT NULL,
       voted_at TIMESTAMP,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_vote_user
           FOREIGN KEY (user_id) REFERENCES users(id),
       CONSTRAINT fk_vote_election
           FOREIGN KEY (election_id) REFERENCES elections(id),
       CONSTRAINT fk_vote_ballot
           FOREIGN KEY (ballot_id) REFERENCES ballots(id),
       CONSTRAINT fk_vote_candidate
           FOREIGN KEY (candidate_id) REFERENCES candidates(id),
       CONSTRAINT unique_user_election
           UNIQUE (user_id, election_id)
   );
   ```

4. **Insert default admin user** (optional):
   ```sql
   INSERT INTO admins (
       full_name,
       email,
       password_hash,
       created_at,
       updated_at
   )
   VALUES (
       'System Admin',
       'admin@votum.com',
       '$2a$12$k2vnWiFuqvZ3EqKpFVMzIuK1nSUrL9sAFhFv.squlBuz5jYQfEKpy',
       NOW(),
       NOW()
   );
   ```

## Running the Application

### Option 1: Using Maven
```bash
mvn spring-boot:run
```

### Option 2: Using Docker
1. Build the Docker image:
   ```bash
   docker build -t votum-backend:1.0 .
   ```

2. Run with Docker Compose:
   ```bash
   docker-compose up
   ```

The application will start on `http://localhost:8080`.

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - User login

### User Management
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile

### Admin Operations
- `GET /api/admin/users` - Get all users
- `PUT /api/admin/users/{id}/approve` - Approve user registration
- `POST /api/admin/elections` - Create election
- `GET /api/admin/elections` - Get all elections

### Election Management
- `GET /api/elections` - Get active elections
- `GET /api/elections/{id}/ballots` - Get election ballots

### Voting
- `POST /api/kiosk/login` - Kiosk login
- `POST /api/kiosk/vote` - Cast vote

## Configuration

Key configuration properties in `application.properties`:
- `spring.datasource.url` - Database URL
- `spring.datasource.username` - Database username
- `spring.datasource.password` - Database password
- `jwt.secret` - JWT signing secret
- `jwt.expiration` - JWT token expiration time (ms)
- `file.storage.path` - Path for secure file storage

## Testing

Run tests with Maven:
```bash
mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, please contact the development team or create an issue in the repository.