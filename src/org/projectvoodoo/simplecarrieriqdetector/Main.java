
package org.projectvoodoo.simplecarrieriqdetector;

import org.projectvoodoo.simplecarrieriqdetector.Detect.DetectTest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Main extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        Detect detect = new Detect(getApplicationContext());
        detect.findEverything();
        detect.dumpFoundInLogcat();

        int detectionScore = detect.getDetectionScore();

        TextView score = (TextView) findViewById(R.id.score);
        score.setText(detectionScore + "");

        LinearLayout details = (LinearLayout) findViewById(R.id.details_list);

        for (DetectTest test : detect.getFound().keySet()) {

            TextView title = new TextView(this);
            title.setText(test.name + "\nconfidence level: " + test.confidenceLevel);
            title.setPadding(8, 8, 8, 8);
            title.setTextSize(20);
            title.setTextColor(Color.WHITE);
            details.addView(title);

            if (detect.getFound().get(test).size() > 0) {
                for (String line : detect.getFound().get(test)) {
                    TextView content = new TextView(this);
                    content.setText(line);
                    content.setPadding(8, 0, 8, 0);
                    details.addView(content);
                }
            } else {
                TextView content = new TextView(this);
                content.setText("Nothing found");
                content.setPadding(8, 0, 8, 0);
                details.addView(content);
            }
        }

    }
}
