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


public class DemoDialog extends DialogFragment {
	
	public static double steps;
	public static double step_length;
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View TrainingView = inflater.inflate(R.layout.demo, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final EditText steps_txt = (EditText)TrainingView.findViewById(R.id.enter_steps);
        final EditText step_length_txt = (EditText)TrainingView.findViewById(R.id.enter_step_length);
        builder.setView(TrainingView)
               .setPositiveButton(R.string.done_str, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   steps = Double.parseDouble(steps_txt.getText().toString());
                	   step_length = Double.parseDouble(step_length_txt.getText().toString());
                	   // start 10 step training sequence
                       //((MainActivity)getActivity()).storeDistance(steps*step_length);
                	   MainActivity.userDistance = steps*step_length;
                	   //MapUI.updatePosition(steps*step_length);
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