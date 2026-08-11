import re
path = r"D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox\ui\system\SystemScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# Fix SectionTitle
text = text.replace('瀛?")")', '瀛?")')

# Fix Toast.makeText
text = text.replace('鏉","",', '鏉",')
text = text.replace('鐢","",', '鐢",')
text = text.replace('鐞","",', '鐞",')
text = text.replace('鐢?",', '鐢",')

# Fix addLog
text = text.replace('鐢?")")', '鐢?")')
text = text.replace('鐞?")")', '鐞?")')
text = text.replace('鐢?")', '鐢")')
text = text.replace('鐞?")', '鐞")')
text = text.replace('?",', '",')
text = text.replace('?)', '")')

text = text.replace('")")', '")')

with open(path, "w", encoding="utf-8") as f:
    f.write(text)
