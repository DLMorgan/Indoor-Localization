package ece596.ucsb.localizedwifi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class MainActivity extends FragmentActivity implements OnClickListener, SensorEventListener {
	
	private SensorManager mSensorManager;
	
	//private Sensor mMagnetic;
	//private Sensor mGravity;
	//private Sensor mSensor;

	// Sensor Energy Data
	private double[] accelEnergy = new double[3];
	private double[] gyroEnergy = new double[3];
	private static double[] energyThreshLow = new double[3];
	private static double[] energyThreshHigh = new double[3];
	private final static int NOACTIVITY = 4;
	private final int ENERGYWINDOWSIZE = 50;
	private double[] calibE = new double[3];
	private double EHIGH = 2.35;
	private double ELOW = 0.15;
	
	private MapUI MyMap;
	
	//consts
	public final static int X_AXIS = 0;
	public final static int Y_AXIS = 1;
	public final static int Z_AXIS = 2;
	
	// values to compute orientation
    public static float[] mInclin;
    public static float[] mRot = new float[16];
    public static float[] mGrav = new float[3];
    public static float[] mGeom = new float[3];
    public static float[] mMag = new float[3];
    public static float[] rotValues = new float[3];
    public static float[] mValues = new float[3];
    public static boolean useCompass;
	
	private Button reset_map_btn, calibrate_btn, scan_btn, location_btn, reset_wifi_btn;
	
	private double[][] candidategrid = {{0.5,0.5},{1.5,0.5},{2.5,0.5},
										{0.5,1.5},{1.5,1.5},{2.5,1.5},
										{0.5,2.5},{1.5,2.5},{2.5,2.5},
										{0.5,3.5},{1.5,3.5},{2.5,3.5}};
	private int previousCandidate = 10;
	public static double userDistance = 0;
	
	//Hard coded Room Number
	private int ROOM_NUMBER = 1;
	
	//sensor Manager
	private SensorManager sensorManager;
	
	// Step Detection Classes
	StepDetector xAxisStepDetector;
	StepDetector yAxisStepDetector;
	StepDetector zAxisStepDetector;
	
	//WEKA Library variables
	//Classifier myLeastSquares;
	LinearRegression myLR;
	Evaluation eval;
	DataSource modelSource;
	Instances modelData;
	DataSource testSource;
	Instances test;
	String header = "@relation Step-Size\n\n@attribute Hieght real\n@attribute freq_times_height real\n@attribute Step_size real\n\n@data\n";

	//FFT library variables
	private final int FFT_SIZE = 512;
	private DoubleFFT_1D fftlib = new DoubleFFT_1D(FFT_SIZE);
	private static double[] accelFFTfreq = {0,0,0};
	private static double[] prevAccelFFTfreq = {0,0,0};
	private static double[] gyroFFTfreq = {0,0,0};
	private static double[] prevGyroFFTfreq = {0,0,0};
	
	// Array size limitation for sensor data
	private final int ARRAY_SIZE = 512;
	
	// data arrays
	private ArrayList<AccelData> accData;  // Accelerometer data array 
	private ArrayList<AccelData> gyroData; // Gyroscope data array 
	private ArrayList<AccelData> gravData; // Gravity data array 
	private ArrayList<AccelData> trainData;
	private double[][] trainPeakData = new double[3][20]; // array for calibration Data
	private double[][] trainTroughData = new double[3][20]; // array for calibration Data
	private double[] peakAvg = new double[3];
	private double[] troughAvg = new double[3];
	private double[] peakThresh = new double[3];
	private double[] troughThresh = new double[3];
	private double[] p2pThresh = new double[3];
	
	// filter data
	private static int ORDER = 10;
	private static double[] A_coeffs = {1, -8.66133120135126, 33.8379111594403, -78.5155814397813, 119.815727607323, -125.635905892562, 91.6674737604170, -45.9506609307247, 15.1439586368786, -2.96290103992494, 0.261309426400923};
	private static double[] B_coeffs = {8.40968636959052e-11, 8.40968636959052e-10, 3.78435886631574e-09, 1.00916236435086e-08, 1.76603413761401e-08, 2.11924096513681e-08, 1.76603413761401e-08, 1.00916236435086e-08, 3.78435886631574e-09, 8.40968636959052e-10, 8.40968636959052e-11};
	
	//value display refresh rate
	private final int REFRESH_RATE 		= 2;                // rate of value/screen updates per second
	private final double SENSOR_DELAY 	= .01;              // sensor delay in seconds 
	private final double Fs		   		= 1/SENSOR_DELAY;   //sampling rate of sensor data in Hz
	private int counter 		   		= 0;                //counter for refresh rate
	
	//WiFi Demo
	WifiManager wifi;
	BroadcastReceiver receiver;
	final Handler mHandler = new Handler();
	TimeProcess sjf=new TimeProcess();
	public static int TPcount=0;
	StringBuilder textStatus;
	StringBuilder candidates;
	public boolean LocationRunning = false;
	private boolean JNIResult = false;
	private  AsyncTask<String, Void, Void> Location_;
	JNI jni;
	public DemoDialog enterStepsDialog;
	
	//display variable
	private TextView myDisplay;    //display for x frequency
	private TextView y_stepFreq;    //display for y frequency
	private TextView z_stepFreq;    //display for z frequency
	private TextView x_stepEnergy;  //display for X axis Energy Detection
	private TextView y_stepEnergy;  //display for Y axis Energy Detection
	private TextView z_stepEnergy;  //display for Z axis Energy Detection
	private TextView step_length_display;   // display for step length
	private TextView thetaWRTN;     //display for angle
	private TextView distance_display;
	
	//display checkboxes
	private CheckBox cb_x_freq;
	private CheckBox cb_x_energy;
	private CheckBox cb_step_num;
	private CheckBox cb_step_length;
	private CheckBox cb_y_freq;
	private CheckBox cb_y_energy;
	private CheckBox cb_thetaWRTN;
	private CheckBox cb_z_freq;
	private CheckBox cb_z_energy;
	private CheckBox cb_distance_display;
	
    // variables for pedometer
    private TextView step_num_display;
    private static int step_value = 0;
	public final static double FREQTHRESH = 0.3;
		
	public static FragmentManager fm;
    public TrainDataDialog enterHeightDialog;
    public static CalibrationDialog calibrateSteps;
    public static boolean calibration_inProgress;
    private double inputHeight;
    private static boolean timeout;
    private int timeoutCount;
    
    private final double wiggleRoom = .2;
    
    private double step_length;
    private double distance;
	
	static

    {

        System.loadLibrary("sun");

    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//getSupportFragmentManager().beginTransaction().add(R.layout.activity_main, (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map), "tag").commit();
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		fm = getSupportFragmentManager();
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		//initialization
        enterHeightDialog = new TrainDataDialog();
        calibrateSteps = new CalibrationDialog();
        enterStepsDialog = new DemoDialog();
        calibration_inProgress = false;
        timeout = false;
        timeoutCount = 0;
        distance = 0;
        
		
        myDisplay = (TextView)findViewById( R.id.myDisplay);
        
		//create arraylist for accelerometer data
		accData = new ArrayList<AccelData>();
		trainData = new ArrayList<AccelData>();

		//Weka Libraries
		try {
			myLR = new LinearRegression();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		//sensor definitions
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), (int) (SENSOR_DELAY*1000000)); //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), (int) (SENSOR_DELAY*10000000)); 	 		 //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), (int) (SENSOR_DELAY*10000000));     //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), (int) (SENSOR_DELAY*10000000));     //convert from seconds to micro seconds


		// Setup WiFi
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		// Get WiFi status
		WifiInfo info = wifi.getConnectionInfo();
		textStatus =  new StringBuilder();
		textStatus.append("\nWiFi Status: " + "\n"+info.toString());

		// List available networks
		List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
		
		if (receiver == null)
			receiver = new WiFiScanReceiver(this);
		
		registerReceiver(receiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
		jni = new JNI();
		
		trainModel();
		
		xAxisStepDetector = new StepDetector(X_AXIS); // AXIS
		yAxisStepDetector = new StepDetector(Y_AXIS);
		zAxisStepDetector = new StepDetector(Z_AXIS);
        
		
		calibrate_btn = (Button) findViewById(R.id.calibrate_btn);
		calibrate_btn.setOnClickListener(this);
		reset_map_btn = (Button) findViewById(R.id.reset_map_btn);
		reset_map_btn.setOnClickListener(this);
		scan_btn = (Button) findViewById(R.id.scan_btn);
		scan_btn.setOnClickListener(this);
		location_btn = (Button) findViewById(R.id.location_btn);
		location_btn.setOnClickListener(this);
		reset_wifi_btn = (Button) findViewById(R.id.reset_wifi_btn);
		reset_wifi_btn.setOnClickListener(this);
		
		MyMap = new MapUI(((SupportMapFragment) fm.findFragmentById(R.id.map)).getMap(), getBaseContext());
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    @Override
    protected void onResume() {
        super.onResume();
       // mSensorManager.registerListener(this, mSensor, (int) (.1*1000000));
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
       // mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
       // mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
    }

	 @Override
	 protected void onStop() { //the activity is not visible anymore
	  Log.d("MyApp","onStop() called");
	  if (this.isFinishing()){
	        System.exit(0);
	    }
	  super.onStop();
	   
	 }
	 @Override
	 protected void onDestroy() {//android has killed this activity
	   Log.d("MyApp","onDestroy() called");
	   super.onDestroy();
	 }
    
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		int type = sensor.getType();
		switch(type){
			case Sensor.TYPE_MAGNETIC_FIELD:
			if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
				useCompass = true;
			else
				useCompass = false;
			
				
		}
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		int type = sensor.getType();
		switch(type){
		
		case Sensor.TYPE_ORIENTATION:
            mValues = event.values;
            if (mGrav != null && mMag != null){
            	MyMap.rotateArrow(CompassHelper.getThetaMatrix(mGrav, mMag));
            }
            break;
		
		case Sensor.TYPE_MAGNETIC_FIELD:
			// TODO can be used to help step detection?
			System.arraycopy(event.values, 0, mMag, 0, 3);
			break;
			
		case Sensor.TYPE_GRAVITY:
			// TODO can be used to help step detection?
			System.arraycopy(event.values, 0, mGrav, 0, 3);
			break;
		
		case Sensor.TYPE_LINEAR_ACCELERATION:
			//****************************Data accumulation******************************
			long timestampAcc = System.currentTimeMillis();
			accData.add(new AccelData(timestampAcc, event.values[X_AXIS], event.values[Y_AXIS], event.values[Z_AXIS]));
			filter();  //10th order butterworth filter
			resizeData(type);
			//***************************************************************************
			
			//****************************Store Calibration Data*****************************
			if (calibration_inProgress){
				trainData.add(new AccelData(timestampAcc, accData.get(accData.size()-1).getValue(X_AXIS), 
						accData.get(accData.size()-1).getValue(Y_AXIS), 
						accData.get(accData.size()-1).getValue(Z_AXIS)));
				calibE = findMaxEnergy(trainData, type);
				for (int i = 0;i<3;i++){
					if (calibE[i] + wiggleRoom/2 > energyThreshHigh[i] + wiggleRoom/2)
						energyThreshHigh[i] = calibE[i] + wiggleRoom/2;
					if (calibE[i] != 0 && calibE[i] < energyThreshLow[i])
						energyThreshLow[i] = calibE[i];
				}
				return;
			}
			//*******************************************************************************
			
			//*****************************Energy Section********************************
			// get energy
			int axisMaxE = 0;
			accelEnergy = findMaxEnergy(accData, type);
			// find max energy axis
			if (accelEnergy[X_AXIS] < accelEnergy[Y_AXIS] || accelEnergy[X_AXIS] < accelEnergy[Z_AXIS])
				axisMaxE = (accelEnergy[Y_AXIS] > accelEnergy[Z_AXIS] ? Y_AXIS:Z_AXIS);
			else
				axisMaxE = X_AXIS;
			
			if (accelEnergy[axisMaxE] < energyThreshLow[axisMaxE])
				axisMaxE = NOACTIVITY;
			//****************************************************************************
			
			//*****************************FFT SECTION************************************
			// get X FFT
			accelFFTfreq[X_AXIS] = (calculateFFT(type, X_AXIS));
			accelFFTfreq[X_AXIS] = (accelFFTfreq[X_AXIS] > 3.2 ? prevAccelFFTfreq[X_AXIS]:accelFFTfreq[X_AXIS]);
			prevAccelFFTfreq[X_AXIS] = accelFFTfreq[X_AXIS];
			// get Y FFT
			accelFFTfreq[Y_AXIS] = (calculateFFT(type, Y_AXIS));
			accelFFTfreq[Y_AXIS] = (accelFFTfreq[Y_AXIS] > 3.2 ? prevAccelFFTfreq[Y_AXIS]:accelFFTfreq[Y_AXIS]);
			prevAccelFFTfreq[Y_AXIS] = accelFFTfreq[Y_AXIS];
			// get Z FFT
			accelFFTfreq[Z_AXIS] = (calculateFFT(type, Z_AXIS));
			accelFFTfreq[Z_AXIS] = (accelFFTfreq[Z_AXIS] > 3.2 ? prevAccelFFTfreq[Z_AXIS]:accelFFTfreq[Z_AXIS]);
			prevAccelFFTfreq[Z_AXIS] = accelFFTfreq[Z_AXIS];
			//******************************************************************************
			
			//****************************STEP DETECTION SECTION****************************
			if (accData.size() == ARRAY_SIZE){
				zAxisStepDetector.updateArray(accData, accelFFTfreq);
				xAxisStepDetector.updateArray(accData, accelFFTfreq);
				yAxisStepDetector.updateArray(accData, accelFFTfreq);
				boolean xStep = xAxisStepDetector.FindStep();
				boolean yStep = yAxisStepDetector.FindStep();
				boolean zStep = zAxisStepDetector.FindStep();
				if ( xStep || yStep || zStep){
					if (accelFFTfreq[X_AXIS] != 0 && accelFFTfreq[Y_AXIS] != 0 && accelFFTfreq[Z_AXIS] != 0
							&& (accelFFTfreq[X_AXIS] - 2*accelFFTfreq[Y_AXIS]) < FREQTHRESH
							&& accelFFTfreq[X_AXIS] - accelFFTfreq[Z_AXIS] < FREQTHRESH
							&& /*energyThreshHigh[X_AXIS]*/ EHIGH > accelEnergy[X_AXIS] && accelEnergy[X_AXIS] > ELOW/*energyThreshLow[X_AXIS]*/
							&& /*energyThreshHigh[Y_AXIS]*/ EHIGH > accelEnergy[Y_AXIS] && accelEnergy[Y_AXIS] > ELOW/*energyThreshLow[Y_AXIS]*/
							&& /*energyThreshHigh[Z_AXIS]*/ EHIGH > accelEnergy[Z_AXIS] && accelEnergy[Z_AXIS] > ELOW/*energyThreshLow[Z_AXIS]*/
							&& calibration_inProgress == false && timeout == false){                                   // && yStep_detect == MAXMIN);
						step_value++;
						timeout = true;
						timeoutCount = 33;
						//***********Step Size Estimation and distance calculation***********************
						 calculateStepSize(); //using current step frequency and hieght parameter
						 calculateDistance();
						//*******************************************************************************
					}
				}
			}
			//*******************************************************************************
			
			//****************************Step detection timeout period**********************
			if (timeoutCount != 0)
				timeoutCount--;
			else
				timeout = false;
			//*******************************************************************************
			
			//*****************************DISPLAY UPDATE SECTION****************************
			counter++;			
			if (counter > Fs/REFRESH_RATE){
				myDisplay.setText(Integer.toString(step_value));
				//updateDisplays(axisMaxE);
				counter = 0;
			}
			//********************************************************************************
			
			break;
			
		default:
			break;
		}
		
	}
	
	@Override
	public void onClick(View arg0) {
		int switchValue = arg0.getId();
		switch (switchValue) {
	
		case R.id.reset_map_btn:
			step_value = 0;
			distance = 0;
			userDistance = 0;
			previousCandidate = 10;
			MapUI.resetArrow();
			break;
		
		case R.id.calibrate_btn:
	        enterHeightDialog.show(fm, "fragment_enter_height");
			break;
		
		case R.id.location_btn:
			Reset();
			if(Integer.toString(ROOM_NUMBER).trim().length() > 0){
				
				Log.d("MyApp", "onClick() wifi.startScan()");
		        mHandler.post(sjf);
				
		}
		
		else{
			Toast.makeText(this, "Please enter the room number",
					Toast.LENGTH_LONG).show();
		}
			//Location.start();
		      //if (Location_ == null){
		    	  Location_ = new Location_(getBaseContext());
		       // }
		      Location_.execute();
			break;
			
		case R.id.scan_btn:
		//if(textRoomNum.getText().toString().trim().length() > 0){
			
			enterStepsDialog.show(fm,"enter_steps_dialog");
			break;
			
		case R.id.reset_wifi_btn:
			Reset();
			break;
			
		default:
			break;
		
		}
		
	}
	
	public class TimeProcess implements Runnable{
		public void run() {
			wifi.startScan();
			if (TPcount>2){
				TPcount=0;
				mHandler.removeCallbacks(sjf);
				textStatus.append("\nScan Done");
				Toast.makeText(getBaseContext(), "Scan Done",Toast.LENGTH_SHORT).show();
				//unregisterReceiver(receiver);
				return;
				}
			mHandler.postDelayed(this, 1000);
			TPcount++;
			
		}
	}
	
	  public class Location_ extends AsyncTask<String, Void, Void> {

		  private Context context;
		  
		  public Location_(Context context){
			  this.context = context;
		  }
		  
	      protected Void doInBackground(String... url) {
	    	  Log.d("myapp","got here 17");
	    	  
	    	  if (LocationRunning == false){
	    		//Looper.prepare();
          		LocationRunning = true;
          	    textStatus = new StringBuilder();
          	    candidates = new StringBuilder();
				int j;
				int[] location=jni.getCInt();
				if (location==null) 
      				//JNIResult = false;
					j = 0;
      			else{
      				textStatus.append("\nYour location maybe:\n");
      				int i=0;
      				while(location[i]!=0){
      					String locationStr =Integer.toString(location[i]);
      					textStatus.append(locationStr+" " );
      					candidates.append(locationStr+" " );
      					i++;
      				}//textStatus.append("\nYour location is:"+location);
      				Log.d("myapp","Your new Location is:"+textStatus);
      				//JNIResult = true;
      			}
      			Log.d("myapp","done runnable");
      			LocationRunning = false;;
  				//selectCandidate(candidates.toString());
            }
	    	return null;
	      }
	      
	      @Override
	      protected void onCancelled() {

	      }
	      
	      @Override
	      protected void onPostExecute(Void result) {
	            // TODO Auto-generated method stub
	            super.onPostExecute(result);
	            selectCandidate(candidates.toString());
	    	  //if (JNIResult == false)
	    	//	  Toast.makeText(context, "Your Location is: -1",Toast.LENGTH_SHORT).show();
	    	 // else
	  		//	  Toast.makeText(context, "Your Location is:"+textStatus,Toast.LENGTH_SHORT).show();
			return;
          }
          
  }
	
	public void Reset(){
		File logFile = new File("/mnt/sdcard/log.file");
		logFile.delete();
		Toast.makeText(this, "Log info removed",Toast.LENGTH_SHORT).show();
	}

	public void appendLog(String text)
	  {       
	     File logFile = new File("/mnt/sdcard/log_roomnum.file");
	     if (!logFile.exists())
	     {
	        try
	        {
	           logFile.createNewFile();
	        } 
	        catch (IOException e)
	        {
	           // TODO Auto-generated catch block
	           e.printStackTrace();
	        }
	     }
	     try
	     {
	        //BufferedWriter for performance, true to set append to file flag
	        BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	        buf.append(text);
	        buf.newLine();
	        buf.close();
	     }
	     catch (IOException e)
	     {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	     }
	  }
	
	public String selectCandidate(String candidates){
		Toast.makeText(this, "Possible Candidates are " + candidates,
				Toast.LENGTH_LONG).show();
		String myCandidate = null;
		String[] myCandidates = null;
		myCandidates = candidates.split(" ");
		
		
		Log.d("myapp","User distance is " +userDistance + " away");
		for (int i=0;i<myCandidates.length;i++){
			int candidateNumber = Integer.parseInt(myCandidates[i]);
			double x_dist = candidategrid[(candidateNumber-1)][0] - candidategrid[previousCandidate-1][0];
			double y_dist = candidategrid[(candidateNumber-1)][1] - candidategrid[previousCandidate-1][1];
			double distance = Math.sqrt(Math.pow(x_dist, 2)+Math.pow(y_dist, 2));
			Log.d("myapp","candidate " +candidateNumber +" is " + distance + " away");
			if (Math.abs(distance - userDistance) < 0.2){
				Log.d("myapp","candidate " +candidateNumber +" is possible candidate");
				myCandidate = Integer.toString(candidateNumber);
			}
			else{
				Log.d("myapp","candidate " +candidateNumber +" ruled out");
				
			}
		}
		if (myCandidate==null){
			Toast.makeText(this, "no possible candidates",
					Toast.LENGTH_LONG).show();
		return "none";
		}
		else{
			previousCandidate = Integer.parseInt(myCandidate);
			Toast.makeText(this, "slected Candidate " + myCandidate,
				Toast.LENGTH_LONG).show();
		}
			userDistance = 0;
		return myCandidate;
	}
	
	/**
	 * 10th Order Butterworth Filter
	 * 
	 * parameters for this filter were created in MATLAB and the coefficients
	 * were hard-coded in. The filter is a 10th order butter worth filter with
	 * a cutoff frequency of 10/3 (repeating) Hz.
	 * 
	 * @param context
	 * @param attrs
	 */
	public void filter(){ // implements a 10th order butterworth filter (coefficients created in MATLAB)
		double[] x_orig = new double[accData.size()];
		double[] y_filt = new double[accData.size()];
		
		for (int i=0;i<accData.size();i++){
			x_orig[i] = accData.get(i).getValue(Z_AXIS);
		}
		
		int nSize = x_orig.length;
		for (int n=0;n<nSize;n++){
			for (int m=0;m<ORDER;m++){
				if (n-m >= 0)
					y_filt[n] += x_orig[n-m]*B_coeffs[m];
				
				if (n-m >=1 && m >=1)
					y_filt[n] -= y_filt[n-m]*A_coeffs[m];
			}
		}
		
		return;
	}
	
	/**
	 * Function to calculate the FFT of sensor data
	 * 
	 * This function will take in a sensor type, (which corresponds
	 * to the variable used for input to the FFT) and the axis (x, y, z)
	 * of the data which you want to take the FFT of. The result is
	 * analyzed and the maximum frequency component (excluding DC) is
	 * stored in the variable FFTfreq.
	 * 
	 * @param context
	 * @param attrs
	 */
	private double calculateFFT(int type, int axis){
		double myFFT[] = new double[FFT_SIZE];
		double mySpectrum[] = new double[FFT_SIZE / 2];
		double freqRange[] = new double[FFT_SIZE / 2 + 1];
		double linspace[] = new double[FFT_SIZE / 2 + 1];
		double curFreq = 0;
		
		switch(type){
		case Sensor.TYPE_LINEAR_ACCELERATION:
			if (accData.size() == FFT_SIZE)
			{
				for(int i=0;i<accData.size();i++)
					myFFT[i] = accData.get(i).getValue(axis);
			}
			break;
		default:
			break;
		}
		
		fftlib.realForward(myFFT);
        for (int i=0;i<FFT_SIZE;i++)
        	myFFT[i] = Math.abs(myFFT[i]);
        
        for (int k = 0; k < FFT_SIZE/2 - 1;k++){
            mySpectrum[k] = Math.sqrt(myFFT[2*k]*myFFT[2*k] + myFFT[2*k+1]*myFFT[2*k+1]);
            linspace[k+1] = (double) 2* (k+1) / FFT_SIZE;
        }
        linspace[0]=0;
        linspace[FFT_SIZE / 2]=1;
        mySpectrum[0] = 0; mySpectrum[1] = 0; mySpectrum[2]=0; //remove DC

    	for (int i = 0;i<linspace.length;i++)
    		freqRange[i] = Fs / 2*linspace[i];  //

    	List<Double> b = Arrays.asList(ArrayUtils.toObject(mySpectrum));
    	if (Collections.max(b) < 20){  // must be noise
    		curFreq = 0;
    		return curFreq;
    	}
    	int max = b.indexOf(Collections.max(b));
    	curFreq = freqRange[max];
    	
    	return curFreq;
	}
	
	/**
	 * Function to Determine the axis (x,y or z) which has the highest energy (activity)
	 * 
	 * This function will take the array of accelerometer data and compute the average 
	 * (absolute value)
	 * format of result is array{x_value,y_value,z_value}
	 * 
	 * @param context
	 * @param attrs
	 */
	private double[] findMaxEnergy(ArrayList<AccelData> data, int type){
		double result[] = {0,0,0};
		double xEnergy = 0;
		double yEnergy = 0;
		double zEnergy = 0;
		int size = 0;
		
		if (data.size() < ENERGYWINDOWSIZE)
			return result;
		
		switch(type){
			case Sensor.TYPE_LINEAR_ACCELERATION:
				for (int i=data.size();i>data.size()-ENERGYWINDOWSIZE;i--){
					xEnergy += Math.abs(data.get(i-1).getValue(X_AXIS));
					yEnergy += Math.abs(data.get(i-1).getValue(Y_AXIS));
					zEnergy += Math.abs(data.get(i-1).getValue(Z_AXIS));
				}
				size = ENERGYWINDOWSIZE;
				break;
			default:
				break;
		}
		
		result[X_AXIS] = xEnergy / size;
		result[Y_AXIS] = yEnergy / size;
		result[Z_AXIS] = zEnergy / size;
		return result;
	}
	
	/**
	 * Function to maintain the size of the array lists
	 * 
	 * This function will take in a type (which corresponds to the array list
	 * you want to resize) and ensures that it is the correct size determined
	 * by the variable SIZE. The function assumes that there is only one array
	 * for each type of arraylist data and modifies this array appropriately
	 * 
	 * @param context
	 * @param attrs
	 */
	private void resizeData(int type){
		switch(type){
		case Sensor.TYPE_LINEAR_ACCELERATION:
			if (accData.size() > ARRAY_SIZE)
				accData.remove(1);
			break;
		default:
			break;
		}
		
		return;
	}
	
	public void startPedometer() {
		// Inflate the menu; this adds items to the action bar if it is present.
		xAxisStepDetector.setThreshVariables(peakAvg[X_AXIS], peakThresh[X_AXIS]+wiggleRoom, troughAvg[X_AXIS], troughThresh[X_AXIS]+wiggleRoom, p2pThresh[X_AXIS]-wiggleRoom); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh
		yAxisStepDetector.setThreshVariables(peakAvg[Y_AXIS], peakThresh[Y_AXIS]+wiggleRoom, troughAvg[Y_AXIS], troughThresh[Y_AXIS]+wiggleRoom, p2pThresh[Y_AXIS]-wiggleRoom); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh
		zAxisStepDetector.setThreshVariables(peakAvg[Z_AXIS], peakThresh[Z_AXIS]+wiggleRoom, troughAvg[Z_AXIS], troughThresh[Z_AXIS]+wiggleRoom, p2pThresh[Z_AXIS]-wiggleRoom); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh

		return;
	}
	
	public void startCalibration(double inputHeight) {
		
		this.inputHeight = inputHeight;
        Toast.makeText(this, "Calibration started", Toast.LENGTH_SHORT).show();
        trainData.clear();        
        calibrateSteps.show(fm, "fragment_calibrate_steps");
        calibration_inProgress = true;
        energyThreshLow[X_AXIS] = 10;
        energyThreshLow[Y_AXIS] = 10;
        energyThreshLow[Z_AXIS] = 10;
        energyThreshHigh[X_AXIS] = 0;
        energyThreshHigh[Y_AXIS] = 0;
        energyThreshHigh[Z_AXIS] = 0;

	}
	
	public void finishCalibration() {
		calibration_inProgress = false;
        Toast.makeText(this, "Calibration finished", Toast.LENGTH_SHORT).show();
        for (int i=0;i<3;i++)
        	extractCalibratedData(i);
        
        startPedometer();
	}
	
	private void extractCalibratedData(int axis){
		//TODO
		/**
		 * find top 10 peaks and bottom 10 troughs (x and z directions)
		 * calculate 
		 * 1) peak average value
		 * 2) largest diff between one of the ten peaks and the computed average
		 * 3) trough average value
		 * 4) largest diff between one of the ten troughs and the computed average
		 * 5) largest difference between a peak and a trough
		 */
		trainPeakData[axis] = findPeaks(trainData, axis);
		trainTroughData[axis] = findTroughs(trainData, axis);
		peakAvg[axis] = calculatePeakAverage(trainPeakData[axis]);
		troughAvg[axis] = calculateTroughAverage(trainTroughData[axis]);
		peakThresh[axis] = findPeakThresh(peakAvg[axis], trainPeakData[axis]);
		troughThresh[axis] = findTroughThresh(troughAvg[axis], trainTroughData[axis]);
		p2pThresh[axis] = findP2PThresh(trainPeakData[axis], trainTroughData[axis]);
		
		Log.d("MyApp","value is " + energyThreshLow[axis]);
		return;
	}
	
	public double[] findPeaks(ArrayList<AccelData> data, int stepAxis) {
		double Prev = 0;
		double Next = 0;
		double[] result = new double[60];
		int k = 0;
		for (int i=0;i<data.size();i++){
			int j = 1;
			while (j < xAxisStepDetector.LOOKLENGTH && (i - j) >= 0 && (i + j) < data.size()) {
				Prev = data.get(i - j).getValue(stepAxis);
				Next = data.get(i + j).getValue(stepAxis);
				if (Prev > data.get(i).getValue(stepAxis) || Next > data.get(i).getValue(stepAxis))
					break; // not a true peak
				j++;
			}
			if (j == xAxisStepDetector.LOOKLENGTH && k < 60) {
				// found a peak
				result[k] = data.get(i).getValue(stepAxis); // store the supposed peak
				k++;
			}
		}
		return result;
	}
	
	public double[] findTroughs(ArrayList<AccelData> data, int stepAxis) {
		double Prev = 0;
		double Next = 0;
		double[] result = new double[60];
		int k = 0;
		for (int i=0;i<data.size();i++){
			int j = 1;
			while (j < xAxisStepDetector.LOOKLENGTH && (i - j) >= 0 && (i + j) < data.size()) {
				Prev = data.get(i - j).getValue(stepAxis);
				Next = data.get(i + j).getValue(stepAxis);
				if (Prev < data.get(i).getValue(stepAxis) || Next < data.get(i).getValue(stepAxis))
					break; // not a true trough
				j++;
			}
			if (j == xAxisStepDetector.LOOKLENGTH && k < 60) {
				// found a trough
				result[k] = data.get(i).getValue(stepAxis); // store the supposed peak
				k++;
			}
		}
		return result;
	}
	
	public double calculatePeakAverage(double[] data) {
		int i = 0;
		int index = 0;
		double avg = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(data));
		while (i<10 && i < data.length){
			avg += Collections.max(b).doubleValue();
			index = b.indexOf(Collections.max(b));
			b.set(index, (double) 0);
			i++;
		}
		return avg/i;
	}
	
	public double calculateTroughAverage(double[] data) {
		int i = 0;
		int index = 0;
		double avg = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(data));
		while (i<10 && i < data.length){
			avg += Collections.min(b).doubleValue();
			index = b.indexOf(Collections.min(b));
			b.set(index, (double) 0);
			i++;
		}
		return avg/i;
	}
	
	public double findPeakThresh(double avg, double[] peaks){
		double result = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(peaks));
		result = (Collections.max(b).doubleValue() - avg);
		return result;
	}
	
	public double findTroughThresh(double avg, double[] troughs){
		double result = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(troughs));
		result = (Math.abs(Collections.min(b).doubleValue()) - Math.abs(avg));
		return result;
	}
	
	public double findP2PThresh(double[] peaks, double[] troughs){
		double result = 0;
		int i = 0;
		int index = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(peaks));
		List<Double> c = Arrays.asList(ArrayUtils.toObject(troughs));
		while (i<9 && i < (peaks.length - 1) && i < troughs.length - 1){
			index = b.indexOf(Collections.max(b));
			b.set(index, (double) 0);
			index = c.indexOf(Collections.min(c));
			c.set(index, (double) 0);
			i++;
		}
		result = (Collections.max(b).doubleValue() - Collections.min(c).doubleValue());
				
		return result;
	}
	
	private void trainModel(){
		//TODO apply least squares model using WEKA and inputHight Variable
		
		try {
			modelSource = new DataSource("/mnt/sdcard/WEKA/ARFF_JAVA.arff");
			modelData = modelSource.getDataSet();
			modelData.setClassIndex(modelData.numAttributes() - 1);
			
			myLR.buildClassifier(modelData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void calculateStepSize(){
		try {
			
			// build file
			
			StringBuilder Builder = new StringBuilder();

			Builder.append(header);
			Builder.append(Double.toString(inputHeight));
			Builder.append(",");
			Builder.append(Double.toString(accelFFTfreq[Z_AXIS]));
			Builder.append(",");
			Builder.append("?");
			String text = Builder.toString();
		    File predFile = new File("/mnt/sdcard/WEKA/pred.arff");
	        try
	        {
	        	predFile.delete();
	        	predFile.createNewFile();
		        BufferedWriter buf = new BufferedWriter(new FileWriter(predFile, true)); 
		        buf.append(text);
		        buf.newLine();
		        buf.close();
	        } 
	        catch (IOException e)
	        {
	           // TODO Auto-generated catch block
	           e.printStackTrace();
	        }
			
			testSource = new DataSource("/mnt/sdcard/WEKA/pred.arff");
			test = testSource.getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			eval = new Evaluation(modelData);
			eval.evaluateModel(myLR, test);
			//step_length = (Double) eval.predictions().get(1);
			String step_length_str = eval.predictions().toString().substring(10, 16);
			step_length = Double.parseDouble(step_length_str);
			Log.d("MyApp","length is " + step_length_str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void calculateDistance() {
		distance += step_length;
		
		return;
	}

}
