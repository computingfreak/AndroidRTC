package org.cfsms.androidrtc;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.List;

import fr.pchab.webrtcclient.PeerConnectionParameters;
import fr.pchab.webrtcclient.WebRtcClient;

public class RtcActivity extends Activity implements WebRtcClient.RtcListener {
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId;

    private static final String[] RequiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    protected PermissionChecker permissionChecker = new PermissionChecker();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.main);
        mSocketAddress = "http://" + getResources().getString(R.string.host) + ":" + getResources().getString(R.string.port) + "/";

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            final List<String> segments = intent.getData().getPathSegments();
            Log.i("sms", "list of segments : " + segments);
            callerId = segments.get(0);
        }

        Bundle b = intent.getBundleExtra("pass");
        int type = b.getInt("type");
        try {
            if (type == 1) {
                String own = b.getString("own");
                Log.i("sms", "Own id = " + own);
                PreferenceManager.getDefaultSharedPreferences(RtcActivity.this).edit().putString("MYUSERNAME", own).apply();
                answer(own);
            } else if (type == 2) {
                String other = b.getString("other");
                String own = b.getString("own");
                if (own.isEmpty() || own.trim().length() == 0) {
                    own = PreferenceManager.getDefaultSharedPreferences(RtcActivity.this).getString("MYUSERNAME", "defaultStringIfNothingFound");
                } else {
                    PreferenceManager.getDefaultSharedPreferences(RtcActivity.this).edit().putString("MYUSERNAME", own).apply();
                }
                Log.i("sms", "own = " + own + ", other id = " + other);
                call(other, own);
            }
        } catch (Exception e) {
            Log.i("sms", "I love Exceptions" + e);
        }
    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        client = new WebRtcClient(this, mSocketAddress, params, VideoRendererGui.getEGLContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
        if (client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if (client != null) {
            client.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if (client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {
        System.out.println("SMS oncallready");
        if (callerId != null) {
            try {
                answer(callerId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            String own = PreferenceManager.getDefaultSharedPreferences(RtcActivity.this).getString("MYUSERNAME", "defaultStringIfNothingFound");
            call(callId, own);
        }
    }

    public void answer(String callerId) throws JSONException {
        System.out.println("SMS answer callerID = " + callerId);
        client.sendMessage(callerId, "init", null);
        startCam(callerId);
    }

    public void call(String callId, String ownUserName) {
        System.out.println("SMS call callID = " + callId);
        Intent msg = new Intent(Intent.ACTION_SEND);
        msg.putExtra(Intent.EXTRA_TEXT, mSocketAddress + callId);
        msg.setType("text/plain");
        Bundle b = new Bundle();
        b.putString("own", ownUserName);
        msg.putExtra("id", b);
        startActivityForResult(Intent.createChooser(msg, "Call someone :"), VIDEO_CALL_SENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VIDEO_CALL_SENT) {
            String ownUserName = "";
            if (data != null) {
                Bundle b = data.getBundleExtra("id");
                ownUserName = data.getStringExtra(b.getString("own"));
            }
            if (ownUserName.isEmpty() || ownUserName.trim().length() == 0) {
                ownUserName = PreferenceManager.getDefaultSharedPreferences(RtcActivity.this).getString("MYUSERNAME", "defaultStringIfNothingFound");
            }
            startCam(ownUserName);
        }
    }

    public void startCam(String ownUserName) {
        // Camera settings
        if (PermissionChecker.hasPermissions(this, RequiredPermissions)) {
            client.start(ownUserName);
        }
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, false);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType, false);
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, false);
    }
}