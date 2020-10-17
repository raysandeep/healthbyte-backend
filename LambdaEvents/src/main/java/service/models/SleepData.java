package service.models;

import java.util.UUID;

import com.amazonaws.services.dynamodbv2.document.Item;

public class SleepData {

	private String objectID;
	private String user_id;
	private String bucketName;
	private String objectKey;
	private String objectTag;
	private Long objectSize;
	private String eventTime;
	private String format;
	private boolean uploadedOnServer;
	private int duration;
	private String startTimeStamp;
	private String endTimeStamp;
	private String session_id;

	public SleepData() {
		this.objectID = UUID.randomUUID().toString();
	}

	
	public SleepData(String user_id,String format,String startTimeStamp,String endTimeStamp,int duration,String session_id) {
		super();
		this.objectID = UUID.randomUUID().toString();
		this.user_id = user_id;
		this.format=format;
		this.startTimeStamp=startTimeStamp;
		this.endTimeStamp = endTimeStamp;
		this.duration=duration;
		this.session_id = session_id;
	}


	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	
	public String getUser_id() {
		return user_id;
	}


	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}


	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getObjectKey() {
		return objectKey;
	}

	public void setObjectKey(String objectKey) {
		this.objectKey = objectKey;
	}

	public String getObjectTag() {
		return objectTag;
	}

	public void setObjectTag(String objectTag) {
		this.objectTag = objectTag;
	}

	public Long getObjectSize() {
		return objectSize;
	}

	public void setObjectSize(Long objectSize) {
		this.objectSize = objectSize;
	}

	public String getEventTime() {
		return eventTime;
	}

	public void setEventTime(String eventTime) {
		this.eventTime = eventTime;
	}
	

	public String getFormat() {
		return format;
	}


	public void setFormat(String format) {
		this.format = format;
	}


	public boolean isUploadedOnServer() {
		return uploadedOnServer;
	}


	public void setUploadedOnServer(boolean uploadedOnServer) {
		this.uploadedOnServer = uploadedOnServer;
	}


	public int getDuration() {
		return duration;
	}


	public void setDuration(int duration) {
		this.duration = duration;
	}


	public String getStartTimeStamp() {
		return startTimeStamp;
	}


	public void setStartTimeStamp(String startTimeStamp) {
		this.startTimeStamp = startTimeStamp;
	}


	public String getEndTimeStamp() {
		return endTimeStamp;
	}


	public void setEndTimeStamp(String endTimeStamp) {
		this.endTimeStamp = endTimeStamp;
	}


	public String getSession_id() {
		return session_id;
	}


	public void setSession_id(String session_id) {
		this.session_id = session_id;
	}


	public Item getItem() {
		Item item = new Item();
		item.with("objectID", this.objectID);
		item.with("user_id", this.user_id);
		item.with("format", this.format);
		item.with("startTimeStamp", this.startTimeStamp);
		item.with("endTimeStamp", this.endTimeStamp);
		item.with("duration", this.duration);
		item.with("session_id", this.session_id);
		return item;
	}
}
