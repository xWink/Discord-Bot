# UNIVERSITY OF GUELPH COURSE SCRAPER 2
# This program creates a CSV file containing
# course, restrictions, prerequisites, and description for every course on webadvisor

from bs4 import BeautifulSoup
import requests
import pandas as pd

titles = []  # List to store name of the product
restricts = []  # List to store the restrictions
prereqs = []  # List to store the prerequisites
descriptions = []  # List to store the descriptions
links = []  # List to store links to all the links to courses

URL = "https://www.uoguelph.ca/registrar/calendars/undergraduate/current/c12/index.shtml"

# gets HTML of the course directory page
courseDir = requests.get(URL)
courseDirHTML = courseDir.content

# setup parser for getting links
soup = BeautifulSoup(courseDirHTML, "html.parser")

linksHTML = soup.find('div', attrs={'class': 'subnav'})

# gets the links of out the html and stores them in links list
for link in linksHTML.findAll('a', href=True):
    links.append(link['href'][1:])  # [1:] to get rid of leading period in link

# gets rid of the first link because its just in the index link
links.pop(0)

for link in links:
    URL = "https://www.uoguelph.ca/registrar/calendars/undergraduate/current/c12" + link

    content = requests.get(URL)
    contentHTML = content.content

    soup = BeautifulSoup(contentHTML, "html.parser")
    #  loops through all courses on site
    for a in soup.findAll('div', attrs={'class': 'course'}):

        #  gets name of course
        name = a.find('tr', attrs={'class': 'title'})

        #  gets course restrictions
        if a.find('tr', attrs={'class': 'restrictions'}):
            restrictions = a.find('tr', attrs={'class': 'restrictions'})
            restrictions = " ".join(restrictions.text.split())
            restricts.append(restrictions)
        else:
            restricts.append("NONE")

        #  gets course prerequisites
        if a.find('tr', attrs={'class': 'prereqs'}):
            prerequisites = a.find('tr', attrs={'class': 'prereqs'})
            prerequisites = " ".join(prerequisites.text.split())
            prereqs.append(prerequisites)
        else:
            prereqs.append("NONE")

        #  gets course description
        if a.find('tr', attrs={'class': 'description'}):
            description = a.find('tr', attrs={'class': 'description'})
            description = " ".join(description.text.split())  # super jank way of removing all the extra space
            descriptions.append(description)
        else:
            descriptions.append("NONE")

        titles.append(name.text.strip())

#  sends all data to a CSV
df = pd.DataFrame({'Course Title': titles, 'Restrictions': restricts, 'Prerequisites': prereqs, 'Descriptions': descriptions})
df.to_csv('courses.tsv', index=False, encoding='utf-8-sig', sep="\t")

