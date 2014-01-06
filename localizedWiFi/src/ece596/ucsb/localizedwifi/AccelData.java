package ece596.ucsb.localizedwifi;


public class AccelData{
	private long timestamp;
	private double x;
	private double y;
	private double z;
	
	public AccelData(long timestamp, double x, double y, double z) {
		this.timestamp = timestamp;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public double getValue(int axis){
		double returnValue = 0;
		switch(axis){
		case MainActivity.X_AXIS:
			returnValue = x;
			break;
		case MainActivity.Y_AXIS:
			returnValue = y;
			break;
		case MainActivity.Z_AXIS:
			returnValue = z;
			break;
		default:
			break;
		}
		return returnValue;
	}
	
	public double setValue(int axis, double value){
		double returnValue = 0;
		switch(axis){
		case MainActivity.X_AXIS:
			this.x = value;
			break;
		case MainActivity.Y_AXIS:
			this.y = value;
			break;
		case MainActivity.Z_AXIS:
			this.z = value;
			break;
		default:
			break;
		}
		return returnValue;
	}

}
