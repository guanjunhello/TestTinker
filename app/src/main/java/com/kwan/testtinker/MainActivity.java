package com.kwan.testtinker;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.tinker.lib.tinker.TinkerInstaller;

public class MainActivity extends AppCompatActivity {

    private TextView tv_test;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_test = (TextView) findViewById(R.id.tv_test);

        findViewById(R.id.btn_show_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = tv_test.getText().toString();
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_load_patch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch_signed_7zip.apk");
            }
        });
    }
}
