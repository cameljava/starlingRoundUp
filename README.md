# Starling Bank Round-Up Application

A Spring Boot application that implements the "round-up" functionality for Starling Bank accounts. This service automatically rounds up transactions to the nearest pound and transfers the difference to a savings goal.

## Features

- Retrieves transactions from Starling Bank accounts
- Calculates round-up amounts for each transaction
- Creates and manages savings goals
- Transfers round-up amounts to savings goals
- OpenAPI documentation with Swagger UI

## Technology Stack

- Java 21
- Spring Boot 3.2.12
- Spring Web
- SpringDoc OpenAPI UI
- Spring Dotenv for environment variable management
- Maven for dependency management and build
- Docker support

## Requirements

- Java 21 or higher
- Maven 3.6 or higher
- Starling Bank API Token (sandbox or production)

## Setup

### Environment Variables

Create a `.env` file in the root directory with the following variables with different values for different environments:

```
STARLING_API_TOKEN=your_starling_api_token
STARLING_API_URL=https://api-sandbox.starlingbank.com
SERVER_PORT=8080
LOG_LEVEL=INFO
```

### Building the Application

```bash
./mvnw clean install
```

### Running the Application Locally

```bash
./mvnw spring-boot:run
```

## API Documentation

Once the application is running, you can access the API documentation at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Docker Support

### Building the Docker Image

```bash
docker build -t starling-roundup .
```

### Running the Docker Container

```bash
docker run -d -p 8080:8080 \
  -e STARLING_API_TOKEN=your_token \
  -e STARLING_API_URL=https://api-sandbox.starlingbank.com \
  -e SERVER_PORT=8080 \
  starling-roundup
```

## API Endpoints

|         Endpoint       | Method |                                                 Description                                                  |
|------------------------|--------|--------------------------------------------------------------------------------------------------------------|
| `/api/v2/feed/roundup` |  POST  | Rounds up all transactions from the past week to the nearest pound and transfers the total to a savings goal |

## Project Structure

```
src/main/java/com/example/starling/roundup/
├── config/             # Configuration classes
├── controller/         # REST API controllers
├── exception/          # Custom exceptions and error handlers
├── model/              # Data models and DTOs
├── service/            # Business logic implementation
└── util/               # Utility classes
```

## Contributors

- **Kevin Lee** - Initial work and project setup
  - Email: klee.java@gmail.com
  - GitHub: [starlingRoundUp](https://github.com:cameljava/starlingRoundUp.git)

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.