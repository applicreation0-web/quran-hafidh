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
    static void requireApplicationForeground(Instrumentation instrumentation){
        instrumentation.waitForIdleSync();
        AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
        org.junit.Assert.assertNotNull("No foreground window for audit capture",root);
        // Only the explicitly identified unrelated Pixel Launcher ANR may be dismissed.
        // Other overlays (including application errors) keep the capture blocked.
        if("android".equals(String.valueOf(root.getPackageName()))
                &&resumeIfPresent(instrumentation)){
            instrumentation.waitForIdleSync();
            root=instrumentation.getUiAutomation().getRootInActiveWindow();
            org.junit.Assert.assertNotNull("No foreground app after launcher ANR recovery",root);
        }
        org.junit.Assert.assertEquals("System/launcher overlay blocks the audited application",
            instrumentation.getTargetContext().getPackageName(),String.valueOf(root.getPackageName()));
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
