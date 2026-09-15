# Integral input materialization

Direct route, approved by the user's instruction to investigate and unblock the
156-candidate workload. Scope: diagnose the retained failure, apply a bounded generic
reader fix if supported, add regression coverage and repeat the ordinary runtime control.
Assess a small fault sample only if the control becomes executable. No broad GA campaign,
application business changes, new inputs, paper edits, branches or commits.

The recorded blocker is CreateTournament argument 4 after successful source setup. The
current package reader's integral scalar ternary promotes int and long branches to long
before boxing. The participant materializer then passes a Long to an Integer DTO setter.
The setup runner's typed numeric conversion masks the same representation problem during
setup. Preserve Jackson's exact integral value and runtime type at the package boundary;
leave application DTO setters and the impact policy unchanged. The participant materializer
also binds numeric assignments to the declared integral destination exactly, preserving
small Long destinations and rejecting fractional or overflowing conversions. Include wide-number boundaries to
avoid introducing truncation or converting every value to int.

Validation: reproduce the failure through package roundtrip plus dummyapp DTO assignment;
verify exact integral types and values, run executor/package regressions, then freeze the
changed reader overlay and replay the original Quizzes control with source/package hashes.
