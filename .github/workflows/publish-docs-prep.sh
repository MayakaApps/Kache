#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install --no-deps -r requirements.txt

set -ex

# Generate the API docs
./gradlew --no-configuration-cache dokkaHtmlMultiModule
