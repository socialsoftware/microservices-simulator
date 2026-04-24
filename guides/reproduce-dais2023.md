# Reproducing DAIS2023 Paper Tests

These tests represent scenarios outlined in the DAIS2023 paper ("A Domain-Driven Design Simulator for Business
Logic-Rich Microservice Systems").

To reproduce the paper results follow the steps:

* Analyze a figure in the paper, fig3a-d and fig4;
* Read the test case code for the figure, including the final assertions that define the expected behavior;
* Run the test case (see below);
* Read the logger INFO messages, they use UPPERCASE. They identify when a functionality and event processing starts and
  ends and what its version number is.
    * For instance, in test-fig4 both functionalities start with the same version number (they are concurrent), but
      addParticipant finishes with a higher number, because it finishes after updateName. It can be observed in the log
      that an exception was thrown, due to the invariant break.

### Figure 3(a)

* Run:
  ```bash
  docker compose up test-fig3a
  ```

### Figure 3(b)

* Run:
  ```bash
  docker compose up test-fig3b
  ```

### Figure 3(c)

* Run:
  ```bash
  docker compose up test-fig3c
  ```

### Figure 3(d)

* Run:
  ```bash
  docker compose up test-fig3d
  ```

### Figure 4

* Run:
  ```bash
  docker compose up test-fig4
  ```