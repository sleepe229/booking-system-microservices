# Hotel Booking Platform

**Hotel Booking Platform** is a microservices-based system for managing hotel reservations.  
It follows an **event-driven architecture (EDA)** and uses **RabbitMQ** for asynchronous communication between services.  
External clients interact with the system via REST and GraphQL APIs exposed by the **Gateway Service**.

---

## Features

* REST and GraphQL APIs for clients, agencies, and mobile apps
* Event-driven microservice communication via RabbitMQ
* Asynchronous booking workflow
* Dedicated audit logging and reporting service
* Scalable and modular architecture
* PostgreSQL-based persistent audit storage

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT (REST/GraphQL)                      │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   Gateway Service       │
                    │  (hotel-service)        │
                    │  - REST API             │
                    │  - GraphQL API          │
                    │  - Validation/Auth      │
                    └────────────┬────────────┘
                                 │
                          Publishes Events
                                 │
                    ┌────────────▼────────────┐
                    │   RabbitMQ Broker       │
                    │  (Message Exchange)     │
                    │  - booking.created      │
                    │  - booking.cancelled    │
                    │  - booking.confirmed    │
                    │  - booking.rejected     │
                    └─┬────────┬──────────┬───┘
                      │        │          │
        ┌─────────────┘        │          └─────────────┐
        │                      │                        │
┌───────▼────────┐    ┌───────▼────────┐    ┌──────────▼─────────┐
│ Audit Service  │    │ Orchestrator   │    │ Notification       │
│ - Logs events  │    │ - Processes    │    │ - Email            │
│ - CSV export   │    │   bookings     │    └────────────────────┘
│ - Reports      │    │ - gRPC client  │    
└────────────────┘    └────────┬───────┘
                               │
                               │ gRPC Call
                               │
                    ┌──────────▼───────────┐
                    │ Discount Service     │
                    │ - gRPC Server        │
                    │ - Calculate discounts│
                    │ - Analytics stub     │
                    └──────────────────────┘
```

---

## Services Overview

| Service                          | Status       | Description                                                |
| -------------------------------- | ------------- | ---------------------------------------------------------- |
| **Gateway Service** (`hotel`)    | Implemented  | REST/GraphQL API for clients; publishes events to RabbitMQ |
| **RabbitMQ Broker**              | Implemented  | Central message bus for event distribution                 |
| **Audit Service** (`audit`)      | Implemented  | Subscribes to events and stores logs/reports in PostgreSQL |
| **PostgreSQL (Audit DB)**        | Implemented  | Persistent audit log storage                               |
| **Booking Orchestrator**         | Planned      | Business logic orchestration for booking workflow          |
| **Discount / Analytics Service** | Planned      | gRPC service for discounts and analytics                   |
| **Notification Service**         | Planned      | Handles client notifications (email/SMS/push)              |

---


---

## Technology Stack

| Layer                       | Technology             |
| ---------------------------- | ---------------------- |
| API Layer                   | REST / GraphQL         |
| Messaging                   | RabbitMQ               |
| Database                    | PostgreSQL             |
| Inter-Service Communication | gRPC (planned)         |
| Serialization               | JSON / Protobuf        |
| Infrastructure              | Docker, Docker Compose |
| Language                    | Java (Spring Boot)     |

---

## Getting Started

### Prerequisites

* Docker 24+
* Docker Compose 2+

No external dependencies are required — RabbitMQ and PostgreSQL are automatically started via Docker Compose.

---

### Environment Configuration

Create an `.env` file from the provided example:

```bash
cp .env.example .env

Example configuration:

RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
DB_URL=jdbc:postgresql://postgres-audit:5432/auditdb
DB_USER=audit_user
DB_PASS=audit_pass
GATEWAY_PORT=8080
AUDIT_PORT=8082
```

### Run the Platform

To start all core services (Gateway, RabbitMQ, PostgreSQL, Audit):

```bash
docker-compose up -d
```

You can verify the status:

```bash
docker ps
```


Available endpoints after startup:

| Component               | URL                            | Description                      |
|--------------------------|---------------------------------|----------------------------------|
| Gateway API              | http://localhost:8080          | Main entrypoint for clients      |
| RabbitMQ Management UI   | http://localhost:15672         | Broker dashboard (guest/guest)   |
| Audit Service            | http://localhost:8082          | Logs and reporting service       |
| PostgreSQL (Audit DB)    | localhost:5432                 | Internal audit database          |

To stop:

```bash
docker-compose down
```

To stop and remove all data (including volumes):

```bash
docker-compose down -v
```

---

## Development

Each service can be developed and run independently.

Example: running the **Gateway Service**

```bash
cd hotel
docker build -t hotel-service .
docker run -p 8080:8080 --env-file ../.env hotel-service
```

Running **Audit Service**

```bash
cd hotel-audit-service
docker build -t audit-service .
docker run -p 8082:8082 --env-file ../.env audit-service
```

Run a standalone **RabbitMQ instance**:

```bash
docker run -d --hostname rabbitmq --name rabbit \
  -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](./LICENSE) file for details.


---
