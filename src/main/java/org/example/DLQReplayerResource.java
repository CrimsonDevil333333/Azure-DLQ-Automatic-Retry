package org.example;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.SubQueue;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Instant;
import java.time.Duration;
import java.util.List;

@Path("/dlq-replay")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DLQReplayerResource {

    @ConfigProperty(name = "azure.servicebus.connection-string")
    String connectionString;

    @ConfigProperty(name = "azure.servicebus.topic-name")
    String topicName;

    @ConfigProperty(name = "azure.servicebus.subscription-name")
    String subscriptionName;

    @GET
    @Path("/{hoursBack}")
    public Response replayDLQMessages(@PathParam("hoursBack") int hoursBack) {
        if (hoursBack <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid hoursBack value, must be greater than 0\"}")
                    .build();
        }

        Instant thresholdTime = Instant.now().minus(Duration.ofHours(hoursBack));

        try (ServiceBusReceiverClient receiverClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .subQueue(SubQueue.DEAD_LETTER_QUEUE) // Connect to Dead Letter Queue
                .buildClient();
                ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .sender()
                        .topicName(topicName)
                        .buildClient()) {

            List<ServiceBusReceivedMessage> messages = receiverClient.receiveMessages(10000).stream().toList();
            int replayedCount = 0;

            for (ServiceBusReceivedMessage message : messages) {
                Instant enqueuedTime = message.getEnqueuedTime().toInstant();

                if (enqueuedTime.isAfter(thresholdTime)) {
                    // Create a new message and copy the body
                    ServiceBusMessage retriedMessage = new ServiceBusMessage(message.getBody());

                    // Add custom application property
                    retriedMessage.getApplicationProperties().put("x-retried-automatically", "true");

                    // Send back to the original topic
                    senderClient.sendMessage(retriedMessage);
                    receiverClient.complete(message); // Remove from DLQ
                    replayedCount++;
                }
            }

            return Response.ok("{\"message\": \"Replayed " + replayedCount + " messages\"}").build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\": \"Error processing messages: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
