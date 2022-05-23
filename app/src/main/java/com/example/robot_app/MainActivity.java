package com.example.robot_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.ubtechinc.cruzr.sdk.dance.DanceConnectionListener;
import com.ubtechinc.cruzr.sdk.dance.DanceControlApi;
import com.ubtechinc.cruzr.sdk.face.CruzrFaceApi;
import com.ubtechinc.cruzr.sdk.ros.RosRobotApi;
import com.ubtechinc.cruzr.sdk.speech.SpeechRobotApi;
import com.ubtechinc.cruzr.serverlibutil.interfaces.InitListener;
import com.ubtechinc.cruzr.serverlibutil.interfaces.SpeechTtsListener;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {


    private Button btn;
    private static final String TAG = "MyTag";
    private String topic, clientID;
    private MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init_Robot();
        init();
    }

    private void init() {
        btn = findViewById(R.id.btn_sub);
        clientID = "cruzr_robot";
        topic = "cruzr/obuda";
        client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883",
                        clientID);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectX();
            }
        });
    }

    private void connectX() {
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    sub();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(MainActivity.this, "NoConnected", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void sub() {
        try {
            client.subscribe(topic, 0);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    String msg = new String(message.getPayload());

                    switch (msg.charAt(0)) {

                        //LedSetOnOff
                        case 'L':
                            msg = msg.substring(2);
                            if (msg.equals("on")) {
                                ledSetOnOff(true);
                            }
                            else if (msg.equals("off")) {
                                ledSetOnOff(false);
                            }
                            break;

                        //LedSetOnOff
                        case 'C':
                            msg = msg.substring(2);

                            String[] partsC = msg.split(",");

                            int r = Integer.parseInt(partsC[0]);
                            int g = Integer.parseInt(partsC[1]);
                            int b = Integer.parseInt(partsC[2]);
                            int times = Integer.parseInt(partsC[3]);

                            ledSetColor(r, g, b, times);

                            break;

                        //Preset action
                        case 'P':
                            msg = msg.substring(2);
                            setCurrentMap("robotproject");
                            run(msg);
                            break;

                        //Head control
                        case 'H':
                            msg = msg.substring(2);
                            int angle = Integer.parseInt(msg);
                            moveHead(angle);
                            break;

                        //Speech
                        case 'T':
                            msg = msg.substring(2);
                            tts(msg);
                            break;

                        //Dance
                        case 'D':
                            msg = msg.substring(2);
                            if (msg.equals("stop")) {
                                stopDance();
                            } else {
                                int num = Integer.parseInt(msg);
                                dance(num);
                            }
                            break;

                        //Move
                        case 'M':
                            msg = msg.substring(2);
                            if (msg.equals("stop")) {
                                stopMove();
                            } else {
                                setCurrentMap("robotproject");

                                String[] partsM = msg.split(",");

                                float x = Float.parseFloat(partsM[0]);
                                float y = Float.parseFloat(partsM[1]);
                                float theta = Float.parseFloat(partsM[2]);
                                float maxSpeed = Float.parseFloat(partsM[3]);

                                moveTo(x, y, theta, maxSpeed);
                            }
                            break;

                        //Error
                        default:
                            Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        }catch (MqttException e) {

        }
    }


    //init
    public void init_Robot() {
        RosRobotApi.get().initializ(this, new InitListener() {
            @Override
            public void onInit() {
                //Initialization successful
            }
        });

        SpeechRobotApi.get().initializ(this, 103, new InitListener() {
            @Override
            public void onInit() {
                //Initialization successful
            }
        });

        DanceControlApi.getInstance().initialize(this, new DanceConnectionListener() {
            @Override
            public void onConnected() {
                //Initialization successful
            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onReconnected() {

            }
        });

        CruzrFaceApi.initCruzrFace(this);
    }


    //Speech
    public void tts(String text) {
        SpeechRobotApi.get().speechStartTTS(text, new SpeechTtsListener());
    }


    //LedSetOnOff
    public void ledSetOnOff(boolean onOff) {
        RosRobotApi.get().ledSetOnOff(onOff);
    }


    //LedSetColor
    public void ledSetColor(int r, int g, int b, int times) {
        RosRobotApi.get().ledSetColor(r, g, b, times);
    }


    //Preset action
    public void run(String run) {
        RosRobotApi.get().run(run);
    }


    //Head control
    public void moveHead(double angle) {
        double PER_SECOND_ANGLE = 30 * Math.PI / 180;
        double duration = Math.abs(angle) / PER_SECOND_ANGLE;
        if (duration < 1) {
            duration = 1;
        }
        RosRobotApi.get().setAngles( "HeadPitch", (float) angle, (float) duration);
    }


    //Dance
    public void dance(int num) {
        List<String> danceNameList = DanceControlApi.getInstance().getDanceList();
        DanceControlApi.getInstance().dance(danceNameList.get(num));
    }


    //Dance stop
    public void stopDance() {
        DanceControlApi.getInstance().stop();
    }


    //Set current Map
    public void setCurrentMap(String map) {
        RosRobotApi.get().setCurrentMap(map);
    }


    //Move
    public void moveTo(float x, float y, float theta, float maxSpeed) {
        RosRobotApi.get().moveTo(x, y, theta, maxSpeed);
    }


    //Move stop
    public void stopMove() {
        RosRobotApi.get().stopMove();
    }

}