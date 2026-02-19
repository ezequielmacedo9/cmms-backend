# Industrial CMMS – Cloud-Based Maintenance Management System

## Overview

This project is a full-stack Computerized Maintenance Management System (CMMS) designed for industrial machine lifecycle management.

The backend is fully deployed to the cloud and follows enterprise-level architecture principles using Spring Boot and JPA/Hibernate.

---

## Tech Stack

### Backend
- Java 17+
- Spring Boot
- Spring Web (REST API)
- Spring Data JPA
- Hibernate
- MySQL
- Maven
- JWT Authentication

### Frontend (In Progress)
- Angular
- Angular Material
- TypeScript

### DevOps
- Railway (Cloud Deployment)
- Git & GitHub

---

## Architecture

Layered Architecture:

Controller → Service → Repository

- DTO Pattern
- Dependency Injection (IoC)
- RESTful API Design
- Cloud Deployment

---

## Features

- Machine registration
- Maintenance order management
- Inventory control
- JWT authentication system
- Cloud-hosted backend

---

## How to Run Locally

1. Clone the repository:
   git clone https://github.com/ezequielmacedo9/cmms-backend.git

2. Configure application.properties with your database credentials

3. Run:
   ./mvnw spring-boot:run

---

## Author

Ezequiel Macedo  
Full Stack Java Developer
