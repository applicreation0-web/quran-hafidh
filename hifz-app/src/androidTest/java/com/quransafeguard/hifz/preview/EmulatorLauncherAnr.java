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
