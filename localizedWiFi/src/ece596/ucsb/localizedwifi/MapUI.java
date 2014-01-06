package ece596.ucsb.localizedwifi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MapUI extends FragmentActivity{
	
	//Map Variables
	private static GoogleMap mMap;	
	private Bitmap HFHFloorPlan;
	private static Bitmap arrow;
	private static LatLng arrow_sw;
	private static LatLng arrow_ne;
	static GroundOverlay arrowOverlay;
	
	//consts
	//double initArrowBot = 34.413169;
	//double initArrowLeft = -119.844672;
	
	static double initArrowBot = 34.413169;
	static double initArrowLeft = -119.844672;
	
	double demoinitArrowBot = 34.414590;
	double demoinitArrowLeft = -119.845395;
	private static double ArrowLatLongSize = 0.000050;
	public static float roomThetaOffset = 20; //in degrees
	private static final LatLng HFH_COORDS = new LatLng(34.413812, -119.84137);
	private static final double RADIUSEARTH = 6367000;
	private static double mapScale = 10.0;
	private static Bitmap myArrow;

	static Matrix matrix = new Matrix();

	public MapUI(GoogleMap aMap, Context context){
		if (aMap != null){
		this.mMap = aMap;
		mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HFH_COORDS, 16));
		
		HFHFloorPlan = BitmapFactory.decodeResource(context.getResources(), R.drawable.hfh_map_new);
		arrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow);
		
		arrow_sw = new LatLng(initArrowBot,initArrowLeft);
		arrow_ne = new LatLng(initArrowBot + ArrowLatLongSize,initArrowLeft + ArrowLatLongSize);
		
		setMap();
		resetArrow();
		
		//starting point for demo
		updateCursor(3.9878, 90, arrow);
		updateCursor(1.6764, 0, arrow);
		}
	}
	
	public void setMap(){
	    //      [ N/S  (decrease to move south), E/W  (decrease to move west) ]
		LatLng hfh_sw = new LatLng(34.413000,-119.846000);
		LatLng hfh_ne = new LatLng(34.415370,-119.836700);
		LatLngBounds hfh_bounds = new LatLngBounds(hfh_sw, hfh_ne);    // get a bounds
	
		// Adds a ground overlay with 70% transparency.
		mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the map
	     .image(BitmapDescriptorFactory.fromBitmap(HFHFloorPlan))
	     .positionFromBounds(hfh_bounds)
	     .transparency(0.7f));
	}
	
	public void setUpMapIfNeeded() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (mMap == null) {
	        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
	                            .getMap();
	        // Check if we were successful in obtaining the map.
	        if (mMap != null) {
	        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HFH_COORDS, 16));
	        	/*
	        	mMap.
	    		  		  map:uiCompass="false"
	    		  		  map:uiRotateGestures="true"
	    		  		  map:uiScrollGestures="true"
	    		  		  map:uiTiltGestures="false"
	    		  		  map:uiZoomControls="false"
	    		  		  map:uiZoomGestures="true"
	    		 */

	        }
	    }
	}
	
	public static void resetArrow(){
		if (arrowOverlay != null)
			arrowOverlay.remove();
		
		LatLngBounds arrow_bounds = new LatLngBounds(new LatLng(initArrowBot,initArrowLeft), new LatLng(initArrowBot + ArrowLatLongSize,initArrowLeft + ArrowLatLongSize));   // get a bounds
		
		arrowOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the arrow
		 .image(BitmapDescriptorFactory.fromBitmap(arrow))
	     .positionFromBounds(arrow_bounds)
	     .transparency(0.7f));
		
		//updateCursor(3.9878, 90, arrow);
		//updateCursor(1.6764, 0, arrow);
	}
	
	public void rotateArrow(Matrix rotation){
		if (MainActivity.mValues != null && rotation != null){
		matrix.setRotate(MainActivity.mValues[0]-90+roomThetaOffset); // anti-clockwise by 90 degrees  for landscape mode 
		//matrix.setRotate(0);
		Bitmap rotatedArrow = Bitmap.createBitmap(arrow , 0, 0, arrow.getWidth(), arrow.getHeight(), 
				(MainActivity.useCompass == true) ? matrix:rotation, true);
		updateCursor( 0, 0, rotatedArrow);
		}
	}
	
	public void updateDisplay(){
		//step_length_view.setText("Step Length is: " + Double.toString(step_length));
		//angleWRTN_view.setText("Angle WRTN is: " + Double.toString(theta) + " degrees");
		//distance_view.setText(Integer.toString(step_num) + " Steps and " + Double.toString(total_distance) + " Meters");
		
		return;
	}	
	
	public double LatLontoMeter(double startLat, double startLon, double endLat, double endLon){
		double dlon = (endLon - startLon);
		double dlat = (endLat - startLat);
		dlon = (dlon * Math.PI/180);
		dlat = (dlat * Math.PI/180);
		double a = Math.pow(Math.sin(dlat/2),2) + Math.cos(startLat*Math.PI/180)*Math.cos(endLat*Math.PI/180)*Math.pow(Math.sin(dlon/2),2);
		double c = 2 * Math.atan2(Math.sqrt(a),  Math.sqrt(1-a));
		double d = RADIUSEARTH * c;
		
		return d;
	}
	
	public static void updatePosition(double distance){
		updateCursor(distance, 
				(MainActivity.useCompass == true)?MainActivity.mValues[0] + roomThetaOffset:Math.toDegrees((CompassHelper.rotValues[0]-Math.PI/2)) + roomThetaOffset,
						arrow);
	}
	
	private static void updateCursor(double distance, double angleWRTN, Bitmap cursor ){
		arrowOverlay.remove();
		if (distance != 0 || angleWRTN != 0){
			arrow_sw = CalculateDerivedPosition(arrow_sw, distance, angleWRTN);  //update current location
			arrow_ne = CalculateDerivedPosition(arrow_ne, distance, angleWRTN);  //update current location
		}
		LatLngBounds arrow_bounds = new LatLngBounds(arrow_sw, arrow_ne);    // get a bounds
		arrowOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the arrow
	     .image(BitmapDescriptorFactory.fromBitmap(cursor))
	     .positionFromBounds(arrow_bounds)
	     .transparency(0.7f));
		
		return;
	}
	
	/// <summary>
	/// Calculates the end-point from a given source at a given range (meters) and bearing (degrees).
	/// This methods uses simple geometry equations to calculate the end-point.
	/// </summary>
	/// <param name="source">Point of origin</param>
	/// <param name="range">Range in meters</param>
	/// <param name="bearing">Bearing in degrees</param>
	/// <returns>End-point from the source given the desired range and bearing.</returns>
	private static LatLng CalculateDerivedPosition(LatLng source, double range, double angleWRTN)
	{
	    double latA = Math.toRadians(source.latitude);
	    double lonA = Math.toRadians(source.longitude);
	    double angularDistance = range * mapScale / RADIUSEARTH;
	    double trueCourse = Math.toRadians(angleWRTN);

	    double lat = Math.asin(
	        Math.sin(latA) * Math.cos(angularDistance) + 
	        Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse));

	    double dlon = Math.atan2(
	        Math.sin(trueCourse) * Math.sin(angularDistance) * Math.cos(latA), 
	        Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

	    double lon = ((lonA + dlon + Math.PI) % (Math.PI*2)) - Math.PI;

	    return new LatLng(Math.toDegrees(lat), Math.toDegrees(lon));
	}

}
