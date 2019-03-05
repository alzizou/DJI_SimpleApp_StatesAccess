package com.dji.FPVDemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.imu.IMUState;

public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;

    private Handler handler;
    private Handler handler1;

    private double droneLocationLat = 0.0d;
    private double droneLocationLng = 0.0d;
    private double droneLocationAlt = 0.0d;
    private double droneVelocityX = 0.0d;
    private double droneVelocityY = 0.0d;
    private double droneVelocityZ = 0.0d;
    private double dronePitch = 0.0d;
    private double droneRoll = 0.0d;
    private double droneYaw = 0.0d;
    private double droneWx = 0.0d;
    private double droneWy = 0.0d;
    private double droneWz = 0.0d;
    private FlightController mFlightController;

    private TextView txt_long;
    private TextView txt_lat;
    private TextView txt_alt;
    private TextView txt_Vx;
    private TextView txt_Vy;
    private TextView txt_Vz;
    private TextView txt_pitch;
    private TextView txt_roll;
    private TextView txt_yaw;
    private TextView txt_Wx;
    private TextView txt_Wy;
    private TextView txt_Wz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_long = findViewById(R.id.textView4);
        txt_lat = findViewById(R.id.textView5);
        txt_alt = findViewById(R.id.textView6);
        txt_Vx = findViewById(R.id.textView13);
        txt_Vy = findViewById(R.id.textView14);
        txt_Vz = findViewById(R.id.textView15);
        txt_pitch = findViewById(R.id.textView19);
        txt_roll = findViewById(R.id.textView20);
        txt_yaw = findViewById(R.id.textView21);
        txt_Wx = findViewById(R.id.textView25);
        txt_Wy = findViewById(R.id.textView26);
        txt_Wz = findViewById(R.id.textView27);

        txt_long.setText(String.valueOf(0.0));
        txt_lat.setText(String.valueOf(0.0));
        txt_alt.setText(String.valueOf(0.0));
        txt_Vx.setText(String.valueOf(0.0));
        txt_Vy.setText(String.valueOf(0.0));
        txt_Vz.setText(String.valueOf(0.0));
        txt_pitch.setText(String.valueOf(0.0d));
        txt_roll.setText(String.valueOf(0.0d));
        txt_yaw.setText(String.valueOf(0.0d));
        txt_Wx.setText(String.valueOf(0.0d));
        txt_Wy.setText(String.valueOf(0.0d));
        txt_Wz.setText(String.valueOf(0.0d));

        handler = new Handler();
        handler1 = new Handler();

        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver1, filter);

        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        Camera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }
        handler1.post(runnable1);
    }

    Runnable runnable1= new Runnable() {
        @Override
        public void run() {
            if (mFlightController != null) {
                mFlightController.setStateCallback(new FlightControllerState.Callback() {

                    @Override
                    public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                        droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                        droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                        droneLocationAlt = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                        droneVelocityX = djiFlightControllerCurrentState.getVelocityX();
                        droneVelocityY = djiFlightControllerCurrentState.getVelocityY();
                        droneVelocityZ = djiFlightControllerCurrentState.getVelocityZ();
                        dronePitch = djiFlightControllerCurrentState.getAttitude().pitch;
                        droneRoll = djiFlightControllerCurrentState.getAttitude().roll;
                        droneYaw = djiFlightControllerCurrentState.getAttitude().yaw;
                    }
                });
            }
            if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                txt_long.setText(String.valueOf(droneLocationLng));
                txt_lat.setText(String.valueOf(droneLocationLat));
                txt_alt.setText(String.valueOf(droneLocationAlt));
                txt_Vx.setText(String.valueOf(droneVelocityX));
                txt_Vy.setText(String.valueOf(droneVelocityY));
                txt_Vz.setText(String.valueOf(droneVelocityZ));
                txt_pitch.setText(String.valueOf(dronePitch));
                txt_roll.setText(String.valueOf(droneRoll));
                txt_yaw.setText(String.valueOf(droneYaw));
            }
            handler1.postDelayed(this, 100);
        }
    };

    protected BroadcastReceiver mReceiver1 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductChange();
        }
    };

    protected void onProductChange() {
        initPreviewer();
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void initFlightController() {

        BaseProduct product1 = FPVDemoApplication.getProductInstance();
        if (product1 != null && product1.isConnected()) {
            if (product1 instanceof Aircraft) {
                mFlightController = ((Aircraft) product1).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    droneLocationAlt = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                    droneVelocityX = djiFlightControllerCurrentState.getVelocityX();
                    droneVelocityY = djiFlightControllerCurrentState.getVelocityY();
                    droneVelocityZ = djiFlightControllerCurrentState.getVelocityZ();
                    dronePitch = djiFlightControllerCurrentState.getAttitude().pitch;
                    droneRoll = djiFlightControllerCurrentState.getAttitude().roll;
                    droneYaw = djiFlightControllerCurrentState.getAttitude().yaw;
                }
            });
        }
        if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
            txt_long.setText(String.valueOf(droneLocationLng));
            txt_lat.setText(String.valueOf(droneLocationLat));
            txt_alt.setText(String.valueOf(droneLocationAlt));
            txt_Vx.setText(String.valueOf(droneVelocityX));
            txt_Vy.setText(String.valueOf(droneVelocityY));
            txt_Vz.setText(String.valueOf(droneVelocityZ));
            txt_pitch.setText(String.valueOf(dronePitch));
            txt_roll.setText(String.valueOf(droneRoll));
            txt_yaw.setText(String.valueOf(droneYaw));
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        initFlightController();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            default:
                break;
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError == null) {
                                                showToast("take photo: success");
                                            } else {
                                                showToast(djiError.getDescription());
                                            }
                                        }
                                    });
                                }
                            }, 2000);
                        }
                    }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }
}
