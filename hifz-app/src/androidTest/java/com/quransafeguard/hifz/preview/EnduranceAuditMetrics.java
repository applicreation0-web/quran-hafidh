package com.quransafeguard.hifz.preview;

/** Measurements only; no arbitrary memory threshold is presented as leak proof. */
final class EnduranceAuditMetrics {
    static void sample(String phase,int completed){
        android.os.Debug.MemoryInfo info=new android.os.Debug.MemoryInfo();
        android.os.Debug.getMemoryInfo(info);
        Runtime runtime=Runtime.getRuntime();
        android.util.Log.i("HaafidhEndurance","phase="+phase+" completed="+completed
            +" uptime_ms="+android.os.SystemClock.uptimeMillis()
            +" pss_kib="+info.getTotalPss()+" native_alloc_bytes="+android.os.Debug.getNativeHeapAllocatedSize()
            +" java_used_bytes="+(runtime.totalMemory()-runtime.freeMemory()));
    }
}
