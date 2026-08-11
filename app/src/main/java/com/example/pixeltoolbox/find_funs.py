import re
import sys

file_path = r'D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox\MainActivity.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if re.match(r'^(?:@.*?\\n)*?(?:private |public |internal )?(?:fun|class|object|interface) ', line.lstrip()):
        print(f"Line {i+1}: {line.strip()}")
