#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
reader_path = ROOT / "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt"
reader = reader_path.read_text(encoding="utf-8")

# The quick-navigation slider must stay continuous and visually calm: no 602 tick marks.
reader = reader.replace(
    """                                                valueRange = FIRST_PAGE.toFloat()..LAST_PAGE.toFloat(),
                                                steps = LAST_PAGE - FIRST_PAGE - 1,
                                                modifier = Modifier.fillMaxWidth()
""",
    """                                                valueRange = FIRST_PAGE.toFloat()..LAST_PAGE.toFloat(),
                                                modifier = Modifier.fillMaxWidth()
""",
    1,
)

anchor = """                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
"""
if anchor not in reader:
    raise SystemExit("Could not locate free-reading bottom controls; refusing an unsafe patch")

replacement = """                                } else {
                                    if (selectedTafsirVerse == null) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Navigation • page $quickNavPage / $LAST_PAGE",
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = shellMuted,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "1 — $LAST_PAGE",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = shellMuted
                                                )
                                            }
                                            Slider(
                                                value = quickNavPage.toFloat(),
                                                onValueChange = { value ->
                                                    val candidate = value.roundToInt()
                                                        .coerceIn(FIRST_PAGE, LAST_PAGE)
                                                    quickNavPage = candidate
                                                    quickNavInput = candidate.toString()
                                                },
                                                onValueChangeFinished = {
                                                    val target = quickNavPage
                                                    showPage(target)
                                                    message = "Navigation rapide • page $target."
                                                },
                                                valueRange = FIRST_PAGE.toFloat()..LAST_PAGE.toFloat(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(28.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
"""
reader = reader.replace(anchor, replacement, 1)
reader_path.write_text(reader, encoding="utf-8")

contract = ROOT / "docs/VISUAL_CONTRACT_0.10.7.md"
contract.write_text("""# Quran Safeguard 0.10.7 — contrat visuel de publication

Statut : bloquant pour la publication 0.10.7.

- L'application reste sobre : fond crème, tons neutres, vert-gris discret, aucune couleur criarde.
- Le contenu de lecture est prioritaire sur le chrome et les commandes.
- Les barres système Android sont respectées ; aucune information ne doit passer sous l'heure, l'encoche ou la barre de navigation.
- Les boutons conservent des cibles tactiles accessibles sans devenir de gros blocs décoratifs.
- La lecture libre conserve un curseur horizontal permanent de navigation 1–604, placé immédiatement au-dessus des commandes basses.
- Pendant le glissement du curseur, le numéro de page prévisualisé est visible ; la page n'est chargée qu'à la fin du glissement.
- Le panneau de navigation exacte par numéro de page reste disponible sans dupliquer le rôle du curseur.
- Le mode de lecture épurée masque le chrome, y compris le curseur, afin de laisser le Mushaf dominer l'écran.
- Le Tafsîr conserve comme appellations principales Jalalayn, Qurtubi et Qushayri.
- Le Tafsîr reste en anglais ; les références coraniques restent interactives ; la poésie conserve uniquement les retours source-vérifiés.
- Le panneau Tafsîr reste compact à l'ouverture et peut être agrandi pour la lecture longue.
- La sélection de verset est neutre, sans gros encadrement orange.
- Les écrans Qur'an utilisent des listes/sections simples plutôt qu'une accumulation de cartes élevées.
- L'interface doit rester compréhensible en niveaux de gris afin de préparer une adaptation liseuse/e-ink.
""", encoding="utf-8")

verify = ROOT / "scripts/verify_0107_visual_contract.py"
verify.write_text(r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

def require(ok, message):
    if not ok:
        raise SystemExit("0.10.7 VISUAL CONTRACT FAILURE: " + message)

reader = read("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
design = read("app/src/main/java/com/quranunlock/guard/SafeguardDesign.kt")
theme = read("app/src/main/java/com/quranunlock/guard/MainActivity.kt")
hub = read("app/src/main/java/com/quranunlock/guard/QuranHubActivity.kt")
panel = read("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
repo = read("app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt")
edition = read("app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt")
contract = read("docs/VISUAL_CONTRACT_0.10.7.md")

require("statusBarsPadding()" in reader and "navigationBarsPadding()" in reader,
        "reader must respect Android system bars")
require("Navigation • page $quickNavPage / $LAST_PAGE" in reader,
        "persistent free-reading page slider label missing")
require(reader.count("valueRange = FIRST_PAGE.toFloat()..LAST_PAGE.toFloat()") >= 2,
        "both quick and persistent page navigation must cover 1..604")
require("onValueChangeFinished" in reader,
        "page navigation must commit on drag finish")
require("steps = LAST_PAGE - FIRST_PAGE - 1" not in reader,
        "hundreds of slider tick marks must not clutter the reader")
require(".height(28.dp)" in reader,
        "persistent page slider must stay visually compact")
require("chromeHidden" in reader and "pureReading" in reader,
        "clean reading mode must remain available")
require("0.84f" in reader and "0.42f" in reader,
        "Tafsir compact/expanded reading states missing")
require("SafeguardReadingSurface = Color(0xFFF4F0E6)" in design,
        "cream reading surface changed")
for old in ("sahelianButtonOrnament", "drawDiamond"):
    require(old not in design, "ornamental button drawing returned: " + old)
for color in ("primary = Color(0xFF3F4943)", "background = Color(0xFFF5F0E6)",
              "surface = Color(0xFFF8F3EA)", "outline = Color(0xFF9B9185)"):
    require(color in theme, "neutral palette marker missing: " + color)
require("ElevatedCard" not in hub and "QuranHubRow" in hub,
        "Qur'an hub must remain a light list rather than elevated-card grid")
require("TextAlign.Justify" not in panel,
        "Tafsir prose must not force stretched justification on narrow screens")
require("stroke: none !important" in edition and "#C8CEC8" in edition,
        "neutral no-outline verse selection changed")
for marker in ('JALALAYN("jalalayn", "Jalalayn")',
               'QURTUBI("qurtubi", "Qurtubi")',
               'QUSHAYRI("qushayri", "Qushayri")'):
    require(marker in repo, "author label changed: " + marker)
for marker in ("curseur horizontal permanent", "niveaux de gris", "Tafsîr reste en anglais"):
    require(marker in contract, "visual contract marker missing: " + marker)
print("0.10.7 visual publication contract: PASS")
''', encoding="utf-8")

print("Applied final 0.10.7 visual contract: persistent free-reading slider + durable publication checks")
