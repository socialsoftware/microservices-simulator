"""Render the paper's reduction figure from the verified full-input counts."""
from pathlib import Path
import json
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

folder = Path(__file__).resolve().parent
rows = sorted((r for r in json.loads((folder / "summary.json").read_text())["rows"]
               if r["inputCap"] == 1000), key=lambda r: r["sagaCount"])
assert [r["sagaCount"] for r in rows] == [2, 3, 4]
plt.rcParams.update({"font.size": 10, "pdf.fonttype": 42,
                     "axes.spines.top": False, "axes.spines.right": False})
fig, axes = plt.subplots(1, 2, figsize=(10.8, 3.7), layout="constrained")
panels = [
    ("Input combinations", "allTuples", [("Without reduction", "allTuples", "#bcc3ca"),
      ("After pruning", "broadTuples", "#4576a9")]),
    ("Normal action orders", "allFullOrders", [("Without reduction", "allFullOrders", "#bcc3ca"),
      ("After pruning", "broadFullOrders", "#4576a9"),
      ("After pruning + compression", "broadCompressed", "#20856a")]),
]
for ax, (title, denominator, series) in zip(axes, panels):
    centres = np.arange(3) * 1.25
    height = .23
    for i, (label, numerator, colour) in enumerate(series):
        values = [100 * int(r[numerator]) / int(r[denominator]) for r in rows]
        y = centres + (i - (len(series) - 1) / 2) * height
        bars = ax.barh(y, values, height=height * .9, color=colour, label=label)
        ax.bar_label(bars, labels=[f"{v:.2f}%" for v in values], padding=3, fontsize=9)
    ax.set_yticks(centres, ["2 Sagas", "3 Sagas", "4 Sagas"])
    ax.invert_yaxis()
    ax.set_xlim(0, 117)
    ax.set_xticks([0, 25, 50, 75, 100])
    ax.set_xlabel("% of the count without reduction")
    ax.set_title(title)
    ax.grid(axis="x", alpha=.15)
    ax.set_axisbelow(True)
    ax.legend(loc="upper center", bbox_to_anchor=(.5, -.24), frameon=False, fontsize=9)
for extension in ("pdf", "png"):
    fig.savefig(folder / f"global-selection-compression-all-inputs.{extension}", dpi=180)
plt.close(fig)
