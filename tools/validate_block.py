#!/usr/bin/env python3
"""
NederLeer block validator.

Validates a block of new vocabulary against five checks and prints a report
listing ONLY the entries that fail (or need review), with the reason:

  1. Articles de/het  -> morphological rules + curated lists (authoritative),
                         spaCy Dutch model (suggestion). Flags mismatches and
                         words that cannot be verified by any source.
  2. Duplicates       -> every new `nl` against all existing vocab in a1..c1.
  3. Field completeness-> nl, it, en, emoji, pos, exampleNl, exampleIt non-empty.
  4. Example coherence -> the nl word (or its stem, for verbs) must appear in
                          exampleNl.
  5. Valid JSON       -> every curriculum file parses.

Usage:
  # validate one or more newly-added units inside a level file:
  python tools/validate_block.py --file app/src/main/assets/curriculum/b1.json --units b1u6,b1u7

  # validate a stand-alone candidate block (a unit object, or a list of items):
  python tools/validate_block.py --candidate newblock.json

Severities:
  ERROR  = must fix (wrong article per authoritative source, duplicate,
           missing field, invalid JSON).
  REVIEW = check by hand (spaCy disagrees, or gender not verifiable, or the
           example may not contain the word).
"""
import argparse
import glob
import json
import os
import re
import sys

CURRICULUM_GLOB = os.path.join(os.path.dirname(__file__), "..",
                               "app/src/main/assets/curriculum/*.json")
REFERENCE = os.path.join(os.path.dirname(__file__), "gender_reference.json")
# Hard-required text fields. `emoji` is optional in the app (function words,
# idioms and many abstract nouns have none) -> flagged as REVIEW, not ERROR.
REQUIRED_FIELDS = ("nl", "it", "en", "pos", "exampleNl", "exampleIt")

SEP_PREFIXES = ["aan", "af", "bij", "binnen", "buiten", "door", "in", "mee",
                "na", "neer", "om", "onder", "op", "over", "rond", "samen",
                "schoon", "tegen", "terug", "toe", "uit", "vast", "voor", "weg"]

# Rule-breakers (authoritative, from standard dictionaries).
MANUAL = {"datum": "de", "ding": "het"}


def rule_gender(noun: str):
    """High-confidence morphological gender, or None."""
    w = noun.lower()
    if w in MANUAL:
        return MANUAL[w]
    # HET
    if w.endswith("je"):                      # diminutives
        return "het"
    if w.endswith(("isme", "ment", "um", "sel")):
        return "het"
    # DE
    if w.endswith(("heid", "teit", "tie", "sie", "age", "ist", "aar")):
        return "de"
    if w.endswith("ing") and len(w) >= 6:     # avoid 'ding'
        return "de"
    return None


def load_reference():
    if os.path.exists(REFERENCE):
        try:
            return json.load(open(REFERENCE, encoding="utf-8"))
        except Exception:
            return {}
    return {}


def load_spacy():
    try:
        import spacy
        return spacy.load("nl_core_news_sm")
    except Exception:
        return None


def spacy_gender(nlp, noun):
    if nlp is None:
        return None
    doc = nlp(noun)
    if not doc:
        return None
    g = doc[0].morph.get("Gender")
    if not g:
        return None
    return {"Neut": "het", "Com": "de"}.get(g[0])


def resolve_gender(noun, ref, nlp):
    """Return (gender, source, authoritative?) or (None, None, False)."""
    head = noun.lower().split()[-1]
    g = rule_gender(head)
    if g:
        return g, "regola", True
    if head in ref:
        return ref[head], "lista", True
    g = spacy_gender(nlp, head)
    if g:
        return g, "spaCy", False
    return None, None, False


def _stem(v: str):
    """Rough present-tense stem: drop -en and de-double final consonant."""
    s = v[:-2] if v.endswith("en") and len(v) > 3 else v
    if len(s) >= 2 and s[-1] == s[-2] and s[-1] not in "aeiou":
        s = s[:-1]                      # pakk -> pak, zwemm -> zwem
    return s


def _lengthen(s: str):
    """Open-syllable vowel lengthening: hop -> hoop, verget -> vergeet."""
    out = {s}
    m = re.search(r"([aeiou])([bcdfghjklmnpqrstvwxz])$", s)
    if m and len(s) >= 3 and s[m.start(1) - 1] not in "aeiou":
        i = m.start(1)
        out.add(s[:i] + s[i] + s[i:])
    return out


def verb_fragments(inf: str):
    base = inf
    for p in SEP_PREFIXES:
        if inf.startswith(p) and len(inf) - len(p) >= 3:
            base = inf[len(p):]
            break
    frags = {inf, base}
    for s in (_stem(inf), _stem(base)):
        frags |= _lengthen(s)
    return {f.lower() for f in frags if len(f) >= 3}


def _shorten(w: str):
    """Open-syllable plural shortening: aandeel->aandel, paneel->panel, muur->mur."""
    m = re.search(r"(aa|ee|oo|uu)([bcdfghjklmnpqrstvwxz])$", w)
    if m:
        i = m.start(1)
        return w[:i] + w[i] + w[i + 2:]
    return w


