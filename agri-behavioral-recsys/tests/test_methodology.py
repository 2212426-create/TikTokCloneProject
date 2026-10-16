"""Biến công cụ / proxy KuaiRand, đầu vào iDCF, cấu hình tác giả và kiểm định theo cặp user."""
import os
import tempfile
import unittest
from unittest.mock import patch

import numpy as np
import pandas as pd
from scipy import sparse

from src.data_pipeline.coat_dataset import load_coat_raw_features
from src.data_pipeline.kuairand_dataset import load_kuairand_author_proxy, load_kuairand_tab_instruments
from src.evaluation.diagnostics import paired_ndcg_difference


class PairedTestTests(unittest.TestCase):
    def test_identical_models_are_not_significant(self):
        m = {"user_ids": [0, 1, 2], "user_ndcg": [0.1, 0.5, 0.9]}
        result = paired_ndcg_difference(m, m)
        self.assertEqual((result["mean_delta_ndcg"], result["wilcoxon_p"], result["users_changed"]), (0.0, 1.0, 0))

    def test_consistent_gain_is_significant(self):
        rng = np.random.default_rng(0)
        base = rng.uniform(0, 0.8, 200)
        model = {"user_ids": list(range(200)), "user_ndcg": list(base + 0.1)}
        result = paired_ndcg_difference(model, {"user_ids": list(range(200)), "user_ndcg": list(base)})
        self.assertAlmostEqual(result["mean_delta_ndcg"], 0.1)
        self.assertLess(result["wilcoxon_p"], 1e-10)
        self.assertEqual((result["wins"], result["losses"]), (200, 0))

    def test_user_order_must_match(self):
        with self.assertRaises(ValueError):
            paired_ndcg_difference({"user_ids": [0, 1], "user_ndcg": [0, 1]},
                                   {"user_ids": [1, 0], "user_ndcg": [0, 1]})


class KuaiRandTabInstrumentTests(unittest.TestCase):
    def make_root(self, root):
        logs = pd.DataFrame({"user_id": [10, 10, 10, 20, 20, 99], "video_id": [1, 2, 1, 1, 2, 1],
                             "tab": [0, 1, 1, 4, 4, 0]})
        logs.iloc[:3].to_csv(os.path.join(root, "log_standard_4_08_to_4_21_pure.csv"), index=False)
        logs.iloc[3:].to_csv(os.path.join(root, "log_standard_4_22_to_5_08_pure.csv"), index=False)
        pd.DataFrame({"user_id": [10, 20], "user_active_degree": ["high", "low"],
                      "register_days_range": ["a", "b"]}).to_csv(
            os.path.join(root, "user_features_pure.csv"), index=False)

    def test_tab_shares_use_only_mapped_train_rows(self):
        with tempfile.TemporaryDirectory() as root:
            self.make_root(root)
            z, tabs = load_kuairand_tab_instruments({10: 0, 20: 1}, {1: 0, 2: 1}, data_dir=root)
        # User 99 không thuộc train đã lọc nên tab 0 chỉ đến từ user 10.
        self.assertEqual(tabs, [0, 1, 4])
        self.assertEqual(z.shape, (2, 3))
        self.assertGreater(z[0, 1], z[1, 1])  # user 10 xem tab 1 nhiều hơn
        self.assertGreater(z[1, 2], z[0, 2])  # user 20 chỉ xem tab 4


def author_user_features(user_ids):
    n = len(user_ids)
    return pd.DataFrame({
        "user_id": user_ids, "user_active_degree": ["high", "low", "high"][:n],
        "is_lowactive_period": [0, 1, 0][:n], "is_live_streamer": [-124, 1, -124][:n],
        "is_video_author": [0, 1, 1][:n], "follow_user_num": [1, 2, 3][:n],
        "follow_user_num_range": ["0", "1-10", "0"][:n], "fans_user_num": [0, 0, 0][:n],
        "fans_user_num_range": ["0", "0", "0"][:n], "friend_user_num": [0, 0, 0][:n],
        "friend_user_num_range": ["0", "0", "0"][:n], "register_days": [5, 6, 7][:n],
        "register_days_range": ["a", "b", "a"][:n], "onehot_feat3": [1, 2, 3][:n],
        "onehot_feat4": [1.0, None, 2.0][:n]})


