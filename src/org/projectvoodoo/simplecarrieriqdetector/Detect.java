
package org.projectvoodoo.simplecarrieriqdetector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

public class Detect {

    private static final String TAG = "Voodoo SimpleCarrierIQDetector";

    private HashMap<DetectTest, ArrayList<String>> found = new HashMap<DetectTest, ArrayList<String>>();

    private Context mContext;

    public Detect(Context c) {
        mContext = c;
    }

    public enum DetectTest {

        KERNEL_INTERFACES("Linux kernel interfaces", 50),
        KERNEL_DRIVERS("Linux kernel drivers", 50),
        DMESG("Linux kernel dmesg log", 100),
        LOGCAT("Android logcat debugging log", 100),
        ETC_CONFIG("ROM configs", 0),
        SERVICES("System services", 70),
        SYSTEM_BINARIES("ROM binaries and daemons", 70),
        RUNNING_PROCESSES("Running processes", 200),
        PACKAGES("Packages", 70),
        SUSPICIOUS_CLASSES("Suspicious classes", 0);

        public String name;
        public int confidenceLevel;

        DetectTest(String name, int confidence) {
            this.name = name;
            this.confidenceLevel = confidence;
        }
    }

    public void findEverything() {
        findKernelDevices();
        findInKallsyms();
        findDmesgStrings();
        findLogcatStrings();
        findEtcConfigText();
        findLogcatStrings();
        findSystemBinaries();
        findSystemService();
        findRunningProcesses();
        findPotentialClasses();
        findPackages();
    }

    /*
     * Finds if applications are registered with Android by its package name
     * specifically, we are looking for commin CIQ package names.
     */

    private void findPackages() {

        String[] potentialPackages = {
                "com.carrieriq.iqagent",
                "com.htc.android.iqagent",
                "com.carrieriq.tmobile"
        };
        ArrayList<String> lines = new ArrayList<String>();

        for (String p : potentialPackages) {
            try {
                mContext.getPackageManager().getApplicationInfo(p, 0);
                lines.add(p);
            } catch (NameNotFoundException e) {
                // if an exception is thrown that means the package was not
                // found or registered with Android
            }

        }
        found.put(DetectTest.PACKAGES, lines);
    }

    /*
     * Find kernel devices like /dev/sdio_tty_ciq_00
     */

