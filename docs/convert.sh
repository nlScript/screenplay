#!/bin/bash

# showdown makehtml -i index.md -e showdown-katex -o index.html -p github

echo '<article class="markdown-body">' > index.html

showdown makehtml -i index.md -e showdown-highlight -a -o index.html -p github

echo '</article>' >> index.html

