#!/usr/bin/env python3
"""Strict source pin for the audited P0 Itqan rotation writer."""
import subprocess
from pathlib import Path
path="hifz-app/src/main/java/com/quransafeguard/hifz/preview/ItqanRegimeStore.java"
source=Path(path).read_text(encoding="utf-8")
from verify_1175_post_nas_entry_delta import canonical
canonical("ItqanRegimeStore.java",source)
expected="75c8aaacdceec9c4b8a591b84b7d1493aa15d515"
previous=subprocess.check_output(["git","show",
    "ecbe005a79e5d89e77b9e9ac6968e6916cc8bdb8:"+path],text=True)
old_enum="    enum MaskProfile { LEGACY_15_5_5_5_10, BALANCED_20_5_5_5_5 }"
new_enum="    enum MaskProfile { LEGACY_15_5_5_5_10, BALANCED_20_5_5_5_5, POST_NAS_THREE }"
old_return='''        return new UnitPlan(ItqanMaintenancePolicy.regimeFor(postNasMaintenance(p)),
            ItqanMaintenancePolicy.protocolForNewSession(),MaskProfile.BALANCED_20_5_5_5_5);'''
new_return='''        ItqanMaintenancePolicy.Regime regime=ItqanMaintenancePolicy.regimeFor(postNasMaintenance(p));
        return new UnitPlan(regime,ItqanMaintenancePolicy.protocolForNewSession(),
            regime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE
                ?MaskProfile.POST_NAS_THREE:MaskProfile.BALANCED_20_5_5_5_5);'''
old_comment='''            // The existing four-field plans are already-started sessions:
            // they remain on the exact legacy 15/5/5/5/10 schedule.'''
new_comment='''            // Old 4/5-field stamps remain valid, including OPEN post-Nās ×4.
            // Only the newly stamped POST_NAS_THREE profile uses ×3.'''
expected_source=previous
for a,b in ((old_enum,new_enum),(old_return,new_return),(old_comment,new_comment)):
    assert expected_source.count(a)==1,"Itqan regime source boundary drift"
    expected_source=expected_source.replace(a,b)
# Strip ONLY the explicitly authorized user-selected, future-dated programme
# addition, then prove every historical writer byte still equals the approved
# post-An-Nas baseline. This is not a blanket relaxation of the freeze.
canonical=source.replace("import java.time.LocalDate;\n","",1)
begin=canonical.index("    /**\n     * Forward-dated, explicit user choice.")
end=canonical.index("    /** Regime and protocol frozen for one Itqān unit. */",begin)
assert begin>0 and end>begin,"Missing reviewed dated regime block"
additional=canonical[begin:end]
assert 'return reached;' in additional
assert '"CONSOLIDATION".equals(programme)' in additional
assert '"ENTRETIEN".equals(programme)' in additional
canonical=canonical[:begin]+canonical[end:]
assert canonical.count("ItqanMaintenancePolicy.regimeFor(maintenanceForNewSession(p))")==2
canonical=canonical.replace("ItqanMaintenancePolicy.regimeFor(maintenanceForNewSession(p))",
    "ItqanMaintenancePolicy.regimeFor(postNasMaintenance(p))")
assert canonical==expected_source,"Existing Itqān rotation writer mutated"
assert subprocess.check_output(["git","hash-object",path],text=True).strip()==expected, "Unreviewed Itqan rotation change"
assert 'static SharedPreferences.Editor appendRotationState(' in source
assert 'return appendRotationState(p,p.edit(),state).commit();' in source
assert 'return e.putString(LEG,latched.leg.name())' in source
print("PASS: exact Itqan writer with atomic append semantics")
