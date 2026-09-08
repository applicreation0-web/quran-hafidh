// Exercise the actual embedded WebView handler without changing any corpus.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const kotlin = fs.readFileSync('app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt', 'utf8');
const source = kotlin.split('<script>')[1].split('</script>')[0]
  .replaceAll('$pageNumber', '1').replaceAll('$BRIDGE_NAME', 'quranSafeguardVerse');
function fixture() {
  let now = 0;
  const listeners = {}, pathListeners = {}, messages = [];
  const path = {
    getAttribute: key => ({surah: '1', ayah: '1'})[key],
    setAttribute() {}, removeAttribute() {}, closest() { return this; },
    classList: {add() {}, remove() {}},
    addEventListener: (type, fn) => { pathListeners[type] = fn; }
  };
  const document = {
    querySelectorAll: () => [path], elementsFromPoint: () => [path],
    body: {style: {}}, addEventListener: (type, fn) => { listeners[type] = fn; }
  };
  const window = {scrollX: 0, scrollY: 0, quranSafeguardVerse: {postMessage: msg => messages.push(JSON.parse(msg))}};
  vm.runInNewContext(source, {window, document, performance: {now: () => now}});
  return {window, messages, pathListeners, emit(type, ms, extra = {}) {
    now = ms;
    listeners[type]({pointerId: 1, isPrimary: true, clientX: 20, clientY: 20, ...extra});
  }};
}
let f = fixture(); f.emit('pointerdown', 0); f.emit('pointerup', 100);
assert.equal(f.messages.length, 0, 'short tap must not open Tafsir');
f = fixture(); f.emit('pointerdown', 0); f.emit('pointerup', 600);
assert.deepEqual(f.messages, [{type: 'verseTap', page: 1, surah: 1, ayah: 1}]);
f = fixture(); f.emit('pointerdown', 0); f.emit('pointermove', 200, {clientY: 60}); f.emit('pointerup', 700);
assert.equal(f.messages.length, 0, 'scroll must not activate a verse');
f = fixture(); f.emit('pointerdown', 0); f.emit('pointerdown', 100, {pointerId: 2, isPrimary: false}); f.emit('pointerup', 700);
assert.equal(f.messages.length, 0, 'pinch must not activate a verse');
f = fixture(); f.emit('pointerdown', 0); f.emit('pointercancel', 100); f.emit('pointerup', 700);
assert.equal(f.messages.length, 0, 'cancelled touch must not activate a verse');
f = fixture(); f.window.qsgTafsir.selectOnTap = true; f.emit('pointerdown', 0); f.emit('pointerup', 100);
assert.equal(f.messages.length, 1, 'visible Tafsir action arms selection');
assert.equal(f.window.qsgTafsir.selectOnTap, false, 'selection is one-shot');
f.emit('pointerdown', 200); f.emit('pointerup', 300);
assert.equal(f.messages.length, 1, 'next short tap must only control chrome');
f = fixture(); f.pathListeners.keydown({key: 'Enter', preventDefault() {}});
assert.equal(f.messages.length, 1, 'keyboard access remains available');
f = fixture(); f.pathListeners.click({detail: 0});
assert.equal(f.messages.length, 1, 'accessibility activation remains available');
console.log('PASS: 9 verse gesture scenarios on the production embedded JavaScript');
