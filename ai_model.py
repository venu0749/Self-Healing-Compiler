import sys
import os
import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

# 🔥 Remove transformer logs completely
os.environ["TRANSFORMERS_VERBOSITY"] = "error"

# 🏆 Better model
model_name = "Salesforce/codet5p-770m"

tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

model.eval()

# 1️⃣ Get file path from Java
file_path = sys.argv[1]

# 2️⃣ Read source code
with open(file_path, "r") as f:
    input_code = f.read()

# 3️⃣ Stronger structured prompt
error_type = sys.argv[2]   # passed from Java

if error_type == "UNDECLARED_VARIABLE":
    instruction = """
Fix undeclared variable errors.
Add missing integer declarations at top.
Do not modify correct code.
"""

elif error_type == "MISSING_SEMICOLON":
    instruction = """
Fix missing semicolons in the code.
Do not change logic.
"""

elif error_type == "DUPLICATE_DECLARATION":
    instruction = """
Remove duplicate variable declarations.
Keep only one valid declaration.
"""

elif error_type == "TYPE_MISMATCH":
    instruction = """
Fix type mismatch errors.
Adjust variable type to match assigned value.
"""

else:
    instruction = "Fix compilation errors without changing program meaning."

prompt = f"""
You are a strict C compiler repair assistant.

{instruction}

Return only corrected C code.

Code:
{input_code}

Corrected Code:
"""

# 4️⃣ Tokenize safely
inputs = tokenizer(
    prompt,
    return_tensors="pt",
    truncation=True,
    max_length=512
)

# 5️⃣ Controlled generation
with torch.no_grad():
    outputs = model.generate(
        **inputs,
        max_new_tokens=150,
        num_beams=5,
        do_sample=False,
        repetition_penalty=1.2,
        length_penalty=1.0,
        early_stopping=True
    )

corrected_code = tokenizer.decode(outputs[0], skip_special_tokens=True)

# 6️⃣ Extract only corrected code part
if "Corrected Code:" in corrected_code:
    corrected_code = corrected_code.split("Corrected Code:")[-1]

# 7️⃣ Print clean output only
print("===START===")
print(corrected_code.strip())
print("===END===")