def example_ok(item):
    nl = item["nl"]
    ex = item.get("exampleNl", "").lower()
    pos = item.get("pos", "")
    if pos == "v":
        return any(f in ex for f in verb_fragments(nl.lower()))
    if pos == "n":
        toks = nl.split()
        core = (" ".join(toks[1:]) if toks and toks[0] in ("de", "het") else nl).lower()
        last = core.split()[-1]
        cands = {core, last, _shorten(core), _shorten(last)}
        return any(c in ex for c in cands if c)
    # adj / adv / prep / conj / phr: word or its first 5 chars
    w = nl.lower()
    return w in ex or (len(w) >= 5 and w[:5] in ex)


def collect_curriculum():
    """Return (items, json_errors) where items = list of dicts with location."""
    items, errors = [], []
    for f in sorted(glob.glob(CURRICULUM_GLOB)):
        try:
            data = json.load(open(f, encoding="utf-8"))
        except Exception as e:
            errors.append((os.path.basename(f), str(e)))
            continue
        for u in data.get("units", []):
            for l in u.get("lessons", []):
                for it in l.get("items", []):
                    items.append({"file": os.path.basename(f), "unit": u["id"],
                                  "item": it})
    return items, errors


def get_block(args, all_items):
    if args.candidate:
        data = json.load(open(args.candidate, encoding="utf-8"))
        if isinstance(data, dict) and "units" in data:
            raw = [it for u in data["units"] for l in u["lessons"] for it in l["items"]]
        elif isinstance(data, dict) and "lessons" in data:
            raw = [it for l in data["lessons"] for it in l["items"]]
        elif isinstance(data, list):
            raw = data
        else:
            raw = [data]
        block = [{"unit": "(candidate)", "item": it} for it in raw]
        existing = all_items
        return block, existing
    units = set(args.units.split(",")) if args.units else set()
    block = [i for i in all_items if i["unit"] in units]
    existing = [i for i in all_items if i["unit"] not in units]
    return block, existing


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--file")
    ap.add_argument("--units")
    ap.add_argument("--candidate")
    args = ap.parse_args()

    ref = load_reference()
    nlp = load_spacy()
    all_items, json_errors = collect_curriculum()
    block, existing = get_block(args, all_items)
    existing_nl = {}
    for i in existing:
        existing_nl.setdefault(i["item"]["nl"].lower(), i)

    reports = []  # (severity, nl, reason)

    for je in json_errors:
        reports.append(("ERROR", je[0], f"JSON non valido: {je[1]}"))

    seen_in_block = {}
    for entry in block:
        it = entry["item"]
        nl = it.get("nl", "(senza nl)")

        # 3. completeness
        missing = [k for k in REQUIRED_FIELDS if not str(it.get(k, "")).strip()]
        if missing:
            reports.append(("ERROR", nl, f"campi mancanti/vuoti: {', '.join(missing)}"))
        if not str(it.get("emoji", "")).strip():
            reports.append(("REVIEW", nl, "emoji mancante (campo opzionale)"))

        # 2. duplicates (vs existing + within block)
        low = nl.lower()
        if low in existing_nl:
            reports.append(("ERROR", nl, f"duplicato: già presente ({existing_nl[low]['unit']})"))
        if low in seen_in_block:
            reports.append(("ERROR", nl, "duplicato all'interno del blocco"))
        seen_in_block[low] = True

        # 1. article
        if it.get("pos") == "n":
            toks = nl.split()
            if toks and toks[0] in ("de", "het"):
                article = toks[0]
                core = " ".join(toks[1:])
                g, src, auth = resolve_gender(core, ref, nlp)
                if g is None:
                    reports.append(("REVIEW", nl, "articolo non verificabile da nessuna fonte"))
                elif g != article:
                    if auth:
                        reports.append(("ERROR", nl, f"articolo errato: dovrebbe essere '{g}' (fonte: {src})"))
                    else:
                        reports.append(("REVIEW", nl, f"spaCy suggerisce '{g}' invece di '{article}'"))
            else:
                reports.append(("ERROR", nl, "sostantivo senza articolo de/het"))

        # 4. example coherence
        if it.get("exampleNl", "").strip() and not example_ok(it):
            reports.append(("REVIEW", nl, "la parola non sembra comparire in exampleNl"))

    # ---- output ----
    errors = [r for r in reports if r[0] == "ERROR"]
    reviews = [r for r in reports if r[0] == "REVIEW"]
    print(f"Blocco: {len(block)} voci  |  esistenti confrontate: {len(existing_nl)}  |  spaCy: {'ON' if nlp else 'OFF'}")
    print(f"Risultato: {len(errors)} ERROR, {len(reviews)} REVIEW\n")
    if not reports:
        print("✅ Tutto a posto: nessun problema rilevato.")
        return 0
    for sev in ("ERROR", "REVIEW"):
        rs = [r for r in reports if r[0] == sev]
        if rs:
            icon = "❌" if sev == "ERROR" else "⚠️ "
            print(f"--- {sev} ({len(rs)}) ---")
            for _, nl, reason in rs:
                print(f"{icon} {nl}: {reason}")
            print()
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
