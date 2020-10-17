package service;

import org.apache.http.HttpStatus;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.serverless.config.DynamoDBOperations;

/**
 * Handler function to handle the S3 insertion event
 * @author Vinay
 *
 */
public class App implements RequestHandler<S3Event, APIGatewayProxyResponseEvent> {

	private static final Regions CLIENT_REGION = Regions.AP_SOUTH_1; 
    private static final String DYNAMODB_TABLE_SLEEPDATA = "SleepData";
    
    /**
     * Triggered when an insertion of object happens in the S3 Bucket
     */
	@Override
	public APIGatewayProxyResponseEvent handleRequest(S3Event event, Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		S3EventNotification.S3EventNotificationRecord record=event.getRecords().get(0);
		String bucketName = record.getS3().getBucket().getName().toString();
		String objectKey  = record.getS3().getObject().getKey().toString();
		String objectTag = record.getS3().getObject().geteTag().toString();
		Long objectSize = record.getS3().getObject().getSizeAsLong();
		String eventTime = record.getEventTime().toString();
		LambdaLogger logger = context.getLogger();
		logger.log(objectKey);
		String objectID=objectKey.substring(0, 36);
		System.out.println("object ID "+objectID);
		
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);
		GetItemSpec spec = new GetItemSpec()
			    .withPrimaryKey("objectID", objectID);
		Table sleepDataTable = dynamoDB.getTable(DYNAMODB_TABLE_SLEEPDATA);
		Item item = sleepDataTable.getItem(spec);
		item.with("bucketName", bucketName);
		item.with("objectKey", objectKey);
		item.with("objectTag", objectTag);
		item.with("objectSize", objectSize);
		item.with("eventTime", eventTime);
		
		DynamoDBOperations.persistData(DYNAMODB_TABLE_SLEEPDATA, item);
		response.setStatusCode(HttpStatus.SC_OK);
		return response;
	}
	


}
