package aws.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Util {
    public static void logEnvironment(Object event, Context context, Gson gson) {
        LambdaLogger logger = context.getLogger();
        // log execution details
        logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        logger.log("CONTEXT: " + gson.toJson(context));
        // log event details
        logger.log("EVENT: " + gson.toJson(event));
        logger.log("EVENT TYPE: " + event.getClass().toString());
    }
    public static Long getPrice(String content) {
        String pClass = "<p class=\"box-price-present";
        String end = "&#x20AB";
        int startIndex = content.indexOf(pClass);
        int endIndex = startIndex + content.substring(startIndex).indexOf(end);
        String subString = content.substring(startIndex, endIndex);
        Pattern pattern = Pattern.compile("[0-9]*[.]?[0-9]{3}[.][0-9]{3}");
        Matcher matcher = pattern.matcher(subString);
        return matcher.find() ? Long.parseLong(matcher.group(0).replaceAll("\\.", "")) : 0L;
    }
}
