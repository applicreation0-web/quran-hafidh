from pathlib import Path

root = Path('hifz-app/src/main/java/com/quransafeguard/hifz/preview')
store = root / 'J10HostBudgetStore.java'
session = (root / 'HifzSessionActivity.java').read_text(encoding='utf-8')
main = (root / 'MainActivity.java').read_text(encoding='utf-8')
review = (root / 'J10ReviewActivity.java').read_text(encoding='utf-8')

assert store.exists(), 'missing persistent J10 host-slot store'
assert 'markSlotConsumed' in session, 'timed host does not record J10-preempted slot'
assert 'isSlotConsumed' in session, 'session does not suppress false normal completion'
assert 'isSlotConsumed' in main, 'Today can reopen a J10-preempted slot'
assert 'addConsumed' in review and 'J10HostBudgetStore' in review, 'J10 elapsed is not persisted separately from normal protocol'
print('J10_HOST_SLOT_OVERRIDE_OK')
