package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** All arithmetic tested separately from Android, V6 receipts and the BOOX renderer. */
public final class HifzNewRepetitionPolicyTest {
    @Test public void sabqiStagesExactly25ThenFourFiveRepMasks() {
        int[] counts=new int[5];
        for(int i=0;i<SabqiFortyFiveRecoveryPolicy.BASE;i++){
            int mask=SabqiFortyFiveRecoveryPolicy.maskForNextRep(i);
            if(mask==0)counts[0]++;
            else if(mask==25)counts[1]++;
            else if(mask==50)counts[2]++;
            else if(mask==75)counts[3]++;
            else if(mask==100)counts[4]++;
            else fail("Unexpected mask: "+mask);
        }
        assertArrayEquals(new int[]{25,5,5,5,5},counts);
        assertTrue(SabqiFortyFiveRecoveryPolicy.maskedFinalRep(44));
        assertFalse(SabqiFortyFiveRecoveryPolicy.maskedFinalRep(39));
        for(int i=45;i<50;i++)assertEquals(0,SabqiFortyFiveRecoveryPolicy.maskForNextRep(i));
        for(int i=50;i<55;i++)assertEquals(100,SabqiFortyFiveRecoveryPolicy.maskForNextRep(i));
        assertEquals(55,SabqiFortyFiveRecoveryPolicy.nextRecoveryTarget(45));
        assertEquals(65,SabqiFortyFiveRecoveryPolicy.nextRecoveryTarget(55));
        assertFalse(SabqiFortyFiveRecoveryPolicy.validTarget(44));
        assertFalse(SabqiFortyFiveRecoveryPolicy.validTarget(46));
    }
    @Test public void itqanStagesTenTenTenAndRecovery() {
        for(int i=0;i<30;i++)
            assertEquals(i<10?0:i<20?50:100,ItqanThirtyRecoveryPolicy.maskForNextRep(i));
        for(int i=30;i<35;i++)assertEquals(0,ItqanThirtyRecoveryPolicy.maskForNextRep(i));
        for(int i=35;i<40;i++)assertEquals(100,ItqanThirtyRecoveryPolicy.maskForNextRep(i));
        assertTrue(ItqanThirtyRecoveryPolicy.finalRecallRep(25));
        assertTrue(ItqanThirtyRecoveryPolicy.finalRecallRep(38));
        assertFalse(ItqanThirtyRecoveryPolicy.finalRecallRep(32));
        assertTrue(ItqanThirtyRecoveryPolicy.shouldExtendAfterRep(30,30,1));
        assertFalse(ItqanThirtyRecoveryPolicy.shouldExtendAfterRep(30,30,0));
        assertEquals(40,ItqanThirtyRecoveryPolicy.nextRecoveryTarget(30));
        assertEquals(50,ItqanThirtyRecoveryPolicy.nextRecoveryTarget(40));
    }
}
