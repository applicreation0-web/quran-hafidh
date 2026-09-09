package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.MessageDigest

/** Isolated audio only. A closed rights gate cannot issue any network request. */
class QuranAudioController(private val context: Context, private val event: (String)->Unit) {
    private val catalogue = runCatching { JSONObject(context.assets.open("reader109/audio.json").bufferedReader().readText()) }.getOrDefault(JSONObject())
    val available: Boolean get() = catalogue.optBoolean("redistributionApproved", false) && catalogue.optJSONArray("surahs")?.length()==114
    private val handler=Handler(Looper.getMainLooper())
    private val manager=context.getSystemService(AudioManager::class.java)
    private var player:MediaPlayer?=null
    private var start=0;private var end=0;private var remaining=1;private var surah=0;private var ayah=0
    private var ready=false
    private val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener { change -> if(change != AudioManager.AUDIOFOCUS_GAIN)pause() }.build()
    private val noisy=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){pause()}}
    init { androidx.core.content.ContextCompat.registerReceiver(context,noisy,IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED) }
    private fun emit(state:String){event(JSONObject().put("state",state).put("surah",surah).put("ayah",ayah).toString())}
    fun playVerse(s:Int,a:Int,repeats:Int){
        if(!available){emit("unavailable");return}
        val record=catalogue.getJSONArray("surahs").getJSONObject(s-1)
        val interval=record.optJSONObject("verses")?.optJSONObject(a.toString()) ?: run {emit("timing_missing");return}
        val from=interval.optInt("timestamp_from",-1);val to=interval.optInt("timestamp_to",-1)
        if(from<0||to<=from){emit("timing_invalid");return}
        val file=File(context.filesDir,"quran-audio/"+catalogue.getString("version")+"/$s.mp3")
        if(!file.isFile){emit("download_required");return}
        releasePlayer();surah=s;ayah=a;start=from;end=to;remaining=repeats;ready=false
        player=MediaPlayer().apply {
            setAudioAttributes(attrs);setDataSource(file.absolutePath)
            setOnErrorListener{_,_,_->emit("error");releasePlayer();true}
            setOnPreparedListener { p -> ready=true;p.seekTo(start.toLong(),MediaPlayer.SEEK_CLOSEST);resume() }
            prepareAsync()
        }
    }
    private val tick=object:Runnable {override fun run(){val p=player?:return;if(!ready)return
        if(p.isPlaying&&p.currentPosition>=end){p.pause();emit("cycle_complete");remaining--;if(remaining>0){p.seekTo(start.toLong(),MediaPlayer.SEEK_CLOSEST);p.start()}else{emit("paused");return}}
        handler.postDelayed(this,40)
    }}
    fun pause(){if(ready)runCatching{player?.pause()};handler.removeCallbacks(tick);manager.abandonAudioFocusRequest(focus);emit("paused")}
    fun resume(){if(!ready||player==null)return;if(manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){player?.start();handler.removeCallbacks(tick);handler.post(tick);emit("playing")}}
    private fun releasePlayer(){handler.removeCallbacks(tick);player?.release();player=null;ready=false}
    fun release(){pause();releasePlayer();runCatching{context.unregisterReceiver(noisy)}}
    /** Resumable download; catalogue audio/timings share a version and SHA-256. Call off the UI thread. */
    fun download(s:Int):Boolean {
        if(!available||s !in 1..114)return false
        val row=catalogue.getJSONArray("surahs").getJSONObject(s-1)
        val url=URL(row.getString("url"));require(url.protocol=="https"&&url.host==catalogue.getString("productionHost"))
        val dir=File(context.filesDir,"quran-audio/"+catalogue.getString("version"));dir.mkdirs()
        val part=File(dir,"$s.part");val target=File(dir,"$s.mp3")
        val conn=url.openConnection() as java.net.HttpURLConnection
        conn.instanceFollowRedirects=false;conn.connectTimeout=15000;conn.readTimeout=15000
        if(part.length()>0)conn.setRequestProperty("Range","bytes=${part.length()}-")
        return try {
            val code=conn.responseCode;require(code==200||code==206)
            if(code==206)require(conn.getHeaderField("Content-Range").startsWith("bytes ${part.length()}-"))
            java.io.FileOutputStream(part,code==206).use { output->conn.inputStream.use{it.copyTo(output)} }
            val md=MessageDigest.getInstance("SHA-256");part.inputStream().use { input->val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;md.update(b,0,n)} }
            val hash=md.digest().joinToString(""){"%02x".format(it)}
            if(hash!=row.getString("sha256")){part.delete();false}else part.renameTo(target)
        } finally {conn.disconnect()}
    }
    fun delete(s:Int){if(s==surah)pause();File(context.filesDir,"quran-audio/"+catalogue.optString("version")+"/$s.mp3").delete()}
}
