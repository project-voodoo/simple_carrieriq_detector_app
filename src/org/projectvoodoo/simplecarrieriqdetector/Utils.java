
package org.projectvoodoo.simplecarrieriqdetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.util.Log;

public class Utils {

    private static final String TAG = "Voodoo CarrierIQDetector Utils";

    public static final String getCommandOutput(String command) {

        StringBuilder output = new StringBuilder();

        try {

            Process p = Runtime.getRuntime().exec(command);
            InputStream is = p.getInputStream();
            InputStreamReader r = new InputStreamReader(is);
            BufferedReader in = new BufferedReader(r);

            String line;
            while ((line = in.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error when running " + command);
        }

        return output.toString();
    }

    public static final ArrayList<String> findInCommandOutput(String command, String[] elements) {

        ArrayList<String> lines = new ArrayList<String>();

        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {

                for (String pattern : elements)
                    if (line.contains(pattern)
                            && !line.contains("SimpleCarrierIQDetector")
                            && !line.contains("com.lookout")
                            && !line.contains("projectvoodoo")) {
                        lines.add(line);
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to analyze " + command);
        }

        return lines;
    }

    public static final ArrayList<String> findInFile(String filename, String[] elements) {
        String line;
        ArrayList<String> lines = new ArrayList<String>();

        try {
            FileInputStream fstream = new FileInputStream(filename);
            BufferedReader in = new BufferedReader(new InputStreamReader(fstream));

            while ((line = in.readLine()) != null) {
                for (String pattern : elements)
                    if (line.contains(pattern))
                        lines.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to analyze " + filename);
        }

        return lines;

    }

    public static void findFiles(String path, String pattern, ArrayList<String> fileList) {
        String[] patterns = {
                pattern,
        };
        findFiles(path, patterns, fileList);
    }

    public static void findFiles(String path, String[] patterns, ArrayList<String> fileList) {

        File root = new File(path);
        File[] list = root.listFiles();

        try {
            for (File f : list) {

                if (f.isDirectory())
                    findFiles(f.getAbsolutePath(), patterns, fileList);
                else
                    for (String pattern : patterns)
                        if (f.getName().matches(pattern))
                            fileList.add(f.getAbsolutePath());

            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Unable to list path: " + path);
        }
    }
}
