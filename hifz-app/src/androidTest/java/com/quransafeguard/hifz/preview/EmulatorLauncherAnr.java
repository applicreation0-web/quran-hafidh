package com.quransafeguard.hifz.preview;

import android.app.Instrumentation;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/** CI emulator only: dismiss an unrelated Pixel Launcher ANR via "Wait".
 * A dialog from the tested application is NEVER dismissed, and all normal
 * QCF/focus/backup assertions stay mandatory. */
final class EmulatorLauncherAnr {
    private EmulatorLauncherAnr(){}

    /** A system dialog can satisfy a crude ink-density gate. Never certify a
     * success screenshot unless the target application actually owns the window.
     * Do not dismiss any dialog here; blocked captures remain failed evidence. */
    /** Wait for the REAL tested application's accessible foreground window.
     * Android 15 can briefly expose no accessibility root while transitioning
     * from ActivityScenario's EmptyActivity to the target Activity. A null root
     * is not a successful screenshot. Only the known Pixel Launcher ANR may be
     * acknowledged; all other persistent windows remain a hard failure.
     */
    static void requireApplicationForeground(Instrumentation instrumentation){
        String expected=instrumentation.getTargetContext().getPackageName();
        String observed="<no accessibility root>";
        long deadline=android.os.SystemClock.uptimeMillis()+12000L;
        do{
            instrumentation.waitForIdleSync();
            AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
            if(root!=null){
                observed=String.valueOf(root.getPackageName());
                if(expected.equals(observed))return;
                if("android".equals(observed))resumeIfPresent(instrumentation);
            }
            android.os.SystemClock.sleep(125L);
        }while(android.os.SystemClock.uptimeMillis()<deadline);
        org.junit.Assert.fail("Audited application never reached foreground for screenshot;"
            +" expected="+expected+" last="+observed);
    }

    static boolean resumeIfPresent(Instrumentation instrumentation){
        AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
        if(root==null||!"android".equals(String.valueOf(root.getPackageName())))return false;
        List<AccessibilityNodeInfo> labels=root.findAccessibilityNodeInfosByText("Pixel Launcher isn't responding");
        boolean launcherAnr=false;
        for(AccessibilityNodeInfo label:labels){
            if("Pixel Launcher isn't responding".equals(String.valueOf(label.getText()))
                    &&label.isVisibleToUser()){launcherAnr=true;break;}
        }
        if(!launcherAnr)return false;
        for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("Wait")){
            if("Wait".equals(String.valueOf(node.getText()))
                    &&node.isEnabled()&&node.isVisibleToUser()){
                if(node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                    Log.w("HaafidhEmulatorCI","Dismissed unrelated Pixel Launcher ANR with Wait");
                    return true;
                }
            }
        }
        return false;
    }
}
