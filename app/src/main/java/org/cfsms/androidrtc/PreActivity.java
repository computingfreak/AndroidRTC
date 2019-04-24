package org.cfsms.androidrtc;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PreActivity extends AppCompatActivity {

    private static final String[] RequiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    protected PermissionChecker permissionChecker = new PermissionChecker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        checkPermissions();

        AppCompatButton register = findViewById(R.id.register);
        AppCompatButton spam = findViewById(R.id.disturbuser);
        final EditText own = findViewById(R.id.own_username);
        final EditText other = findViewById(R.id.friends_username);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent trigger = new Intent(PreActivity.this, RtcActivity.class);
                Bundle b = new Bundle();
                b.putString("own",own.getText().toString());
                b.putInt("type",1);
                trigger.putExtra("pass",b);
                startActivity(trigger);
                finish();
            }
        });

        spam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent trigger = new Intent(PreActivity.this, RtcActivity.class);
                Bundle b = new Bundle();
                b.putString("other",other.getText().toString());
                b.putString("own", own.getText().toString());
                b.putInt("type",2);
                trigger.putExtra("pass",b);
                startActivity(trigger);
                finish();
            }
        });
    }

    private void checkPermissions() {
        permissionChecker.verifyPermissions(this, RequiredPermissions, new PermissionChecker.VerifyPermissionsCallback() {

            @Override
            public void onPermissionAllGranted() {

            }

            @Override
            public void onPermissionDeny(String[] permissions) {
                Toast.makeText(PreActivity.this, "Please grant required permissions.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
