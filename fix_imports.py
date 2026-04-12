import os
import re

def organize_imports(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    package_line = None
    imports = []
    others = []
    in_imports = False
    
    for line in lines:
        if line.startswith('package '):
            package_line = line
        elif line.startswith('import '):
            imports.append(line)
            in_imports = True
        elif line.strip() == '' and in_imports:
            continue
        elif in_imports and not line.startswith('import '):
            in_imports = False
            others.append(line)
        else:
            others.append(line)

    if not imports:
        return

    static_imports = sorted([i for i in imports if 'static ' in i])
    normal_imports = [i for i in imports if 'static ' not in i]

    groups = [[], [], [], [], [], [], [], [], []]
    # 0: java.
    # 1: javax.
    # 2: org.
    # 3: net.
    # 4: com. (not nhncorp|navercorp|naver)
    # 5: anything NOT starting with java., javax., com., org., net.
    # 6: com.nhncorp.
    # 7: com.navercorp.
    # 8: com.naver.

    for imp in normal_imports:
        name = imp.replace('import ', '').replace(';', '').strip()
        if name.startswith('java.'):
            groups[0].append(imp)
        elif name.startswith('javax.'):
            groups[1].append(imp)
        elif name.startswith('org.'):
            groups[2].append(imp)
        elif name.startswith('net.'):
            groups[3].append(imp)
        elif name.startswith('com.nhncorp.'):
            groups[6].append(imp)
        elif name.startswith('com.navercorp.'):
            groups[7].append(imp)
        elif name.startswith('com.naver.'):
            groups[8].append(imp)
        elif name.startswith('com.'):
            groups[4].append(imp)
        else:
            # Matches Group 5: (?!java\.|javax\.|com\.|org\.|net\.)
            groups[5].append(imp)

    # Sort within each group
    for i in range(len(groups)):
        groups[i].sort()

    organized_imports = []
    if static_imports:
        organized_imports.extend(static_imports)
        organized_imports.append('')

    for g in groups:
        if g:
            organized_imports.extend(g)
            organized_imports.append('')

    # Remove last empty line if exists
    if organized_imports and organized_imports[-1] == '':
        organized_imports.pop()

    # Reconstruct file
    new_content = []
    if package_line:
        new_content.append(package_line)
        new_content.append('')
    
    new_content.extend(organized_imports)
    new_content.append('')
    new_content.extend(others)

    # Clean up empty lines between package and imports
    result = '\n'.join(new_content)
    result = re.sub(r'\n{3,}', '\n\n', result)
    
    # Ensure file ends with a single newline (as required by NewlineAtEndOfFile)
    result = result.rstrip() + '\n'

    with open(file_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(result)

def main():
    for root, dirs, files in os.walk('.'):
        for file in files:
            if file.endswith('.java'):
                organize_imports(os.path.join(root, file))

if __name__ == '__main__':
    main()
