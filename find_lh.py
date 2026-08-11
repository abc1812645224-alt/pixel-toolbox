import os
import sqlite3

def search_local_history():
    appdata = os.environ.get('LOCALAPPDATA', '')
    google_dir = os.path.join(appdata, 'Google')
    
    if not os.path.exists(google_dir):
        print('Google dir not found')
        return

    for p in os.listdir(google_dir):
        if 'AndroidStudio' in p:
            lh_dir = os.path.join(google_dir, p, 'LocalHistory')
            if os.path.exists(lh_dir):
                print('Found LocalHistory:', lh_dir)
                # Try to search recent files in LocalHistory? LocalHistory is not plain text usually...
                
search_local_history()
