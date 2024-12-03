package com.example.cameraapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ImageView ivClickedImage;
    private ExecutorService executorService;
    private ImageCapture imageCapture = null;
    private File outputDirectory;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private final int REQUEST_CODE = 10;
    String cameraId = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        previewView = findViewById(R.id.previewView);
        FloatingActionButton btnCaptureImage = findViewById(R.id.btnCaptureImage);
        ImageButton btnFlipCamera = findViewById(R.id.btnFlipCamera);
        ivClickedImage = findViewById(R.id.ivClickedImage);

        if (allPermissionsGranted()) {
            startCamera(false);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE);
        }

        btnFlipCamera.setOnClickListener(v -> {
            switchCamera();
        });

        btnCaptureImage.setOnClickListener(v -> {
            takePhoto();
        });
        outputDirectory = getOutputDirectory();
        executorService = Executors.newSingleThreadExecutor();
    }

    private void switchCamera() {
        startCamera(cameraId.equals("Front"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera(false);
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "Permissions not granted",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission: REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void startCamera(boolean isFrontCamera) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (Exception e) {
                Toast.makeText(
                        getApplicationContext(),
                        e.getLocalizedMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder().build();

            CameraSelector cameraSelector;

            if (isFrontCamera) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraId = "Back";
            } else {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraId = "Front";
            }

            try {
                assert cameraProvider != null;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        MainActivity.this, cameraSelector, preview, imageCapture
                );
            } catch (Exception e) {
                Toast.makeText(
                        getApplicationContext(),
                        e.getLocalizedMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private File getOutputDirectory() {
        File mediaDir = null;
        if (getExternalMediaDirs().length > 0) {
            mediaDir = new File(getExternalMediaDirs()[0], getResources().getString(R.string.app_name));
            mediaDir.mkdirs();
        }
        return (mediaDir != null && mediaDir.exists()) ? mediaDir : getFilesDir();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss";
        File photoFile = new File(outputDirectory, new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri imgUri = Uri.fromFile(photoFile);
                        ivClickedImage.setImageURI(imgUri);
                        new Handler().postDelayed(() -> ivClickedImage.setVisibility(View.GONE), 2000);
                        ivClickedImage.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(
                                getApplicationContext(),
                                exception.getLocalizedMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}