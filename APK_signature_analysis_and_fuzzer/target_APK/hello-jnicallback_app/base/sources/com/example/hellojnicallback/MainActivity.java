package com.example.hellojnicallback;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
/* loaded from: classes3.dex */
public class MainActivity extends AppCompatActivity {
    String msg;
    TextView tickView;
    int hour = 0;
    int minute = 0;
    int second = 0;
    int fieldToRetrieve = 7;

    public native void StopTicks();

    public native String getStringFromJNI(String str);

    public native void startTicks();

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.tickView = (TextView) findViewById(R.id.tickView);
        String readFromFile = readFromFile("/data/user/0/com.example.hellojnicallback", "file_in.txt");
        this.msg = readFromFile;
        String jstr = getStringFromJNI(readFromFile);
        Log.i("LivenessTest", "Done");
        ((TextView) findViewById(R.id.hellojniMsg)).setText(jstr);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        this.second = 0;
        this.minute = 0;
        this.hour = 0;
        this.msg = readFromFile("/data/user/0/com.example.hellojnicallback", "file_in.txt");
        startTicks();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPause() {
        super.onPause();
        StopTicks();
    }

    private void updateTimer() {
        int i = this.second + 1;
        this.second = i;
        if (i >= 60) {
            int i2 = this.minute + 1;
            this.minute = i2;
            this.second = i - 60;
            if (i2 >= 60) {
                this.hour++;
                this.minute = i2 - 60;
            }
        }
        runOnUiThread(new Runnable() { // from class: com.example.hellojnicallback.MainActivity.1
            @Override // java.lang.Runnable
            public void run() {
                String ticks = "" + MainActivity.this.hour + ":" + MainActivity.this.minute + ":" + MainActivity.this.second;
                MainActivity.this.tickView.setText(ticks);
            }
        });
    }

    private String getContentFromUrl(String url) {
        StringBuilder content = new StringBuilder();
        try {
            URL urlObj = new URL(url);
            URLConnection urlConnection = urlObj.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                content.append(line + "\n");
            }
            bufferedReader.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return content.toString();
    }

    private String readFromFile(String path, String file_name) {
        Environment.getExternalStorageDirectory();
        String complete_file_name = path + "/" + file_name;
        File file = new File(complete_file_name);
        StringBuilder text = new StringBuilder();
        try {
            FileReader file_r = new FileReader(file);
            BufferedReader br = new BufferedReader(file_r);
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return text.toString();
    }

    private int forkTest(String msg) {
        System.out.println("[1-JAVA] DEBUG");
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        System.out.println("[2-JAVA] DEBUG");
        int numThreads = threads.size();
        System.out.println("[3-JAVA] DEBUG");
        Log.i("ForkTest", "Test msg: " + msg + " (#threads = " + numThreads + ")");
        Log.i("ForkTest", "Threads names:");
        System.out.println("[4-JAVA] DEBUG");
        System.out.println("[5-JAVA] DEBUG");
        for (Thread thread : threads) {
            Log.i("ForkTest", thread.getName());
        }
        return numThreads;
    }

    static {
        System.loadLibrary("hello-jnicallback");
    }
}
