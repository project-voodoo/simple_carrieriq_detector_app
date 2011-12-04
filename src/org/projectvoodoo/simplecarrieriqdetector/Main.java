
package org.projectvoodoo.simplecarrieriqdetector;

import org.projectvoodoo.simplecarrieriqdetector.Detect.DetectTest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Main extends Activity {

    private DetectorTask dt = new DetectorTask();
    private Detect detect;
    private Boolean ccDev = true;
    private final static String TAG = "Voodoo SimpleCarrierIQDetector Main";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Display version in app title.
         */
        try {
            setTitle(getTitle() + " "
                    + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (NameNotFoundException e) {
        }

        setContentView(R.layout.main);

        // run asynchronously the detection stuff
        dt = new DetectorTask();
        dt.execute();

    }

    android.view.View.OnClickListener sendClickListener = new android.view.View.OnClickListener() {

        @Override
        public void onClick(View v) {
            askUserForReportCC();
        }
    };

    private void askUserForReportCC() {
        ccDev = true;

        AlertDialog builder = new AlertDialog.Builder(this)
                .setMultiChoiceItems(R.array.dialog,
                        new boolean[] {
                            true
                        },
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton,
                                    boolean isChecked) {

                                ccDev = isChecked;
                            }
                        })
                .setPositiveButton(R.string.send_report,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                sendReport();
                            }
                        })
                .create();

        builder.show();

    }

    /*
     * export the detection results by mail or anything able to share text
     */

    private void sendReport() {

        int detectionScore = detect.getDetectionScore();

        String content = "";
        content += getString(R.string.email_template);
        content += "Build fingerprint:\n" + Build.FINGERPRINT + "\n\n";

        if (detectionScore == 0)
            content += getString(R.string.not_found);
        else if (detect.getFound().get(DetectTest.RUNNING_PROCESSES).size() > 0)
            content += getString(R.string.found_active);
        else
            content += getString(R.string.found_inactive);

        content += "\nDetection score: " + detect.getDetectionScore();

        for (DetectTest test : detect.getFound().keySet()) {
            content += "\n\n\nTest for: " + test.name + "\n(" + test + ", weight "
                    + test.weight + ")\n";

            if (detect.getFound().get(test).size() == 0) {
                content += "\n    nothing found";
            } else {
                for (String line : detect.getFound().get(test))
                    content += "\n    found:    " + line;
            }
        }
        try {
            String appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            content += "\n\n\n-- \n";
            content += getString(R.string.app_name) + " " + appVersion;

        } catch (Exception e) {
        }

        String[] mailCC = {
                "Project Voodoo developer <pr" +
                        "oject.v" +
                        "oodoo.co" +
                        "ntact@g" +
                        "mai" +
                        "l.com>"
        };

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, Build.BRAND + " " + Build.MODEL + " ("
                + Build.DEVICE + ")"
                + " Voodoo Carrier IQ Detector report");
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        if (ccDev)
            sendIntent.putExtra(Intent.EXTRA_CC, mailCC);
        sendIntent.setType("text/plain");

        Intent chooser = Intent.createChooser(sendIntent, "send report mail");
        chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(chooser);

    }

    private class DetectorTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected void onPostExecute(Integer detectionScore) {
            super.onPostExecute(detectionScore);

            TextView resultDisplay = (TextView) findViewById(R.id.result_display);
            if (detectionScore == 0) {
                resultDisplay.setText(R.string.not_found);
                resultDisplay.setTextColor(Color.GREEN);
            } else if (detect.getFound().get(DetectTest.RUNNING_PROCESSES).size() > 0) {
                resultDisplay.setText(R.string.found_active);
                resultDisplay.setTextColor(Color.RED);
            } else {
                resultDisplay.setText(R.string.found_inactive);
                resultDisplay.setTextColor(Color.YELLOW);
            }

            TextView numericScore = (TextView) findViewById(R.id.numeric_score);
            if (detectionScore > 0)
                numericScore.setText("detection score (less is better): " + detectionScore);
            else
                numericScore.setVisibility(View.GONE);

            LinearLayout details = (LinearLayout) findViewById(R.id.details_list);

            for (DetectTest test : detect.getFound().keySet()) {

                TextView title = new TextView(Main.this);
                title.setText(test.name + "\nweight: " + test.weight);
                title.setPadding(8, 8, 8, 8);
                title.setTextSize(20);
                title.setTextColor(Color.WHITE);
                details.addView(title);

                if (detect.getFound().get(test).size() > 0) {
                    for (String line : detect.getFound().get(test)) {
                        TextView content = new TextView(Main.this);
                        content.setText(line);
                        content.setPadding(8, 0, 8, 0);
                        details.addView(content);
                    }
                } else {
                    TextView content = new TextView(Main.this);
                    content.setText("Nothing found");
                    content.setPadding(8, 0, 8, 0);
                    details.addView(content);
                }
            }

            Button sendButton = (Button) findViewById(R.id.send_report);
            sendButton.setEnabled(true);
            sendButton.setOnClickListener(sendClickListener);

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ProgressBar pb = (ProgressBar) findViewById(R.id.detectionProgress);
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            ProgressBar pb = (ProgressBar) findViewById(R.id.detectionProgress);
            pb.setProgress(0);
            pb.setVisibility(View.INVISIBLE);
        }

        protected Integer doInBackground(Void... params) {

            detect = new Detect(getApplicationContext());
            detect.findEverything();
            detect.dumpFoundInLogcat();

            return detect.getDetectionScore();
        }

    }
}
