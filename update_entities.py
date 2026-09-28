import re

with open('app/src/main/java/com/example/data/local/Entities.kt', 'r') as f:
    content = f.read()

content = content.replace("val age: Int = 0,", "val age: Int = 0,\n    val dateOfBirth: String = \"\",")

with open('app/src/main/java/com/example/data/local/Entities.kt', 'w') as f:
    f.write(content)
