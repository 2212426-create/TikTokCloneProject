# -*- coding: utf-8 -*-
"""
Recommendation engine phục vụ app TopTop.

Huấn luyện thật trên tương tác TopTop đã ghi log (BCE trên exposure quan sát,
cùng hàm mục tiêu với benchmark):
  - "mf":  MatrixFactorization (src/models/matrix_factorization.py)
  - "cdr": DR-BIAS + CDR (src/causal/cdr/cdr_model.py), propensity = tần suất
           exposure theo item (cùng proxy dùng cho KuaiRand)
  - "popularity": log(1 + watchCount + 5 * totalLikes), dùng cho user mới.
IViDR cần đặc trưng user làm biến công cụ và proxy; dữ liệu TopTop chưa có nên
yêu cầu "ividr" được phục vụ bằng mô hình khả dụng và trả về lý do rõ ràng.
Project: TopTop (agri-behavioral-recsys)
"""

import threading
import time

import numpy as np
import pandas as pd
import torch
import torch.nn.functional as F

from src.causal.cdr.cdr_model import CDRFramework
from src.models.matrix_factorization import MatrixFactorization
from src.utils.helpers import set_seed
from src.api.firebase_bridge import behavior_label

TRAINABLE_MODELS = ("mf", "cdr")
UNSUPPORTED_MODELS = {
    "ividr": "IViDR cần đặc trưng user làm biến công cụ/proxy; dữ liệu TopTop chưa có.",
}


