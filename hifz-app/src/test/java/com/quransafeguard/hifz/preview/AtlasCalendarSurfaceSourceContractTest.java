package com.quransafeguard.hifz.preview;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;
/** Real Atlas grammar, not bordered tiles carrying an Atlas label. */
public final class AtlasCalendarSurfaceSourceContractTest {
    private String main()throws Exception{
        Path p=Path.of("src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        if(!Files.exists(p))p=Path.of("hifz-app",p.toString());return Files.readString(p);
    }
    @Test public void dayRailUsesActualAtlasLineDotsAndSelectionRings()throws Exception{
        String s=main();assertTrue(s.contains("class AtlasDayRail extends LinearLayout"));
        assertTrue(s.contains("canvas.drawLine("));assertTrue(s.contains("selected?9:5"));
        assertTrue(s.contains("canvas.drawCircle(x,y,Ui.dp(MainActivity.this,14),paint)"));
        assertTrue(s.contains("canvas.drawCircle(x,y,Ui.dp(MainActivity.this,18),paint)"));
        assertFalse(s.contains("calendarOutline("));
    }
    @Test public void surfacesAreWhiteAndDetailsRemainAccessibleWithoutCards()throws Exception{
        String s=main();assertTrue(s.contains("private LinearLayout plainColumn()"));
        assertTrue(s.contains("column.setBackgroundColor(android.graphics.Color.WHITE)"));
        assertTrue(s.contains("scheduleDetail(activity.detail)"));assertTrue(s.contains("openCalendarActivity(activity,date)"));
        assertTrue(s.contains("top.setOnClickListener(expand)"));
        assertTrue(s.contains("data.completed+\"/?\""));
    }
}
