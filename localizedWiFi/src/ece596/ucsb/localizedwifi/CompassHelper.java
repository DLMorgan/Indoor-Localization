package ece596.ucsb.localizedwifi;

import android.graphics.Matrix;
import android.hardware.SensorManager;

public class CompassHelper {
	
	// values to compute orientation
    private static float[] mInclin;
    private static float[] mRot = new float[16];
    public static float[] rotValues = new float[3];
    static Matrix matrix = new Matrix();
	
    
	public static Matrix getThetaMatrix(float mGrav[], float mGeom[]){
	    if (mGrav != null && mGeom != null) {
	    	boolean success = SensorManager.getRotationMatrix(mRot, mInclin, mGrav, mGeom);
			if (success){
				SensorManager.getOrientation(mRot, rotValues);
				//rotValues[0] = (float) (((rotValues[0] + Math.PI/2) *180/Math.PI));
				//if (rotValues[0] < 0)
				//	rotValues[0] = rotValues[0] + 360;
				
				//matrix.reset();
				matrix.setRotate((float)Math.toDegrees((rotValues[0]-Math.PI/2)) + MapUI.roomThetaOffset);
				
				return matrix;
			}
	    }
	    return null;
	}
	
}