class KuaiRandIDCFInputTests(unittest.TestCase):
    def test_exposure_keeps_non_clicks_and_sparse_ivae_matches_dense(self):
        from src.causal.idcf.ivae import train_exposure_ivae
        from src.causal.idcf.train_idcf import kuairand_inputs
        with tempfile.TemporaryDirectory() as root:
            KuaiRandTabInstrumentTests().make_root(root)
            author_user_features([10, 20]).to_csv(os.path.join(root, "user_features_pure.csv"), index=False)
            data = {"train_u": np.array([0, 0, 1, 1]), "train_i": np.array([0, 1, 0, 1]),
                    "train_y": np.array([1, 0, 0, 0], dtype=np.float32), "n_users": 2, "n_items": 2,
                    "user_mapping": {10: 0, 20: 1}, "video_mapping": {1: 0, 2: 1}, "data_dir": root}
            inputs = kuairand_inputs(data)
        self.assertTrue(sparse.issparse(inputs["exposure"]))
        np.testing.assert_array_equal(inputs["exposure"].toarray() > 0, np.ones((2, 2), dtype=bool))
        self.assertEqual(inputs["instrument"].shape, (2, 3))
        self.assertEqual(inputs["proxy"].shape[0], 2)
        rng = np.random.default_rng(0)
        dense = (rng.random((20, 8)) < 0.3).astype(np.float32)
        w = np.eye(4, dtype=np.float32)[rng.integers(0, 4, 20)]
        kw = dict(latent_dim=3, hidden_dim=8, epochs=3, patience=3, batch_size=8, seed=1)
        m_dense, s_dense, _ = train_exposure_ivae(dense, w, **kw)
        m_sparse, s_sparse, _ = train_exposure_ivae(sparse.csr_matrix(dense), w, **kw)
        np.testing.assert_allclose(m_dense.numpy(), m_sparse.numpy(), atol=1e-6)
        np.testing.assert_allclose(s_dense.numpy(), s_sparse.numpy(), atol=1e-6)


class AuthorProxyTests(unittest.TestCase):
    def test_author_proxy_is_one_hot_with_nan_category(self):
        with tempfile.TemporaryDirectory() as root:
            author_user_features([10, 20, 30]).to_csv(os.path.join(root, "user_features_pure.csv"), index=False)
            w = load_kuairand_author_proxy({10: 2, 20: 0, 30: 1}, data_dir=root)
        # 8 cột còn lại sau khi bỏ cột thô, is_lowactive_period và onehot_feat3; mỗi user đúng một 1 mỗi cột.
        np.testing.assert_array_equal(w.sum(1), [8, 8, 8])
        self.assertTrue(set(np.unique(w)) <= {0.0, 1.0})
        self.assertFalse(np.array_equal(w[0], w[2]))


class AuthorConfigTests(unittest.TestCase):
    def test_benchmark_mf_uses_author_mse_configuration(self):
        from experiments.workflows import run_all_benchmarks as bench
        calls = []

        def fake_mf(**kwargs):
            calls.append(kwargs)
            raise RuntimeError("stop after capturing config")
        try:
            load_coat_raw_features()
        except FileNotFoundError:
            self.skipTest("Coat raw data unavailable")
        with patch.object(bench, "train_baseline_mf", fake_mf), self.assertRaises(RuntimeError):
            bench.run_all_benchmarks(dataset_filter="coat", model_filter="mf")
        cfg = calls[0]
        self.assertEqual((cfg["loss_name"], cfg["decay_scope"], cfg["scheduler_name"], cfg["train_global_bias"]),
                         ("mse", "all", "none", False))
        self.assertEqual((cfg["lr"], cfg["weight_decay"]), (5e-4, 1e-6))


class CdrAuthorProtocolTests(unittest.TestCase):
    def test_author_metrics_score_zero_positive_users_as_one_and_bound_recall(self):
        from experiments.workflows.cdr_author_protocol import author_metrics
        x = np.array([[0, i] for i in range(7)] + [[1, 0], [1, 1]])
        y = np.array([1, 1, 1, 1, 1, 1, 0, 0, 0], dtype=np.float32)
        scores = np.array([7, 6, 5, 4, 3, 2, 1, 1, 0], dtype=np.float32)
        ndcg, recall = author_metrics(scores, x, y)
        # user 0: top-5 toàn dương (NDCG 1, Recall 5/min(5,6)=1); user 1 không có dương -> 1.
        self.assertEqual((ndcg, recall), (1.0, 1.0))

    def test_naive_bayes_propensity_depends_only_on_label(self):
        from experiments.workflows.cdr_author_protocol import naive_bayes_inverse_propensity
        x = np.array([[0, 0], [1, 1], [2, 2], [3, 3]])
        w = naive_bayes_inverse_propensity(x, np.array([1., 0., 1., 0.]), np.array([1., 0., 0., 0.]))
        self.assertEqual(w[0], w[2])
        self.assertEqual(w[1], w[3])
        self.assertNotEqual(w[0], w[1])


if __name__ == "__main__":
    unittest.main()
