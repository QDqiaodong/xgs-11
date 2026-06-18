import os
import re

components_dir = 'components'
for filename in os.listdir(components_dir):
    if not filename.endswith('.vue'):
        continue
    filepath = os.path.join(components_dir, filename)
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extract template
    template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
    template = template_match.group(1).strip() if template_match else ''
    
    # Extract script
    script_match = re.search(r'<script.*?>(.*?)</script>', content, re.DOTALL)
    script = script_match.group(1).strip() if script_match else ''
    
    # Extract style
    style_match = re.search(r'<style.*?>(.*?)</style>', content, re.DOTALL)
    style = style_match.group(1).strip() if style_match else ''
    
    # Create JS file
    js_filename = filename.replace('.vue', '.js')
    js_filepath = os.path.join(components_dir, js_filename)
    
    # We need to inject template into the exported object.
    
    # Escape backticks and ${} in template
    template_escaped = template.replace('`', '\\`').replace('$', '\\$')
    
    js_content = script
    # Look for export default {
    
    if 'export default {' in js_content:
        js_content = js_content.replace('export default {', f'export default {{\n  template: `{template_escaped}`,\n')
    else:
        # if no script, just export template
        js_content = f'export default {{\n  template: `{template_escaped}`\n}};'
        
    with open(js_filepath, 'w', encoding='utf-8') as f:
        f.write(js_content)
        
    # Append style to css/styles.css
    if style:
        with open('css/styles.css', 'a', encoding='utf-8') as f:
            f.write('\n/* Styles from ' + filename + ' */\n')
            f.write(style + '\n')
            
    # Delete original .vue file
    os.remove(filepath)
    print(f"Converted {filename} to {js_filename}")
