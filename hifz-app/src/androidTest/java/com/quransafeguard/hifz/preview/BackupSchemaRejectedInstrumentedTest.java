package com.quransafeguard.hifz.preview;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Isolated Android test installation only. */
public final class BackupSchemaRejectedInstrumentedTest {
 @Test public void malformedSchemasPreserveAllStoresAndJournal() throws Exception {
  Context context=ApplicationProvider.getApplicationContext();new HifzPrefs(context);
  JSONObject backup=HifzBackup.export(context);
  String[] names={"quran_hifz_preview_v1","quran_hifz_dashboard_ledger_v1","quran_hifz_j10_v1",
    "hifz_preview_session_gates","free_mem_preview","quran_hifz_restore_journal_v1"};
  Map<String,Map<String,?>> before=new LinkedHashMap<>();
  for(String name:names)before.put(name,context.getSharedPreferences(name,0).getAll());
  for(int version:new int[]{1,2,3})for(Object[] schema:new Object[][]{
    {"s","6"},{"i",999},{"i",0},{"i",5.5},{"i","6"},{null,null}}){
   JSONObject corrupt=new JSONObject(backup.toString()).put("version",version);
   if(schema[0]==null)corrupt.getJSONObject("values").remove("schema");
   else corrupt.getJSONObject("values").put("schema",new JSONObject().put("t",schema[0]).put("v",schema[1]));
   assertThrows(JSONException.class,()->HifzBackup.restore(context,corrupt));
   for(String name:names)assertEquals(name,before.get(name),context.getSharedPreferences(name,0).getAll());
  }
  for(String badKey:new String[]{"unexpected","mask"}){
    JSONObject malformed=new JSONObject(backup.toString());
    if("mask".equals(badKey))malformed.getJSONObject("free_mem")
      .put("mask",new JSONObject().put("t","i").put("v",99));
    else malformed.getJSONObject("free_mem")
      .put("unexpected",new JSONObject().put("t","s").put("v","invalid"));
    assertThrows(JSONException.class,()->HifzBackup.restore(context,malformed));
    for(String name:names)assertEquals(name,before.get(name),context.getSharedPreferences(name,0).getAll());
  }
  assertEquals(PreviewConfig.SCHEMA_VERSION,context.getSharedPreferences(names[0],0).getInt("schema",0));
 }
 @Test public void backupImportLimitsRejectOversizedInputWithoutChangingProgress() throws Exception {
  byte[] exact=new byte[16*1024*1024];
  byte[] oversized=new byte[16*1024*1024+1];
  assertEquals(exact.length,SettingsActivity.readBoundedBackup(new java.io.ByteArrayInputStream(exact)).length);
  assertThrows(java.io.IOException.class,()->SettingsActivity.readBoundedBackup(new java.io.ByteArrayInputStream(oversized)));
  assertThrows(java.io.IOException.class,()->SettingsActivity.readBoundedBackup(null));
 }
}
