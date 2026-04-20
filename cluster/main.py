from typing import Dict
from collections import defaultdict
import hdbscan
from sentence_transformers import SentenceTransformer
from sklearn.preprocessing import normalize
import ollama
import os
import json
import hashlib
import numpy as np


class TransactionCategorizer:
    def __init__(self):
        self.embedder = SentenceTransformer("all-MiniLM-L6-v2")

    def _get_embeddings(self, descriptions: list[str]) -> np.ndarray:
        key = hashlib.md5("".join(descriptions).encode()).hexdigest()
        cache_path = f"/tmp/{key}.npy"
        if os.path.exists(cache_path):
            return np.load(cache_path)
        embs = self.embedder.encode(descriptions, batch_size=64, show_progress_bar=False)
        np.save(cache_path, embs)
        return embs

    def _label_clusters_batch(self, cluster_samples: dict[int, list[str]]) -> dict[int, str]:
        prompt_parts = ["Label these transaction clusters with ONE category each."]
        prompt_parts.append("Categories: Food & Dining, Shopping, Transport, Bills, Entertainment, Income, Other")
        prompt_parts.append("")

        for cluster_id, samples in cluster_samples.items():
            prompt_parts.append(f"Cluster {cluster_id}:")
            for s in samples[:3]:
                prompt_parts.append(f"  - {s}")
            prompt_parts.append("")

        prompt_parts.append('Return as JSON: {"0": "Category", "1": "Category", ...}')

        response = ollama.generate(
            model="qwen3.5",
            prompt="\n".join(prompt_parts),
            format="json"
        )

        try:
            return json.loads(response["response"].strip())
        except json.JSONDecodeError:
            return {cid: "Other" for cid in cluster_samples}

    def categorize(self, transactions: list[Dict]) -> list[Dict]:
        descriptions = [t["description"] for t in transactions]

        embeddings = self._get_embeddings(descriptions)
        embeddings = normalize(embeddings)  # L2 normalize → euclidean == cosine

        clusterer = hdbscan.HDBSCAN(min_cluster_size=2, metric="euclidean")
        labels = clusterer.fit_predict(embeddings)

        print("Labels:", labels)

        clusters = defaultdict(list)
        for i, label in enumerate(labels):
            clusters[label].append(transactions[i])

        cluster_samples = {
            cid: [t["description"] for t in txns[:5]]
            for cid, txns in clusters.items()
            if cid != -1
        }

        cluster_names = self._label_clusters_batch(cluster_samples)
        cluster_names[-1] = "Uncategorized"

        for i, transaction in enumerate(transactions):
            cluster_id = str(labels[i])
            transaction["category"] = cluster_names.get(cluster_id, "Uncategorized")

        return transactions


if __name__ == "__main__":

    categorizer = TransactionCategorizer()
    categorized = categorizer.categorize(transactions)

    with open("txns.json", "w") as f:
        json.dump(categorized, f, indent=2)

    print(json.dumps(categorized, indent=2))