"""University of Guelph course scraper.

Builds ``courses.tsv`` from the public academic calendars: every undergraduate course and every graduate course,
with everything the calendar says about each (terms offered, hours, credits, description, prerequisites,
co-requisites, equivalent courses, restrictions, department and location). The bot reads the file at startup, so
rerun this once a year when a new calendar comes out and commit the result.

    pip install beautifulsoup4 requests
    python3 scrape.py            # writes courses.tsv next to this file
    python3 scrape.py --check    # fetches everything and reports problems without writing
    python3 scrape.py --dates-only   # rebuilds only dates.tsv

It also writes ``dates.tsv``: the important dates from the undergraduate schedule of dates (last day to drop,
exam periods, holidays), one line per date and event, which the bot shows with ``!dates`` and can remind people of.

The first line of the file names the columns, and the bot finds columns by name, so adding one here never breaks
reading the file.
"""
import argparse
import datetime
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


SCHEDULE_INDEX = "/undergraduate-calendar/schedule-dates/"
DATE_COLUMNS = ["term", "date", "description"]
MONTHS = {m: i + 1 for i, m in enumerate(
    ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"])}


def year_for(weekday, month, day):
    """The calendar writes 'Friday, September 4' with no year. Weekdays repeat every 5 to 11 years, so within a window
    of a few years around today only one year fits."""
    today = datetime.date.today()
    for year in range(today.year - 1, today.year + 3):
        try:
            if datetime.date(year, month, day).strftime("%A") == weekday:
                return year
        except ValueError:
            continue
    return None


def scrape_dates(session):
    """Every dated event on the schedule-of-dates pages, as (term, ISO date, description) rows."""
    soup = fetch(session, SCHEDULE_INDEX)
    pages = {}  # link -> what the calendar calls it, for example "Fall Semester"
    for link in soup.find_all("a", href=True):
        href = link["href"]
        if href.startswith(SCHEDULE_INDEX) and href != SCHEDULE_INDEX and href.count("/") == SCHEDULE_INDEX.count("/") + 1:
            pages.setdefault(href, tidy(link.get_text(" ")))
    rows = []
    seen = set()
    pattern = re.compile(r"^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday), (\w+) (\d{1,2})")
    for page, term in pages.items():
        page_soup = fetch(session, page)
        for tr in page_soup.find_all("tr"):
            cells = tr.find_all(["td", "th"])
            if len(cells) < 2:
                continue
            m = pattern.match(tidy(cells[0].get_text(" ")))
            if not m or m.group(2) not in MONTHS:
                continue
            year = year_for(m.group(1), MONTHS[m.group(2)], int(m.group(3)))
            if year is None:
                continue
            iso = datetime.date(year, MONTHS[m.group(2)], int(m.group(3))).isoformat()
            # each event is a list item; a date with several events has several. <br> inside one is just a space
            for br in cells[1].find_all("br"):
                br.replace_with(" ")
            items = cells[1].find_all("li")
            events = [tidy(li.get_text("")) for li in items] if items else [tidy(cells[1].get_text(""))]
            events = [e for e in events if e]
            for event in events:
                event = event.replace(" -- ", " \u2013 ").replace("-- ", "\u2013 ").replace(" --", " \u2013")
                if (iso, event) not in seen:  # the 6 and 12 week summer pages repeat the same dates
                    seen.add((iso, event))
                    rows.append({"term": term, "date": iso, "description": event})
    rows.sort(key=lambda r: r["date"])
    return rows


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


def write_dates(rows):
    target = Path(__file__).with_name("dates.tsv")
    with open(target, "w", encoding="utf-8", newline="") as out:
        out.write("\t".join(DATE_COLUMNS) + "\n")
        for row in rows:
            out.write("\t".join(row[column] for column in DATE_COLUMNS) + "\n")
    print(f"wrote {target} ({len(rows)} dates)", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="report problems without writing the file")
    parser.add_argument("--dates-only", action="store_true", help="rebuild dates.tsv only (a few seconds)")
    args = parser.parse_args()
    if args.dates_only:
        write_dates(scrape_dates(requests.Session()))
        return
    rows = scrape()
    dates = scrape_dates(requests.Session())
    problems = [r["code"] for r in rows if not r["title"] or not r["credits"] or not re.match(r"^[A-Z]+\*\d{4}$", r["code"])]
    print(f"{len(rows)} courses, {len(problems)} with a missing title, credits or odd code", file=sys.stderr)
    for code in problems[:20]:
        print("  check: " + code, file=sys.stderr)
    print(f"{len(dates)} dated events", file=sys.stderr)
    if args.check:
        return
    write_dates(dates)
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
