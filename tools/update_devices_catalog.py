#!/usr/bin/env python3
"""Update Guise's single JSON device catalog from MobileModels-csv."""

import argparse
import csv
import json
from pathlib import Path

SUPPORTED_TYPES = {"mob", "pad", "tv", "tv_hub", "watch"}
REQUIRED_COLUMNS = {
    "model", "dtype", "brand", "brand_title", "code", "code_alias",
    "model_name", "ver_name",
}


def optional(value: str):
    value = value.strip()
    return value or None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("csv", type=Path, help="MobileModels-csv models.csv")
    parser.add_argument("catalog", type=Path, help="Existing devices.json to update")
    parser.add_argument("--source-revision", required=True)
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    brands = catalog.get("brands", [])
    identities = {brand["key"]: brand for brand in brands}
    if not identities or len(identities) != len(brands):
        raise ValueError("Catalog must contain unique brand keys")

    with args.csv.open(encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        if set(reader.fieldnames or ()) != REQUIRED_COLUMNS:
            raise ValueError("Unexpected MobileModels CSV schema")
        rows = list(reader)

    models_by_brand = {key: {} for key in identities}
    unknown_brands = set()
    for row in rows:
        key = row["brand"].strip()
        model = row["model"].strip()
        name = row["model_name"].strip()
        dtype = row["dtype"].strip()
        if not key or not model or not name or dtype not in SUPPORTED_TYPES:
            continue
        if key not in identities:
            unknown_brands.add(key)
            continue
        # Match the former SQLite query: one entry for each brand/model pair.
        models_by_brand[key].setdefault(model, {
            "model": model,
            "name": name,
            "device": optional(row["code_alias"]) or optional(row["code"]) or "",
            "code": optional(row["code"]),
            "type": dtype,
            "versionName": optional(row["ver_name"][1:] if row["ver_name"].startswith("#") else row["ver_name"]),
        })

    if unknown_brands:
        raise ValueError(
            "Add identities for new MobileModels brands before updating: "
            + ", ".join(sorted(unknown_brands))
        )

    for brand in brands:
        brand["models"] = sorted(
            models_by_brand[brand["key"]].values(),
            key=lambda item: (item["name"], item["model"]),
        )

    catalog["sourceRevision"] = args.source_revision
    temporary = args.catalog.with_suffix(".json.tmp")
    temporary.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    temporary.replace(args.catalog)
    print(
        "Wrote %d models across %d brands to %s"
        % (sum(len(brand["models"]) for brand in brands), len(brands), args.catalog)
    )


if __name__ == "__main__":
    main()