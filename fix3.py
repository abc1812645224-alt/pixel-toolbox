import re

path = r"D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox\ui\system\SystemScreen.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# Replace any \ufffd (replacement character) that appears right before a comma or parenthesis
# in common patterns.

# Fix addLog
text = re.sub(r'\ufffd\)', '")', text)
text = re.sub(r'\ufffd"\)', '")', text)

# Fix Toast
text = re.sub(r'\ufffd,\s*Toast\.LENGTH_SHORT', '", Toast.LENGTH_SHORT', text)
text = re.sub(r'\ufffd",\s*Toast\.LENGTH_SHORT', '", Toast.LENGTH_SHORT', text)
text = re.sub(r'\ufffd\s*else', '" else', text)
text = re.sub(r'\ufffd"\s*else', '" else', text)
text = re.sub(r'\ufffd\s*\n', '")\n', text)

with open(path, "w", encoding="utf-8") as f:
    f.write(text)
