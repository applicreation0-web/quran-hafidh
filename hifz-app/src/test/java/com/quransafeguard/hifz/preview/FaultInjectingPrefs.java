package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;
import java.util.*;

/** Persistent in-memory preferences with one injected atomic commit failure. */
final class FaultInjectingPrefs implements SharedPreferences {
 final Map<String,Object> disk=new LinkedHashMap<>();
 int commits,failCommit=-1;
 public Map<String,?> getAll(){return new HashMap<>(disk);}
 public String getString(String k,String d){Object v=disk.get(k);return v instanceof String?(String)v:d;}
 @SuppressWarnings("unchecked")
 public Set<String> getStringSet(String k,Set<String>d){Object v=disk.get(k);return v instanceof Set?(Set<String>)v:d;}
 public int getInt(String k,int d){Object v=disk.get(k);return v instanceof Integer?(Integer)v:d;}
 public long getLong(String k,long d){Object v=disk.get(k);return v instanceof Long?(Long)v:d;}
 public float getFloat(String k,float d){Object v=disk.get(k);return v instanceof Float?(Float)v:d;}
 public boolean getBoolean(String k,boolean d){Object v=disk.get(k);return v instanceof Boolean?(Boolean)v:d;}
 public boolean contains(String k){return disk.containsKey(k);}
 public Editor edit(){return new E();}
 public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
 public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
 private final class E implements Editor {
   final Map<String,Object> pending=new LinkedHashMap<>();boolean clear;
   public Editor putString(String k,String v){pending.put(k,v);return this;}
   public Editor putStringSet(String k,Set<String>v){pending.put(k,v);return this;}
   public Editor putInt(String k,int v){pending.put(k,v);return this;}
   public Editor putLong(String k,long v){pending.put(k,v);return this;}
   public Editor putFloat(String k,float v){pending.put(k,v);return this;}
   public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
   public Editor remove(String k){pending.put(k,this);return this;}
   public Editor clear(){clear=true;return this;}
   public boolean commit(){
     commits++;
     if(commits==failCommit)return false;
     if(clear)disk.clear();
     for(Map.Entry<String,Object> e:pending.entrySet()){
       if(e.getValue()==this||e.getValue()==null)disk.remove(e.getKey());
       else disk.put(e.getKey(),e.getValue());
     }
     return true;
   }
   public void apply(){commit();}
 }
}
