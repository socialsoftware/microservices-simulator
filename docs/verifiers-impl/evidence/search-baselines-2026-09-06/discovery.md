# Online discovery by attempt budget

| Workload | Strategy | Seed | Budget | First positive | Positive scenarios | COMPLETE | Wall s |
| --- | --- | --- | ---: | --- | ---: | ---: | ---: |
| benchmark | deterministic | None | 12 | 4 | 9 | 12 | 224.34 |
| benchmark | random | 11 | 12 | 2 | 6 | 12 | 226.37 |
| benchmark | random | 29 | 12 | 3 | 6 | 12 | 271.22 |
| benchmark | random | 47 | 12 | 1 | 5 | 12 | 267.67 |
| benchmark | random | 71 | 12 | 1 | 9 | 12 | 297.78 |
| benchmark | random | 101 | 12 | 3 | 6 | 12 | 298.21 |
| create-quiz | deterministic | None | 4 | not found | 0 | 4 | 98.06 |
| create-quiz | random | 11 | 4 | not found | 0 | 4 | 97.29 |
| create-quiz | random | 29 | 4 | not found | 0 | 4 | 97.53 |
| create-quiz | random | 47 | 4 | not found | 0 | 4 | 97.55 |
| create-quiz | random | 71 | 4 | not found | 0 | 4 | 99.02 |
| create-quiz | random | 101 | 4 | not found | 0 | 4 | 96.33 |
| anonymize-event | deterministic | None | 3 | not found | 0 | 3 | 75.85 |
| anonymize-event | random | 11 | 3 | not found | 0 | 3 | 72.98 |
| anonymize-event | random | 29 | 3 | not found | 0 | 3 | 74.83 |
| anonymize-event | random | 47 | 3 | not found | 0 | 3 | 76.00 |
| anonymize-event | random | 71 | 3 | not found | 0 | 3 | 78.00 |
| anonymize-event | random | 101 | 3 | not found | 0 | 3 | 73.83 |

## benchmark

Cumulative distinct positive scenario IDs; every attempt consumes budget.

| Budget | deterministic | random 11 | random 29 | random 47 | random 71 | random 101 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 0 | 0 | 0 | 1 | 1 | 0 |
| 2 | 0 | 1 | 0 | 2 | 1 | 0 |
| 3 | 0 | 2 | 1 | 2 | 2 | 1 |
| 4 | 1 | 3 | 1 | 2 | 3 | 2 |
| 5 | 2 | 4 | 1 | 2 | 4 | 2 |
| 6 | 3 | 5 | 2 | 2 | 5 | 2 |
| 7 | 4 | 5 | 3 | 2 | 6 | 2 |
| 8 | 5 | 5 | 3 | 3 | 6 | 3 |
| 9 | 6 | 5 | 4 | 4 | 6 | 4 |
| 10 | 7 | 5 | 4 | 4 | 7 | 4 |
| 11 | 8 | 6 | 5 | 4 | 8 | 5 |
| 12 | 9 | 6 | 6 | 5 | 9 | 6 |

## create-quiz

Cumulative distinct positive scenario IDs; every attempt consumes budget.

| Budget | deterministic | random 11 | random 29 | random 47 | random 71 | random 101 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 0 | 0 | 0 | 0 | 0 | 0 |
| 2 | 0 | 0 | 0 | 0 | 0 | 0 |
| 3 | 0 | 0 | 0 | 0 | 0 | 0 |
| 4 | 0 | 0 | 0 | 0 | 0 | 0 |

## anonymize-event

Cumulative distinct positive scenario IDs; every attempt consumes budget.

| Budget | deterministic | random 11 | random 29 | random 47 | random 71 | random 101 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 0 | 0 | 0 | 0 | 0 | 0 |
| 2 | 0 | 0 | 0 | 0 | 0 | 0 |
| 3 | 0 | 0 | 0 | 0 | 0 | 0 |