class RecEngine:
    def __init__(self, snapshot_loader, interaction_log=None, models=TRAINABLE_MODELS,
                 embedding_dim=16, epochs=150, lr=0.01, weight_decay=1e-3,
                 seed=42, retrain_every=20, min_interactions=10, device=None):
        unknown = set(models) - set(TRAINABLE_MODELS)
        if unknown:
            raise ValueError(f"Unsupported models: {sorted(unknown)}")
        self.snapshot_loader = snapshot_loader
        self.interaction_log = interaction_log
        self.model_names = tuple(models)
        self.embedding_dim = embedding_dim
        self.epochs = epochs
        self.lr = lr
        self.weight_decay = weight_decay
        self.seed = seed
        self.retrain_every = retrain_every
        self.min_interactions = min_interactions
        self.device = device or torch.device("cpu")
        self._lock = threading.RLock()
        self.watched = {}
        self.pending = []
        self.models = {}
        self.training_stats = {}
        self.refresh()

    # ------------------------------------------------------------------ data
    def refresh(self):
        """Nạp lại dữ liệu Firebase/export và huấn luyện lại toàn bộ mô hình."""
        snapshot = self.snapshot_loader()
        with self._lock:
            self.snapshot = snapshot
            # Tương tác đã ghi vào log được nạp lại từ file; không có log thì giữ trong bộ nhớ.
            if self.interaction_log is not None:
                self.pending = []
            self._fit()

    def _interactions(self):
        frame = self.snapshot.interactions
        if self.pending:
            extra = pd.DataFrame(self.pending, columns=frame.columns)
            frame = (pd.concat([frame, extra], ignore_index=True)
                     .groupby(["user_id", "video_id"], as_index=False)
                     .agg(label=("label", "max"), play_time_ms=("play_time_ms", "max"),
                          is_like=("is_like", "max"), source=("source", "first")))
        return frame

    def _fit(self):
        started = time.time()
        frame = self._interactions()
        self.video_ids = list(self.snapshot.videos)
        self.i2idx = {vid: i for i, vid in enumerate(self.video_ids)}
        self.user_ids = sorted(frame["user_id"].unique().tolist())
        self.u2idx = {uid: i for i, uid in enumerate(self.user_ids)}
        self.popularity = self._popularity(frame)
        self.models = {}
        n = len(frame)
        if n >= self.min_interactions and frame["label"].nunique() > 1:
            u = torch.tensor(frame["user_id"].map(self.u2idx).to_numpy(), dtype=torch.long, device=self.device)
            i = torch.tensor(frame["video_id"].map(self.i2idx).to_numpy(), dtype=torch.long, device=self.device)
            y = torch.tensor(frame["label"].to_numpy(dtype=np.float32), device=self.device)
            for name in self.model_names:
                self.models[name] = getattr(self, f"_train_{name}")(u, i, y)
        self.training_stats = {
            "interactions": n,
            "positives": int(frame["label"].sum()) if n else 0,
            "users_with_history": len(self.user_ids),
            "videos": len(self.video_ids),
            "trained_models": sorted(self.models),
            "train_seconds": round(time.time() - started, 3),
            "trained_at": int(time.time() * 1000),
        }

    def _popularity(self, frame):
        scores = np.array([np.log1p(v.get("watchCount", 0) + 5.0 * v.get("totalLikes", 0))
                           for v in self.snapshot.videos.values()], dtype=np.float64)
        if len(frame):
            pos = frame[frame["label"] > 0]["video_id"].map(self.i2idx).to_numpy()
            scores += np.log1p(np.bincount(pos, minlength=len(scores)))
        return scores

    # -------------------------------------------------------------- training
    def _new_mf(self):
        return MatrixFactorization(len(self.user_ids), len(self.video_ids),
                                   embedding_dim=self.embedding_dim).to(self.device)

    def _train_mf(self, u, i, y):
        set_seed(self.seed)
        model = self._new_mf()
        # L2 chỉ trên embedding, không phạt bias (như src/baselines/train_mf.py).
        optimizer = torch.optim.Adam([
            {"params": [model.user_embedding.weight, model.item_embedding.weight],
             "weight_decay": self.weight_decay},
            {"params": [model.user_bias.weight, model.item_bias.weight, model.global_bias],
             "weight_decay": 0.0},
        ], lr=self.lr)
        model.train()
        for _ in range(self.epochs):
            optimizer.zero_grad()
            loss = F.binary_cross_entropy_with_logits(model(u, i), y)
            loss.backward()
            optimizer.step()
        model.eval()
        return model

    def _train_cdr(self, u, i, y):
        set_seed(self.seed)
        model = CDRFramework(len(self.user_ids), len(self.video_ids),
                             embedding_dim=self.embedding_dim, un_thres=10.0).to(self.device)
        counts = torch.bincount(i, minlength=len(self.video_ids)).float()
        item_prop = torch.clamp(counts / (counts.max() + 1e-6), 0.05, 1.0)
        inv_p = 1.0 / item_prop[i]
        opt_rec = torch.optim.Adam(model.rec_model.parameters(), lr=self.lr, weight_decay=self.weight_decay)
        opt_imp = torch.optim.Adam(model.imp_model.parameters(), lr=self.lr, weight_decay=self.weight_decay)
        gen = torch.Generator(device="cpu").manual_seed(self.seed)
        model.train()
        for _ in range(self.epochs):
            u_un = torch.randint(0, len(self.user_ids), (len(u),), generator=gen).to(self.device)
            i_un = torch.randint(0, len(self.video_ids), (len(u),), generator=gen).to(self.device)
            opt_rec.zero_grad()
            opt_imp.zero_grad()
            loss_rec, loss_imp, _ = model.compute_cdr_losses(u, i, y, inv_p, u_un, i_un, mc_samples=10)
            loss_rec.backward()
            loss_imp.backward()
            opt_rec.step()
            opt_imp.step()
        model.eval()
        return model.rec_model

    # ------------------------------------------------------------- serving
    def resolve_model(self, requested):
        requested = (requested or "mf").lower()
        if requested in self.models:
            return requested, None
        if requested == "popularity":
            return "popularity", None
        reason = UNSUPPORTED_MODELS.get(requested)
        if reason is None and requested in self.model_names:
            reason = f"Chưa đủ dữ liệu để huấn luyện '{requested}' (cần >= {self.min_interactions} tương tác, có cả hai nhãn)."
        elif reason is None:
            reason = f"Mô hình '{requested}' không tồn tại."
        fallback = next((m for m in ("mf", "cdr") if m in self.models), "popularity")
        return fallback, reason

    def _scores(self, model_name, user_idx):
        if model_name == "popularity" or user_idx is None:
            return self.popularity
        with torch.no_grad():
            logits = self.models[model_name].predict_user_all(torch.tensor([user_idx], device=self.device))
        return torch.sigmoid(logits[0]).cpu().numpy().astype(np.float64)

    def recommend(self, user_id, k=10, model="mf", filter_rejected=True, exclude_video_ids=()):
        with self._lock:
            model_used, reason = self.resolve_model(model)
            user_idx = self.u2idx.get(str(user_id))
            cold = user_idx is None
            if cold and model_used != "popularity":
                model_used, reason = "popularity", "User chưa có lịch sử tương tác (cold-start)."
            scores = self._scores(model_used, user_idx)
            excluded = set(map(str, exclude_video_ids)) | self.watched.get(str(user_id), set())

            def eligible(vid, skip_excluded):
                video = self.snapshot.videos[vid]
                if not video.get("videoUri"):
                    return False
                if filter_rejected and str(video.get("moderationStatus", "")).lower() == "rejected":
                    return False
                return not (skip_excluded and vid in excluded)

            candidates = [j for j, vid in enumerate(self.video_ids) if eligible(vid, True)]
            if not candidates:
                # Đã xem hết: cho phép xem lại thay vì trả về rỗng.
                candidates = [j for j, vid in enumerate(self.video_ids) if eligible(vid, False)]
            candidates = np.asarray(candidates, dtype=np.int64)
            if len(candidates) == 0:
                return [], model_used, cold, reason
            order = candidates[np.lexsort((candidates, -scores[candidates]))][:k]
            results = []
            for rank, j in enumerate(order, 1):
                video = dict(self.snapshot.videos[self.video_ids[j]])
                video.pop("_doc_id", None)
                video.update(rank=rank, score=round(float(scores[j]), 6),
                             video_id=video["videoId"], model=model_used)
                results.append(video)
            return results, model_used, cold, reason

    def log_interaction(self, user_id, video_id, play_time_ms, is_like):
        user_id, video_id = str(user_id), str(video_id)
        label = behavior_label(is_like, play_time_ms)
        if self.interaction_log is not None:
            self.interaction_log.append(user_id, video_id, play_time_ms, is_like)
        retrained = False
        with self._lock:
            self.watched.setdefault(user_id, set()).add(video_id)
            known = video_id in self.i2idx
            if known:
                self.pending.append({"user_id": user_id, "video_id": video_id, "label": label,
                                     "play_time_ms": int(play_time_ms), "is_like": bool(is_like),
                                     "source": "api_live"})
                if self.retrain_every and len(self.pending) % self.retrain_every == 0:
                    self._fit()
                    retrained = True
        return {"status": "recorded", "user_id": user_id, "video_id": video_id,
                "is_positive_interaction": bool(label), "known_video": known, "retrained": retrained}

    def health(self):
        with self._lock:
            return {"status": "ok", "data_source": self.snapshot.source,
                    "models_available": sorted(self.models) + ["popularity"],
                    "device": str(self.device), **self.training_stats}
