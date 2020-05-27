package eu.tivian.musico.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import eu.tivian.musico.R;

/**
 * Custom scanner activity extending from {@link Activity} to display a custom layout form scanner view.
 */
public class CustomScannerActivity extends Activity implements
        DecoratedBarcodeView.TorchListener {
    /**
     * Manages barcode scanning for a CaptureActivity.
     */
    private CaptureManager capture;

    /**
     * Encapsulates BarcodeView, ViewfinderView and status text.
     */
    private DecoratedBarcodeView barcodeScannerView;

    /**
     * Creates and initializes the UI for the activity.
     *
     * @param savedInstanceState if the activity is being re-initialized after previously being
     *        shut down then this Bundle contains the data it most recently supplied in
     *        {@link #onSaveInstanceState(Bundle)}. This value may be {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_scanner);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setTorchListener(this);

        if (hasFlash())
            onTorchOff();

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.setShowMissingCameraPermissionDialog(false);
        capture.decode();
    }

    /**
     * Called after {@link #onRestoreInstanceState(Bundle)}, {@link #onRestart()}, or {@link #onPause()},
     *  for the activity to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    /**
     * Called as part of the activity lifecycle when the user no longer actively interacts with the activity,
     *  but it is still visible on screen.
     */
    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    /**
     * Perform any final cleanup before an activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    /**
     * Saves the current activity configuration.
     *
     * @param outState {@link Bundle} in which to place your saved state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    /**
     * Called when a key was pressed down and not handled by any of the views inside of the activity.
     *
     * @param keyCode the value in {@link KeyEvent#getKeyCode()}.
     * @param event description of the key event.
     * @return {@code true} to prevent this event from being propagated further,
     *         or {@code false} to indicate that you have not handled this event
     *         and it should continue to be propagated.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    /**
     * Check if the device's camera has a flashlight.
     *
     * @return {@code true} if there is Flashlight, otherwise {@code false}.
     */
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * Switches on the flashlight.
     */
    @Override
    public void onTorchOn() {
        barcodeScannerView.setOnClickListener(v -> barcodeScannerView.setTorchOff());
    }

    /**
     * Switches off the flashlight.
     */
    @Override
    public void onTorchOff() {
        barcodeScannerView.setOnClickListener(v -> barcodeScannerView.setTorchOn());
    }

    /**
     * Callback for the result from requesting permissions.
     *
     * @param requestCode the request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions the requested permissions. This value cannot be {@code null}.
     * @param grantResults the grant results for the corresponding permissions which is either
     *        {@link PackageManager#PERMISSION_GRANTED} or {@link PackageManager#PERMISSION_DENIED}.
     *        This value cannot be {@code null}.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
