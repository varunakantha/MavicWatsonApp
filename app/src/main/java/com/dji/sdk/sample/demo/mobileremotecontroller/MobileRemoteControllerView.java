package com.dji.sdk.sample.demo.mobileremotecontroller;

import android.app.Service;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.controller.MainActivity;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.OnScreenJoystick;
import com.dji.sdk.sample.internal.utils.OnScreenJoystickListener;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Transcript;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;

import java.util.List;
import java.util.concurrent.Semaphore;

import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks.CompletionCallback;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;
import dji.sdk.mobilerc.MobileRemoteController;
import dji.sdk.products.Aircraft;

//import com.dji.sdk.sample.demo.ASRActivity;

/**
 * Class for mobile remote controller.
 */
public class MobileRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, PresentableView {

    FlightController flightController = ModuleVerificationUtil.getFlightController();

    Semaphore mutx = new Semaphore(1, true);
    private ToggleButton btnSimulator;
    private Button btnVoiceTest;
    private Button btnTakeOff;
    private Button autoLand;
    private Button forceLand;

    private TextView textView;

    private OnScreenJoystick screenJoystickRight;
    private OnScreenJoystick screenJoystickLeft;
    private MobileRemoteController mobileRemoteController;
    private FlightControllerKey isSimulatorActived;

    private MainActivity mainActivity;

    private SpeechToText speechService;
    private MicrophoneHelper microphoneHelper;
    private MicrophoneInputStream capture;
    private boolean listening = false;

    public MobileRemoteControllerView(Context context) {
        super(context);
        mainActivity = (MainActivity) context;
        init(context);
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setUpListeners();
    }

    @Override
    protected void onDetachedFromWindow() {
        tearDownListeners();
        super.onDetachedFromWindow();
    }

