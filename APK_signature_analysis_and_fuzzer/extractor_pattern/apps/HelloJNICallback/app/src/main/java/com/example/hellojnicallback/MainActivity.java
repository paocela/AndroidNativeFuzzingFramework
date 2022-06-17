/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.hellojnicallback;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    int hour = 0;
    int minute = 0;
    int second = 0;
    int fieldToRetrieve = 7;
    String msg;

    TextView tickView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tickView = (TextView) findViewById(R.id.tickView);

        msg = readFromFile("/data/user/0/com.example.hellojnicallback", "file_in.txt");

        String jstr = getStringFromJNI(msg);

        Log.i("LivenessTest", "Done");

        ((TextView)findViewById(R.id.hellojniMsg)).setText(jstr);
    }
    @Override
    public void onResume() {
        super.onResume();
        hour = minute = second = 0;

        msg = readFromFile("/data/user/0/com.example.hellojnicallback", "file_in.txt");

        // ((TextView)findViewById(R.id.hellojniMsg)).setText(getStringFromJNI(msg));
        startTicks();
    }

    @Override
    public void onPause () {
        super.onPause();
        StopTicks();
    }

    /*
     * A function calling from JNI to update current timer
     */
    @Keep
    private void updateTimer() {
        ++second;
        if(second >= 60) {
            ++minute;
            second -= 60;
            if(minute >= 60) {
                ++hour;
                minute -= 60;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String ticks = "" + MainActivity.this.hour + ":" +
                        MainActivity.this.minute + ":" +
                        MainActivity.this.second;
                MainActivity.this.tickView.setText(ticks);
            }
        });
    }

    private String getContentFromUrl(String url) {
        StringBuilder content = new StringBuilder();
        // Use try and catch to avoid the exceptions
        try
        {
            URL urlObj = new URL(url); // creating a url object
            URLConnection urlConnection = urlObj.openConnection(); // creating a urlconnection object

            // wrapping the urlconnection in a bufferedreader
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            // reading from the urlconnection using the bufferedreader
            while ((line = bufferedReader.readLine()) != null)
            {
                content.append(line + "\n");
            }
            bufferedReader.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return content.toString();
    }


    /*
     * A function to read file from SD card (gives permissions denied in accessing SD storage)
     */
    private String readFromFile(String path, String file_name) {
        //Find the directory for the SD Card using the API
        //*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();

        // Construct filename
        String complete_file_name = path + "/" + file_name;

        // Get the text file
        File file = new File(complete_file_name);

        // Read text from file
        StringBuilder text = new StringBuilder();

        try {
            FileReader file_r = new FileReader(file);
            BufferedReader br = new BufferedReader(file_r);
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return text.toString();
    }

    private void forkTest(String msg) {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        int numThreads = threads.size();
        Log.i("ForkTest", "Test msg: " + msg + " (#threads = " + numThreads + ")");
        Log.i("ForkTest", "Threads names:");
        Iterator<Thread> threadsIter = threads.iterator();
        while (threadsIter.hasNext()) {
            Log.i("ForkTest", threadsIter.next().getName());
        }
    }

    static {
        // System.loadLibrary("testLib");
        System.loadLibrary("hello-jnicallback");
    }

    public native  String getStringFromJNI(String str);
    public native void startTicks();
    public native void StopTicks();
}
