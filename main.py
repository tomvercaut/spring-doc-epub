import argparse
import os.path

import helper
from spring import Spring


class AppOptions:

    def __init__(self):
        self.spring_version = "latest"
        self.project = "spring-framework"
        self.output_dir = "output"

    def __str__(self) -> str:
        return helper.to_json(self)


def parse_app_args() -> AppOptions:
    app_path = os.path.basename(__file__)
    parser = argparse.ArgumentParser(
        prog=app_path,
        description="Convert online documentation to EPUB format")
    parser.add_argument("--spring-version", type=str, default="latest", help="Project version for which the "
                                                                             "documentation needs to be generated.")
    parser.add_argument("--project", type=str, default="spring-framework",  help="Spring project name")
    parser.add_argument("--output-dir", type=str, default="output", help="Directory where the output is written.")
    args = parser.parse_args()
    options = AppOptions()
    options.spring_version = args.spring_version
    options.project = args.project
    options.output_dir = args.output_dir
    return args

def get_url(project_name: str, version: str) -> str:
    if version == "latest":
        return f"https://docs.spring.io/{project_name}/reference/"
    else:
        return f"https://docs.spring.io/{project_name}/reference/{version}"

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    options = parse_app_args()
    print(f"{options}")
    base_url = get_url(options.project, options.spring_version)
    print(f"base url: {base_url}")

    spring = Spring(base_url)
    spring.build(options.output_dir)

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
