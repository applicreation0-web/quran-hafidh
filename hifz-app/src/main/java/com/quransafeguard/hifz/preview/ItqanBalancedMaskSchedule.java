package com.quransafeguard.hifz.preview;

/**
 * New Itqan deep-first-pass x40 sequence. A persisted per-unit mask profile selects
 * this for NEW sessions only; older sessions continue through PreviewConfig's frozen
 * 15/5/5/5/10 mask mapping. New post-An-Nas ×3 and OPEN legacy ×4 are
 * distinguished by the persisted unit stamp. Legacy LIGHT remains unchanged.
 */
final class ItqanBalancedMaskSchedule {
    private ItqanBalancedMaskSchedule(){}

    static int maskForNextRep(int completed) {
        if(completed<0||completed>=40)return 0;
        int next=completed+1;
        if(next<=20)return 0;
        if(next<=25)return 25;
        if(next<=30)return 50;
        if(next<=35)return 75;
        return 100;
    }

    static int maskForNextRep(ItqanRegimeStore.UnitPlan plan,int completed) {
        if(plan==null)throw new IllegalArgumentException("unit plan required");
        if(plan.regime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE)
            return ItqanMaintenancePolicy.maskForNextRep(plan,completed);
        if(plan.protocol==AnchoringQueue.ItqanProtocol.LIGHT
                ||plan.maskProfile==ItqanRegimeStore.MaskProfile.LEGACY_15_5_5_5_10)
            return ItqanMaintenancePolicy.maskForNextRep(plan.regime,plan.protocol,completed);
        return maskForNextRep(completed);
    }
}
