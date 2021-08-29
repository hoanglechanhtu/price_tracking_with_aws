package aws.price.tracking;


import aws.util.Util;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.xspec.S;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

// Handler value: example.HandlerCWEvents
public class PushUrl implements RequestHandler<ScheduledEvent, String> {
    String tableName = "url_pool";
    String queueName = "UrlPoolQueue.fifo";

    final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    final DynamoDB dynamoDB = new DynamoDB(client);
    final Table table = dynamoDB.getTable(tableName);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
    final String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        Map<String, Object> expressionAttributeValues = new HashMap<>();
        long time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000;
        expressionAttributeValues.put(":time", time);
        ItemCollection<ScanOutcome> items = table.scan("next_process_time < :time",
                null,
                null,
                expressionAttributeValues
        );
        logger.log("Scan of " + tableName + " for items with a next process time less than " + time);
        List<SendMessageBatchRequestEntry> bodies = new ArrayList<>();
        int i = 0;
        for (Item item : items) {
            String itemName = item.getString("url").substring(35);
            bodies.add(i, new SendMessageBatchRequestEntry(itemName, item.toJSON()).withMessageGroupId(itemName));
            logger.log(item.toJSONPretty());
            i++;
        }

        SendMessageBatchRequest send_batch_request = new SendMessageBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(
                       bodies);
        sqs.sendMessageBatch(send_batch_request);


        String response = "200 OK";
        // log execution details
        Util.logEnvironment(event, context, gson);
        return response;
    }
}