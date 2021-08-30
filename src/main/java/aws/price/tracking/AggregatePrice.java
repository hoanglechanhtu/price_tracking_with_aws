package aws.price.tracking;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.sql.*;

public class AggregatePrice implements RequestHandler<SQSEvent, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            String url = "jdbc:mysql://price-tracking.chzc0pfh4lgm.ap-southeast-1.rds.amazonaws.com:3306/price_tracking?useSSL=false";
            String username = "admin";
            String password = "Lehuuvolong123!";
            logger.log("Trying to connect");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();

            String query = "INSERT INTO price_log ("
                    + " url,"
                    + " process_at,"
                    + " price) VALUES ("
                    + "?, ?, ?)";

            PreparedStatement preparedStatement = conn.prepareStatement(query);
            sqsEvent.getRecords().forEach(event -> {
                String body = event.getBody();
                JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
                try {
                    preparedStatement.setString(1, jsonObject.get("url").getAsString());
                    preparedStatement.setLong(2, jsonObject.get("date").getAsLong());
                    preparedStatement.setLong(3, jsonObject.get("price").getAsLong());
                    preparedStatement.executeUpdate();
                } catch (SQLException throwables) {
                    logger.log(throwables.getMessage());
                }
            });

            preparedStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
            logger.log("Caught exception: " + e.getMessage());
        }
        return "200 OK";
    }
}
