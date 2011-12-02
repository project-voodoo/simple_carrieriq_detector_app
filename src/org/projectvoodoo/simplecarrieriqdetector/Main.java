
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
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            TextView score = (TextView) findViewById(R.id.score);
            score.setText(result + "");

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
