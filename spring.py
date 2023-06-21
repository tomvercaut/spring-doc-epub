import os.path

import bs4
import urllib3
from bs4 import BeautifulSoup

import helper


class Index:
    def __init__(self):
        self.depth = 0
        self.href = ""
        self.name = ""
        self.items = []  # List[Index]

    def __str__(self) -> str:
        return helper.to_json(self)


def parse_nav_index(li: bs4.element) -> Index | None:
    index = Index()
    if li.name != "li":
        print(f"Expected a <li> HTML element but got {li.name}")
        return None
    li_class = li['class']
    # print(f"class: {li_class}")
    if li['class'] != ['nav-item']:
        print(f"Expected a <li> HTML element to contain a class equal to 'nav-item'")
        return None
    if li['data-depth'] == '':
        print(f"Expected a non-empty HTML data-element attribute in a li element.")
        return None
    index.depth = int(li['data-depth'][0])

    a = li.find("a", recursive=False)
    if a is not None:
        index.href = a['href']
        index.name = a.text

    ul = li.find("ul", attrs={"class": "nav-list"}, recursive=False)
    if ul is not None:
        lis = ul.find_all("li", attrs={"class": "nav-item"}, recursive=False)
        for nested_li in lis:
            nested_index = parse_nav_index(nested_li)
            if nested_index is not None:
                index.items.append(nested_index)
        pass

    return index


class Spring:

    def __init__(self, base_url: str):
        self._base_url = base_url

    def build(self, output_dir: str):
        if (os.path.exists(output_dir)):
            print(f"Output directory [{output_dir}] already exists, remove it manually and restart the application.")
            exit(1)
        self._build_index()

    def _build_index(self):
        body = self._get_html_body(self._base_url)
        nav_items = self._get_nav(body)
        s = str(nav_items)
        fp = open("nav_items.json", 'w')
        fp.write(s)
        fp.close()

    def _get_nav(self, body: bs4.element) -> Index:
        # body_div = body.find("div")
        nav = body.find("nav", attrs={"class": "nav-menu"})
        nav_ul = nav.ul
        indices = parse_nav_index(nav_ul.li)
        # /html/body/div[1]/div/aside/div[1]/div/nav
        return indices

    def _get_html_body(self, url: str):
        response = urllib3.request("GET", url)
        if response.status != 200:
            print(f"HTTP status code was {response.status} [expected 200] for a GET request to {url}")
            exit(1)

        html_data = response.data.decode("UTF-8")
        soup = BeautifulSoup(html_data, 'html.parser')
        return soup.body
