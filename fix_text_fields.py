import os
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Find all OutlinedTextFields and insert colors if not present
    # This might be tricky with regex because of multiline.
    # Alternatively, we can just replace OutlinedTextField( with OutlinedTextField(colors = OutlinedTextFieldDefaults.colors(focusedTextColor = androidx.compose.ui.graphics.Color.Black, unfocusedTextColor = androidx.compose.ui.graphics.Color.Black),
    
    # Let's just do a simple replacement for OutlinedTextField(
    
    # We need to make sure we don't double add it.
    if "focusedTextColor = Color.Black" in content or "focusedTextColor = androidx.compose.ui.graphics.Color.Black" in content:
        # maybe we already added it in this file (like AuthScreen)
        # Let's skip or be careful
        pass
        
    # Better approach: Just replace "OutlinedTextField(" with "OutlinedTextField(colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = androidx.compose.ui.graphics.Color.Black, unfocusedTextColor = androidx.compose.ui.graphics.Color.Black),"
    # BUT wait, what if colors is already specified in the OutlinedTextField?
    
    pass

