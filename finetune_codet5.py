"""
finetune_codet5.py  —  Fine-tune Salesforce/codet5-base for multi-error C repair

Improvements over v1:
  • Builds human-readable prompts matching inference format
  • Adds train/eval split for honest loss tracking
  • Mixed-error examples: randomly combines two single-error examples
    into one multi-error training sample (novel — addresses "limited to
    single error type" gap from literature survey)
  • MPS / CUDA / CPU auto-detection
"""

import json
import random
import torch
from datasets import Dataset
from transformers import (
    AutoTokenizer,
    AutoModelForSeq2SeqLM,
    TrainingArguments,
    Trainer,
    EarlyStoppingCallback,
)

# ── Device ────────────────────────────────────────────────────────────────────
device = ("mps"  if torch.backends.mps.is_available() else
          "cuda" if torch.cuda.is_available()          else "cpu")
print(f"Device: {device}")

# ── Error label map (must match ai_model.py) ──────────────────────────────────
ERROR_LABELS = {
    "missing semicolon":     "MISSING_SEMICOLON",
    "undeclared variable":   "UNDECLARED_VARIABLE",
    "duplicate declaration": "DUPLICATE_DECLARATION",
    "type mismatch":         "TYPE_MISMATCH",
    "printf mismatch":       "PRINTF_MISMATCH",
    "assignment error":      "ASSIGNMENT_ERROR",
    "initialization":        "UNINITIALIZED_USE",
    "missing return":        "MISSING_SEMICOLON",   # treated same
}

def label_to_description(label: str) -> str:
    label_l = label.lower()
    for key, _ in ERROR_LABELS.items():
        if key in label_l:
            return key
    return label_l

def build_prompt(error_desc: str, code: str) -> str:
    return (f"Fix {error_desc} in this C program.\n"
            f"Return ONLY the corrected C code. No explanations.\n\n"
            f"Code:\n{code}\n\nCorrected Code:\n")

# ── Load dataset ──────────────────────────────────────────────────────────────
with open("repair_dataset.json") as f:
    raw_data = json.load(f)

# Parse items — input format: "Fix <label>\n<code>"
items = []
for item in raw_data:
    inp = item["input"]
    first_line, *rest = inp.split("\n", 1)
    code  = rest[0].strip() if rest else ""
    label = first_line.replace("Fix ", "").strip().lower()
    items.append({
        "label":  label,
        "code":   code,
        "output": item["output"],
    })

# ── Build training samples ────────────────────────────────────────────────────
inputs_list  = []
outputs_list = []

# Single-error samples
for item in items:
    desc = label_to_description(item["label"])
    inputs_list.append(build_prompt(desc, item["code"]))
    outputs_list.append(item["output"])

# Multi-error samples (novel: combine two different error types)
random.seed(42)
by_label: dict = {}
for item in items:
    by_label.setdefault(item["label"], []).append(item)

labels = list(by_label.keys())
for _ in range(min(50, len(items) // 2)):
    l1, l2 = random.sample(labels, 2)
    s1 = random.choice(by_label[l1])
    s2 = random.choice(by_label[l2])
    # Combine both erroneous snippets into one file (simple concat with newline)
    combined_code   = s1["code"] + "\n// next function\n" + s2["code"]
    combined_output = s1["output"] + "\n// next function\n" + s2["output"]
    combined_desc   = label_to_description(s1["label"]) + " and " + label_to_description(s2["label"])
    inputs_list.append(build_prompt(combined_desc, combined_code))
    outputs_list.append(combined_output)

print(f"Total training samples: {len(inputs_list)}")

dataset = Dataset.from_dict({"input": inputs_list, "output": outputs_list})
split   = dataset.train_test_split(test_size=0.1, seed=42)
train_ds, eval_ds = split["train"], split["test"]

# ── Tokenizer / model ─────────────────────────────────────────────────────────
MODEL_NAME = "Salesforce/codet5-base"
tokenizer  = AutoTokenizer.from_pretrained(MODEL_NAME)
model      = AutoModelForSeq2SeqLM.from_pretrained(MODEL_NAME)
model.to(device)

# ── Tokenization ──────────────────────────────────────────────────────────────
MAX_IN  = 384
MAX_OUT = 256

def tokenize(batch):
    enc = tokenizer(batch["input"],  max_length=MAX_IN,  truncation=True, padding="max_length")
    lab = tokenizer(batch["output"], max_length=MAX_OUT, truncation=True, padding="max_length")
    enc["labels"] = lab["input_ids"]
    return enc

train_tok = train_ds.map(tokenize, batched=True, remove_columns=["input", "output"])
eval_tok  = eval_ds.map(tokenize,  batched=True, remove_columns=["input", "output"])

# ── Training args ─────────────────────────────────────────────────────────────
args = TrainingArguments(
    output_dir              = "./trained_model",
    learning_rate           = 3e-5,
    per_device_train_batch_size = 4,
    per_device_eval_batch_size  = 4,
    num_train_epochs        = 8,
    warmup_steps            = 50,
    weight_decay            = 0.01,
    logging_dir             = "./logs",
    logging_steps           = 20,
    evaluation_strategy     = "epoch",
    save_strategy           = "epoch",
    load_best_model_at_end  = True,
    save_total_limit        = 2,
    remove_unused_columns   = False,
    fp16                    = False,   # MPS doesn't support fp16 yet
)

trainer = Trainer(
    model           = model,
    args            = args,
    train_dataset   = train_tok,
    eval_dataset    = eval_tok,
    callbacks       = [EarlyStoppingCallback(early_stopping_patience=2)],
)

print("Starting fine-tuning …")
trainer.train()

trainer.save_model("./trained_model")
tokenizer.save_pretrained("./trained_model")
print("Model saved → ./trained_model")
