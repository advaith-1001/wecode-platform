# WeCode - Collaborative Editor Backend

[![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?&style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)

This repository contains the backend services for **WeCode**, a real-time collaborative code editor and execution platform. It features a decoupled, microservice-based architecture designed for scalability, security, and resilience.
Frontend Repo: [React Frontend Repo](https://github.com/advaith-1001/wecode)

---

## ## Key Features ✨

* **Real-Time Sync**: Manages collaborative sessions using a **Spring Boot WebSocket (STOMP)** server to relay Yjs CRDT updates between clients.
* **Asynchronous Job Processing**: Utilizes **Redis** as a robust message queue to decouple API requests from the actual code execution, ensuring a responsive API.
* **Secure Sandboxed Execution**: Leverages a **Node.js worker** to run untrusted code in isolated, resource-limited Docker containers for multiple languages.
* **Persistent Room State**: Uses Redis to store the last execution result for each room, allowing new users to join and see the latest state.

---

## ## Tech Stack 🛠️

* **API & WebSocket Gateway**: Spring Boot, Spring WebSockets (Java)
* **Code Execution Engine**: Node.js, Dockerode
* **Message Queue & Cache**: Redis
* **Containerization**: Docker, Docker Compose

---

## ## System Architecture 🏗️

The backend consists of three core services running in Docker containers, orchestrated by Docker Compose.

+------------------+      +------------------------+      +-------------------+
|      Client      |----->| Spring Boot App (API)  |<---->|       Redis       |
| (React Frontend) |      | (WebSocket Gateway)    |      | (Queue & Storage) |
+------------------+      +------------------------+      +-------------------+
       ^     |                       |                           ^
       |     | WebSocket Sync        |                           | Job Queue
       |     |                       |                           |
       +-----+                       |                      +----v----+
                                     |                      | Node.js |
                                     +--------------------->| Worker  |
                                      (Docker Socket Mount) +---------+
                                                               |
                                                               | Controls
                                                               v
                                                      +---------------------+
                                                      | Ephemeral Docker    |
                                                      | Execution Container |
                                                      +---------------------+

### ### Workflow 1: Real-Time Collaboration

1.  A client connects to the **Spring Boot App** via a WebSocket.
2.  When a user types, a Yjs update is sent over the WebSocket to a `/app/room/sync/{roomId}` destination.
3.  The Spring Boot controller receives this update and immediately broadcasts it to all other clients subscribed to the `/topic/room/sync/{roomId}` topic.

### ### Workflow 2: Code Execution

1.  A client sends a `POST` request to the `/api/run` endpoint on the **Spring Boot App**.
2.  The controller generates a unique `jobId`, saves the job's initial state and `roomId` to a Redis Hash, and pushes the job payload into a Redis List acting as a queue (`code-queue`).
3.  The API immediately responds with `202 Accepted`, making the frontend feel fast.
4.  The **Node.js Worker**, which is constantly listening to the queue, picks up the job.
5.  The worker creates a temporary local directory and uses the mounted Docker Socket (`/var/run/docker.sock`) to command the host's Docker daemon.
6.  A new, isolated Docker container is created for the specified language (e.g., `python:3.9-slim`). The code is mounted into this container.
7.  The code runs inside the sandbox. The worker captures the output (`stdout`/`stderr`).
8.  The worker updates the job's status and output in the corresponding Redis Hash.
9.  The worker publishes the `jobId` to a Redis Pub/Sub channel (`job-updates`).
10. The Spring Boot app, subscribed to this channel, receives the notification and pushes the final result to all clients in the room via WebSocket.

---

## ## Getting Started

### ### Prerequisites

* [Git](https://git-scm.com/)
* [Docker](https://www.docker.com/products/docker-desktop/)
* [Docker Compose](https://docs.docker.com/compose/install/)

### ### Local Setup

1.  **Clone the repository:**
    ```sh
    git clone [https://github.com/your-username/your-backend-repo.git](https://github.com/your-username/your-backend-repo.git)
    cd your-backend-repo
    ```

2.  **Create the temporary worker directory:**
    This directory is required for the worker to share code files with the execution containers.
    ```sh
    mkdir worker-tmp
    ```

3.  **Build and run the services:**
    This command will build the images and start the Spring Boot, Node.js, and Redis containers. The `-d` flag runs them in the background.
    ```sh
    docker-compose up --build -d
    ```

4.  **Verify the services are running:**
    ```sh
    docker-compose ps
    ```
    You should see all three services (`redis`, `spring-boot-app`, `worker`) with a "running" status. The Spring Boot API will be accessible at `http://localhost:8080`.

---

## ## 
