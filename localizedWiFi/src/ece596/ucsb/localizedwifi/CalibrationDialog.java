package ece596.ucsb.localizedwifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;


public class CalibrationDialog extends DialogFragment {
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.calibration, null))
               .setPositiveButton(R.string.done_str, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       ((MainActivity)getActivity()).finishCalibration();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}