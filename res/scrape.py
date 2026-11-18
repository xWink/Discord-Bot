"""University of Guelph course scraper.

Builds ``courses.tsv`` from the public academic calendars: every undergraduate course and every graduate course,
with everything the calendar says about each (terms offered, hours, credits, description, prerequisites,
co-requisites, equivalent courses, restrictions, department and location). The bot reads the file at startup, so
rerun this once a year when a new calendar comes out and commit the result.

    pip install beautifulsoup4 requests
    python3 scrape.py            # writes courses.tsv next to this file
    python3 scrape.py --check    # fetches everything and reports problems without writing

The first line of the file names the columns, and the bot finds columns by name, so adding one here never breaks
reading the file.
"""
import argparse
import re
import sys
import time
from pathlib import Path

import requests
from bs4 import BeautifulSoup

SITE = "https://calendar.uoguelph.ca"

# Each source: a label, the page that links to one page per subject, and the start of those links.
SOURCES = [
    ("undergraduate", "/undergraduate-calendar/course-descriptions/", "/undergraduate-calendar/course-descriptions/"),
    ("graduate", "/graduate-calendar/appendix-courses/", "/graduate-calendar/appendix-courses/"),
]

# The calendar marks each fact with a class like "detail-prerequisite_s_". Left is that class, right is our column.
FIELDS = {
    "detail-typically_offered": "offered",
    "detail-inst_method": "hours",
    "detail-hours_html": "credits",
    "detail-prerequisite_s_": "prerequisites",
    "detail-corequisite_s_": "corequisites",
    "detail-co_requisite_s_": "corequisites",
    "detail-equate_s_": "equates",
    "detail-restriction": "restrictions",
    "detail-offering": "offering",
    "detail-department_s_": "departments",
    "detail-location_s_": "locations",
}
COLUMNS = ["source", "code", "title", "offered", "hours", "credits", "description", "prerequisites", "corequisites",
           "equates", "restrictions", "offering", "departments", "locations"]

PAUSE_SECONDS = 0.4  # be polite to the university's server


def tidy(text):
    """Collapses the calendar's runs of spaces and line breaks, and the spaces it leaves just inside brackets."""
    text = " ".join(text.split())
    return re.sub(r"([(\[]) | ([)\]])", r"\1\2", text)


def fetch(session, path):
    response = session.get(SITE + path, timeout=60)
    response.raise_for_status()
    time.sleep(PAUSE_SECONDS)
    return BeautifulSoup(response.content, "html.parser")


def subject_pages(session, index_path, prefix):
    """The page for every subject (CIS, MATH, ...) linked from a source's index."""
    soup = fetch(session, index_path)
    pages = []
    for link in soup.find_all("a", href=True):
        href = link["href"]
        if href.startswith(prefix) and href != prefix and href.count("/") == prefix.count("/") + 1:
            if href not in pages:
                pages.append(href)
    return pages


def label_free(span):
    """The value of a labelled fact, without its label ("Prerequisite(s): ")."""
    label = span.find("strong")
    text = tidy(span.get_text(" "))
    if label is not None and span.get("class") and "margin--default" in span.get("class"):
        text = tidy(text[len(tidy(label.get_text(" "))):])
    return text


def parse_block(block, source, unknown):
    row = {column: "" for column in COLUMNS}
    row["source"] = source
    for span in block.find_all("span", class_="text"):
        detail = next((c for c in span.get("class", []) if c.startswith("detail-")), None)
        if detail is None:
            continue
        if detail == "detail-code":
            row["code"] = tidy(span.get_text(" "))
        elif detail == "detail-title":
            row["title"] = tidy(span.get_text(" "))
        elif detail in FIELDS:
            value = label_free(span)
            if FIELDS[detail] == "hours":
                value = value.strip("() ")
            elif FIELDS[detail] == "credits":
                value = value.strip("[] ")
            row[FIELDS[detail]] = value
        else:
            unknown.add(detail)
    extra = block.find(class_="courseblockextra")
    row["description"] = tidy(extra.get_text(" ")) if extra else ""
    return row


def scrape():
    session = requests.Session()
    session.headers["User-Agent"] = "course-catalogue-scraper (student project)"
    rows, unknown, seen = [], set(), set()
    for source, index_path, prefix in SOURCES:
        pages = subject_pages(session, index_path, prefix)
        print(f"{source}: {len(pages)} subjects", file=sys.stderr)
        for page in pages:
            for block in fetch(session, page).find_all(class_="courseblock"):
                row = parse_block(block, source, unknown)
                if row["code"] and row["code"] not in seen:  # a course cross-listed in two subjects appears twice
                    seen.add(row["code"])
                    rows.append(row)
    if unknown:
        print("Facts the calendar has that we do not keep: " + ", ".join(sorted(unknown)), file=sys.stderr)
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="report problems without writing the file")
    args = parser.parse_args()
    rows = scrape()
    problems = [r["code"] for r in rows if not r["title"] or not r["credits"] or not re.match(r"^[A-Z]+\*\d{4}$", r["code"])]
    print(f"{len(rows)} courses, {len(problems)} with a missing title, credits or odd code", file=sys.stderr)
    for code in problems[:20]:
        print("  check: " + code, file=sys.stderr)
    if args.check:
        return
    target = Path(__file__).with_name("courses.tsv")
    # Every value has been through tidy(), so none contains a tab or a line break: plain joining is safe and keeps
    # quotation marks in descriptions exactly as the calendar has them.
    with open(target, "w", encoding="utf-8", newline="") as out:
        out.write("\t".join(COLUMNS) + "\n")
        for row in rows:
            out.write("\t".join(row[column] for column in COLUMNS) + "\n")
    print(f"wrote {target}", file=sys.stderr)


if __name__ == "__main__":
    main()
