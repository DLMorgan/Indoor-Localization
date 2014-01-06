package ece596.ucsb.localizedwifi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;


public class WiFiScanReceiver extends BroadcastReceiver {
  private static final String TAG = "WiFiScanReceiver";
  MainActivity Activity;
  
  public WiFiScanReceiver(MainActivity activity) {
    super();
    this.Activity = activity;
  }

  //
  
  int i=0;
  @Override
  public void onReceive(Context c, Intent intent) {
    
    	
    	List<ScanResult> results = Activity.wifi.getScanResults();
    	ScanResult bestSignal = null;
    	for (ScanResult result : results) {
    		if (bestSignal == null || WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0)
            bestSignal = result;
    		Log.i("wifiscan", "[scan] "+result.toString());
      	}
    
    	//results_string.add(i, result.toString());
    	long time1 = System.currentTimeMillis();
    	appendLog("total "+results.size()+" signals, system time at logging: "+Long.toString(time1)+"  (ms)");
    	for (ScanResult result : results) {
    		if( (result.toString()).contains("Web")|| (result.toString()).contains("shen") || (result.toString()).contains("Res")) {
    			appendLog(result.toString() + " "+Long.toString(time1));
    		}
    	}
    	String message = String.format("%s networks found. %s is the strongest.",
        results.size(), bestSignal.SSID);
    	Log.d(TAG, "onReceive() message: " + message);
    	
    
    //Toast.makeText(wifiDemo, "Done", Toast.LENGTH_SHORT).show();
  }

  
  
  public void appendLog(String text)
  {       
     File logFile = new File("/mnt/sdcard/log.file");
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


  
}
