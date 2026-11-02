package com.quransafeguard.hifz.preview;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.WindowInsets;
import android.graphics.Insets;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.Assert.*;

public final class Independent11710VisualBoundsInstrumentedTest {
 private static Object field(Object o,String name) {
  try{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
  catch(Exception e){throw new AssertionError(e);}
 }
 @Test public void explanatoryParagraphUsesRealLineBreaks() {
  try(ActivityScenario<SettingsActivity> s=ActivityScenario.launch(SettingsActivity.class)) {
   s.onActivity(a->{String text=((TextView)field(a,"renforcementSchemaNote")).getText().toString();
    assertFalse("Explanatory text must not print literal backslash-n sequences",text.contains("\\n"));
    assertTrue("Numbered paragraphs must use actual line breaks",text.contains("\n1."));});
  }
 }
 @Test public void settingsScrollViewportKeepsStatusAndNavigationInsetsWhenContentScrolls() {
  try(ActivityScenario<SettingsActivity> s=ActivityScenario.launch(SettingsActivity.class)) {
   s.onActivity(a->{
    LinearLayout page=(LinearLayout)field(a,"currentScreen");
    ScrollView scroll=(ScrollView)((Map<?,?>)field(a,"screens")).get(page);
    WindowInsets window=scroll.getRootWindowInsets();assertNotNull(window);
    Insets bars=window.getInsets(WindowInsets.Type.systemBars());
    assertTrue("System bar padding must belong to the stationary ScrollView viewport",scroll.getPaddingTop()>=bars.top);
    assertTrue("Navigation padding must stay outside the moving content",scroll.getPaddingBottom()>=bars.bottom);
    assertTrue("Viewport must clip moving text out of system bars",scroll.getClipToPadding());
   });
  }
 }
}
