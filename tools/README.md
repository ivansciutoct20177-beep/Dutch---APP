# tools/ — validatore dei blocchi di vocabolario

`validate_block.py` controlla automaticamente ogni nuovo blocco di vocabolario
prima del commit. Stampa **solo** le voci che falliscono un controllo, col motivo.

## Controlli
1. **Articoli `de`/`het`** — per ogni sostantivo:
   - *regole morfologiche* affidabili (`-je`→het, `-ing/-heid/-teit/-tie/-age`→de,
     `-ment/-um/-isme/-sel`→het) e *liste curate* (`gender_reference.json`) =
     **autorevoli** → se contraddicono l'articolo è **ERROR**;
   - *spaCy* (modello olandese) = **suggerimento** → se contraddice è **REVIEW**;
   - se nessuna fonte conosce la parola → **REVIEW** ("non verificabile").
2. **Duplicati** — ogni `nl` nuovo contro tutti i vocaboli esistenti in a1..c1.
3. **Completezza campi** — `nl, it, en, emoji, pos, exampleNl, exampleIt` non vuoti.
4. **Coerenza esempio** — la parola (o la radice, per i verbi separabili) deve
   comparire in `exampleNl`.
5. **JSON valido** — tutti i file del curriculum si caricano.

Severità: **ERROR** = da correggere · **REVIEW** = da controllare a mano.

## Uso
```bash
# unità appena aggiunte dentro un file di livello:
python3 tools/validate_block.py --file app/src/main/assets/curriculum/b1.json --units b1u6,b1u7

# blocco candidato a sé stante (oggetto unità, oggetto lezione, o lista di item):
python3 tools/validate_block.py --candidate nuovo_blocco.json
```
Exit code `1` se ci sono ERROR, `0` altrimenti (utile in CI/script).

## Fonti dei generi
- `gender_reference.json`: liste curate aperte unite
  ([karlhorky/nederlands-lidwoord-spel](https://github.com/karlhorky/nederlands-lidwoord-spel),
  [MariusHeyneke/Dutchwords](https://github.com/MariusHeyneke/Dutchwords)). ~593 parole.
- **spaCy** (opzionale ma consigliato) per coprire qualsiasi sostantivo:
  ```bash
  pip install spacy click typer
  python3 -m spacy download nl_core_news_sm
  ```
  Senza spaCy il validatore funziona lo stesso, ma più parole risultano
  "non verificabili" (REVIEW).
