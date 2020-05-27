package eu.tivian.musico.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import eu.tivian.musico.R;
import eu.tivian.musico.SharedViewModel;

/**
 * Setting dialog.
 */
public class SettingsDialogFragment extends DialogFragment {
    /**
     * The Last.fm username.
     */
    private EditText username;

    /**
     * Chosen app language.
     */
    private Spinner language;

    /**
     * Creates the settings dialog, which gives the option to change the language of the application
     *  and change which Last.fm user info should be shown in the app.
     *
     * @param savedInstanceState always {@code null}.
     * @return a new {@link Dialog} instance to be displayed by the {@link Fragment}.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_settings, null);

        username = view.findViewById(R.id.et_username);
        language = view.findViewById(R.id.spinner_language);

        final SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.getLanguage().observe(this, pos -> language.setSelection(pos));
        viewModel.getUsername().observe(this, login -> username.setText(login));

        builder.setView(view).setTitle(R.string.title_settings)
           .setNegativeButton(R.string.dialog_cancel, null)
           .setPositiveButton(R.string.dialog_save, (dialog, which) -> {
               viewModel.setLanguage(language.getSelectedItemPosition());
               viewModel.setUsername(username.getText().toString());
        });

        return builder.create();
    }
}