    private void findKernelDevices() {

        String[] devicePatterns = {
                "sdio_tty_ciq.*"
        };

        String[] socketPatterns = {
                "iqbrd"
        };

        ArrayList<String> kernelStuff = new ArrayList<String>();

        try {
            for (File f : new File("/dev/").listFiles()) {

                for (String fileNamePattern : devicePatterns) {
                    if (f.getName().matches(fileNamePattern)) {
                        Log.i(TAG, "suspicious device file found: " + f.getAbsolutePath());
                        kernelStuff.add(f.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to list /dev");
            e.getStackTrace();
        }

        try {
            for (File f : new File("/dev/socket").listFiles()) {

                for (String fileNamePattern : socketPatterns) {
                    if (f.getName().matches(fileNamePattern)) {
                        Log.i(TAG, "suspicious socket found: " + f.getAbsolutePath());
                        kernelStuff.add(f.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to list /dev sockets");
            e.getStackTrace();
        }

        found.put(DetectTest.KERNEL_INTERFACES, kernelStuff);
    }

    /*
     * Find kernel drivers implementations
     */

    private void findInKallsyms() {

        String[] elements = {
                    "_ciq_",
            };

        ArrayList<String> deviceWorkarounds = new ArrayList<String>();
        deviceWorkarounds.add("sholes");
        deviceWorkarounds.add("umts_sholes");
        deviceWorkarounds.add("jordan");
        deviceWorkarounds.add("umts_jordan");

        ArrayList<String> lines = new ArrayList<String>();

        if (deviceWorkarounds.contains(Build.DEVICE))
            Log.d(TAG, "kallsyms detection workaround for " + Build.DEVICE + " kernel bug");
        else
            lines.addAll(Utils.findInFile("/proc/kallsyms", elements));

        found.put(DetectTest.KERNEL_DRIVERS, lines);

    }

    /*
     * Search in dmesg Linux kernel log
     */

    private void findDmesgStrings() {

        String[] elements = {
                "iq.logging",
                "iq.service",
                "iq.cadet",
                "iq.bridge",
                "SDIO_CIQ",
                "com.carrieriq.",
                "com/carrieriq/",
                "ttyCIQ",
                "iqagent"
        };

        ArrayList<String> lines = Utils.findInCommandOutput("dmesg", elements);

        found.put(DetectTest.DMESG, lines);
    }

    /*
     * Android debugging Logcat might be full of Carrier IQ stuff or not.
     */

    private void findLogcatStrings() {

        String[] elements = {
                "AppWatcherCIQ",
                "IQService",
                "IQBridge",
                "IQClient",
                "IQ_METRIC",
                "_CIQ",
                "IQ Agent",
                "carrieriq",
                "iqagent",
                "KernelPanicCiqBroadcastReceiver",
                ".iqd"
        };

        ArrayList<String> lines = Utils.findInCommandOutput("logcat -d", elements);

        found.put(DetectTest.LOGCAT, lines);
    }

    /*
     * Carrier IQ can be configured by text files
     */

    private void findEtcConfigText() {
        ArrayList<String> filesList = new ArrayList<String>();
        ArrayList<String> stringsFound = new ArrayList<String>();

        String[] elements = {
                "enableCIQ",
        };

        Utils.findFiles("/etc/", ".*.txt", filesList);
        for (String filename : filesList) {
            Log.d(TAG, "txt file for analysis found: " + filename);
            stringsFound.addAll(Utils.findInFile(filename, elements));
        }

        found.put(DetectTest.ETC_CONFIG, stringsFound);
    }

    /*
     * Carrier IQ is implemented as system binary daemon
     */

    private void findSystemBinaries() {
        ArrayList<String> filesList = new ArrayList<String>();

        String[] elements = {
                "iqmsd",
                "libiq_.*",
                "iqbridged"
        };

        Utils.findFiles("/system", elements, filesList);

        found.put(DetectTest.SYSTEM_BINARIES, filesList);
    }

    /*
     * There might be a dedicated system service running
     */

    private void findSystemService() {
        String[] elements = {
                "carrieriq",
        };
        ArrayList<String> lines = Utils.findInCommandOutput("service list", elements);

        found.put(DetectTest.SERVICES, lines);
    }

    /*
     * Find stuff in running process
     */

    private void findRunningProcesses() {

        String[] elements = {
                "iqmsd",
                "iqbridged",
                "iqd"
        };

        ArrayList<String> lines = Utils.findInCommandOutput("ps", elements);

        found.put(DetectTest.RUNNING_PROCESSES, lines);
    }

    /*
     * Try to call certain classes directly
     */

    private void findPotentialClasses() {

        String[] classes = {
                "com.carrieriq.iqagent.service.receivers.BootCompletedReceiver",
                "com.carrieriq.iqagent.IQService"
        };

        ArrayList<String> lines = new ArrayList<String>();

        for (String suspiciousClass : classes) {
            try {
                Class.forName(suspiciousClass);

                // no error here, that means we found the class!
                lines.add(suspiciousClass);

            } catch (Exception e) {
                // exception here is a good thing, it means the offending
                // class is not existing in the system
            }
        }
        found.put(DetectTest.SUSPICIOUS_CLASSES, lines);
    }

    /*
     * for self-debugging purposes
     */

    public void dumpFoundInLogcat() {

        for (DetectTest test : found.keySet()) {
            Log.i(TAG, "Test for " + test.name);

            if (found.get(test).size() == 0) {
                Log.i(TAG, "\tnothing found");
            } else {

                for (String line : found.get(test))
                    Log.i(TAG, "\tfound:\t" + line);
            }
        }
    }

    /*
     * Returns if something was detected or not
     */

    public int getDetectionScore() {

        int score = 0;

        for (DetectTest test : found.keySet()) {
            if (found.get(test).size() > 0) {
                Log.d(TAG, "Increase detection score by confidence level " + test.confidenceLevel
                        + " for " + test);
                score += test.confidenceLevel;
            }
        }

        return score;
    }

    public HashMap<DetectTest, ArrayList<String>> getFound() {
        return found;
    }
}
