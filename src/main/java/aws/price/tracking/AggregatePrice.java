package aws.price.tracking;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class AggregatePrice implements RequestHandler<SQSEvent, String> {
    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Invoked JDBCSample.getCurrentTime");

        String currentTime = "unavailable";

        // Get time from DB server
        try {
            String url = "jdbc:mysql://price-tracking.chzc0pfh4lgm.ap-southeast-1.rds.amazonaws.com:3306/price_tracking";
            String username = "admin";
            String password = "*****!";

            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT NOW()");

            if (resultSet.next()) {
                currentTime = resultSet.getObject(1).toString();
            }

            logger.log("Successfully executed query.  Result: " + currentTime);

        } catch (Exception e) {
            e.printStackTrace();
            logger.log("Caught exception: " + e.getMessage());
        }

        return currentTime;
    }
}
