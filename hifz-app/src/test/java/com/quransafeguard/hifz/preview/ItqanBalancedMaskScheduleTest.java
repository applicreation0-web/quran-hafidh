package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ItqanBalancedMaskScheduleTest {
    @Test public void exactNewFortyRepetitionProtocol() {
        int[] counts=new int[5];
        for(int rep=0;rep<40;rep++) {
            int mask=ItqanBalancedMaskSchedule.maskForNextRep(rep);
            int expected=rep<20?0:rep<25?25:rep<30?50:rep<35?75:100;
            assertEquals("repetition "+(rep+1),expected,mask);
            counts[mask==0?0:mask==25?1:mask==50?2:mask==75?3:4]++;
        }
        assertArrayEquals(new int[]{20,5,5,5,5},counts);
        assertEquals(0,ItqanBalancedMaskSchedule.maskForNextRep(-1));
        assertEquals(0,ItqanBalancedMaskSchedule.maskForNextRep(40));
    }

    @Test public void oldInProgressFourPartStampStaysOnLegacySequence() {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("itqanRep",18);
        disk.disk.put("itqanUnitStart","49:1");
        disk.disk.put("itqanUnitEnd","49:13");
        disk.disk.put(ItqanRegimeStore.UNIT_PLAN,"49:1|49:13|DEEP_FIRST_PASS|FULL");
        ItqanRegimeStore.UnitPlan plan=ItqanRegimeStore.resolvePlan(disk,"49:1","49:13",
            AnchoringQueue.ItqanProtocol.FULL);
        assertEquals(ItqanRegimeStore.MaskProfile.LEGACY_15_5_5_5_10,plan.maskProfile);
        assertEquals(25,ItqanBalancedMaskSchedule.maskForNextRep(plan,18));
        assertTrue(ItqanRegimeStore.stampPlan(disk,"49:1","49:13",plan));
        assertEquals(plan.maskProfile,ItqanRegimeStore.resolvePlan(disk,"49:1","49:13",
            AnchoringQueue.ItqanProtocol.FULL).maskProfile);
    }

    @Test public void newProfileIsPinnedThroughRestartAndZeroRepRetry() {
        InMemoryPrefs disk=new InMemoryPrefs();
        ItqanRegimeStore.UnitPlan plan=ItqanRegimeStore.resolvePlan(disk,"49:1","49:13",
            AnchoringQueue.ItqanProtocol.LIGHT);
        assertEquals(AnchoringQueue.ItqanProtocol.FULL,plan.protocol);
        assertEquals(ItqanRegimeStore.MaskProfile.BALANCED_20_5_5_5_5,plan.maskProfile);
        assertTrue(ItqanRegimeStore.stampPlan(disk,"49:1","49:13",plan));
        disk.disk.put("itqanRep",16);
        disk.disk.put("itqanUnitStart","49:1");
        disk.disk.put("itqanUnitEnd","49:13");
        assertEquals(0,ItqanBalancedMaskSchedule.maskForNextRep(
            ItqanRegimeStore.resolvePlan(disk,"49:1","49:13",plan.protocol),16));
        disk.disk.put("itqanRep",0); // explicit assistance restart of same unit
        assertEquals(plan.maskProfile,ItqanRegimeStore.resolvePlan(disk,"49:1","49:13",
            plan.protocol).maskProfile);
        disk.disk.put("itqanUnitStart",""); // final commit closes session
        disk.disk.put("itqanUnitEnd","");
        assertEquals(ItqanRegimeStore.MaskProfile.BALANCED_20_5_5_5_5,
            ItqanRegimeStore.resolvePlan(disk,"49:1","49:13",plan.protocol).maskProfile);
    }

    @Test public void postNasAndLegacyLightRemainExactlyUntouched() {
        ItqanRegimeStore.UnitPlan maintenance=new ItqanRegimeStore.UnitPlan(
            ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE,
            AnchoringQueue.ItqanProtocol.FULL,
            ItqanRegimeStore.MaskProfile.BALANCED_20_5_5_5_5);
        for(int i=0;i<4;i++)assertEquals(i<2?0:100,
            ItqanBalancedMaskSchedule.maskForNextRep(maintenance,i));
        ItqanRegimeStore.UnitPlan legacyLight=new ItqanRegimeStore.UnitPlan(
            ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS,
            AnchoringQueue.ItqanProtocol.LIGHT,
            ItqanRegimeStore.MaskProfile.LEGACY_15_5_5_5_10);
        for(int i=0;i<35;i++)assertEquals(
            PreviewConfig.itqanMaskForNextRep(i,AnchoringQueue.ItqanProtocol.LIGHT),
            ItqanBalancedMaskSchedule.maskForNextRep(legacyLight,i));
    }
}
