package aws.price.tracking;


import aws.util.Util;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

// Handler value: example.HandlerCWEvents
public class PullUrl implements RequestHandler<SQSEvent, String> {
    String queueName = "ItemPrice";
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
    final String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        List<SendMessageBatchRequestEntry> itemPrices = new ArrayList<>();
        event.getRecords().forEach(msg -> {
            String body = msg.getBody();
            JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
            try {
                String urlPath = jsonObject.get("url").getAsString();
                URL url = new URL(urlPath);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod(jsonObject.get("method").getAsString());
                con.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                JsonObject itemPrice = new JsonObject();
                long price = Util.getPrice(content.toString());
                itemPrice.addProperty("url", urlPath);
                itemPrice.addProperty("price", price);
                itemPrice.addProperty("date", Instant.now().toEpochMilli());
                itemPrices.add(new SendMessageBatchRequestEntry(UUID.randomUUID().toString(), itemPrice.toString()));
                logger.log("\n"  + jsonObject.get("url").getAsString() + " price = " + price);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        SendMessageBatchRequest send_batch_request = new SendMessageBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(
                        itemPrices);
        sqs.sendMessageBatch(send_batch_request);

        Util.logEnvironment(event, context, gson);



        return "200 OK";
    }
}