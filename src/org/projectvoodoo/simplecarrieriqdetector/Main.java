
package org.projectvoodoo.simplecarrieriqdetector;

import org.projectvoodoo.simplecarrieriqdetector.Detect.DetectTest;
import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Main extends Activity {

    private DetectorTask dt = new DetectorTask();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // run asynchronously the detection stuff
        dt = new DetectorTask();
        dt.execute();
    }

    private class DetectorTask extends AsyncTask<Void, Integer, Integer> {

        private Detect detect;

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
                resultDisplay.setTextColor(Color.RED);
            }

            TextView numericScore = (TextView) findViewById(R.id.numeric_score);
            if (detectionScore > 0)
                numericScore.setText("detection score (less is better): " + detectionScore);
            else
                numericScore.setVisibility(View.GONE);

            LinearLayout details = (LinearLayout) findViewById(R.id.details_list);

            for (DetectTest test : detect.getFound().keySet()) {

                TextView title = new TextView(Main.this);
                title.setText(test.name + "\nconfidence level: " + test.confidenceLevel);
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
