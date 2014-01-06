package ece596.ucsb.localizedwifi;

import java.util.ArrayList;

import android.util.Log;

public class StepDetector {
	private ArrayList<AccelData> Data; // Accelerometer data array

	private double dataCurr;
	private long dataCurrTS;
	private double peakValue;
	private long peakTS; // find a way to use this to improve accuracy (done?)
	private boolean peakVerified;
	private double troughValue;
	private long troughTS; // find a way to use this to improve accuracy
	private boolean troughVerified;
	private double peakAvg; // not final, has init value and constantly updated
	private double troughAvg;
	private double waveformAvg; // find a way to use this to improve accuracy
	private final int stepAxis;
	private double PEAKAVGTHRESH; // should be determined from training
										// phase (largest diff between peak and
										// avg peak)
	private double TROUGHAVGTHRESH; // should be determined from training
											// phase (largest diff between
											// trough and avg trough)
	private double DIFFAVGTHRESH; // should be determined from training
										// phase (smallest diff between peak and
										// trough)
	public final int LOOKLENGTH = 20;
	private double TROUGHTIMEOUT = 0;
	private int peakLookCounter;
	private int troughLookCounter;
	private boolean Once;

	public StepDetector(int AXIS) {
		this.dataCurr = 0;
		this.dataCurrTS = 0;
		this.peakValue = 0;
		this.peakTS = 0;
		this.troughValue = 0;
		this.troughTS = 0;
		this.troughAvg = 0;
		this.waveformAvg = 0;
		this.peakLookCounter = 0;
		this.troughLookCounter = 0;
		this.peakVerified = false;
		this.troughVerified = false;
		
		this.peakAvg = 1;
		this.troughAvg = -1;
		this.PEAKAVGTHRESH = 100;
		this.TROUGHAVGTHRESH = 100;
		this.DIFFAVGTHRESH = 200;
		this.stepAxis = AXIS; // cannot be changed (final)
		

	}
	
	public void setThreshVariables(double initPeakAvg, double peakThresh,
			double inittroughAvg, double troughThresh, double diffThresh){
		this.peakAvg = initPeakAvg;
		this.troughAvg = inittroughAvg;
		this.PEAKAVGTHRESH = peakThresh;
		this.TROUGHAVGTHRESH = troughThresh;
		this.DIFFAVGTHRESH = diffThresh;
	}

	public void updateArray(ArrayList<AccelData> Data, double[] currFreq) {
		this.Data = Data;
		this.TROUGHTIMEOUT = 1000 / currFreq[this.stepAxis]; // timeout in ms
		return;
	}

	public boolean FindStep() {
		boolean stepVerified = false;

		this.dataCurr = this.Data.get(this.Data.size() - 1).getValue(
				this.stepAxis);
		this.dataCurrTS = this.Data.get(this.Data.size() - 1).getTimestamp();

		findPeak();
		this.peakVerified = verifyPeak();

		if (this.peakVerified) {
			if (this.stepAxis == MainActivity.Y_AXIS && Once == true){
				Once = false;
				stepVerified = verifyStep();
				return stepVerified;
			}
			findTrough();
			troughTimeout();
			this.troughVerified = verifyTrough();
			if (this.troughVerified) {
				stepVerified = verifyStep();
				resetDetect();
				return stepVerified;
			}
		}

		return stepVerified;
	}

	public void findPeak() {
		double Prev = 0;
		if (this.peakValue == 0) {
			int j = 1;
			while (j < this.LOOKLENGTH) {
				Prev = this.Data.get(this.Data.size() - 1 - j).getValue(
						this.stepAxis);
				if (Prev > this.dataCurr)
					break; // not a true peak
				j++;
			}
			if (j == this.LOOKLENGTH
					&& Math.abs(this.dataCurr - this.peakAvg) < this.PEAKAVGTHRESH) {
				// found a peak, need to verify
				this.peakValue = this.dataCurr; // store the supposed peak
				this.peakTS = this.dataCurrTS; // get peak time stamp
				//Log.d("MyApp", "found a peak");
			}
		}
		return;
	}

	public boolean verifyPeak() {
		// if you have a peak, and its not been verified (yet - counter
		// determines yet)
		if (this.peakValue != 0 && this.peakVerified == false
				&& this.peakLookCounter < this.LOOKLENGTH) {
			// still need to verify the current "supposed" peak
			if (this.peakValue > this.dataCurr) {
				this.peakLookCounter++;
			} else { // not a true peak resume find peak with current value
				this.peakValue = 0;
				this.peakTS = 0;
				this.peakLookCounter = 0;
				findPeak();
			}
			return false;
		} else {
			//Log.d("MyApp", "peak Verified");
			this.peakLookCounter = 0;
		}
		return true;
	}

	public void findTrough() {
		if (this.troughValue == 0) {
			// we have a verified peak, lets look for a trough here
			double Prev = this.Data.get(this.Data.size() - 2).getValue(
					this.stepAxis);
			long PrevTS = this.Data.get(this.Data.size() - 2).getTimestamp();
			if (this.dataCurr > Prev) { // started going back up = possible
										// trough
				if (Math.abs(Prev - this.troughAvg) < this.TROUGHAVGTHRESH) {
					//Log.d("MyApp", "found a trough");
					this.troughValue = Prev;
					this.troughTS = PrevTS;
				}
			}
		}
		return;
	}

	public boolean verifyTrough() {
		// if you have a trough, and its not been verified (yet - counter
		// determines yet)
		if (this.troughValue != 0 && this.troughVerified == false
				&& this.troughLookCounter < this.LOOKLENGTH) {
			// still need to verify the current "supposed" trough
			if (this.troughValue < this.dataCurr) {
				this.troughLookCounter++;
			} else { // not a true trough resume find trough with current value
				this.troughValue = 0;
				this.troughTS = 0;
				this.troughLookCounter = 0;
				findTrough();
			}
			return false;
		} else {
			//Log.d("MyApp", "trough Verified");
			this.troughLookCounter = 0;
		}
		return true;
	}

	public boolean verifyStep() {
		if (this.stepAxis == MainActivity.Y_AXIS){
			if (this.peakValue > this.peakAvg) {
				//Log.d("MyApp", "Step Verified! " + this.stepAxis);
				peakValue = 0;
				return true;
			}
			peakValue = 0;
			if (this.troughValue < this.troughAvg) {
				//Log.d("MyApp", "Step Verified! " + this.stepAxis);
				troughValue = 0;
				return true;
			}
			troughValue = 0;
			return false;
		}
		if ((this.peakValue - this.troughValue) > this.DIFFAVGTHRESH) {
			//Log.d("MyApp", "Step Verified! " + this.stepAxis);
			return true;
		}
		return false;
	}

	public void troughTimeout() {

		if (this.peakTS - this.dataCurrTS > this.TROUGHTIMEOUT) {
			//Log.d("MyApp", "Trough Timeout");
			resetDetect();
		}

		return;
	}

	public void resetDetect() {
		this.peakValue = 0;
		this.peakTS = 0;
		this.troughValue = 0;
		this.troughTS = 0;
		this.peakVerified = false;
		this.troughVerified = false;
		this.Once = true;
	}

}
