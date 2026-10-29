# FitMaxxer 

> An open-source calorie and macro tracker designed to help people keep track of their nutrition and achieve their fitness goals.

## Table of Contents
- [About the Project](#-about-the-project)
- [System Architecture](#️-system-architecture)
- [Technology Stack](#️-technology-stack)
- [Core Features & Roadmap](#️-core-features--roadmap)
- [Domain-Driven Design (DDD)](#️-domain-driven-design-ddd)
- [Project Structure](#-project-structure)
- [Getting Started & Installation](#️-getting-started--installation)
- [Privacy & Data Security](#-privacy-data-security)
- [Contributing](#-contributing)
- [License](#-license)

## About the Project
Maintaining a consistent and healthy diet is a major challenge in daily life. **FitMaxxer** is a mobile-first application designed to make tracking daily macros and calories straightforward, intuitive, and efficient. 

The project bridges a robust Java backend with a modern Flutter mobile interface, focusing heavily on simplicity and core tracking capabilities for everyday use.

## System Architecture
This application utilizes a **hybrid client-server architecture**:
1. **The Core Backend:** A Java Spring Boot application that manages business logic, nutrition calculations, and communicates with external food database APIs. Account management and persistent data are designed to be hosted remotely.
2. **The Mobile Frontend:** A cross-platform mobile application built with **Flutter**, communicating with the backend via a REST API.
3. **Local-Client & API Integration:** The client interacts directly with the backend server, which in turn aggregates and queries external food information services.

## Technology Used
- **Backend Core:** Java 21, Spring Boot 3.x, Spring Data JPA
- **Database (Development/Initial):** H2 Database (transitional database for early development, non-production)
- **Frontend UI:** Flutter (Dart)
- **Architecture Pattern:** Domain-Driven Design (DDD)
- **Build Tools:** Gradle (Kotlin DSL)

## Core Features & Roadmap

### Phase 1: MVP
- **Daily Meal & Macro Tracking:** Simple logging system for meals, calories, and macronutrients.
- **Calorie & Macro Calculation:** Automatic estimation based on daily consumption.
- **Food Database Search:** Query food items through the backend API.
- **Barcode Scanner:** Quick-add items via barcode scanning.
- **User Profile & Goals:** Personalized setup that suggests macro targets based on user input.

## Domain-Driven Design (DDD)
To maintain high software engineering standards, the Java backend follows **Domain-Driven Design (DDD)** principles, structured into decoupled layers:
- **Domain Layer:** Contains pure business logic, entities, value objects, and domain services, completely isolated from Spring or infrastructure frameworks.
- **Application Layer:** Orchestrates use cases and application services.
- **Infrastructure Layer:** Handles technical concerns such as database persistence, external API integrations, and Spring Boot REST Controllers.

## Project Structure
```text
FitMaxxer/
├── backend/                  # Spring Boot Java application (Domain-Driven Design)
│   ├── src/main/java/com/pedro/fitmaxxer/
│   │   ├── domain/           # Core business logic & entities
│   │   ├── application/      # Use cases & services
│   │   └── infrastructure/   # DB persistence, controllers, external APIs
│   └── build.gradle.kts
├── mobile/                   # Flutter cross-platform mobile application
│   ├── lib/                  # Dart source code (models, services, views, widgets)
│   └── pubspec.yaml
└── docs/                     # Architecture diagrams and documentation

## Getting Started & Installation
Coming soon.

## Privacy & Data Security
FitMaxxer respects user data privacy. While user accounts and tracking data are securely stored via our hosted infrastructure, nutrition logs and personal goals remain private to the user account.

## Contributing
Contributions, feature suggestions, and architectural feedback are highly welcome! Feel free to open an issue or submit a pull request.

## License
This project is open-source and available under the [MIT License](LICENSE).