    private void init(Context context) {
        setClickable(true);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_mobile_rc, this, true);
        initAllKeys();
        initUI();
    }

    private void initAllKeys() {
        isSimulatorActived = FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE);
    }

    private void initUI() {
        btnVoiceTest = (Button) findViewById(R.id.btn_voice_test);
        btnTakeOff = (Button) findViewById(R.id.btn_take_off);
        autoLand = (Button) findViewById(R.id.btn_auto_land);
        autoLand.setOnClickListener(this);
        forceLand = (Button) findViewById(R.id.btn_force_land);
        forceLand.setOnClickListener(this);
        btnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);

        textView = (TextView) findViewById(R.id.textview_simulator);

        screenJoystickRight = (OnScreenJoystick) findViewById(R.id.directionJoystickRight);
        screenJoystickLeft = (OnScreenJoystick) findViewById(R.id.directionJoystickLeft);

        btnVoiceTest.setOnClickListener(this);
        btnTakeOff.setOnClickListener(this);
        btnSimulator.setOnCheckedChangeListener(MobileRemoteControllerView.this);

        Boolean isSimulatorOn = (Boolean) KeyManager.getInstance().getValue(isSimulatorActived);
        if (isSimulatorOn != null && isSimulatorOn) {
            btnSimulator.setChecked(true);
            textView.setText("Simulator is On.");
        }

        initServices();
    }

    //********************************************************************************************
    private void initServices() {
        speechService = initSpeechToTextService();
        microphoneHelper = new MicrophoneHelper(mainActivity);
    }

    private SpeechToText initSpeechToTextService() {
        SpeechToText service = new SpeechToText();
        String username = getContext().getString(R.string.speech_text_username);
        String password = getContext().getString(R.string.speech_text_password);
        service.setUsernameAndPassword(username, password);
        service.setEndPoint(getContext().getString(R.string.speech_text_url));
        return service;
    }

    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder().continuous(true).contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel").interimResults(true).inactivityTimeout(50000).build();
    }
    //********************************************************************************************

    private void setUpListeners() {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(final SimulatorState djiSimulatorStateData) {
                    ToastUtils.setResultToText(textView,
                            "Yaw : "
                                    + djiSimulatorStateData.getYaw()
                                    + ","
                                    + "X : "
                                    + djiSimulatorStateData.getPositionX()
                                    + "\n"
                                    + "Y : "
                                    + djiSimulatorStateData.getPositionY()
                                    + ","
                                    + "Z : "
                                    + djiSimulatorStateData.getPositionZ());
                }
            });
        } else {
            ToastUtils.setResultToToast("Disconnected!");
        }
        try {
            mobileRemoteController =
                    ((Aircraft) DJISampleApplication.getAircraftInstance()).getMobileRemoteController();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (mobileRemoteController != null) {
            textView.setText(textView.getText() + "\n" + "Mobile Connected");
        } else {
            textView.setText(textView.getText() + "\n" + "Mobile Disconnected");
        }
        screenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }

                if (mobileRemoteController != null) {
                    mobileRemoteController.setLeftStickHorizontal(pX);
                    mobileRemoteController.setLeftStickVertical(pY);
                }
            }
        });

        screenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                if (mobileRemoteController != null) {
                    mobileRemoteController.setRightStickHorizontal(pX);
                    mobileRemoteController.setRightStickVertical(pY);
                }
            }
        });
    }

    private void tearDownListeners() {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(null);
        }
        screenJoystickLeft.setJoystickListener(null);
        screenJoystickRight.setJoystickListener(null);
    }

    @Override
    public void onClick(View v) {
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        if (flightController == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_voice_test:
                if (!listening) {
                    capture = microphoneHelper.getInputStream(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                speechService.recognizeUsingWebSocket(capture, getRecognizeOptions(),
                                        new MicrophoneRecognizeDelegate());
                            } catch (Exception e) {
                                Toast.makeText(mainActivity, "Speech Capture error. " + e, Toast.LENGTH_LONG);
                            }
                        }
                    }).start();
                    listening = true;
                } else {
                    microphoneHelper.closeInputStream();
                    listening = false;
                }
                break;
            case R.id.btn_take_off:

                flightController.startTakeoff(new CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;
            case R.id.btn_force_land:
                flightController.confirmLanding(new CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;
            case R.id.btn_auto_land:
                flightController.startLanding(new CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == btnSimulator) {
            onClickSimulator(b);
        }
    }

    private void onClickSimulator(boolean isChecked) {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator == null) {
            return;
        }
        if (isChecked) {

            textView.setVisibility(VISIBLE);
            simulator.start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10),
                    new CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    });
        } else {

//            textView.setVisibility(INVISIBLE);
            simulator.stop(new CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_mobile_remote_controller;
    }

    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {

        String TAG = MicrophoneRecognizeDelegate.class.getSimpleName();
        @Override
        public void onTranscription(SpeechResults speechResults) {
            final List<Transcript> results = speechResults.getResults();
            if (results != null && !results.isEmpty()) {
                String text = results.get(0).getAlternatives().get(0).getTranscript();
                final String displayText = text;
                if (!text.isEmpty()) {
                    text = text.trim();
                }

                Log.d(TAG, "Text recognized as " + text);

                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(displayText);
                    }
                });

                if (text.equalsIgnoreCase("start")) {
                    try {
                        mutx.acquire();
                        Log.d(TAG, "TAKEOFF");
                        flightController.startTakeoff(new CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                mutx.release();
                                if (djiError != null) {
                                    Log.d(TAG, "dji error " + djiError);
                                    DialogUtils.showDialogBasedOnError(getContext(), djiError);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e("error ", e.getMessage());
                    }
                } else if (text.equalsIgnoreCase("stop")) {
                    try {
                        mutx.acquire();
                        Log.d(TAG, "LANDING");
                        flightController.startLanding(new CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                mutx.release();
                                if (djiError != null) {
                                    Log.d(TAG, "dji error " + djiError);
                                    DialogUtils.showDialogBasedOnError(getContext(), djiError);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, "error : " + e);
        }

        @Override
        public void onDisconnected() {
            Log.e(TAG, "disconnected");

        }
    }
}
