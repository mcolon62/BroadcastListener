package it.challenge_engineering.www.broadcastlistener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Marco on 19/07/2017.
 */

public class PasswordDialog extends DialogFragment {

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // public String nomeOspite = "";
    // public String messaggioAllarme;

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) getActivity();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString() + " must implement NoticeDialogListener");
        }
    }

    static PasswordDialog newInstance() {
        return new PasswordDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.dialogPWD, null);
        Button b1 = (Button) view.findViewById(R.id.alarmCloseButton);
        // TextView alarmMessage = (TextView) view.findViewById(R.id.messaggioAllarme);
        // alarmMessage.setText(messaggioAllarme);
        // TextView  patientName = (TextView) view.findViewById(R.id.nomeAllarme);
        // patientName.setText(nomeOspite);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                mListener.onDialogPositiveClick(PasswordDialog.this);
            }
        });

        builder.setView(view);
        return builder.create();
    }
}
