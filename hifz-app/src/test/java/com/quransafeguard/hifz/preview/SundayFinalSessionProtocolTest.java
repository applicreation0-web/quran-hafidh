package com.quransafeguard.hifz.preview;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.*;

public final class SundayFinalSessionProtocolTest {
    @Test public void eachPhysicalPageRunsOneVisibleThenTwoMaskedWithoutContinuousUnit() {
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle c=engine.startCycle("learning-final-2026-10-25",
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("page1",ConsolidationCycleEngine.Protocol.SUNDAY_PAGE));
        c=engine.addUnit(c,new ConsolidationCycleEngine.Unit("page2",ConsolidationCycleEngine.Protocol.SUNDAY_PAGE));
        ConsolidationCycleEngine.Session session=engine.openSession(c,"frozen");
        assertEquals(3,session.quotaAt(0));
        assertEquals(3,session.quotaAt(1));
        for(int i=0;i<3;i++) {
            assertEquals(0,session.nextUnitIndex());
            assertEquals(i,session.donePerStage());
            session=engine.recordRepetition(session);
        }
        assertEquals(1,session.nextUnitIndex());
        for(int i=0;i<3;i++)session=engine.recordRepetition(session);
        assertTrue(session.readyToClose());
    }
    @Test public void frozenOpenExtendedSessionsRetainTheirThreeVisibleProtocolRepetitions() {
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle c=engine.startCycle("learning-final-legacy",
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("old",ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED));
        ConsolidationCycleEngine.Session old=engine.openSession(c,"legacy");
        old=engine.recordRepetition(old);
        ConsolidationCycleEngine.Session restored=engine.restoreOpenSession(c,"legacy",1,
            old.stage(),old.nextUnitIndex(),old.donePerStage());
        assertEquals(1,restored.donePerStage());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED,restored.cycle().units().get(0).protocol());
    }
}
