# Voice Chair 2.3 (Fabric, Minecraft 26.1.2)

Alles im Kreativmodus unter "Funktionsbloecke".

## Gegenstaende
- **Jurystuhl**: zwei Bloecke hoch (Sitz + hohe Lehne). Rechtsklick = hinsetzen, Shift = aufstehen.
  Platz fuer 2 Bloecke noetig. Beim Abbauen verschwinden beide Haelften, es faellt ein Stuhl.
- **Coach-Buzzer**: winziger Block (kleiner als ein Spielerkopf). Er gehoert zu dem Stuhl, vor dem er auf der
  umgedrehten Seite steht (max. 3 Bloecke, hoechstens 1 Block seitlich). Das ist die Seite, auf der du beim
  Platzieren des Stuhls standest. Rechtsklick: dieser Stuhl dreht sich um, bekommt einen weissen Rand,
  der Name erscheint. Mit Buzzer-Sound.
- **Namensfliese**: auf die umgedrehte Seite vor den Stuhl legen (max. 3 Bloecke). Der Name erscheint in Weiss, sobald der
  Stuhl gebuzzert hat. Name aendern: Namensschild im Amboss benennen, Rechtsklick auf die Fliese.
- **Lied-Ende-Knopf** (Item):
  - Rechtsklick in die Luft: alle noch nicht gebuzzerten Stuehle im Umkreis (48 Bloecke) drehen sich
    um, bleiben aber ROT und zeigen KEINEN Namen. Wer schon gebuzzert hat, bleibt weiss mit Namen.
  - Shift + Rechtsklick in die Luft: alle Stuehle zurueck auf rot, Namen weg.
  - Rechtsklick auf einen Stuhl: nur dieser Stuhl dreht sich (rot, ohne Namen).

## Einstellen
- Sitzhoehe: `SEAT_Y_OFFSET` in `SeatManager.java`
- Namenshoehe: `NAME_Y_OFFSET` in `NameTileBlock.java`
- Reichweite Lied-Ende-Knopf: `RADIUS` in `ChairNetwork.java`
- Reichweite Buzzer und Fliese vor dem Stuhl: `REACH` in `ChairLinks.java`

## Bauen
Siehe .github/workflows/build.yml (GitHub Actions) oder lokal mit Gradle.
