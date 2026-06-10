package com.dashlander.console;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Dashlander Console\nNative uploader alpha");
        title.setTextSize(22);
        root.addView(title);

        Button inspect = new Button(this);
        inspect.setText("Inspect GitHub uploads");
        root.addView(inspect);

        Button upload = new Button(this);
        upload.setText("Upload selected level");
        root.addView(upload);

        log = new TextView(this);
        log.setText("Ready. Termux replacement begins here.\n");
        log.setTextSize(14);
        root.addView(log);

        inspect.setOnClickListener(v -> append("Inspect flow not wired yet.\n"));
        upload.setOnClickListener(v -> append("Upload flow not wired yet.\n"));

        setContentView(scroll);
    }

    private void append(String text) {
        log.append(text);
    }
}
