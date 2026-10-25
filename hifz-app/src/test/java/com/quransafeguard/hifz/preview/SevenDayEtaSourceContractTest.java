package com.quransafeguard.hifz.preview;
import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
/** Forecast-only corrections cannot alter the actual Hifz V6 state. */
public final class SevenDayEtaSourceContractTest {
  @Test public void estimatesUseSixWeekdaysAndGuardZeroForBothFamilies() throws Exception {
    Path p=Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
    if(!Files.exists(p))p=Paths.get("..",p.toString());
    String java=new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    assertTrue(java.contains("mode==HifzPrefs.WeeklyProgram.RECOMMENDED)return PreviewConfig.STABILIZATION_WEEKLY_LINES;"));
    assertTrue(java.contains("(6-effectiveLearningDaysPerWeek())*7"));
    assertTrue(java.contains("int sabqiDays=prefs.weeklyProgramMode()==HifzPrefs.WeeklyProgram.RECOMMENDED"));
    assertTrue(java.contains("if(weeklyPace==0)return remaining+"));
    assertTrue(java.contains("aucun jour Itqān programmé"));
    assertTrue(java.contains("aucun jour Sabqi programmé"));
    assertFalse(java.contains("int weeks=(remaining+PreviewConfig.STABILIZATION_WEEKLY_LINES-1)/PreviewConfig.STABILIZATION_WEEKLY_LINES;"));
    String eta=java.substring(java.indexOf("private int forecastItqanLinesPerWeek()"),java.indexOf("private void confirmReset()"));
    assertFalse(eta.contains("transitionV6Lines"));
    assertFalse(eta.contains("p.edit()"));
  }
}
