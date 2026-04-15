package com.example.tiktokcloneproject.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.tiktokcloneproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends Activity implements View.OnClickListener {
    private final String TAG = "CameraActivity";
    CameraManager manager;
    FrameLayout cameraFrameLayout;
    TextureView textureFront;
    String frontId, backId, defaultId;
    Size previewSize, videoSize;
    Button btnUploadVideo, btnClose, btnStopRecording, btnStartRecording;
    ImageButton btnFlip;
    FirebaseAuth mAuth;
    FirebaseUser user;

    MediaRecorder mediaRecorder;
    File videoFileHolder;
    boolean isRecording = false;
    String userId;
    File videoFolder;
    Animation animRotate;

    CameraDevice mainCamera;
    CameraCaptureSession mainCaptureSession;
    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;
    CaptureRequest.Builder captureRequestBuilder;

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {}
        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) { return true; }
        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        userId = (user != null) ? user.getUid() : "guest";

        createVideoFolder();
        
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnStartRecording = findViewById(R.id.button_record);
        btnClose = findViewById(R.id.button_close);
        btnFlip = findViewById(R.id.imb_flip_camera);
        btnStopRecording = findViewById(R.id.button_stop);

        btnUploadVideo.setOnClickListener(this);
        btnStartRecording.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnFlip.setOnClickListener(this);
        btnStopRecording.setOnClickListener(this);

        textureFront = findViewById(R.id.texture_view_front);
        animRotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
    }

    private void setupCamera(int width, int height) {
        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics character = manager.getCameraCharacteristics(id);
                if (character.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) frontId = id;
                else if (character.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) backId = id;
            }
            defaultId = (defaultId == null) ? backId : defaultId;
            StreamConfigurationMap map = manager.getCameraCharacteristics(defaultId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
            videoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), width, height);
        } catch (Exception e) { Log.e(TAG, "setupCamera error", e); }
    }

    private void connectCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }
        try {
            manager.openCamera(defaultId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mainCamera = camera;
                    startPreview();
                }
                @Override
                public void onDisconnected(@NonNull CameraDevice camera) { closeCamera(); }
                @Override
                public void onError(@NonNull CameraDevice camera, int error) { closeCamera(); }
            }, backgroundHandler);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void startPreview() {
        if (mainCamera == null || !textureFront.isAvailable()) return;
        try {
            SurfaceTexture texture = textureFront.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = mainCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            mainCamera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mainCaptureSession = session;
                    try {
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) { e.printStackTrace(); }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
            }, backgroundHandler);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void startRecording() {
        if (mainCamera == null) return;
        try {
            closeCaptureSession(); // Đóng session cũ trước khi tạo session mới để tránh crash
            videoFileHolder = createVideoFileName();
            mediaRecorder = new MediaRecorder();
            setupMediaRecorder();

            SurfaceTexture texture = textureFront.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(texture);
            Surface recordSurface = mediaRecorder.getSurface();

            captureRequestBuilder = mainCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(recordSurface);

            mainCamera.createCaptureSession(Arrays.asList(previewSurface, recordSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mainCaptureSession = session;
                    try {
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                        runOnUiThread(() -> {
                            try {
                                mediaRecorder.start();
                                isRecording = true;
                                updateUI(true);
                            } catch (Exception e) { Log.e(TAG, "mediaRecorder start fail", e); }
                        });
                    } catch (CameraAccessException e) { e.printStackTrace(); }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
            }, backgroundHandler);
        } catch (Exception e) { Log.e(TAG, "startRecording fail", e); }
    }

    private void stopRecording() {
        if (!isRecording) return;
        try {
            isRecording = false;
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            closeCamera();
            startUploadingActivity(Uri.fromFile(videoFileHolder));
        } catch (Exception e) { Log.e(TAG, "stopRecording fail", e); }
    }

    private void updateUI(boolean recording) {
        btnStopRecording.setVisibility(recording ? View.VISIBLE : View.GONE);
        btnStartRecording.setVisibility(recording ? View.GONE : View.VISIBLE);
        btnFlip.setVisibility(recording ? View.GONE : View.VISIBLE);
        btnUploadVideo.setVisibility(recording ? View.GONE : View.VISIBLE);
    }

    private void closeCaptureSession() {
        if (mainCaptureSession != null) {
            mainCaptureSession.close();
            mainCaptureSession = null;
        }
    }

    private void closeCamera() {
        closeCaptureSession();
        if (mainCamera != null) {
            mainCamera.close();
            mainCamera = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_record) startRecording();
        else if (id == R.id.button_stop) stopRecording();
        else if (id == R.id.imb_flip_camera) {
            defaultId = Objects.equals(defaultId, frontId) ? backId : frontId;
            closeCamera();
            connectCamera();
        } else if (id == R.id.btnUploadVideo) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            startActivityForResult(intent, 5);
        } else if (id == R.id.button_close) finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureFront.isAvailable()) {
            setupCamera(textureFront.getWidth(), textureFront.getHeight());
            connectCamera();
        } else {
            textureFront.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread("CameraBackground");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundHandlerThread != null) {
            backgroundHandlerThread.quitSafely();
            try { backgroundHandlerThread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
            backgroundHandlerThread = null; backgroundHandler = null;
        }
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height) {
        for (Size s : choices) {
            if (s.getWidth() == 1280 || s.getWidth() == 1920) return s;
        }
        return choices[0];
    }

    private void createVideoFolder() {
        videoFolder = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "TopTopVideos");
        if (!videoFolder.exists()) videoFolder.mkdirs();
    }

    private File createVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        return File.createTempFile(userId + "_" + timestamp, ".mp4", videoFolder);
    }

    private void setupMediaRecorder() throws IOException {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoFileHolder.getAbsolutePath());
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.prepare();
    }

    private void startUploadingActivity(Uri uri) {
        Intent i = new Intent(this, DescriptionVideoActivity.class);
        i.putExtra("videoUri", uri.toString());
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 5 && resultCode == RESULT_OK && data != null) {
            startUploadingActivity(data.getData());
        }
    }
}
