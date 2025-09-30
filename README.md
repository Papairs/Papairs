# Papairs Project

A simple full-stack application with Vue.js frontend and Spring Boot microservices backend.

## Project Structure

```
Papairs/
├── frontend/                 # Vue.js frontend with Tailwind CSS
├── backend/
│   ├── auth-service/        # Authentication service (Port 8081)
│   └── docs-service/        # Documentation service (Port 8082)
├── start-all.bat           # Windows batch script to run all services
├── start-all.ps1           # PowerShell script to run all services
└── README.md              # This file
```

## Prerequisites

- **Node.js** (v16 or higher) - for Vue.js frontend
- **Java 17** - for Spring Boot backends
- **Maven** - for building Spring Boot applications

## Quick Start

### Option 1: Use the startup script (Recommended)

Double-click `start-all.bat` or run in PowerShell:
```powershell
.\start-all.ps1
```

### Option 2: Manual startup

1. **Start Auth Service:**
   ```bash
   cd backend/auth-service
   mvn spring-boot:run
   ```

2. **Start Docs Service:**
   ```bash
   cd backend/docs-service  
   mvn spring-boot:run
   ```

3. **Start Frontend:**
   ```bash
   cd frontend
   npm install  # Only needed first time
   npm run serve
   ```

## Access Points

- **Frontend:** http://localhost:3000
- **Auth Service:** http://localhost:8081
- **Docs Service:** http://localhost:8082

## Services Overview

### Frontend (Vue.js + Tailwind CSS)
- Port: 3000
- Simple responsive UI with navigation
- Home page with service testing buttons
- Documentation page displaying mock data

### Auth Service (Spring Boot)
- Port: 8081
- Endpoints:
  - `GET /api/auth/health` - Health check
  - `POST /api/auth/login` - User login (use username: "test", password: "test")
  - `POST /api/auth/register` - User registration
  - `GET /api/auth/validate` - Token validation

### Docs Service (Spring Boot)  
- Port: 8082
- Endpoints:
  - `GET /api/docs/health` - Health check
  - `GET /api/docs/all` - Get all documents
  - `GET /api/docs/{id}` - Get specific document
  - `POST /api/docs` - Create new document
  - `PUT /api/docs/{id}` - Update document
  - `DELETE /api/docs/{id}` - Delete document

## Development

### Frontend Development
```bash
cd frontend
npm install
npm run serve    # Development server
npm run build    # Production build
npm run lint     # Lint code
```

### Backend Development
Each service can be run independently:
```bash
cd backend/auth-service  # or docs-service
mvn clean install
mvn spring-boot:run
```

## Technologies Used

### Frontend
- Vue.js 3
- Vue Router 4
- Tailwind CSS 3
- Axios for HTTP requests

### Backend
- Spring Boot 3.1.3
- Spring Web
- Spring Data JPA
- H2 Database (in-memory)
- Maven

## Features

- ✅ Responsive UI with Tailwind CSS
- ✅ Microservices architecture
- ✅ CORS configured for frontend-backend communication
- ✅ Mock authentication system
- ✅ Document management system
- ✅ Health check endpoints
- ✅ Easy startup with scripts

## Next Steps

1. Replace mock authentication with real JWT implementation
2. Add persistent database (PostgreSQL/MySQL)
3. Add user management features
4. Implement real document CRUD operations
5. Add API documentation with Swagger
6. Add unit and integration tests
7. Containerize with Docker