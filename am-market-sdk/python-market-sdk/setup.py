# coding: utf-8

from setuptools import setup, find_packages

NAME = "am-market-client"
VERSION = "1.0.0"
REQUIRES = [
    "urllib3 >= 1.25.3",
    "python-dateutil",
    "pydantic >= 2",
    "typing-extensions >= 4.7.1",
]

setup(
    name=NAME,
    version=VERSION,
    description="AM Market Data API Python Client",
    author="AM Portfolio Team",
    author_email="support@amportfolio.com",
    url="",
    keywords=["OpenAPI", "OpenAPI-Generator", "am-market-client", "AM-Portfolio"],
    install_requires=REQUIRES,
    packages=find_packages(exclude=["test", "tests"]),
    include_package_data=True,
    license="Private License",
    long_description_content_type='text/markdown',
    package_data={"am_market_client": ["py.typed"]},
)
