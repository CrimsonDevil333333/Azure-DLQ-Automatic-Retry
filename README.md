# Quarkus Dead Letter Queue (DLQ) Replayer

## Overview
This Quarkus application replays messages from an Azure Service Bus Dead Letter Queue (DLQ) back to the original topic. The application provides a REST endpoint that allows users to specify a time range (in hours) for replaying messages.

## Features
- Connects to an Azure Service Bus topic subscription's DLQ
- Filters messages based on the `enqueuedTime` property
- Resends eligible messages to the original topic
- Deletes successfully replayed messages from the DLQ
- Provides a RESTful API for controlling message replay

## Requirements
- Java 17+
- Quarkus
- Azure Service Bus SDK
- Maven
- An Azure Service Bus instance with a topic and a subscription

## Configuration
The application requires the following configuration properties in `application.properties`:

```properties
azure.servicebus.connection-string=<your-azure-servicebus-connection-string>
azure.servicebus.topic-name=<your-topic-name>
azure.servicebus.subscription-name=<your-subscription-name>
```

## Running the Application
### Locally
1. Set up the required configuration properties in `application.properties`.
2. Build and run the Quarkus application:
   ```sh
   mvn clean package
   java -jar target/quarkus-app/quarkus-run.jar
   ```

### In Development Mode
```sh
mvn quarkus:dev
```

## API Usage
### Endpoint: Replay DLQ Messages
**URL:** `GET /dlq-replay/{hoursBack}`

**Description:** Replays messages from the DLQ that were enqueued within the specified past hours.

**Example Request:**
```sh
curl -X GET "http://localhost:8080/dlq-replay/24" -H "Content-Type: application/json"
```

**Response:**
```json
{
  "message": "Replayed X messages"
}
```

## Error Handling
If an invalid `hoursBack` value is provided, the response will be:
```json
{
  "error": "Invalid hoursBack value, must be greater than 0"
}
```

If an error occurs during message processing, the response will be:
```json
{
  "error": "Error processing messages: <error-details>"
}
```

## Deployment
### Docker
To containerize the application, create a `Dockerfile`:
```dockerfile
FROM quay.io/quarkus/quarkus-micro-image:latest
COPY target/quarkus-app /deployments/
CMD ["java", "-jar", "/deployments/quarkus-run.jar"]
```

Then build and run the container:
```sh
docker build -t dlq-replayer .
docker run -p 8080:8080 -e azure.servicebus.connection-string=<your-connection-string> dlq-replayer
```

## Conclusion
This Quarkus-based service provides an efficient way to replay messages from an Azure Service Bus DLQ, ensuring that messages are not lost and can be reprocessed as needed. 🎯