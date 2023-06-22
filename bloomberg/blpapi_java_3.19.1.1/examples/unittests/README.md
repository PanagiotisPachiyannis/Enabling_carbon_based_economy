# BLPAPI Testing Support

`com.bloomberglp.blpapi.test.TestUtil` provides users with the ability to
test their applications offline through the creation of custom events for
their applications.

We are providing in this project
- a simple application [`mktnotifier`](src/main/java/com/bloomberglp/blpapiexamples/unittests/mktnotifier/README.md) complete with tests.
- a simple [`resolver`](src/main/java/com/bloomberglp/blpapiexamples/unittests/snippets/resolver/README.md) implementation complete with tests.
- some [unit tests](src/test/java/com/bloomberglp/blpapiexamples/unittests/snippets/README.md) which demonstrate the construction of some common messages
  and events.

The `main` directory contains a `mktnotifier` directory and a `snippets`
directory. The source code for the `mktnotifier` application is located in
`mktnotifier`. The source code for the `resolver` is located in the `resolver`
subdirectory of the `snippets` directory.

The `test` directory contains the tests for both the `mktnotifier` and the
`resolver`, as well as example unit tests for common messages and events.
