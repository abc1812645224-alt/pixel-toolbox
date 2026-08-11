import os

file_path = r'D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox\MainActivity.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

if len(lines) < 1000:
    print(f"Error: MainActivity.kt only has {len(lines)} lines! Was it not restored properly?")
    exit(1)

def get_start_with_annotations(line_idx):
    idx = line_idx - 1
    while idx > 0:
        if lines[idx-1].strip().startswith('@') or lines[idx-1].strip().startswith('//') or lines[idx-1].strip() == '':
            idx -= 1
        else:
            break
    return idx

methods = {
    'MainActivity': 109,
    'PixelToolboxApp': 221,
    'ShizukuAuthCard': 507,
    'ExecutionLogCard': 532,
    'SignalScreen': 623,
    'ImsToggleRow_1': 741,
    'ImsGroupSwitchRow': 767,
    'SystemScreen': 794,
    'isWhitelisted': 850,
    'ToolboxScreen': 1292,
    'DonateCard': 1362,
    'QqGroupCard': 1394,
    'BootManagerScreen': 1425,
    'scanBootReceivers': 1674,
    'BatteryData': 1713,
    'BatteryInfoCard': 1721,
    'handleResult': 1789,
    'ImsToggleRow_2': 1800,
    'AboutScreen': 1826,
    'AcknowledgementItem': 2023,
    'DisclaimerScreen': 2057,
    'DisclaimerSection': 2177,
    'createLockScreenShortcut': 2189,
    'installDesktopLauncher': 2202,
    'querySystemPowerMode': 2239,
    'parsePowerModeNumber': 2256
}

sorted_methods = sorted(methods.items(), key=lambda x: x[1])

blocks = {}
for i, (name, start_line) in enumerate(sorted_methods):
    start_idx = get_start_with_annotations(start_line)
    if i < len(sorted_methods) - 1:
        end_idx = get_start_with_annotations(sorted_methods[i+1][1])
    else:
        end_idx = len(lines)
    
    blocks[name] = "".join(lines[start_idx:end_idx])

imports_str = "".join(lines[0:get_start_with_annotations(109)])

base_dir = r'D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox'

def write_file(subpath, pkg, content_blocks):
    path = os.path.join(base_dir, subpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        new_imports = imports_str.replace('package com.example.pixeltoolbox', f'package {pkg}')
        f.write(new_imports)
        f.write("\n")
        f.write("import androidx.lifecycle.ViewModel\n")
        f.write("import androidx.lifecycle.viewModelScope\n")
        f.write("import dagger.hilt.android.lifecycle.HiltViewModel\n")
        f.write("import javax.inject.Inject\n")
        f.write("import com.example.pixeltoolbox.*\n")
        f.write("import com.example.pixeltoolbox.ui.*\n")
        f.write("import com.example.pixeltoolbox.ui.signal.*\n")
        f.write("import com.example.pixeltoolbox.ui.system.*\n")
        f.write("import com.example.pixeltoolbox.ui.tools.*\n")
        f.write("import com.example.pixeltoolbox.ui.about.*\n")
        f.write("\n")
        for b in content_blocks:
            if b in blocks:
                f.write(blocks[b])
    print(f"Wrote {subpath}")

write_file(r'ui\MainScreen.kt', 'com.example.pixeltoolbox.ui', [
    'PixelToolboxApp', 'ShizukuAuthCard', 'ExecutionLogCard', 'DisclaimerScreen', 'DisclaimerSection'
])

write_file(r'ui\signal\SignalScreen.kt', 'com.example.pixeltoolbox.ui.signal', [
    'SignalScreen', 'ImsToggleRow_1', 'ImsGroupSwitchRow'
])

write_file(r'ui\system\SystemScreen.kt', 'com.example.pixeltoolbox.ui.system', [
    'SystemScreen', 'BatteryData', 'BatteryInfoCard', 'isWhitelisted', 'querySystemPowerMode', 'parsePowerModeNumber', 'handleResult'
])

write_file(r'ui\tools\ToolboxScreen.kt', 'com.example.pixeltoolbox.ui.tools', [
    'ToolboxScreen', 'BootManagerScreen', 'scanBootReceivers', 'createLockScreenShortcut', 'installDesktopLauncher'
])

write_file(r'ui\about\AboutScreen.kt', 'com.example.pixeltoolbox.ui.about', [
    'AboutScreen', 'DonateCard', 'QqGroupCard', 'AcknowledgementItem', 'ImsToggleRow_2'
])

# Write MainActivity
with open(os.path.join(base_dir, 'MainActivity.kt'), 'w', encoding='utf-8') as f:
    f.write(imports_str)
    f.write("import dagger.hilt.android.AndroidEntryPoint\n")
    f.write("import com.example.pixeltoolbox.ui.MainScreen\n")
    f.write("\n")
    f.write("@AndroidEntryPoint\n")
    f.write(blocks['MainActivity'])

print("Done splitting via exact lines.")
