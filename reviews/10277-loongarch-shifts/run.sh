#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 2 ]]; then
  echo "Usage: bash run.sh GHIDRA_HOME GHIDRA_SOURCE" >&2
  exit 2
fi

review_dir="$(cd "$(dirname "$0")" && pwd)"
ghidra_home="$(cd "$1" && pwd)"
source_dir="$(cd "$2" && pwd)"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

base=c8d842450e18d132a4f154702878be27daa21a43
head=b0d0ce58824b8ba3e4c098d8e23676911d4b1a8c
language_dir=Ghidra/Processors/Loongarch/data/languages
mkdir -p "$work_dir/install" "$work_dir/spec" "$work_dir/projects"
python3 "$review_dir/generate.py" "$work_dir/shifts.bin"
cp -R "$ghidra_home/." "$work_dir/install/"
for revision in "$base" "$head"; do
  spec_dir="$work_dir/spec/$revision"
  mkdir -p "$spec_dir"
  git -C "$source_dir" archive "$revision" "$language_dir" |
    tar -x -C "$spec_dir" --strip-components=5
  "$work_dir/install/support/sleigh" "$spec_dir/loongarch64_f64.slaspec"
  cp "$spec_dir/loongarch64_f64.sla" "$work_dir/install/$language_dir/"
  echo "REVISION $revision"
  "$work_dir/install/support/analyzeHeadless" "$work_dir/projects" ShiftReview \
    -import "$work_dir/shifts.bin" -loader BinaryLoader \
    -processor Loongarch:LE:64:lp64d -noanalysis \
    -scriptPath "$review_dir" -postScript ShiftReview.java -deleteProject |
    tee "$work_dir/$revision.log"
  if [[ "$revision" == "$base" ]]; then
    grep -F 'SHIFT_SUMMARY checks=1152 mismatches=355' "$work_dir/$revision.log"
  else
    grep -F 'SHIFT_SUMMARY checks=1152 mismatches=0' "$work_dir/$revision.log"
  fi
done
