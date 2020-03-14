package it.challenge_engineering.www.broadcastlistener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by Marco on 03/03/2018.
 */

public class PasswordDialog extends DialogFragment
{

    EditText editPassword = null;
    TextView message = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.pwd_dialog, null);
        Button b1 = (Button) view.findViewById(R.id.pwdOkButton);
        Button b2 = (Button) view.findViewById(R.id.pwdCancelButton);

        editPassword = (EditText) view.findViewById(R.id.pwdText);
        message = (TextView) view.findViewById(R.id.msg);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // mListener.onDialogPositiveClick(PasswordDialog.this);

                CharSequence cseq = editPassword.getText();
                String pass = cseq.toString();
                if (pass.equals(("mmfmmf1965")))
                {
                    message.setText("Password corretta!");
                    ((MainActivity)getActivity()).ActivateLogging();
                    dismiss();
                }
                else
                {
                    message.setText("Password errata!");
                }
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                dismiss();
            }
        });

        builder.setView(view);
        return builder.create();
    }

}
