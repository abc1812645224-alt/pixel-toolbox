import re

path = r"D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox\ui\system\SystemScreen.kt"

with open(path, "r", encoding="utf-8") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    # Fix broken strings that end with a question mark instead of closing quote
    if 'Toast.makeText(context,' in line and '", Toast.LENGTH_SHORT).show()' not in line:
        line = line.replace('?, Toast.LENGTH_SHORT).show()', '", Toast.LENGTH_SHORT).show()')
        line = line.replace('?Toast.LENGTH_SHORT).show()', '", Toast.LENGTH_SHORT).show()')
        line = line.replace(' Toast.LENGTH_SHORT).show()', '", Toast.LENGTH_SHORT).show()')
        line = line.replace(', Toast.LENGTH_SHORT).show()', '", Toast.LENGTH_SHORT).show()')
        lines[i] = line
    
    if 'addLog("' in line and '")' not in line:
        line = line.replace('?\n', '")\n')
        line = line.replace(')\n', '")\n')
        line = line.replace('\n', '")\n')
        lines[i] = line

    if 'SectionTitle("' in line and '")' not in line:
        lines[i] = line.replace('?\n', '")\n').replace(')\n', '")\n').replace('\n', '")\n')
        
    if 'if (ok)' in line and 'else' in line:
        line = line.replace('? else', '" else')
        line = line.replace('?,', '",')
        line = line.replace('?, ', '", ')
        line = line.replace('?\n', '",\n')
        line = line.replace(' \n', '",\n')
        line = line.replace('\n', '",\n')
        lines[i] = line
        
with open(path, "w", encoding="utf-8") as f:
    f.writelines(lines)
