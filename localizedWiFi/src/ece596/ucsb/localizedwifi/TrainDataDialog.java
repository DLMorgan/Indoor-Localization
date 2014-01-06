package ece596.ucsb.localizedwifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public class TrainDataDialog extends DialogFragment {
	
	public static double height_feet;
	public static double height_inches;
	public static double height_metric;
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View TrainingView = inflater.inflate(R.layout.fragment_enter_height, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final EditText Height_feet_text = (EditText)TrainingView.findViewById(R.id.enter_height_feet);
        final EditText Height_inches_text = (EditText)TrainingView.findViewById(R.id.enter_height_inches);
        builder.setView(TrainingView)
               .setPositiveButton(R.string.done_str, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   height_feet = Double.parseDouble(Height_feet_text.getText().toString());
                	   height_inches = Double.parseDouble(Height_inches_text.getText().toString());
                	   height_metric = convertToMetric(height_feet, height_inches);
                	   // start 10 step training sequence
                       ((MainActivity)getActivity()).startCalibration(height_metric);
                   }
               })
               .setNegativeButton(R.string.cancel_str, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // do nothing
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
    public double convertToMetric(double feet, double inches){
    	return ((feet*12 + inches)/39.370);
    }
}