#!/usr/bin/env bash
# Converts the palettes sliding/folding door assets to Create Fly's layout: the door is drawn entirely by
# SlidingDoorRenderer, so the blockstate is a single particle-only model, and sliding doors use one combined
# two-block partial (block / block_windowed, from palettes/doors/block) instead of per-half block models.
# Run from the repo root.
set -euo pipefail

assets=src/generated/resources/assets/railways
models=$assets/models/block/palettes

# "texture": "value" from a generated door model
tex() { sed -n "s/.*\"$2\": *\"\([^\"]*\)\".*/\1/p" "$1"; }

for dir in "$models"/*/sliding_door "$models"/*/folding_door; do
    [ -d "$dir" ] || continue
    for w in "" _windowed; do
        bottom=$dir/block_bottom$w.json
        top=$dir/block_top$w.json
        [ -f "$bottom" ] && [ -f "$top" ] || continue
        if [ "$(basename "$dir")" = sliding_door ]; then
            printf '{\n  "parent": "railways:block/palettes/doors/block",\n  "textures": {\n    "block_particle": "%s",\n    "bottom": "%s",\n    "side": "%s",\n    "top": "%s"\n  }\n}\n' \
                "$(tex "$bottom" block_particle)" "$(tex "$bottom" bottom)" "$(tex "$bottom" side)" "$(tex "$top" top)" \
                > "$dir/block$w.json"
        fi
        if [ -z "$w" ]; then
            printf '{\n  "textures": {\n    "particle": "%s"\n  }\n}\n' "$(tex "$bottom" block_particle)" > "$dir/particle.json"
        fi
    done
done

for bs in "$assets"/blockstates/*sliding_locometal_door.json "$assets"/blockstates/*folding_locometal_door.json; do
    model=$(sed -n 's/.*"model": *"\([^"]*\)\/block_bottom".*/\1/p' "$bs" | head -1)
    [ -n "$model" ] || { echo "no door model in $bs" >&2; exit 1; }
    printf '{\n  "variants": {\n    "": {\n      "model": "%s/particle"\n    }\n  }\n}\n' "$model" > "$bs"
done

echo "door models and blockstates converted"
