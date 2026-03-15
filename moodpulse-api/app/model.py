from transformers import pipeline
import torch
import re
from dotenv import load_dotenv

load_dotenv()

LABEL_NAMES = ["joy", "sadness", "anger", "fear", "love", "surprise"]
MODEL_ID    = "reiiham/moodpulse-emotion"

class EmotionClassifier:
    def __init__(self):
        print(f"Loading model: {MODEL_ID}")
        device = 0 if torch.cuda.is_available() else -1
        self._pipeline = pipeline(
            "text-classification",
            model=MODEL_ID,
            return_all_scores=True,
            device=device
        )
        print("Model loaded successfully!")

    def is_loaded(self) -> bool:
        return self._pipeline is not None

    def clean_text(self, text: str) -> str:
        text = re.sub(r'\[.*?\]', '', text)
        text = re.sub(r'\n{2,}', ' ', text)
        return text.strip()[:400]

    def predict(self, text: str) -> dict:
        cleaned = self.clean_text(text)

        inputs = self._pipeline.tokenizer(
            cleaned,
            return_tensors="pt",
            truncation=True,
            padding=True,
            max_length=128
        )

        # Remove token_type_ids — DistilBERT doesn't support them
        inputs.pop("token_type_ids", None)

        device = self._pipeline.device
        inputs = {k: v.to(device) for k, v in inputs.items()}

        with torch.no_grad():
            outputs = self._pipeline.model(**inputs)

        probs = torch.nn.functional.softmax(outputs.logits, dim=-1)[0]
        return {LABEL_NAMES[i]: float(probs[i]) for i in range(len(LABEL_NAMES))}