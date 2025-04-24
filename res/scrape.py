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

URL = "https://calendar.uoguelph.ca/undergraduate-calendar/course-descriptions/"

# gets HTML of the course directory page
courseDir = requests.get(URL)
courseDirHTML = courseDir.content

# setup parser for getting links
soup = BeautifulSoup(courseDirHTML, "html.parser")
linksHTML = soup.find(class_="az_sitemap")

# gets the links of out the html and stores them in links list
for link in linksHTML.findAll('a', href=True):
    if link.get('href')[0] != "#":
        links.append(link.get('href'))

for link in links:
    URL = "https://calendar.uoguelph.ca" + link

    content = requests.get(URL)
    contentHTML = content.content

    soup = BeautifulSoup(contentHTML, "html.parser")
    #  loops through all courses on site
    for a in soup.findAll(class_="courseblock"):

        #  gets name of course
        name = ""
        tmp = a.find(class_="text detail-code margin--small text--semibold text--big")
        if(tmp):
            name += tmp.text +" "

        tmp = a.find(class_="text detail-title margin--small text--semibold text--big")
        if(tmp):
            name += tmp.text +" "
            
        tmp = a.find(class_="text detail-typically_offered margin--small text--semibold text--big")
        if(tmp):
            name += tmp.text +" "

        tmp = a.find(class_="text detail-inst_method margin--small text--semibold text--big")
        if(tmp):
            name += tmp.text +" "

        tmp = a.find(class_="text detail-hours_html margin--small text--semibold text--big")
        if(tmp):
            name += tmp.text + " "


        #  gets course restrictions
        if a.find(class_="text detail-restriction margin--default"):
            restrictions = a.find(class_="text detail-restriction margin--default").text
            restrictions = " ".join(restrictions.split())
            restricts.append(restrictions)
        else:
            restricts.append("NONE")

         #  gets course prerequisites
        if a.find(class_="text detail-prerequisite_s_ margin--default"):
            prerequisites = a.find(class_="text detail-prerequisite_s_ margin--default")
            prerequisites = " ".join(prerequisites.text.split())
            prereqs.append(prerequisites)
        else:
            prereqs.append("NONE")
        
        #  gets course description
        if a.find(class_="courseblockextra noindent"):
            description = a.find(class_="courseblockextra noindent")
            description = " ".join(description.text.split())  # super jank way of removing all the extra space
            descriptions.append(description)
        else:
            descriptions.append("NONE")

        titles.append(name.strip())

#  sends all data to a CSV
df = pd.DataFrame({'Course Title': titles, 'Restrictions': restricts, 'Prerequisites': prereqs, 'Descriptions': descriptions})
df.to_csv('courses.tsv', index=False, encoding='utf-8-sig', sep="\t")
