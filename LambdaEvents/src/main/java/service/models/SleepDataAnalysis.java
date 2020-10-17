
package service.models;

import com.amazonaws.services.dynamodbv2.document.Item;

public class SleepDataAnalysis {

	private String objectID;
	private String user_id;
	private String session_id;
	private String frequency_shift;
	private String max_frequency;
	private String min_frequency;
	private String sampling_rate;
	private String format;
	private String analysis_image_1;
	private String analysis_image_2;
	private String sleep_apnea;
	private String sleep_type;

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

	public String getFrequency_shift() {
		return frequency_shift;
	}

	public void setFrequency_shift(String frequency_shift) {
		this.frequency_shift = frequency_shift;
	}

	public String getMax_frequency() {
		return max_frequency;
	}

	public void setMax_frequency(String max_frequency) {
		this.max_frequency = max_frequency;
	}

	public String getMin_frequency() {
		return min_frequency;
	}

	public void setMin_frequency(String min_frequency) {
		this.min_frequency = min_frequency;
	}

	public String getSampling_rate() {
		return sampling_rate;
	}

	public void setSampling_rate(String sampling_rate) {
		this.sampling_rate = sampling_rate;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getAnalysis_image_1() {
		return analysis_image_1;
	}

	public void setAnalysis_image_1(String analysis_image_1) {
		this.analysis_image_1 = analysis_image_1;
	}

	public String getSleep_apnea() {
		return sleep_apnea;
	}

	public void setSleep_apnea(String sleep_apnea) {
		this.sleep_apnea = sleep_apnea;
	}

	public String getSleep_type() {
		return sleep_type;
	}

	public void setSleep_type(String sleep_type) {
		this.sleep_type = sleep_type;
	}

	public String getSession_id() {
		return session_id;
	}

	public void setSession_id(String session_id) {
		this.session_id = session_id;
	}

	public String getAnalysis_image_2() {
		return analysis_image_2;
	}

	public void setAnalysis_image_2(String analysis_image_2) {
		this.analysis_image_2 = analysis_image_2;
	}

	public Item getItem() {
		Item item = new Item();
		item.with("objectID", objectID);
		item.with("user_id", user_id);
		item.with("session_id", session_id);
		item.with("frequency_shift", frequency_shift);
		item.with("max_frequency", max_frequency);
		item.with("min_frequency", min_frequency);
		item.with("sampling_rate", sampling_rate);
		item.with("format", format);
		item.with("analysis_image_1", analysis_image_1);
		item.with("analysis_image_2", analysis_image_2);
		item.with("sleep_apnea", sleep_apnea);
		item.with("sleep_type", sleep_type);
		return item;
	}
}
