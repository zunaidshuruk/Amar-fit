with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

# delete lines 199 to 206 inclusive (0-indexed 199 is line 200)
# line 200 is index 199, line 207 is index 206
del lines[199:207]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(lines)
