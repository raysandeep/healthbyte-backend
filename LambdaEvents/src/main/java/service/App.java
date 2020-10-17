package service;

import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.serverless.config.DynamoDBOperations;

import service.models.SleepData;
import service.models.SleepDataAnalysis;

/**
 * Each method is a Lambda function
 * 
 * @author Vinay
 *
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private static final Regions CLIENT_REGION = Regions.AP_SOUTH_1;
	private static final String DYNAMODB_TABLE_SLEEPDATA = "SleepData";
	private static final String DYNAMODB_TABLE_SLEEPDATA_ANALYSIS = "SleepDataAnalysis";
	private static final String DYNAMODB_TABLE_USER = "User";
	private static final String BUCKET_NAME = "healthbyte";

	/**
	 * Generate PreSigned URL To get the S3 Object
	 */
	public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
		LambdaLogger logger = context.getLogger();
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

		String key = input.getQueryStringParameters().get("key");
		response.setStatusCode(HttpStatus.SC_OK);
		try {
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(CLIENT_REGION).build();

			// Set the pre-signed URL to expire after one hour.
			java.util.Date expiration = new java.util.Date();
			long expTimeMillis = expiration.getTime();
			expTimeMillis += 1000 * 60 * 60;
			expiration.setTime(expTimeMillis);

			// Generate the pre-signed URL.
			System.out.println("Generating pre-signed URL.");
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(BUCKET_NAME, key)
					.withMethod(HttpMethod.GET).withExpiration(expiration);
			URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
			Map<String, String> responseBody = new HashMap<String, String>();
			responseBody.put("URL", url.toString());
			String responseBodyString = new JSONObject(responseBody).toString();
			response.setBody(responseBodyString);
			return response;
		} catch (SdkClientException e) {
			logger.log(e.getMessage());
		}
		response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		return response;
	}

	/**
	 * Obtain Pre-Signed URL for inserting and S3 object of format="format"
	 * 
	 * @param input
	 * @param context
	 * @return
	 */
	public APIGatewayProxyResponseEvent getPresignedUrl(final APIGatewayProxyRequestEvent input,
			final Context context) {
		LambdaLogger logger = context.getLogger();
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);

		// Get the user entry
		String body = input.getBody();
		logger.log(body);
		JSONObject payload = new JSONObject(body);

		try {
			String user_id = payload.getString("userID");
			String session_id = payload.getString("session_id");
			Table userTable = dynamoDB.getTable(DYNAMODB_TABLE_USER);

			GetItemSpec spec = new GetItemSpec().withPrimaryKey("user_id", user_id);
			Item item = userTable.getItem(spec);

			if (item == null) {
				Map<String, String> responseBody = new HashMap<String, String>();
				responseBody.put("message", "User doesn't exist");
				String responseBodyString = new JSONObject(responseBody).toString();
				response.setBody(responseBodyString);
				response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				return response;
			}

			JSONObject resourceInfo = payload.getJSONObject("resourceInfo");
			String format = resourceInfo.getString("format");
			String startTimeStamp = resourceInfo.getString("startTimeStamp");
			String endTimeStamp = resourceInfo.getString("endTimeStamp");
			int duration = resourceInfo.getInt("duration");

			SleepData sleepData = new SleepData(user_id, format, startTimeStamp, endTimeStamp, duration, session_id);
			String objectKey = sleepData.getObjectID() + "." + format;
			logger.log("inserting sleep data");
			// Insert SleepData Entry
			Item sleepDataItem = sleepData.getItem();
			DynamoDBOperations.persistData(DYNAMODB_TABLE_SLEEPDATA, sleepDataItem);
			logger.log("inserted sleep data");

			JSONArray resources = item.hasAttribute("resources") ? new JSONArray(item.getJSON("resources"))
					: new JSONArray();
			JSONObject resourceDetails = new JSONObject();
			resourceDetails.put("objectID", sleepData.getObjectID());
			resourceDetails.put("format", sleepData.getFormat());
			resources.put(resourceDetails);
			item.with("resources", resources.toList());
			DynamoDBOperations.persistData(DYNAMODB_TABLE_USER, item);

			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(CLIENT_REGION).build();

			// Set the pre-signed URL to expire after one hour.
			java.util.Date expiration = new java.util.Date();
			long expTimeMillis = expiration.getTime();
			expTimeMillis += 1000 * 60 * 60;
			expiration.setTime(expTimeMillis);

			// Generate the pre-signed URL.
			System.out.println("Generating pre-signed URL.");
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(BUCKET_NAME,
					objectKey).withMethod(HttpMethod.PUT).withExpiration(expiration);
			URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
			Map<String, String> responseBody = new HashMap<String, String>();
			responseBody.put("URL", url.toString());
			String responseBodyString = new JSONObject(responseBody).toString();
			response.setBody(responseBodyString);
			response.setStatusCode(HttpStatus.SC_OK);
			return response;
		} catch (SdkClientException | JSONException e) {
			logger.log(e.getMessage());
			Map<String, String> responseBody = new HashMap<String, String>();
			responseBody.put("message", e.getMessage());
			String responseBodyString = new JSONObject(responseBody).toString();
			response.setBody(responseBodyString);
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return response;
		}
	}

	public APIGatewayProxyResponseEvent addUser(final APIGatewayProxyRequestEvent input, final Context context) {

		LambdaLogger logger = context.getLogger();
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		try {
			String body = input.getBody();
			JSONObject payload = new JSONObject(body);
			Item item = new Item();
			String user_id = UUID.randomUUID().toString();
			String email = payload.has("email") ? payload.getString("email") : "";
			AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
			DynamoDB dynamoDB = new DynamoDB(client);
			Table userTable = dynamoDB.getTable(DYNAMODB_TABLE_USER);

			HashMap<String, String> nameMap = new HashMap<String, String>();
			nameMap.put("#em", "email");

			HashMap<String, Object> valueMap = new HashMap<String, Object>();
			valueMap.put(":uemail", email);
			ScanSpec scanSpec = new ScanSpec().withProjectionExpression("user_id,email")
					.withFilterExpression("#em = :uemail").withNameMap(nameMap).withValueMap(valueMap);

			ItemCollection<ScanOutcome> items = userTable.scan(scanSpec);

			if (items.iterator().hasNext()) {
				Item userItem = items.iterator().next();
				Map<String, String> responseBody = new HashMap<String, String>();
				responseBody.put("message", "Email already exists");
				responseBody.put("user_id", userItem.getString("user_id"));
				String responseBodyString = new JSONObject(responseBody).toString();
				response.setBody(responseBodyString);
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				return response;
			}

			String phone = payload.has("phone") ? payload.getString("phone") : "";
			int age = payload.getInt("age");
			String first_name = payload.has("first_name") ? payload.getString("first_name") : "";
			String last_name = payload.has("last_name") ? payload.getString("last_name") : "";
			String creationDate = new Date().toString();
			item.with("user_id", user_id);
			item.with("email", email);
			item.with("phone", phone);
			item.with("age", age);
			item.with("first_name", first_name);
			item.with("last_name", last_name);
			item.with("created_on", creationDate);
			DynamoDBOperations.persistData(DYNAMODB_TABLE_USER, item);
			Map<String, String> responseBody = new HashMap<String, String>();
			responseBody.put("message", "Created User");
			responseBody.put("user_id", user_id);
			String responseBodyString = new JSONObject(responseBody).toString();
			response.setBody(responseBodyString);
			response.setStatusCode(HttpStatus.SC_OK);
			return response;
		} catch (ConditionalCheckFailedException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(e.getMessage());
			Map<String, String> responseBody = new HashMap<String, String>();
			responseBody.put("message", e.getMessage());
			String responseBodyString = new JSONObject(responseBody).toString();
			response.setBody(responseBodyString);
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return response;
		}

	}

	public APIGatewayProxyResponseEvent addSleepDataAnlysis(final APIGatewayProxyRequestEvent input,
			final Context context) {

		LambdaLogger logger = context.getLogger();
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		try {
			String body = input.getBody();
			JSONObject payload = new JSONObject(body);
			SleepDataAnalysis sleepDataAnalysis = new SleepDataAnalysis();
			sleepDataAnalysis.setUser_id(payload.getString("user_id"));
			sleepDataAnalysis.setObjectID(payload.getString("objectID"));
			sleepDataAnalysis.setSession_id(payload.getString("session_id"));
			sleepDataAnalysis.setFormat(payload.getString("format"));
			sleepDataAnalysis.setFrequency_shift(payload.getString("frequency_shift"));
			sleepDataAnalysis.setAnalysis_image_1(payload.getString("analysis_image_1"));
			sleepDataAnalysis.setAnalysis_image_2(payload.getString("analysis_image_2"));
			sleepDataAnalysis.setMax_frequency(payload.getString("max_frequency"));
			sleepDataAnalysis.setMin_frequency(payload.getString("min_frequency"));
			sleepDataAnalysis.setSleep_apnea(payload.getString("sleep_apnea"));
			sleepDataAnalysis.setSleep_type(payload.getString("sleep_quality"));
			sleepDataAnalysis.setSampling_rate(payload.getString("sampling_rate"));

			Item item = sleepDataAnalysis.getItem();
			DynamoDBOperations.persistData(DYNAMODB_TABLE_SLEEPDATA_ANALYSIS, item);
			Map<String, String> responseBody = new HashMap<String, String>();
			responseBody.put("message", "Added analysis");
			String responseBodyString = new JSONObject(responseBody).toString();
			response.setBody(responseBodyString);
			response.setStatusCode(HttpStatus.SC_OK);

			return response;
		} catch (SdkClientException | JSONException e) {
			e.printStackTrace();
			logger.log(e.getMessage());
			Map<String, String> responseBody = new HashMap<String, String>();
			responseBody.put("message", e.getMessage());
			String responseBodyString = new JSONObject(responseBody).toString();
			response.setBody(responseBodyString);
			response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return response;
		}
	}

	public APIGatewayProxyResponseEvent getSleepDataAnalysis(final APIGatewayProxyRequestEvent input,
			final Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);
		String objectID = input.getQueryStringParameters().get("objectID");
		Table sleepDataAnalysisTable = dynamoDB.getTable(DYNAMODB_TABLE_SLEEPDATA_ANALYSIS);

		GetItemSpec spec = new GetItemSpec().withPrimaryKey("objectID", objectID);
		Item item = sleepDataAnalysisTable.getItem(spec);
		response.setBody(new JSONObject(item.toJSON()).toString());
		response.setStatusCode(HttpStatus.SC_OK);
		return response;

	}

	/**
	 * Obtain the sleep data from the DB
	 * 
	 * @param input
	 * @param context
	 * @return
	 */
	public APIGatewayProxyResponseEvent getSleepData(final APIGatewayProxyRequestEvent input, final Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		JSONArray data = getData(DYNAMODB_TABLE_SLEEPDATA);
		response.setBody(data.toString());
		response.setStatusCode(HttpStatus.SC_OK);
		return response;

	}

	public APIGatewayProxyResponseEvent getAllUserData(final APIGatewayProxyRequestEvent input, final Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		JSONArray data = getData(DYNAMODB_TABLE_USER);
		System.out.println(data.toString());
		response.setBody(data.toString());
		response.setStatusCode(HttpStatus.SC_OK);
		return response;
	}

	public APIGatewayProxyResponseEvent getUserData(final APIGatewayProxyRequestEvent input, final Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);
		Table userTable = dynamoDB.getTable(DYNAMODB_TABLE_USER);
		Table sleepDataTable = dynamoDB.getTable(DYNAMODB_TABLE_SLEEPDATA);

		ScanSpec scanSpec = getFilterExpression(input.getQueryStringParameters());

		ItemCollection<ScanOutcome> items = userTable.scan(scanSpec);
		
		Iterator<Item> itr = items.iterator();
		
		JSONArray resultList= new JSONArray();
		while(itr.hasNext()) {
			Item item = itr.next();
			if(item.hasAttribute("resources")) {
				JSONArray res = new JSONArray(item.getJSON("resources"));
				JSONArray resources = new JSONArray();
				for (int i = 0; i < res.length(); i++) {
					String objectID = res.getJSONObject(i).getString("objectID");
					GetItemSpec resourceSpec = new GetItemSpec().withPrimaryKey("objectID", objectID);
					Item sleepDataItem = sleepDataTable.getItem(resourceSpec);
					resources.put(new JSONObject(sleepDataItem.toJSON()));
				}
				item.with("resources", resources.toList());
			}
			resultList.put(new JSONObject(item.toJSON()));
		}
		
		
		response.setBody(resultList.toString());
		response.setStatusCode(HttpStatus.SC_OK);
		return response;
	}

	private ScanSpec getFilterExpression(Map<String,String> queryParams) {
		String expr="";
		HashMap<String, String> nameMap = new HashMap<String, String>();
		HashMap<String, Object> valueMap = new HashMap<String, Object>();
		try {
			Iterator keysIterator = queryParams.keySet().iterator();
			int counter =1;
			while(keysIterator.hasNext()) {
				String key = keysIterator.next().toString();
				nameMap.put("#"+key, key);
				valueMap.put(":"+counter,queryParams.get(key));
				expr+="#"+key+" = :"+counter+" and ";
				counter++;
			}
			
			expr = (counter!=1)?expr.substring(0,expr.length()-4):expr;
			System.out.println(expr);
			System.out.println(new JSONObject(nameMap).toString());
			System.out.println(new JSONObject(valueMap).toString());
			
			ScanSpec scanSpec = new ScanSpec()
					.withFilterExpression(expr).withNameMap(nameMap).withValueMap(valueMap);
			
			return scanSpec;
		} catch (Exception e) {
			ScanSpec scanSpec = new ScanSpec();
			return scanSpec;
		}
	}

	private static JSONArray getData(String tableName) {
		JSONArray list = new JSONArray();
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);
		Table table = dynamoDB.getTable(tableName);
		ItemCollection<ScanOutcome> items = table.scan();

		Iterator<Item> iterator = items.iterator();
		while (iterator.hasNext()) {
			Item item = iterator.next();
			JSONObject data = new JSONObject(item.toJSON());
			list.put(data);
		}
		return list;
	}

	private String decodeString(String encodedString) {
		byte[] byteArray = Base64.decodeBase64(encodedString.getBytes());
		return new String(byteArray);
	}

}
