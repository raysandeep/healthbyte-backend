package com.serverless.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class DynamoDBOperations {

	public static PutItemOutcome persistData(final String DYNAMODB_TABLE_NAME, Item item) 
		      throws ConditionalCheckFailedException {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);

		Table table = dynamoDB.getTable(DYNAMODB_TABLE_NAME);
		System.out.println(item.toJSON());
		PutItemOutcome outcome = table.putItem(item);
		        return outcome;
		            
		 }
}
